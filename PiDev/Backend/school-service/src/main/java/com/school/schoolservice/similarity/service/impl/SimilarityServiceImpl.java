package com.school.schoolservice.similarity.service.impl;

import com.school.schoolservice.joboffer.entity.JobOffer;
import com.school.schoolservice.joboffer.repository.JobOfferRepository;
import com.school.schoolservice.similarity.dto.SimilarityResultDto;
import com.school.schoolservice.similarity.service.SimilarityService;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimilarityServiceImpl implements SimilarityService {

    private final JobOfferRepository jobOfferRepository;

    // ✅ Poids des critères
    private static final double WEIGHT_COSINUS  = 0.50;
    private static final double WEIGHT_CONTRACT = 0.25;
    private static final double WEIGHT_LOCATION = 0.15;
    private static final double WEIGHT_SALARY   = 0.10;

    // ✅ Mots vides à ignorer (stop words)
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to",
            "for", "of", "with", "by", "from", "is", "are", "was", "were",
            "be", "been", "being", "have", "has", "had", "do", "does", "did",
            "will", "would", "could", "should", "may", "might", "shall",
            "we", "you", "he", "she", "it", "they", "i", "my", "our",
            "your", "his", "her", "its", "their", "this", "that", "these",
            "those", "not", "no", "nor", "so", "yet", "both", "either",
            "le", "la", "les", "de", "du", "des", "un", "une", "et", "ou",
            "en", "au", "aux", "par", "sur", "dans", "avec", "pour", "que",
            "qui", "ce", "se", "sa", "son", "ses", "nous", "vous", "ils"
    );

    @Override
    public List<SimilarityResultDto> findSimilarOffers(Long offerId, int topN) {
        // ✅ Récupère l'offre cible
        JobOffer targetOffer = jobOfferRepository.findById(offerId)
                .orElseThrow(() -> new RuntimeException("Offre non trouvée : " + offerId));

        // ✅ Récupère toutes les offres actives sauf la cible
        List<JobOffer> allOffers = jobOfferRepository.findByActiveTrue()
                .stream()
                .filter(o -> !o.getId().equals(offerId))
                .collect(Collectors.toList());

        if (allOffers.isEmpty()) return List.of();

        // ✅ Calcule TF-IDF pour toutes les offres
        List<JobOffer> corpus = new ArrayList<>(allOffers);
        corpus.add(targetOffer);

        Map<Long, Map<String, Double>> tfidfVectors = computeTfIdf(corpus);

        // ✅ Vecteur de l'offre cible
        Map<String, Double> targetVector = tfidfVectors.get(targetOffer.getId());

        // ✅ Calcule similarité pour chaque offre
        return allOffers.stream()
                .map(offer -> {
                    Map<String, Double> offerVector = tfidfVectors.get(offer.getId());

                    double cosinusScore  = cosineSimilarity(targetVector, offerVector);
                    double contractScore = calculateContractScore(targetOffer, offer);
                    double locationScore = calculateLocationScore(targetOffer, offer);
                    double salaryScore   = calculateSalaryScore(targetOffer, offer);

                    double totalScore = (cosinusScore  * WEIGHT_COSINUS)
                            + (contractScore * WEIGHT_CONTRACT)
                            + (locationScore * WEIGHT_LOCATION)
                            + (salaryScore   * WEIGHT_SALARY);

                    totalScore = Math.min(totalScore, 1.0);

                    return SimilarityResultDto.builder()
                            .offerId(offer.getId())
                            .offerTitle(offer.getTitle())
                            .offerCompany(offer.getCompany())
                            .offerLocation(offer.getLocation())
                            .offerContractType(offer.getContractType() != null
                                    ? offer.getContractType().name() : "-")
                            .offerSalary(offer.getSalary())
                            .offerActive(offer.getActive())
                            .similarityScore(Math.round(totalScore * 100.0) / 100.0)
                            .similarityPercent((int) Math.round(totalScore * 100))
                            .cosinusScore(Math.round(cosinusScore  * 100.0) / 100.0)
                            .contractScore(Math.round(contractScore * 100.0) / 100.0)
                            .locationScore(Math.round(locationScore * 100.0) / 100.0)
                            .salaryScore(Math.round(salaryScore   * 100.0) / 100.0)
                            .build();
                })
                .filter(r -> r.getSimilarityPercent() > 10) // ✅ Ignore les très différentes
                .sorted((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()))
                .limit(topN)
                .collect(Collectors.toList());
    }

    // ─── TF-IDF ─────────────────────────────────

    private Map<Long, Map<String, Double>> computeTfIdf(List<JobOffer> offers) {
        // ✅ Tokenise chaque offre
        Map<Long, List<String>> tokenizedOffers = new HashMap<>();
        for (JobOffer offer : offers) {
            tokenizedOffers.put(offer.getId(), tokenize(buildText(offer)));
        }

        // ✅ Calcule IDF
        Map<String, Double> idf = computeIdf(tokenizedOffers);

        // ✅ Calcule TF-IDF pour chaque offre
        Map<Long, Map<String, Double>> result = new HashMap<>();
        for (Map.Entry<Long, List<String>> entry : tokenizedOffers.entrySet()) {
            result.put(entry.getKey(), computeTf(entry.getValue(), idf));
        }
        return result;
    }

    private String buildText(JobOffer offer) {
        // ✅ Combine titre + description + lieu + type contrat
        StringBuilder sb = new StringBuilder();
        if (offer.getTitle() != null)       sb.append(offer.getTitle()).append(" ");
        if (offer.getDescription() != null) sb.append(offer.getDescription()).append(" ");
        if (offer.getLocation() != null)    sb.append(offer.getLocation()).append(" ");
        if (offer.getContractType() != null) sb.append(offer.getContractType().name()).append(" ");
        if (offer.getCompany() != null)     sb.append(offer.getCompany()).append(" ");
        return sb.toString();
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) return List.of();
        return Arrays.stream(text.toLowerCase()
                        .replaceAll("[^a-zA-ZÀ-ÿ\\s]", " ") // garde lettres + accents
                        .split("\\s+"))
                .filter(w -> w.length() > 2)
                .filter(w -> !STOP_WORDS.contains(w))
                .collect(Collectors.toList());
    }

    private Map<String, Double> computeIdf(Map<Long, List<String>> tokenizedOffers) {
        int totalDocs = tokenizedOffers.size();
        Map<String, Integer> docFreq = new HashMap<>();

        for (List<String> tokens : tokenizedOffers.values()) {
            Set<String> uniqueTokens = new HashSet<>(tokens);
            for (String token : uniqueTokens) {
                docFreq.merge(token, 1, Integer::sum);
            }
        }

        Map<String, Double> idf = new HashMap<>();
        for (Map.Entry<String, Integer> entry : docFreq.entrySet()) {
            idf.put(entry.getKey(),
                    Math.log((double) totalDocs / (1 + entry.getValue())));
        }
        return idf;
    }

    private Map<String, Double> computeTf(List<String> tokens,
                                          Map<String, Double> idf) {
        if (tokens.isEmpty()) return Map.of();

        // ✅ TF
        Map<String, Long> freq = tokens.stream()
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()));

        Map<String, Double> tfidf = new HashMap<>();
        for (Map.Entry<String, Long> entry : freq.entrySet()) {
            double tf = (double) entry.getValue() / tokens.size();
            double idfVal = idf.getOrDefault(entry.getKey(), 0.0);
            tfidf.put(entry.getKey(), tf * idfVal);
        }
        return tfidf;
    }

    // ─── Similarité Cosinus ──────────────────────

    private double cosineSimilarity(Map<String, Double> vecA,
                                    Map<String, Double> vecB) {
        if (vecA.isEmpty() || vecB.isEmpty()) return 0.0;

        // ✅ Produit scalaire A·B
        double dotProduct = 0.0;
        for (Map.Entry<String, Double> entry : vecA.entrySet()) {
            if (vecB.containsKey(entry.getKey())) {
                dotProduct += entry.getValue() * vecB.get(entry.getKey());
            }
        }

        // ✅ Normes ||A|| et ||B||
        double normA = Math.sqrt(vecA.values().stream()
                .mapToDouble(v -> v * v).sum());
        double normB = Math.sqrt(vecB.values().stream()
                .mapToDouble(v -> v * v).sum());

        if (normA == 0 || normB == 0) return 0.0;

        return dotProduct / (normA * normB);
    }

    // ─── Scores additionnels ─────────────────────

    private double calculateContractScore(JobOffer a, JobOffer b) {
        if (a.getContractType() == null || b.getContractType() == null) return 0.5;
        if (a.getContractType() == b.getContractType()) return 1.0;
        // Contrats proches
        String ta = a.getContractType().name();
        String tb = b.getContractType().name();
        if ((ta.equals("CDI") && tb.equals("CDD")) ||
                (ta.equals("CDD") && tb.equals("CDI"))) return 0.5;
        if ((ta.equals("STAGE") && tb.equals("ALTERNANCE")) ||
                (ta.equals("ALTERNANCE") && tb.equals("STAGE"))) return 0.6;
        return 0.1;
    }

    private double calculateLocationScore(JobOffer a, JobOffer b) {
        if (a.getLocation() == null || b.getLocation() == null) return 0.5;
        String la = a.getLocation().toLowerCase().trim();
        String lb = b.getLocation().toLowerCase().trim();
        if (la.equals(lb)) return 1.0;
        if (la.contains(lb) || lb.contains(la)) return 0.7;
        return 0.0;
    }

    private double calculateSalaryScore(JobOffer a, JobOffer b) {
        try {
            if (a.getSalary() == null || b.getSalary() == null) return 0.5;
            double sa = Double.parseDouble(a.getSalary().trim());
            double sb = Double.parseDouble(b.getSalary().trim());
            if (sa == 0 || sb == 0) return 0.5;
            double ratio = Math.abs(sa - sb) / Math.max(sa, sb);
            if (ratio <= 0.10) return 1.0;
            if (ratio <= 0.25) return 0.7;
            if (ratio <= 0.50) return 0.4;
            return 0.1;
        } catch (NumberFormatException e) {
            return 0.5;
        }
    }
}