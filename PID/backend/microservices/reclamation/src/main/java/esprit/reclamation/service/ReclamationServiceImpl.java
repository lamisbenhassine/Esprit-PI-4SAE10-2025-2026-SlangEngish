package esprit.reclamation.service;

import esprit.reclamation.entity.Reclamation;
import esprit.reclamation.repository.ReclamationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReclamationServiceImpl implements ReclamationService {

    private final ReclamationRepository reclamationRepository;

    public ReclamationServiceImpl(ReclamationRepository reclamationRepository) {
        this.reclamationRepository = reclamationRepository;
    }

    @Override
    public Reclamation create(Reclamation reclamation) {
        if (reclamation.getStudentId() == null) {
            throw new RuntimeException("studentId est obligatoire");
        }
        return reclamationRepository.save(reclamation);
    }

    @Override
    public List<Reclamation> getAll() {
        return reclamationRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public List<Reclamation> getByStudentId(Long studentId) {
        return reclamationRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
    }

    @Override
    public List<Reclamation> getUnreadNotifications(Long studentId) {
        return reclamationRepository.findByStudentIdAndNotificationReadFalseAndReponseAdminIsNotNullOrderByCreatedAtDesc(studentId);
    }

    @Override
    public Reclamation getById(Long id) {
        return reclamationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reclamation introuvable avec l'id : " + id));
    }

    @Override
    public Reclamation update(Long id, Reclamation reclamation) {
        Reclamation existing = getById(id);
        existing.setSujet(reclamation.getSujet());
        existing.setDescription(reclamation.getDescription());
        if (reclamation.getStudentId() != null) {
            existing.setStudentId(reclamation.getStudentId());
        }
        return reclamationRepository.save(existing);
    }

    @Override
    public Reclamation traiterParAdmin(Long id, String statut, String reponseAdmin) {
        Reclamation existing = getById(id);
        if (statut == null || statut.isBlank()) {
            throw new RuntimeException("Le statut est obligatoire");
        }
        if (reponseAdmin == null || reponseAdmin.isBlank()) {
            throw new RuntimeException("La reponse admin est obligatoire");
        }
        existing.setStatut(statut);
        existing.setReponseAdmin(reponseAdmin.trim());
        existing.setNotificationRead(false);
        return reclamationRepository.save(existing);
    }

    @Override
    public Reclamation markNotificationAsRead(Long id) {
        Reclamation existing = getById(id);
        existing.setNotificationRead(true);
        return reclamationRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        Reclamation existing = getById(id);
        reclamationRepository.delete(existing);
    }
}
