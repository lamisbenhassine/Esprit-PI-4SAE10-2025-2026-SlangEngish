package com.school.schoolservice.application.service.impl;

import com.school.schoolservice.application.dto.CvAnalysisResultDto;
import com.school.schoolservice.application.entity.Application;
import com.school.schoolservice.application.repository.ApplicationRepository;
import com.school.schoolservice.application.service.CvAnalysisService;
import com.school.schoolservice.joboffer.entity.JobOffer;
import com.school.schoolservice.joboffer.repository.JobOfferRepository;
import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
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
public class CvAnalysisServiceImpl implements CvAnalysisService {

    private final ApplicationRepository applicationRepository;
    private final JobOfferRepository jobOfferRepository;

    private static final List<String> SKILL_KEYWORDS = Arrays.asList(
            "java", "spring", "angular", "python", "javascript", "typescript",
            "react", "nodejs", "sql", "mysql", "mongodb", "docker", "git",
            "marketing", "accounting", "finance", "management", "sales",
            "excel", "powerpoint", "word", "sap", "crm",
            "leadership", "teamwork", "communication", "english", "french",
            "arabic", "project management", "agile", "scrum",
            "figma", "photoshop", "illustrator", "design", "ux", "ui",
            "recruitment", "training", "payroll", "hr", "human resources",
            "law", "audit", "tax", "budget", "analysis", "reporting"
    );

    private static final List<String> LOCATIONS = Arrays.asList(
            "tunis", "sfax", "sousse", "monastir", "bizerte", "nabeul",
            "gabes", "ariana", "ben arous", "manouba", "marsa", "carthage",
            "paris", "lyon", "marseille", "toulouse", "bordeaux", "lille",
            "remote", "télétravail", "hybride"
    );

    @Override
    public CvAnalysisResultDto analyzeCV(Long applicationId) {
        System.out.println("🔍 Analyse CV pour applicationId=" + applicationId);

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException(
                        "Application not found: " + applicationId));

        JobOffer jobOffer = jobOfferRepository.findById(application.getJobOfferId())
                .orElseThrow(() -> new RuntimeException(
                        "JobOffer not found: " + application.getJobOfferId()));

        String cvText = extractTextFromPdf(application.getCvUrl());
        System.out.println("📄 Texte extrait (" + cvText.length() + " caractères)");

        String cvLower = cvText.toLowerCase();

        List<String> detectedSkills = SKILL_KEYWORDS.stream()
                .filter(cvLower::contains)
                .collect(Collectors.toList());
        System.out.println("✅ Compétences détectées : " + detectedSkills);

        String detectedLocation = LOCATIONS.stream()
                .filter(cvLower::contains)
                .findFirst()
                .orElse(null);
        System.out.println("📍 Localisation détectée : " + detectedLocation);

        int experienceYears = detectExperienceYears(cvLower);
        System.out.println("📅 Expérience détectée : " + experienceYears + " ans");

        String detectedEmail = detectEmail(cvText);

        double skillsScore    = calculateSkillsScore(cvLower, jobOffer.getDescription());
        double locationScore  = calculateLocationScore(detectedLocation, jobOffer.getLocation());
        double experienceScore = calculateExperienceScore(experienceYears);

        double overallScore = (skillsScore * 0.50)
                + (locationScore * 0.30)
                + (experienceScore * 0.20);

        int overallPercent = (int) Math.round(overallScore * 100);

        String matchLevel;
        if (overallPercent >= 75)      matchLevel = "Excellent";
        else if (overallPercent >= 50) matchLevel = "Good";
        else if (overallPercent >= 30) matchLevel = "Average";
        else                           matchLevel = "Low";

        System.out.println("🎯 Score global : " + overallPercent + "% (" + matchLevel + ")");

        return CvAnalysisResultDto.builder()
                .applicationId(applicationId)
                .jobOfferId(application.getJobOfferId())
                .applicantName(application.getApplicantName())
                .extractedText(cvText.length() > 500
                        ? cvText.substring(0, 500) + "..." : cvText)
                .detectedSkills(detectedSkills)
                .detectedLocation(detectedLocation)
                .detectedExperienceYears(experienceYears)
                .detectedEmail(detectedEmail)
                .skillsScore(Math.round(skillsScore * 100.0) / 100.0)
                .locationScore(Math.round(locationScore * 100.0) / 100.0)
                .experienceScore(Math.round(experienceScore * 100.0) / 100.0)
                .overallScore(Math.round(overallScore * 100.0) / 100.0)
                .overallPercent(overallPercent)
                .matchLevel(matchLevel)
                .build();
    }

    // ─── Extraction PDF ───────────────────────────────────────────────────────

    private String extractTextFromPdf(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            System.out.println("⚠️ URL CV vide");
            return "";
        }
        try {
            byte[] bytes;

            if (fileUrl.startsWith("http")) {
                // ✅ Essaie d'abord en local
                String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1)
                        .trim()
                        .replace("\r", "")
                        .replace("\n", "")
                        .replace("%0A", "");
                fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);
                File localFile = new File("uploads/" + fileName);
                if (localFile.exists()) {
                    bytes = Files.readAllBytes(localFile.toPath());
                    System.out.println("✅ Chargement local : " + localFile.getAbsolutePath());
                } else {
                    // ✅ Fallback HTTP (encode spaces)
                    String safeUrl = fileUrl.trim().replace(" ", "%20");
                    bytes = new URL(safeUrl).openStream().readAllBytes();
                    System.out.println("✅ Chargement HTTP : " + safeUrl);
                }
            } else {
                // ✅ Chemin local
                File file = new File(fileUrl);
                if (!file.exists()) {
                    // ✅ Essaie avec juste le nom du fichier
                    String fileName = fileUrl.replaceAll(".*[/\\\\]", "");
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
                return text;
            }
        } catch (Exception e) {
            System.out.println("❌ Erreur PDF : " + e.getMessage());
            log.error("PDF extraction error: {}", e.getMessage());
            return "";
        }
    }

    // ─── Calculs scores ───────────────────────────────────────────────────────

    private double calculateSkillsScore(String cvLower, String jobDescription) {
        if (jobDescription == null || jobDescription.isEmpty()) return 0.5;
        String descLower = jobDescription.toLowerCase();
        List<String> requiredSkills = SKILL_KEYWORDS.stream()
                .filter(descLower::contains)
                .collect(Collectors.toList());
        if (requiredSkills.isEmpty()) return 0.5;
        long matches = requiredSkills.stream()
                .filter(cvLower::contains)
                .count();
        return (double) matches / requiredSkills.size();
    }

    private double calculateLocationScore(String detectedLocation, String offerLocation) {
        if (detectedLocation == null || offerLocation == null) return 0.5;
        String ol = offerLocation.toLowerCase();
        if (ol.contains(detectedLocation) || detectedLocation.contains(ol)) return 1.0;
        if (detectedLocation.equals("remote") || detectedLocation.equals("télétravail")) return 0.8;
        return 0.2;
    }

    private double calculateExperienceScore(int years) {
        if (years >= 5) return 1.0;
        if (years >= 3) return 0.8;
        if (years >= 1) return 0.6;
        if (years == 0) return 0.4;
        return 0.3;
    }

    private int detectExperienceYears(String cvLower) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(\\d+)\\s*(?:ans?|years?|an)\\s*(?:d'expérience|experience|exp)?");
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

    private String detectEmail(String text) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group() : null;
    }
}