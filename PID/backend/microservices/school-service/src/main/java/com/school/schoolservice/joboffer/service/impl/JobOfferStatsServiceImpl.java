package com.school.schoolservice.joboffer.service.impl;

import com.school.schoolservice.application.repository.ApplicationRepository;
import com.school.schoolservice.joboffer.dto.JobOfferStatsDto;
import com.school.schoolservice.joboffer.dto.JobOfferStatsDto.OfferPopularityDto;
import com.school.schoolservice.joboffer.entity.JobOffer;
import com.school.schoolservice.joboffer.repository.JobOfferRepository;
import com.school.schoolservice.joboffer.service.JobOfferStatsService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobOfferStatsServiceImpl implements JobOfferStatsService {

    private final JobOfferRepository jobOfferRepository;
    private final ApplicationRepository applicationRepository;

    @Override
    public JobOfferStatsDto getStats() {
        List<JobOffer> allOffers = jobOfferRepository.findAll();
        List<JobOffer> activeOffers = jobOfferRepository.findByActiveTrue();
        long totalApplications = applicationRepository.count();

        // ✅ Offres actives/expirées
        long totalOffers = allOffers.size();
        long active = activeOffers.size();
        long expired = totalOffers - active;

        // ✅ Offres par type de contrat
        Map<String, Long> offersByContractType = new HashMap<>();
        for (JobOffer offer : allOffers) {
            String type = offer.getContractType() != null ? offer.getContractType().name() : "AUTRE";
            offersByContractType.merge(type, 1L, Long::sum);
        }

        // ✅ Salaire moyen par type de contrat
        Map<String, Double> avgSalaryByContractType = new HashMap<>();
        Map<String, List<Double>> salariesByType = new HashMap<>();
        for (JobOffer offer : allOffers) {
            if (offer.getSalary() != null && offer.getContractType() != null) {
                try {
                    // ✅ Parse le String en Double
                    double salary = Double.parseDouble(offer.getSalary().trim());
                    String type = offer.getContractType().name();
                    salariesByType.computeIfAbsent(type, k -> new ArrayList<>()).add(salary);
                } catch (NumberFormatException e) {
                    // ✅ Ignore si le salaire n'est pas un nombre valide
                }
            }
        }
        salariesByType.forEach((type, salaries) -> {
            double avg = salaries.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            avgSalaryByContractType.put(type, Math.round(avg * 100.0) / 100.0);
        });

        // ✅ Candidatures par statut
        Map<String, Long> applicationsByStatus = new HashMap<>();
        List<Object[]> statusCounts = applicationRepository.countByStatus();
        for (Object[] row : statusCounts) {
            applicationsByStatus.put(row[0].toString(), (Long) row[1]);
        }

        // ✅ Top 5 offres les plus populaires
        List<Object[]> popularOffers = applicationRepository.countByJobOfferIdOrderByCount();
        List<OfferPopularityDto> top5 = new ArrayList<>();
        String mostPopularTitle = "-";
        String mostPopularCompany = "-";
        long mostPopularCount = 0;

        int rank = 0;
        for (Object[] row : popularOffers) {
            if (rank >= 5) break;
            Long jobOfferId = (Long) row[0];
            Long count = (Long) row[1];

            JobOffer offer = jobOfferRepository.findById(jobOfferId).orElse(null);
            if (offer == null) continue;

            double fillRate = Math.min((count / 5.0) * 100, 100);

            top5.add(OfferPopularityDto.builder()
                    .id(offer.getId())
                    .title(offer.getTitle())
                    .company(offer.getCompany())
                    .contractType(offer.getContractType() != null ? offer.getContractType().name() : "-")
                    .applicationCount(count)
                    .fillRate(Math.round(fillRate * 10.0) / 10.0)
                    .build());

            if (rank == 0) {
                mostPopularTitle = offer.getTitle();
                mostPopularCompany = offer.getCompany();
                mostPopularCount = count;
            }
            rank++;
        }

        // ✅ Taux de remplissage par offre
        Map<String, Double> fillRateByOffer = new HashMap<>();
        for (OfferPopularityDto dto : top5) {
            fillRateByOffer.put(dto.getTitle(), dto.getFillRate());
        }

        return JobOfferStatsDto.builder()
                .totalOffers(totalOffers)
                .activeOffers(active)
                .expiredOffers(expired)
                .totalApplications(totalApplications)
                .mostPopularOfferTitle(mostPopularTitle)
                .mostPopularOfferCompany(mostPopularCompany)
                .mostPopularOfferApplications(mostPopularCount)
                .avgSalaryByContractType(avgSalaryByContractType)
                .applicationsByStatus(applicationsByStatus)
                .offersByContractType(offersByContractType)
                .fillRateByOffer(fillRateByOffer)
                .top5Offers(top5)
                .build();
    }
}
