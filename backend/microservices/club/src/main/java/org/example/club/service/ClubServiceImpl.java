package org.example.club.service;

import org.example.club.entity.Club;
import org.example.club.repository.ClubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ClubServiceImpl implements ClubService {

    @Autowired
    private ClubRepository clubRepository;

    @Override
    public Club createClub(Club club) {
        if (club.getNom() == null || club.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du club est obligatoire");
        }
        if (clubRepository.existsByNom(club.getNom())) {
            throw new IllegalArgumentException("Un club avec ce nom existe déjà");
        }
        return clubRepository.save(club);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Club> getAllClubs() {
        return clubRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Club> getClubById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("L'ID du club ne peut pas être null");
        }
        return clubRepository.findById(id);
    }

    @Override
    public Club updateClub(Long id, Club club) {
        if (id == null) {
            throw new IllegalArgumentException("L'ID du club ne peut pas être null");
        }
        
        Club existingClub = clubRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Club non trouvé avec l'ID: " + id));
        
        // Mise à jour des champs
        if (club.getNom() != null && !club.getNom().trim().isEmpty()) {
            // Vérifier si le nom n'est pas déjà utilisé par un autre club
            Optional<Club> clubWithSameName = clubRepository.findByNom(club.getNom());
            if (clubWithSameName.isPresent() && !clubWithSameName.get().getId().equals(id)) {
                throw new IllegalArgumentException("Un club avec ce nom existe déjà");
            }
            existingClub.setNom(club.getNom());
        }
        
        if (club.getDescription() != null) {
            existingClub.setDescription(club.getDescription());
        }
        
        if (club.getType() != null) {
            existingClub.setType(club.getType());
        }
        
        if (club.getImage() != null) {
            existingClub.setImage(club.getImage());
        }
        
        if (club.getStatut() != null) {
            existingClub.setStatut(club.getStatut());
        }
        
        if (club.getDateCreation() != null) {
            existingClub.setDateCreation(club.getDateCreation());
        }
        
        if (club.getIdResponsable() != null) {
            existingClub.setIdResponsable(club.getIdResponsable());
        }
        
        return clubRepository.save(existingClub);
    }

    @Override
    public void deleteClub(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("L'ID du club ne peut pas être null");
        }
        
        if (!clubRepository.existsById(id)) {
            throw new RuntimeException("Club non trouvé avec l'ID: " + id);
        }
        
        clubRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Club> getClubByNom(String nom) {
        return clubRepository.findByNom(nom);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Club> getClubsByType(String type) {
        return clubRepository.findByType(type);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Club> getClubsByStatut(String statut) {
        return clubRepository.findByStatut(statut);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Club> getClubsByResponsable(Long idResponsable) {
        return clubRepository.findByIdResponsable(idResponsable);
    }
}

