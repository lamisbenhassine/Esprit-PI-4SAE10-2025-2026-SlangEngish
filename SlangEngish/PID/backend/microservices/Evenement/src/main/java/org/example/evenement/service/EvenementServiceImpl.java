package org.example.evenement.service;

import org.example.evenement.client.ClubApiClient;
import org.example.evenement.client.ClubInfoDto;
import org.example.evenement.entity.Evenement;
import org.example.evenement.entity.EventStatus;
import org.example.evenement.repository.EvenementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EvenementServiceImpl implements EvenementService {

    @Autowired
    private EvenementRepository evenementRepository;

    @Autowired
    private ClubApiClient clubApiClient;

    @Override
    public Evenement createEvenement(Evenement evenement) {
        if (evenement.getTitre() == null || evenement.getTitre().trim().isEmpty()) {
            throw new IllegalArgumentException("Le titre de l'événement est obligatoire");
        }
        if (evenement.getDate() == null) {
            throw new IllegalArgumentException("La date de l'événement est obligatoire");
        }
        if (evenementRepository.existsByTitre(evenement.getTitre())) {
            throw new IllegalArgumentException("Un événement avec ce titre existe déjà");
        }
        resolveClubOrganisateur(evenement);
        return evenementRepository.save(evenement);
    }

    /**
     * Remplit nomClubOrganisateur à partir du microservice Club si idClub est renseigné.
     */
    private void resolveClubOrganisateur(Evenement evenement) {
        if (evenement.getIdClub() != null) {
            ClubInfoDto club = clubApiClient.getClubById(evenement.getIdClub());
            if (club == null || club.getNom() == null || club.getNom().isBlank()) {
                throw new IllegalArgumentException("Club introuvable pour l'organisation de l'événement");
            }
            evenement.setNomClubOrganisateur(club.getNom().trim());
        } else {
            evenement.setIdClub(null);
            evenement.setNomClubOrganisateur(null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Evenement> getAllEvenements() {
        return evenementRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Evenement> getEvenementById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("L'ID de l'événement ne peut pas être null");
        }
        return evenementRepository.findById(id);
    }

    @Override
    public Evenement updateEvenement(Long id, Evenement evenement) {
        if (id == null) {
            throw new IllegalArgumentException("L'ID de l'événement ne peut pas être null");
        }
        
        Evenement existingEvenement = evenementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé avec l'ID: " + id));
        
        if (evenement.getTitre() != null && !evenement.getTitre().trim().isEmpty()) {
            Optional<Evenement> evenementWithSameTitre = evenementRepository.findByTitre(evenement.getTitre());
            if (evenementWithSameTitre.isPresent() && !evenementWithSameTitre.get().getId().equals(id)) {
                throw new IllegalArgumentException("Un événement avec ce titre existe déjà");
            }
            existingEvenement.setTitre(evenement.getTitre());
        }
        
        if (evenement.getDescription() != null) {
            existingEvenement.setDescription(evenement.getDescription());
        }
        
        if (evenement.getType() != null) {
            existingEvenement.setType(evenement.getType());
        }
        
        if (evenement.getDate() != null) {
            existingEvenement.setDate(evenement.getDate());
        }
        
        if (evenement.getHeure() != null) {
            existingEvenement.setHeure(evenement.getHeure());
        }
        
        if (evenement.getLieu() != null) {
            existingEvenement.setLieu(evenement.getLieu());
        }
        
        if (evenement.getCapacite() != null) {
            existingEvenement.setCapacite(evenement.getCapacite());
        }
        
        if (evenement.getImage() != null) {
            existingEvenement.setImage(evenement.getImage());
        }
        
        if (evenement.getStatus() != null) {
            existingEvenement.setStatus(evenement.getStatus());
        }

        if (evenement.getIdClub() != null) {
            ClubInfoDto club = clubApiClient.getClubById(evenement.getIdClub());
            if (club == null || club.getNom() == null || club.getNom().isBlank()) {
                throw new IllegalArgumentException("Club introuvable pour l'organisation de l'événement");
            }
            existingEvenement.setIdClub(evenement.getIdClub());
            existingEvenement.setNomClubOrganisateur(club.getNom().trim());
        } else {
            existingEvenement.setIdClub(null);
            existingEvenement.setNomClubOrganisateur(null);
        }
        
        return evenementRepository.save(existingEvenement);
    }

    @Override
    public void deleteEvenement(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("L'ID de l'événement ne peut pas être null");
        }
        
        if (!evenementRepository.existsById(id)) {
            throw new RuntimeException("Événement non trouvé avec l'ID: " + id);
        }
        
        evenementRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Evenement> getEvenementByTitre(String titre) {
        return evenementRepository.findByTitre(titre);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Evenement> getEvenementsByType(String type) {
        return evenementRepository.findByType(type);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Evenement> getEvenementsByStatus(EventStatus status) {
        return evenementRepository.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Evenement> getEvenementsByDate(LocalDate date) {
        return evenementRepository.findByDate(date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Evenement> getEvenementsByLieu(String lieu) {
        return evenementRepository.findByLieu(lieu);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Evenement> getEvenementsAVenir(LocalDate date) {
        return evenementRepository.findByDateAfter(date);
    }
}

