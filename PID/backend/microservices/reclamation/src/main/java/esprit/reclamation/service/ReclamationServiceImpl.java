package esprit.reclamation.service;

import esprit.reclamation.dto.ReclamationAdminPageDto;
import esprit.reclamation.entity.Reclamation;
import esprit.reclamation.entity.StudentReclamationBlock;
import esprit.reclamation.repository.ReclamationRepository;
import esprit.reclamation.repository.StudentReclamationBlockRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ReclamationServiceImpl implements ReclamationService {

    private final ReclamationRepository reclamationRepository;
    private final StudentReclamationBlockRepository studentReclamationBlockRepository;
    private final ReclamationSentimentAnalyzer sentimentAnalyzer;
    private final List<String> badWords;
    private final List<Pattern> badWordPatterns;

    public ReclamationServiceImpl(
            ReclamationRepository reclamationRepository,
            StudentReclamationBlockRepository studentReclamationBlockRepository,
            ReclamationSentimentAnalyzer sentimentAnalyzer,
            @Value("${app.moderation.bad-words:insulte,idiot,stupid,fuck,shit}") String badWordsConfig
    ) {
        this.reclamationRepository = reclamationRepository;
        this.studentReclamationBlockRepository = studentReclamationBlockRepository;
        this.sentimentAnalyzer = sentimentAnalyzer;
        this.badWords = Arrays.stream(badWordsConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());
        this.badWordPatterns = this.badWords.stream()
                .map(word -> Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE))
                .collect(Collectors.toList());
    }

    @Override
    public Reclamation create(Reclamation reclamation) {
        if (reclamation.getStudentId() == null) {
            throw new RuntimeException("studentId est obligatoire");
        }
        reclamation.setUrgencyLevel(null);
        reclamation.setEmotionTags(null);
        reclamation.setPriorityScore(null);
        reclamation.setSentimentLabel(null);
        enforceStudentCooldownIfReported(reclamation.getStudentId());

        String rawSujet = reclamation.getSujet();
        String rawDesc = reclamation.getDescription();

        applySentiment(reclamation, rawSujet, rawDesc);
        ModerationResult moderation = moderateText(rawSujet, rawDesc);
        reclamation.setSujet(moderation.sanitizedSujet);
        reclamation.setDescription(moderation.sanitizedDescription);
        reclamation.setContainsBadWords(moderation.containsBadWords);
        return reclamationRepository.save(reclamation);
    }

    @Override
    public ReclamationAdminPageDto getAdminPage(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<Reclamation> result = reclamationRepository.findAllOrderByPriorityDesc(pageable);
        long pending = reclamationRepository.countByStatut("EN_ATTENTE");
        long inProgress = reclamationRepository.countInProgressStatuses();
        long processed = reclamationRepository.countByStatut("RESOLUE");
        return new ReclamationAdminPageDto(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize(),
                pending,
                inProgress,
                processed
        );
    }

    @Override
    public List<Reclamation> getByStudentId(Long studentId) {
        return reclamationRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
    }

    @Override
    public List<Reclamation> getUnreadNotifications(Long studentId) {
        return reclamationRepository.findByStudentIdAndNotificationReadFalseAndReponseAdminIsNotNullOrderByCreatedAtDesc(studentId);
    }

    @Override
    public Reclamation getById(Long id) {
        return reclamationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reclamation introuvable avec l'id : " + id));
    }

    @Override
    public Reclamation update(Long id, Reclamation reclamation) {
        Reclamation existing = getById(id);
        String rawSujet = reclamation.getSujet();
        String rawDesc = reclamation.getDescription();
        applySentiment(existing, rawSujet, rawDesc);
        ModerationResult moderation = moderateText(rawSujet, rawDesc);
        existing.setSujet(moderation.sanitizedSujet);
        existing.setDescription(moderation.sanitizedDescription);
        existing.setContainsBadWords(moderation.containsBadWords);
        if (reclamation.getStudentId() != null) {
            existing.setStudentId(reclamation.getStudentId());
        }
        return reclamationRepository.save(existing);
    }

    @Override
    public Reclamation traiterParAdmin(Long id, String statut, String reponseAdmin) {
        Reclamation existing = getById(id);
        if (statut == null || statut.isBlank()) {
            throw new RuntimeException("Le statut est obligatoire");
        }
        if (reponseAdmin == null || reponseAdmin.isBlank()) {
            throw new RuntimeException("La reponse admin est obligatoire");
        }
        existing.setStatut(statut);
        existing.setReponseAdmin(reponseAdmin.trim());
        existing.setNotificationRead(false);
        return reclamationRepository.save(existing);
    }

    @Override
    public Reclamation markNotificationAsRead(Long id) {
        Reclamation existing = getById(id);
        existing.setNotificationRead(true);
        return reclamationRepository.save(existing);
    }

    @Override
    public Reclamation reportStudent(Long id, String reportReason) {
        Reclamation existing = getById(id);
        if (reportReason == null || reportReason.isBlank()) {
            throw new RuntimeException("Report reason is required");
        }
        existing.setStudentReported(true);
        existing.setReportReason(reportReason.trim());
        existing.setReportedAt(LocalDateTime.now());
        Reclamation saved = reclamationRepository.save(existing);

        LocalDateTime blockedUntil = LocalDateTime.now().plusDays(3);
        StudentReclamationBlock block = studentReclamationBlockRepository.findByStudentId(existing.getStudentId())
                .orElseGet(StudentReclamationBlock::new);
        block.setStudentId(existing.getStudentId());
        block.setBlockedUntil(blockedUntil);
        studentReclamationBlockRepository.save(block);

        return saved;
    }

    @Override
    public Reclamation unblockStudent(Long id) {
        Reclamation existing = getById(id);
        Long studentId = existing.getStudentId();

        studentReclamationBlockRepository.findByStudentId(studentId)
                .ifPresent(studentReclamationBlockRepository::delete);

        // Also clear report flags on all reported reclamations for this student
        List<Reclamation> studentReclamations = reclamationRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
        for (Reclamation rec : studentReclamations) {
            if (Boolean.TRUE.equals(rec.getStudentReported())) {
                rec.setStudentReported(false);
                rec.setReportReason(null);
                rec.setReportedAt(null);
                reclamationRepository.save(rec);
            }
        }

        return getById(id);
    }

    @Override
    public void delete(Long id) {
        Reclamation existing = getById(id);
        reclamationRepository.delete(existing);
    }

    private ModerationResult moderateText(String sujet, String description) {
        String sanitizedSujet = safe(sujet);
        String sanitizedDescription = safe(description);
        boolean containsBadWords = false;

        for (Pattern pattern : badWordPatterns) {
            if (pattern.matcher(sanitizedSujet).find() || pattern.matcher(sanitizedDescription).find()) {
                containsBadWords = true;
            }
            sanitizedSujet = pattern.matcher(sanitizedSujet).replaceAll("*****");
            sanitizedDescription = pattern.matcher(sanitizedDescription).replaceAll("*****");
        }

        return new ModerationResult(sanitizedSujet, sanitizedDescription, containsBadWords);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void applySentiment(Reclamation target, String sujet, String description) {
        ReclamationSentimentAnalyzer.SentimentSnapshot snap = sentimentAnalyzer.analyze(sujet, description);
        target.setUrgencyLevel(snap.getUrgencyLevel());
        target.setEmotionTags(snap.getEmotionTagsCsv());
        target.setPriorityScore(snap.getPriorityScore());
        target.setSentimentLabel(snap.getSentimentLabel());
    }

    private void enforceStudentCooldownIfReported(Long studentId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusDays(3);

        // Fallback safety: if any report exists in last 3 days, block too.
        boolean reportedInLast3Days = reclamationRepository
                .existsByStudentIdAndStudentReportedTrueAndReportedAtAfter(studentId, threshold);
        if (reportedInLast3Days) {
            throw new RuntimeException("You were reported because of your language and cannot create a new reclamation for 3 days. Please consult the administration.");
        }

        studentReclamationBlockRepository.findByStudentId(studentId)
                .ifPresent(block -> {
                    LocalDateTime blockedUntil = block.getBlockedUntil();
                    if (blockedUntil != null && blockedUntil.isAfter(now)) {
                        throw new RuntimeException("You were reported because of your language and cannot create a new reclamation for 3 days. Please consult the administration. Blocked until: " + blockedUntil);
                    }
                });
    }

    private static class ModerationResult {
        private final String sanitizedSujet;
        private final String sanitizedDescription;
        private final boolean containsBadWords;

        private ModerationResult(String sanitizedSujet, String sanitizedDescription, boolean containsBadWords) {
            this.sanitizedSujet = sanitizedSujet;
            this.sanitizedDescription = sanitizedDescription;
            this.containsBadWords = containsBadWords;
        }
    }
}
