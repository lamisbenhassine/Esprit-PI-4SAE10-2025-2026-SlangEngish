package esprit.reclamation.service;

import esprit.reclamation.dto.BackfillMlResponseDto;
import esprit.reclamation.dto.ReclamationAdminPageDto;
import esprit.reclamation.entity.Reclamation;

import java.util.List;

public interface ReclamationService {
    Reclamation create(Reclamation reclamation);

    ReclamationAdminPageDto getAdminPage(int page, int size, String categorieMl, String sort);

    List<String> getDistinctMlCategories();

    /** Recalcule categorieMl via le service Python pour les lignes encore vides (max limit). */
    BackfillMlResponseDto backfillMlCategories(int limit);

    List<Reclamation> getByStudentId(Long studentId);
    List<Reclamation> getUnreadNotifications(Long studentId);

    Reclamation getById(Long id);

    Reclamation update(Long id, Reclamation reclamation);
    Reclamation traiterParAdmin(Long id, String statut, String reponseAdmin);
    Reclamation markNotificationAsRead(Long id);
    Reclamation reportStudent(Long id, String reportReason);
    Reclamation unblockStudent(Long id);

    void delete(Long id);
}
