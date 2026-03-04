package org.example.evenement.service;

import org.example.evenement.dto.EvenementPopulaireDto;
import org.example.evenement.dto.EvolutionMensuelleDto;
import org.example.evenement.dto.StatistiquesEvenementDto;
import org.example.evenement.entity.Evenement;
import org.example.evenement.entity.InscriptionEvenement;
import org.example.evenement.repository.EvenementRepository;
import org.example.evenement.repository.InscriptionEvenementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StatistiquesEvenementServiceImpl implements StatistiquesEvenementService {

    @Autowired
    private EvenementRepository evenementRepository;

    @Autowired
    private InscriptionEvenementRepository inscriptionRepository;

    private static final DateTimeFormatter MOIS_FORMAT = DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH);

    @Override
    public StatistiquesEvenementDto getStatistiques() {
        List<Evenement> evenements = evenementRepository.findAll();

        // Taux de remplissage : moyenne des (inscriptions/capacité)*100 pour les événements avec capacité
        double tauxRemplissage = calculerTauxRemplissage(evenements);

        // Moyenne de participation : total inscriptions / nombre d'événements
        double moyenneParticipation = calculerMoyenneParticipation(evenements);

        // Événement le plus populaire
        EvenementPopulaireDto plusPopulaire = trouverPlusPopulaire(evenements);

        // Évolution mensuelle
        List<EvolutionMensuelleDto> evolutionMensuelle = calculerEvolutionMensuelle();

        return new StatistiquesEvenementDto(tauxRemplissage, moyenneParticipation, plusPopulaire, evolutionMensuelle);
    }

    private double calculerTauxRemplissage(List<Evenement> evenements) {
        List<Double> tauxList = new ArrayList<>();
        for (Evenement ev : evenements) {
            if (ev.getCapacite() != null && ev.getCapacite() > 0) {
                long count = inscriptionRepository.countByEvenementId(ev.getId());
                double taux = Math.min(100.0, (count * 100.0) / ev.getCapacite());
                tauxList.add(taux);
            }
        }
        if (tauxList.isEmpty()) return 0;
        return tauxList.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private double calculerMoyenneParticipation(List<Evenement> evenements) {
        if (evenements.isEmpty()) return 0;
        long total = 0;
        for (Evenement ev : evenements) {
            total += inscriptionRepository.countByEvenementId(ev.getId());
        }
        return (double) total / evenements.size();
    }

    private EvenementPopulaireDto trouverPlusPopulaire(List<Evenement> evenements) {
        if (evenements.isEmpty()) return null;
        Evenement maxEv = null;
        long maxCount = -1;
        for (Evenement ev : evenements) {
            long count = inscriptionRepository.countByEvenementId(ev.getId());
            if (count > maxCount) {
                maxCount = count;
                maxEv = ev;
            }
        }
        if (maxEv == null) return null;
        return new EvenementPopulaireDto(maxEv.getId(), maxEv.getTitre(), maxCount);
    }

    private List<EvolutionMensuelleDto> calculerEvolutionMensuelle() {
        List<InscriptionEvenement> allInscriptions = inscriptionRepository.findAll();
        Map<YearMonth, Long> byMonth = new TreeMap<>();

        for (InscriptionEvenement ins : allInscriptions) {
            Evenement ev = ins.getEvenement();
            if (ev != null && ev.getDate() != null) {
                YearMonth ym = YearMonth.from(ev.getDate());
                byMonth.merge(ym, 1L, Long::sum);
            }
        }

        return byMonth.entrySet().stream()
                .map(e -> new EvolutionMensuelleDto(
                        e.getKey().format(MOIS_FORMAT),
                        e.getValue()
                ))
                .collect(Collectors.toList());
    }
}
