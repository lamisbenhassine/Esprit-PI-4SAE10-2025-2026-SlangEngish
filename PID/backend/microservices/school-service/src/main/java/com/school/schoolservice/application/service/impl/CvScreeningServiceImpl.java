package com.school.schoolservice.application.service.impl;

import com.school.schoolservice.application.dto.CvScreeningRequestDto;
import com.school.schoolservice.application.dto.CvScreeningResultDto;
import com.school.schoolservice.application.entity.Application;
import com.school.schoolservice.application.repository.ApplicationRepository;
import com.school.schoolservice.application.service.CvScreeningService;
import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CvScreeningServiceImpl implements CvScreeningService {

    private final ApplicationRepository applicationRepository;

    @Override
    public List<CvScreeningResultDto> screenCVs(CvScreeningRequestDto request) {
        System.out.println("🔍 CV Screening pour offre: " + request.getJobOfferId());

        List<Application> applications = request.getJobOfferId() != null
                ? applicationRepository.findByJobOfferId(request.getJobOfferId())
                : applicationRepository.findAll();

        System.out.println("📋 " + applications.size() + " candidatures à analyser");

        List<CvScreeningResultDto> results = applications.stream()
                .filter(app -> app.getCvUrl() != null && !app.getCvUrl().isEmpty())
                .map(app -> analyzeApplication(app, request))
                .sorted((a, b) -> Integer.compare(b.getOverallScore(), a.getOverallScore()))
                .limit(request.getTopN() > 0 ? request.getTopN() : 5)
                .collect(Collectors.toList());

        AtomicInteger rank = new AtomicInteger(1);
        results.forEach(r -> r.setRank(rank.getAndIncrement()));

        System.out.println("✅ Top " + results.size() + " candidats trouvés");
        return results;
    }

    private CvScreeningResultDto analyzeApplication(
            Application app, CvScreeningRequestDto request) {

        String cvText = extractTextFromPdf(app.getCvUrl());
        String cvLower = cvText.toLowerCase();

        System.out.println("📊 " + app.getApplicantName()
                + " → " + cvText.length() + " chars extraits");

        // ✅ Vérifie si PDF lisible
        boolean pdfReadable = cvText.length() > 50;

        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();
        double skillsScore;
        double locationScore;
        double experienceScore;
        String detectedLocation = null;
        int detectedExperienceYears = 0;

        if (pdfReadable) {
            // ✅ Analyse normale
            if (request.getRequiredSkills() != null) {
                for (String skill : request.getRequiredSkills()) {
                    if (cvLower.contains(skill.toLowerCase().trim())) {
                        matchedSkills.add(skill);
                    } else {
                        missingSkills.add(skill);
                    }
                }
            }

            skillsScore = request.getRequiredSkills() != null
                    && !request.getRequiredSkills().isEmpty()
                    ? (double) matchedSkills.size() / request.getRequiredSkills().size()
                    : 0.5;

            detectedLocation = detectLocation(cvLower);
            locationScore = calculateLocationScore(
                    detectedLocation, request.getPreferredLocation());

            detectedExperienceYears = detectExperienceYears(cvLower);
            experienceScore = calculateExperienceScore(
                    detectedExperienceYears, request.getMinExperienceYears());

            System.out.println("   Skills matchés: " + matchedSkills);
            System.out.println("   Location: " + detectedLocation);
            System.out.println("   Expérience: " + detectedExperienceYears + " ans");

        } else {
            // ✅ PDF non lisible → score bas différencié
            System.out.println("⚠️ PDF non lisible pour : " + app.getApplicantName());
            skillsScore = 0.2;
            locationScore = 0.2;
            experienceScore = 0.2;
            if (request.getRequiredSkills() != null) {
                missingSkills.addAll(request.getRequiredSkills());
            }
        }

        // ✅ Score global pondéré
        double overallScore = (skillsScore * 0.50)
                + (locationScore * 0.30)
                + (experienceScore * 0.20);

        int overallPercent = (int) Math.round(overallScore * 100);

        String matchLevel;
        if (overallPercent >= 75)      matchLevel = "Excellent";
        else if (overallPercent >= 50) matchLevel = "Good";
        else if (overallPercent >= 30) matchLevel = "Average";
        else                           matchLevel = "Low";

        System.out.println("👤 " + app.getApplicantName()
                + " → Score: " + overallPercent + "% ("
                + matchLevel + ") | PDF: "
                + (pdfReadable ? "✅ lisible" : "⚠️ non lisible"));

        return CvScreeningResultDto.builder()
                .applicationId(app.getId())
                .applicantName(app.getApplicantName())
                .applicantEmail(app.getApplicantEmail())
                .cvUrl(app.getCvUrl())
                .overallScore(overallPercent)
                .matchLevel(matchLevel)
                .matchedSkills(matchedSkills)
                .missingSkills(missingSkills)
                .detectedLocation(detectedLocation)
                .detectedExperienceYears(detectedExperienceYears)
                .skillsScore(Math.round(skillsScore * 100.0) / 100.0)
                .locationScore(Math.round(locationScore * 100.0) / 100.0)
                .experienceScore(Math.round(experienceScore * 100.0) / 100.0)
                .rank(0)
                .build();
    }

    // ─── PDF Extraction ───────────────────────────────────────────────────────

    private String extractTextFromPdf(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) return "";
        try {
            byte[] bytes;
            System.out.println("🔍 Lecture PDF : " + fileUrl);

            if (fileUrl.startsWith("http")) {
                // ✅ Essaie d'abord en local depuis l'URL
                String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1)
                        .trim()
                        .replace("\r", "")
                        .replace("\n", "")
                        .replace("%0A", "");
                fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);

                File localFile = new File("uploads/" + fileName);
                System.out.println("📂 Chemin local : " + localFile.getAbsolutePath());

                if (localFile.exists()) {
                    System.out.println("✅ Fichier local trouvé !");
                    bytes = Files.readAllBytes(localFile.toPath());
                } else {
                    // ✅ Sinon via HTTP
                    System.out.println("🌐 Tentative HTTP...");
                    String safeUrl = fileUrl.trim().replace(" ", "%20");
                    bytes = new URL(safeUrl).openStream().readAllBytes();
                }
            } else {
                // ✅ Chemin local direct
                File file = new File(fileUrl.trim());
                if (!file.exists()) {
                    String fileName = fileUrl.replaceAll(".*[/\\\\]", "").trim();
                    file = new File("uploads/" + fileName);
                }
                if (!file.exists()) {
                    System.out.println("❌ Fichier non trouvé : " + fileUrl);
                    return "";
                }
                System.out.println("✅ Chargement local : " + file.getAbsolutePath());
                bytes = Files.readAllBytes(file.toPath());
            }

            try (PDDocument document = Loader.loadPDF(bytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                System.out.println("✅ PDF extrait : " + text.length() + " chars");
                if (text.length() > 0) {
                    System.out.println("📝 Aperçu : "
                            + text.substring(0, Math.min(150, text.length()))
                            .replaceAll("[\\r\\n]+", " "));
                }
                return text;
            }
        } catch (Exception e) {
            System.out.println("❌ Erreur PDF : " + e.getMessage());
            log.error("PDF error: {}", e.getMessage());
            return "";
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String detectLocation(String cvLower) {
        List<String> cities = List.of(
                "tunis", "sfax", "sousse", "monastir", "bizerte",
                "nabeul", "ariana", "marsa", "carthage", "gabes",
                "manouba", "ben arous", "paris", "lyon",
                "marseille", "remote", "télétravail"
        );
        return cities.stream()
                .filter(cvLower::contains)
                .findFirst()
                .orElse(null);
    }

    private double calculateLocationScore(String detected, String preferred) {
        if (preferred == null || preferred.isEmpty()) return 0.5;
        if (detected == null) return 0.2;
        if (detected.equalsIgnoreCase(preferred.trim())) return 1.0;
        if (detected.contains(preferred.toLowerCase().trim())
                || preferred.toLowerCase().contains(detected)) return 0.7;
        return 0.2;
    }

    private int detectExperienceYears(String cvLower) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(\\d+)\\s*(?:ans?|years?|an|yr)");
        java.util.regex.Matcher matcher = pattern.matcher(cvLower);
        int maxYears = 0;
        while (matcher.find()) {
            try {
                int years = Integer.parseInt(matcher.group(1));
                if (years > maxYears && years < 50) maxYears = years;
            } catch (NumberFormatException ignored) {}
        }
        return maxYears;
    }

    private double calculateExperienceScore(int detected, int required) {
        if (required <= 0) return 0.8;
        if (detected >= required) return 1.0;
        if (detected >= required - 1) return 0.7;
        if (detected > 0) return 0.4;
        return 0.2;
    }
}