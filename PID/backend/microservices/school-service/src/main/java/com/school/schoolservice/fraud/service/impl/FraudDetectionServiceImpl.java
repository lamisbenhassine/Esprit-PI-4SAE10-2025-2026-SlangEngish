package com.school.schoolservice.fraud.service.impl;

import com.school.schoolservice.application.entity.Application;
import com.school.schoolservice.fraud.dto.FraudResultDto;
import com.school.schoolservice.fraud.entity.FraudScore;
import com.school.schoolservice.fraud.repository.FraudScoreRepository;
import com.school.schoolservice.fraud.service.FraudDetectionService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionServiceImpl implements FraudDetectionService {

    private final FraudScoreRepository repository;

    // ✅ Poids de chaque critère
    private static final double WEIGHT_SPEED     = 0.30;
    private static final double WEIGHT_EMAIL     = 0.25;
    private static final double WEIGHT_DUPLICATE = 0.25;
    private static final double WEIGHT_VOLUME    = 0.20;

    // ✅ Seuils
    private static final double SUSPICIOUS_THRESHOLD = 0.50;
    private static final double BLOCKED_THRESHOLD    = 0.75;

    // ✅ Emails suspects
    private static final List<String> SUSPICIOUS_DOMAINS = List.of(
            "test.com", "fake.com", "temp.com", "trash.com",
            "mailinator.com", "yopmail.com", "guerrillamail.com"
    );

    private static final Pattern SUSPICIOUS_EMAIL_PATTERN =
            Pattern.compile("^(test|fake|spam|dummy|aaa|bbb|xxx|admin123).*@.*");

    @Override
    public FraudResultDto analyze(Application application) {
        List<String> reasons = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // ✅ 1. Score Vitesse — candidatures rapides
        double speedScore = calculateSpeedScore(
                application.getStudentId(), now, reasons);

        // ✅ 2. Score Email — email suspect
        double emailScore = calculateEmailScore(
                application.getApplicantEmail(), reasons);

        // ✅ 3. Score Doublon — même offre déjà postulée
        double duplicateScore = calculateDuplicateScore(
                application.getStudentId(), application.getJobOfferId(), reasons);

        // ✅ 4. Score Volume — trop de candidatures aujourd'hui
        double volumeScore = calculateVolumeScore(
                application.getStudentId(), now, reasons);

        // ✅ Score total pondéré
        double totalScore = (speedScore   * WEIGHT_SPEED)
                + (emailScore   * WEIGHT_EMAIL)
                + (duplicateScore * WEIGHT_DUPLICATE)
                + (volumeScore  * WEIGHT_VOLUME);

        totalScore = Math.min(totalScore, 1.0);

        // ✅ Niveau de fraude
        String fraudLevel;
        if (totalScore >= BLOCKED_THRESHOLD) {
            fraudLevel = "BLOCKED";
        } else if (totalScore >= SUSPICIOUS_THRESHOLD) {
            fraudLevel = "SUSPICIOUS";
        } else {
            fraudLevel = "CLEAN";
        }

        // ✅ Sauvegarde en base
        FraudScore fraudScore = FraudScore.builder()
                .applicationId(application.getId())
                .studentId(application.getStudentId())
                .jobOfferId(application.getJobOfferId())
                .applicantEmail(application.getApplicantEmail())
                .speedScore(Math.round(speedScore     * 100.0) / 100.0)
                .emailScore(Math.round(emailScore     * 100.0) / 100.0)
                .duplicateScore(Math.round(duplicateScore * 100.0) / 100.0)
                .volumeScore(Math.round(volumeScore   * 100.0) / 100.0)
                .totalScore(Math.round(totalScore     * 100.0) / 100.0)
                .fraudLevel(fraudLevel)
                .reasons(String.join("|", reasons))
                .detectedAt(now)
                .build();

        repository.save(fraudScore);

        log.info("🔍 Fraude analysée — studentId={} level={} score={}",
                application.getStudentId(), fraudLevel,
                Math.round(totalScore * 100) + "%");

        return FraudResultDto.builder()
                .applicationId(application.getId())
                .studentId(application.getStudentId())
                .jobOfferId(application.getJobOfferId())
                .applicantEmail(application.getApplicantEmail())
                .speedScore(speedScore)
                .emailScore(emailScore)
                .duplicateScore(duplicateScore)
                .volumeScore(volumeScore)
                .totalScore(totalScore)
                .fraudLevel(fraudLevel)
                .reasons(reasons)
                .detectedAt(now)
                .build();
    }

    @Override
    public List<FraudResultDto> getAllSuspicious() {
        return repository.findByFraudLevelIn(List.of("SUSPICIOUS", "BLOCKED"))
                .stream()
                .map(f -> FraudResultDto.builder()
                        .applicationId(f.getApplicationId())
                        .studentId(f.getStudentId())
                        .jobOfferId(f.getJobOfferId())
                        .applicantEmail(f.getApplicantEmail())
                        .speedScore(f.getSpeedScore())
                        .emailScore(f.getEmailScore())
                        .duplicateScore(f.getDuplicateScore())
                        .volumeScore(f.getVolumeScore())
                        .totalScore(f.getTotalScore())
                        .fraudLevel(f.getFraudLevel())
                        .reasons(f.getReasons() != null
                                ? List.of(f.getReasons().split("\\|"))
                                : List.of())
                        .detectedAt(f.getDetectedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ─── Calculs ────────────────────────────────

    private double calculateSpeedScore(Long studentId,
                                       LocalDateTime now, List<String> reasons) {
        // Candidatures dans la dernière minute
        LocalDateTime oneMinuteAgo = now.minusMinutes(1);
        long recentCount = repository.countRecentByStudentId(studentId, oneMinuteAgo);

        if (recentCount >= 5) {
            reasons.add("🚨 " + recentCount + " candidatures en moins d'1 minute");
            return 1.0;
        } else if (recentCount >= 3) {
            reasons.add("⚠️ " + recentCount + " candidatures en moins d'1 minute");
            return 0.7;
        } else if (recentCount >= 2) {
            reasons.add("⚠️ Candidatures rapides détectées");
            return 0.4;
        }
        return 0.0;
    }

    private double calculateEmailScore(String email, List<String> reasons) {
        if (email == null || email.isEmpty()) {
            reasons.add("⚠️ Email manquant");
            return 0.6;
        }

        String emailLower = email.toLowerCase().trim();

        // Domaine suspect
        for (String domain : SUSPICIOUS_DOMAINS) {
            if (emailLower.endsWith("@" + domain)) {
                reasons.add("🚨 Domaine email suspect : " + domain);
                return 1.0;
            }
        }

        // Pattern suspect
        if (SUSPICIOUS_EMAIL_PATTERN.matcher(emailLower).matches()) {
            reasons.add("⚠️ Email suspect : " + email);
            return 0.8;
        }

        // Email trop court ou invalide
        if (!email.contains("@") || email.length() < 6) {
            reasons.add("⚠️ Format email invalide");
            return 0.9;
        }

        return 0.0;
    }

    private double calculateDuplicateScore(Long studentId,
                                           Long jobOfferId, List<String> reasons) {
        long duplicates = repository.countDuplicates(studentId, jobOfferId);

        if (duplicates >= 2) {
            reasons.add("🚨 Candidature en double détectée (" + duplicates + "x)");
            return 1.0;
        } else if (duplicates == 1) {
            reasons.add("⚠️ Déjà candidaté à cette offre");
            return 0.8;
        }
        return 0.0;
    }

    private double calculateVolumeScore(Long studentId,
                                        LocalDateTime now, List<String> reasons) {
        LocalDateTime startOfDay = LocalDateTime.of(
                LocalDate.now(), LocalTime.MIDNIGHT);
        long todayCount = repository.countTodayByStudentId(studentId, startOfDay);

        if (todayCount >= 20) {
            reasons.add("🚨 " + todayCount + " candidatures aujourd'hui");
            return 1.0;
        } else if (todayCount >= 10) {
            reasons.add("⚠️ " + todayCount + " candidatures aujourd'hui");
            return 0.6;
        } else if (todayCount >= 5) {
            reasons.add("ℹ️ " + todayCount + " candidatures aujourd'hui");
            return 0.3;
        }
        return 0.0;
    }

    @Override
    public List<FraudResultDto> getAllFraud() {
        return repository.findAll()
                .stream()
                .map(f -> FraudResultDto.builder()
                        .applicationId(f.getApplicationId())
                        .studentId(f.getStudentId())
                        .jobOfferId(f.getJobOfferId())
                        .applicantEmail(f.getApplicantEmail())
                        .speedScore(f.getSpeedScore())
                        .emailScore(f.getEmailScore())
                        .duplicateScore(f.getDuplicateScore())
                        .volumeScore(f.getVolumeScore())
                        .totalScore(f.getTotalScore())
                        .fraudLevel(f.getFraudLevel())
                        .reasons(f.getReasons() != null
                                ? List.of(f.getReasons().split("\\|"))
                                : List.of())
                        .detectedAt(f.getDetectedAt())
                        .build())
                .sorted((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()))
                .collect(Collectors.toList());
    }

}