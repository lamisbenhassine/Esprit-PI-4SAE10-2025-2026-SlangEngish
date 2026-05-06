package org.example.evenement.service;

import org.example.evenement.client.UserApiClient;
import org.example.evenement.client.UserInfoDto;
import org.example.evenement.dto.InscriptionResultDto;
import org.example.evenement.dto.StatutInscriptionDto;
import org.example.evenement.entity.Evenement;
import org.example.evenement.entity.InscriptionEvenement;
import org.example.evenement.entity.ListeAttenteEvenement;
import org.example.evenement.repository.EvenementRepository;
import org.example.evenement.repository.InscriptionEvenementRepository;
import org.example.evenement.repository.ListeAttenteEvenementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class InscriptionEvenementServiceImpl implements InscriptionEvenementService {

    @Autowired
    private InscriptionEvenementRepository inscriptionRepository;

    @Autowired
    private EvenementRepository evenementRepository;

    @Autowired
    private ListeAttenteEvenementRepository listeAttenteRepository;

    @Autowired
    private UserApiClient userApiClient;

    @Autowired
    private EmailService emailService;

    @Value("${app.invitation.base-url:http://localhost:4200}")
    private String invitationBaseUrl;

    @Override
    public InscriptionResultDto createInscription(InscriptionEvenement inscription) {
        if (inscription.getIdEtudiant() == null) {
            throw new IllegalArgumentException("L'ID de l'étudiant est obligatoire");
        }
        if (inscription.getEvenement() == null || inscription.getEvenement().getId() == null) {
            throw new IllegalArgumentException("L'événement est obligatoire");
        }

        Evenement evenement = evenementRepository.findById(inscription.getEvenement().getId())
                .orElseThrow(() -> new RuntimeException("Événement non trouvé avec l'ID: " + inscription.getEvenement().getId()));

        if (inscriptionRepository.existsByIdEtudiantAndEvenementId(
                inscription.getIdEtudiant(),
                inscription.getEvenement().getId())) {
            throw new IllegalArgumentException("L'étudiant est déjà inscrit à cet événement");
        }

        if (listeAttenteRepository.existsByIdEtudiantAndEvenementId(inscription.getIdEtudiant(), evenement.getId())) {
            throw new IllegalArgumentException("L'étudiant est déjà en liste d'attente pour cet événement");
        }

        // Vérifier conflit d'horaires : même jour + même heure
        verifierConflitHoraires(inscription.getIdEtudiant(), evenement);

        long nombreInscriptions = inscriptionRepository.countByEvenementId(evenement.getId());
        boolean capaciteDefinie = evenement.getCapacite() != null;
        boolean complet = capaciteDefinie && nombreInscriptions >= evenement.getCapacite();

        if (complet) {
            // Vérifier conflit d'horaires même pour la liste d'attente
            verifierConflitHoraires(inscription.getIdEtudiant(), evenement);

            int nextPosition = (int) (listeAttenteRepository.countByEvenementId(evenement.getId()) + 1);
            ListeAttenteEvenement attente = new ListeAttenteEvenement();
            attente.setIdEtudiant(inscription.getIdEtudiant());
            attente.setEvenement(evenement);
            attente.setPosition(nextPosition);
            attente.setDateAjout(LocalDateTime.now());
            listeAttenteRepository.save(attente);

            String nomEtudiant = getNomEtudiant(inscription.getIdEtudiant());
            emailService.sendListeAttente(getEmailEtudiant(inscription.getIdEtudiant()), nomEtudiant, evenement.getTitre(), nextPosition);

            return new InscriptionResultDto(null, true, nextPosition);
        }

        if (inscription.getDateInscription() == null) {
            inscription.setDateInscription(LocalDate.now());
        }
        inscription.setEvenement(evenement);
        InscriptionEvenement saved = inscriptionRepository.save(inscription);

        String invitationUrl = invitationBaseUrl + "/invitation/" + saved.getId();
        String nomEtudiant = getNomEtudiant(inscription.getIdEtudiant());
        String dateStr = evenement.getDate() != null ? evenement.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
        String lieu = evenement.getLieu() != null ? evenement.getLieu() : "";
        emailService.sendInscriptionConfirmation(
                getEmailEtudiant(inscription.getIdEtudiant()),
                nomEtudiant,
                evenement.getTitre(),
                dateStr,
                lieu,
                invitationUrl
        );

        return new InscriptionResultDto(saved, false, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InscriptionEvenement> getAllInscriptions() {
        return inscriptionRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InscriptionEvenement> getInscriptionById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("L'ID de l'inscription ne peut pas être null");
        }
        return inscriptionRepository.findById(id);
    }

    @Override
    public InscriptionEvenement updateInscription(Long id, InscriptionEvenement inscription) {
        if (id == null) {
            throw new IllegalArgumentException("L'ID de l'inscription ne peut pas être null");
        }

        InscriptionEvenement existingInscription = inscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inscription non trouvée avec l'ID: " + id));

        if (inscription.getIdEtudiant() != null) {
            existingInscription.setIdEtudiant(inscription.getIdEtudiant());
        }

        if (inscription.getDateInscription() != null) {
            existingInscription.setDateInscription(inscription.getDateInscription());
        }

        if (inscription.getEvenement() != null && inscription.getEvenement().getId() != null) {
            Evenement evenement = evenementRepository.findById(inscription.getEvenement().getId())
                    .orElseThrow(() -> new RuntimeException("Événement non trouvé avec l'ID: " + inscription.getEvenement().getId()));
            existingInscription.setEvenement(evenement);
        }

        return inscriptionRepository.save(existingInscription);
    }

    @Override
    public void deleteInscription(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("L'ID de l'inscription ne peut pas être null");
        }

        InscriptionEvenement ins = inscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inscription non trouvée avec l'ID: " + id));
        Long evenementId = ins.getEvenement().getId();
        inscriptionRepository.delete(ins);
        promouvoirPremierDeListeAttente(evenementId);
    }

    @Override
    public void desinscrireEtudiant(Long idEtudiant, Long evenementId) {
        Optional<InscriptionEvenement> inscription = inscriptionRepository
                .findByIdEtudiantAndEvenementId(idEtudiant, evenementId);

        if (inscription.isPresent()) {
            inscriptionRepository.delete(inscription.get());
            promouvoirPremierDeListeAttente(evenementId);
        } else if (listeAttenteRepository.existsByIdEtudiantAndEvenementId(idEtudiant, evenementId)) {
            listeAttenteRepository.deleteByEvenementIdAndIdEtudiant(evenementId, idEtudiant);
            reordonnerPositionsListeAttente(evenementId);
        } else {
            throw new RuntimeException("Inscription ou liste d'attente non trouvée pour cet étudiant et cet événement");
        }
    }

    private void promouvoirPremierDeListeAttente(Long evenementId) {
        Optional<ListeAttenteEvenement> first = listeAttenteRepository.findFirstByEvenementIdOrderByPositionAsc(evenementId);
        if (first.isEmpty()) return;

        ListeAttenteEvenement attente = first.get();
        Evenement evenement = attente.getEvenement();
        Long idEtudiant = attente.getIdEtudiant();

        InscriptionEvenement newInscription = new InscriptionEvenement();
        newInscription.setIdEtudiant(idEtudiant);
        newInscription.setEvenement(evenement);
        newInscription.setDateInscription(LocalDate.now());
        InscriptionEvenement saved = inscriptionRepository.save(newInscription);

        listeAttenteRepository.delete(attente);
        reordonnerPositionsListeAttente(evenementId);

        String invitationUrl = invitationBaseUrl + "/invitation/" + saved.getId();
        String nomEtudiant = getNomEtudiant(idEtudiant);
        String dateStr = evenement.getDate() != null ? evenement.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
        String lieu = evenement.getLieu() != null ? evenement.getLieu() : "";
        emailService.sendPromotionFromListeAttente(
                getEmailEtudiant(idEtudiant),
                nomEtudiant,
                evenement.getTitre(),
                dateStr,
                lieu,
                invitationUrl
        );
    }

    /**
     * Vérifie qu'il n'y a pas de conflit d'horaires (même jour, même heure) avec les inscriptions
     * ou la liste d'attente existantes de l'étudiant.
     */
    private void verifierConflitHoraires(Long idEtudiant, Evenement nouvelEvenement) {
        LocalDate date = nouvelEvenement.getDate();
        LocalTime heure = nouvelEvenement.getHeure();

        // Vérifier les inscriptions confirmées
        for (InscriptionEvenement ins : inscriptionRepository.findByIdEtudiant(idEtudiant)) {
            Evenement ev = ins.getEvenement();
            if (ev != null && ev.getId() != null && !ev.getId().equals(nouvelEvenement.getId())
                    && hasConflitHoraires(date, heure, ev.getDate(), ev.getHeure())) {
                throw new IllegalArgumentException(
                    "Conflit d'horaires : vous êtes déjà inscrit à un événement le " +
                    (date != null ? date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "?") +
                    (heure != null ? " à " + heure.toString().substring(0, 5) : "") +
                    ". Annulez l'autre inscription pour vous inscrire à celui-ci.");
            }
        }

        // Vérifier la liste d'attente
        for (ListeAttenteEvenement att : listeAttenteRepository.findByIdEtudiant(idEtudiant)) {
            Evenement ev = att.getEvenement();
            if (ev != null && ev.getId() != null && !ev.getId().equals(nouvelEvenement.getId())
                    && hasConflitHoraires(date, heure, ev.getDate(), ev.getHeure())) {
                throw new IllegalArgumentException(
                    "Conflit d'horaires : vous êtes déjà en liste d'attente pour un événement le " +
                    (date != null ? date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "?") +
                    (heure != null ? " à " + heure.toString().substring(0, 5) : "") +
                    ". Annulez l'autre inscription pour vous inscrire à celui-ci.");
            }
        }
    }

    private boolean hasConflitHoraires(LocalDate date1, LocalTime heure1, LocalDate date2, LocalTime heure2) {
        if (date1 == null || date2 == null) return false;
        if (!date1.equals(date2)) return false;
        // Même jour : conflit si même heure ou si l'un des deux n'a pas d'heure (événement toute la journée)
        if (heure1 == null || heure2 == null) return true;
        return heure1.equals(heure2);
    }

    private void reordonnerPositionsListeAttente(Long evenementId) {
        List<ListeAttenteEvenement> list = listeAttenteRepository.findByEvenementIdOrderByPositionAsc(evenementId);
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setPosition(i + 1);
            listeAttenteRepository.save(list.get(i));
        }
    }

    private String getNomEtudiant(Long idEtudiant) {
        UserInfoDto user = userApiClient.getUserById(idEtudiant);
        if (user == null) return "Invité";
        String first = user.getFirstName() != null ? user.getFirstName() : "";
        String last = user.getLastName() != null ? user.getLastName() : "";
        return (first + " " + last).trim().isEmpty() ? "Invité" : (first + " " + last).trim();
    }

    private String getEmailEtudiant(Long idEtudiant) {
        UserInfoDto user = userApiClient.getUserById(idEtudiant);
        return (user != null && user.getEmail() != null) ? user.getEmail() : "";
    }

    @Override
    @Transactional(readOnly = true)
    public List<InscriptionEvenement> getInscriptionsByEtudiant(Long idEtudiant) {
        return inscriptionRepository.findByIdEtudiant(idEtudiant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InscriptionEvenement> getInscriptionsByEvenement(Long evenementId) {
        return inscriptionRepository.findByEvenementId(evenementId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEtudiantInscrit(Long idEtudiant, Long evenementId) {
        return inscriptionRepository.existsByIdEtudiantAndEvenementId(idEtudiant, evenementId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEtudiantEnListeAttente(Long idEtudiant, Long evenementId) {
        return listeAttenteRepository.existsByIdEtudiantAndEvenementId(idEtudiant, evenementId);
    }

    @Override
    @Transactional(readOnly = true)
    public int getPositionListeAttente(Long idEtudiant, Long evenementId) {
        return listeAttenteRepository.findByIdEtudiantAndEvenementId(idEtudiant, evenementId)
                .map(ListeAttenteEvenement::getPosition)
                .orElse(0);
    }

    @Override
    @Transactional(readOnly = true)
    public StatutInscriptionDto getStatutInscription(Long idEtudiant, Long evenementId) {
        boolean inscrit = inscriptionRepository.existsByIdEtudiantAndEvenementId(idEtudiant, evenementId);
        boolean enListeAttente = listeAttenteRepository.existsByIdEtudiantAndEvenementId(idEtudiant, evenementId);
        int position = enListeAttente ? getPositionListeAttente(idEtudiant, evenementId) : 0;
        return new StatutInscriptionDto(inscrit, enListeAttente, position);
    }

    @Override
    @Transactional(readOnly = true)
    public long countInscriptionsByEvenement(Long evenementId) {
        return inscriptionRepository.countByEvenementId(evenementId);
    }
}
