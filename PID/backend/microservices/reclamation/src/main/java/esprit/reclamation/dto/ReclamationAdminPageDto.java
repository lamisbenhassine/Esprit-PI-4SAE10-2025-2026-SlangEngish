package esprit.reclamation.dto;

import esprit.reclamation.entity.Reclamation;

import java.util.List;

/**
 * Paginated admin list with global status counts (all rows in DB, not only current page).
 */
public record ReclamationAdminPageDto(
        List<Reclamation> content,
        long totalElements,
        int totalPages,
        int number,
        int size,
        long pendingTotal,
        long inProgressTotal,
        long processedTotal
) {
}
