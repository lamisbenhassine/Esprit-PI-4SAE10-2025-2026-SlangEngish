package com.school.schoolservice.application.service.impl;

import com.school.schoolservice.application.dto.PlagiatResultDto;
import com.school.schoolservice.application.entity.Application;
import com.school.schoolservice.application.repository.ApplicationRepository;
import com.school.schoolservice.application.service.PlagiatService;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
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
public class PlagiatServiceImpl implements PlagiatService {

    private final ApplicationRepository applicationRepository;

    // ✅ Seuils
   // private static final double PLAGIAT_THRESHOLD  = 0.80;
    //private static final double SUSPECT_THRESHOLD  = 0.60;
    private static final double PLAGIAT_THRESHOLD = 0.30; // était 0.80
    private static final double SUSPECT_THRESHOLD = 0.10; // était 0.60

    // ✅ Stop words
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to",
            "for", "of", "with", "by", "from", "is", "are", "was", "were",
            "be", "been", "have", "has", "had", "do", "does", "did", "will",
            "would", "could", "should", "may", "might", "i", "my", "we",
            "you", "he", "she", "it", "they", "this", "that", "not", "so",
            "le", "la", "les", "de", "du", "des", "un", "une", "et", "en",
            "je", "mon", "ma", "mes", "nous", "que", "qui", "par", "sur"
    );

    @Override
    public List<PlagiatResultDto> detectPlagiat(Long jobOfferId) {
        System.out.println("🔍 Détection plagiat pour offre: " + jobOfferId);

        // ✅ Récupère les candidatures
        List<Application> applications = jobOfferId != null
                ? applicationRepository.findByJobOfferId(jobOfferId)
                : applicationRepository.findAll();

        applications = applications.stream()
                .filter(a -> a.getCvUrl() != null && !a.getCvUrl().isEmpty())
                .collect(Collectors.toList());

        System.out.println("📋 " + applications.size() + " CVs à comparer");

        if (applications.size() < 2) {
            System.out.println("⚠️ Pas assez de CVs pour comparer");
            return List.of();
        }

        // ✅ Extrait le texte de chaque CV
        Map<Long, String> cvTexts = new HashMap<>();
        for (Application app : applications) {
            String text = extractTextFromPdf(app.getCvUrl());
            cvTexts.put(app.getId(), text);
            System.out.println("📄 " + app.getApplicantName()
                    + " → " + text.length() + " chars");
        }

        // ✅ Calcule TF-IDF pour tous les CVs
        Map<Long, Map<String, Double>> tfidfVectors = computeTfIdf(cvTexts);

        // ✅ Compare chaque paire
        List<PlagiatResultDto> results = new ArrayList<>();

        for (int i = 0; i < applications.size(); i++) {
            for (int j = i + 1; j < applications.size(); j++) {
                Application app1 = applications.get(i);
                Application app2 = applications.get(j);

                Map<String, Double> vec1 = tfidfVectors.get(app1.getId());
                Map<String, Double> vec2 = tfidfVectors.get(app2.getId());

                double similarity = cosineSimilarity(vec1, vec2);
                int percent = (int) Math.round(similarity * 100);

                System.out.println("🔄 " + app1.getApplicantName()
                        + " vs " + app2.getApplicantName()
                        + " → " + percent + "%");

                // ✅ Filtre seulement les suspects et plagiat
                if (similarity >= SUSPECT_THRESHOLD) {
                    String level;
                    if (similarity >= PLAGIAT_THRESHOLD) {
                        level = "PLAGIAT";
                        System.out.println("🚨 PLAGIAT détecté !");
                    } else {
                        level = "SUSPECT";
                        System.out.println("⚠️ SUSPECT détecté !");
                    }

                    results.add(PlagiatResultDto.builder()
                            .applicationId1(app1.getId())
                            .applicantName1(app1.getApplicantName())
                            .applicantEmail1(app1.getApplicantEmail())
                            .applicationId2(app2.getId())
                            .applicantName2(app2.getApplicantName())
                            .applicantEmail2(app2.getApplicantEmail())
                            .similarityScore(Math.round(similarity * 100.0) / 100.0)
                            .similarityPercent(percent)
                            .plagiatLevel(level)
                            .build());
                }
            }
        }

        // ✅ Trie par similarité décroissante
        results.sort((a, b) ->
                Integer.compare(b.getSimilarityPercent(), a.getSimilarityPercent()));

        System.out.println("✅ " + results.size() + " paires suspectes trouvées");
        return results;
    }

    // ─── TF-IDF ───────────────────────────────────────────────────────────────

    private Map<Long, Map<String, Double>> computeTfIdf(Map<Long, String> texts) {
        // ✅ Tokenise
        Map<Long, List<String>> tokenized = new HashMap<>();
        for (Map.Entry<Long, String> entry : texts.entrySet()) {
            tokenized.put(entry.getKey(), tokenize(entry.getValue()));
        }

        // ✅ IDF
        Map<String, Double> idf = computeIdf(tokenized);

        // ✅ TF-IDF
        Map<Long, Map<String, Double>> result = new HashMap<>();
        for (Map.Entry<Long, List<String>> entry : tokenized.entrySet()) {
            result.put(entry.getKey(), computeTf(entry.getValue(), idf));
        }
        return result;
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) return List.of();
        return Arrays.stream(text.toLowerCase()
                        .replaceAll("[^a-zA-ZÀ-ÿ\\s]", " ")
                        .split("\\s+"))
                .filter(w -> w.length() > 2)
                .filter(w -> !STOP_WORDS.contains(w))
                .collect(Collectors.toList());
    }

    private Map<String, Double> computeIdf(Map<Long, List<String>> tokenized) {
        int totalDocs = tokenized.size();
        Map<String, Integer> docFreq = new HashMap<>();

        for (List<String> tokens : tokenized.values()) {
            new HashSet<>(tokens).forEach(t ->
                    docFreq.merge(t, 1, Integer::sum));
        }

        Map<String, Double> idf = new HashMap<>();
        docFreq.forEach((term, freq) ->
                idf.put(term, Math.log((double) totalDocs / (1 + freq))));
        return idf;
    }

    private Map<String, Double> computeTf(List<String> tokens,
                                          Map<String, Double> idf) {
        if (tokens.isEmpty()) return Map.of();

        Map<String, Long> freq = tokens.stream()
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()));

        Map<String, Double> tfidf = new HashMap<>();
        freq.forEach((term, count) -> {
            double tf = (double) count / tokens.size();
            double idfVal = idf.getOrDefault(term, 0.0);
            tfidf.put(term, tf * idfVal);
        });
        return tfidf;
    }

    // ─── Cosinus ──────────────────────────────────────────────────────────────

    private double cosineSimilarity(Map<String, Double> vecA,
                                    Map<String, Double> vecB) {
        if (vecA == null || vecB == null ||
                vecA.isEmpty() || vecB.isEmpty()) return 0.0;

        double dotProduct = 0.0;
        for (Map.Entry<String, Double> entry : vecA.entrySet()) {
            if (vecB.containsKey(entry.getKey())) {
                dotProduct += entry.getValue() * vecB.get(entry.getKey());
            }
        }

        double normA = Math.sqrt(vecA.values().stream()
                .mapToDouble(v -> v * v).sum());
        double normB = Math.sqrt(vecB.values().stream()
                .mapToDouble(v -> v * v).sum());

        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (normA * normB);
    }

    // ─── PDF Extraction ───────────────────────────────────────────────────────

    private String extractTextFromPdf(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) return "";
        try {
            byte[] bytes;

            if (fileUrl.startsWith("http")) {
                String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1)
                        .trim().replaceAll("[\\r\\n%0A]", "");
                File localFile = new File("uploads/" + fileName);

                if (localFile.exists()) {
                    bytes = Files.readAllBytes(localFile.toPath());
                } else {
                    bytes = new URL(fileUrl.trim()).openStream().readAllBytes();
                }
            } else {
                File file = new File(fileUrl.trim());
                if (!file.exists()) {
                    String fileName = fileUrl.replaceAll(".*[/\\\\]", "").trim();
                    file = new File("uploads/" + fileName);
                }
                if (!file.exists()) return "";
                bytes = Files.readAllBytes(file.toPath());
            }

            try (PDDocument document = Loader.loadPDF(bytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        } catch (Exception e) {
            log.error("PDF error: {}", e.getMessage());
            return "";
        }
    }
}