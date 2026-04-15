package com.school.schoolservice.matching.service.impl;

import com.school.schoolservice.joboffer.entity.JobOffer;
import com.school.schoolservice.joboffer.repository.JobOfferRepository;
import com.school.schoolservice.matching.dto.MatchingResultDto;
import com.school.schoolservice.matching.dto.VisitorProfileDto;
import com.school.schoolservice.matching.service.MatchingService;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingServiceImpl implements MatchingService {

    private final JobOfferRepository jobOfferRepository;

    private static final double WEIGHT_LOCATION = 0.30;
    private static final double WEIGHT_CONTRACT  = 0.25;
    private static final double WEIGHT_SALARY    = 0.20;
    private static final double WEIGHT_KEYWORDS  = 0.25;

    @Override
    public List<MatchingResultDto> getMatchingOffersForVisitor(VisitorProfileDto visitor) {
        List<JobOffer> activeOffers = jobOfferRepository.findByActiveTrue();
        return activeOffers.stream()
                .map(offer -> calculateMatch(offer, visitor))
                .sorted((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()))
                .collect(Collectors.toList());
    }

    @Override
    public MatchingResultDto getMatchScore(Long offerId, VisitorProfileDto visitor) {
        JobOffer offer = jobOfferRepository.findById(offerId)
                .orElseThrow(() -> new RuntimeException("Offre non trouvée"));
        return calculateMatch(offer, visitor);
    }

    private MatchingResultDto calculateMatch(JobOffer offer, VisitorProfileDto visitor) {
        double totalScore  = 0.0;
        double totalWeight = 0.0;
        double locationScore = 0.5;
        double contractScore = 0.5;
        double salaryScore   = 0.5;
        double keywordScore  = 0.5;

        if (visitor != null) {
            if (visitor.getVille() != null && !visitor.getVille().isEmpty()) {
                locationScore = calculateLocationScore(offer.getLocation(), visitor.getVille());
                totalScore  += locationScore * WEIGHT_LOCATION;
                totalWeight += WEIGHT_LOCATION;
            }
            if (visitor.getTypeContrat() != null && !visitor.getTypeContrat().isEmpty()) {
                contractScore = calculateContractScore(
                        offer.getContractType() != null ? offer.getContractType().name() : null,
                        visitor.getTypeContrat());
                totalScore  += contractScore * WEIGHT_CONTRACT;
                totalWeight += WEIGHT_CONTRACT;
            }
            if (visitor.getSalaireSouhaite() != null && visitor.getSalaireSouhaite() > 0) {
                salaryScore = calculateSalaryScore(offer.getSalary(), visitor.getSalaireSouhaite());
                totalScore  += salaryScore * WEIGHT_SALARY;
                totalWeight += WEIGHT_SALARY;
            }
            if (visitor.getCompetences() != null && !visitor.getCompetences().isEmpty()) {
                keywordScore = calculateKeywordScore(offer.getDescription(), visitor.getCompetences());
                totalScore  += keywordScore * WEIGHT_KEYWORDS;
                totalWeight += WEIGHT_KEYWORDS;
            }
            totalScore = totalWeight > 0 ? totalScore / totalWeight : 0.5;
        } else {
            totalScore = 0.5;
        }

        int percent = (int) Math.round(totalScore * 100);
        String level;
        if (percent >= 75)      level = "Excellent";
        else if (percent >= 50) level = "Good";
        else if (percent >= 30) level = "Average";
        else                    level = "Low";

        return MatchingResultDto.builder()
                .offerId(offer.getId())
                .offerTitle(offer.getTitle())
                .offerCompany(offer.getCompany())
                .offerLocation(offer.getLocation())
                .offerContractType(offer.getContractType() != null
                        ? offer.getContractType().name() : "-")
                .offerSalary(offer.getSalary())
                .matchScore(totalScore)
                .matchPercent(percent)
                .matchLevel(level)
                .locationScore(Math.round(locationScore * 100.0) / 100.0)
                .contractScore(Math.round(contractScore * 100.0) / 100.0)
                .salaryScore(Math.round(salaryScore * 100.0) / 100.0)
                .keywordScore(Math.round(keywordScore * 100.0) / 100.0)
                .build();
    }

    private double calculateLocationScore(String offerLocation, String ville) {
        if (offerLocation == null || ville == null) return 0.5;
        String ol = offerLocation.toLowerCase().trim();
        String pl = ville.toLowerCase().trim();
        if (ol.equals(pl)) return 1.0;
        if (ol.contains(pl) || pl.contains(ol)) return 0.7;
        return 0.0;
    }

    private double calculateContractScore(String offerContract, String typeContrat) {
        if (offerContract == null || typeContrat == null) return 0.5;
        if (offerContract.equalsIgnoreCase(typeContrat)) return 1.0;
        if ((offerContract.equals("CDI") && typeContrat.equals("CDD")) ||
                (offerContract.equals("CDD") && typeContrat.equals("CDI"))) return 0.4;
        if ((offerContract.equals("STAGE") && typeContrat.equals("ALTERNANCE")) ||
                (offerContract.equals("ALTERNANCE") && typeContrat.equals("STAGE"))) return 0.5;
        return 0.1;
    }

    private double calculateSalaryScore(String offerSalaryStr, Double salaireSouhaite) {
        if (offerSalaryStr == null || salaireSouhaite == null || salaireSouhaite == 0) return 0.5;
        try {
            double offerSalary = Double.parseDouble(offerSalaryStr.trim());
            double ratio = Math.abs(offerSalary - salaireSouhaite) / salaireSouhaite;
            if (ratio <= 0.10) return 1.0;
            if (ratio <= 0.20) return 0.8;
            if (ratio <= 0.40) return 0.5;
            if (ratio <= 0.60) return 0.3;
            return 0.1;
        } catch (NumberFormatException e) {
            return 0.5;
        }
    }

    private double calculateKeywordScore(String offerDescription, String competences) {
        if (offerDescription == null || competences == null) return 0.5;
        String desc = offerDescription.toLowerCase();
        List<String> skills = Arrays.stream(competences.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        if (skills.isEmpty()) return 0.5;
        long matches = skills.stream().filter(desc::contains).count();
        return (double) matches / skills.size();
    }
}