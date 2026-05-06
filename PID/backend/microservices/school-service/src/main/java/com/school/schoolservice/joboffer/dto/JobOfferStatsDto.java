package com.school.schoolservice.joboffer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobOfferStatsDto {

    // ✅ Général
    private long totalOffers;
    private long activeOffers;
    private long expiredOffers;
    private long totalApplications;

    // ✅ Offre la plus populaire
    private String mostPopularOfferTitle;
    private String mostPopularOfferCompany;
    private long mostPopularOfferApplications;

    // ✅ Salaire moyen par type de contrat
    private Map<String, Double> avgSalaryByContractType;

    // ✅ Candidatures par statut
    private Map<String, Long> applicationsByStatus;

    // ✅ Offres par type de contrat
    private Map<String, Long> offersByContractType;

    // ✅ Taux de remplissage (candidatures/5 max)
    private Map<String, Double> fillRateByOffer;

    // ✅ Top 5 offres
    private List<OfferPopularityDto> top5Offers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OfferPopularityDto {
        private Long id;
        private String title;
        private String company;
        private String contractType;
        private long applicationCount;
        private double fillRate;
    }
}