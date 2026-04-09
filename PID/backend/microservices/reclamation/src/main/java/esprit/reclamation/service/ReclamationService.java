package esprit.reclamation.service;

import esprit.reclamation.entity.Reclamation;

import java.util.List;

public interface ReclamationService {
    Reclamation create(Reclamation reclamation);

    List<Reclamation> getAll();
    List<Reclamation> getByStudentId(Long studentId);
    List<Reclamation> getUnreadNotifications(Long studentId);

    Reclamation getById(Long id);

    Reclamation update(Long id, Reclamation reclamation);
    Reclamation traiterParAdmin(Long id, String statut, String reponseAdmin);
    Reclamation markNotificationAsRead(Long id);

    void delete(Long id);
}
