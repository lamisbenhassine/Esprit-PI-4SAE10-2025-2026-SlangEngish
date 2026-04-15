package esprit.reclamation.controller;

import esprit.reclamation.dto.BackfillMlResponseDto;
import esprit.reclamation.dto.ReclamationAdminPageDto;
import esprit.reclamation.dto.AdminReponseRequest;
import esprit.reclamation.dto.ChatbotAssistRequest;
import esprit.reclamation.dto.ChatbotAssistResponse;
import esprit.reclamation.dto.ReportStudentRequest;
import esprit.reclamation.dto.StudentBlockStatusResponse;
import esprit.reclamation.entity.Reclamation;
import esprit.reclamation.repository.StudentReclamationBlockRepository;
import esprit.reclamation.service.ReclamationChatbotService;
import esprit.reclamation.service.ReclamationService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/reclamations")
public class ReclamationController {

    private static <T> ResponseEntity<T> okNoStore(T body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .body(body);
    }

    private final ReclamationService reclamationService;
    private final ReclamationChatbotService reclamationChatbotService;
    private final StudentReclamationBlockRepository studentReclamationBlockRepository;

    public ReclamationController(ReclamationService reclamationService, ReclamationChatbotService reclamationChatbotService,
                                 StudentReclamationBlockRepository studentReclamationBlockRepository) {
        this.reclamationService = reclamationService;
        this.reclamationChatbotService = reclamationChatbotService;
        this.studentReclamationBlockRepository = studentReclamationBlockRepository;
    }

    @GetMapping("/health")
    public String health() {
        return "Reclamation service is running";
    }

    @PostMapping
    public ResponseEntity<Reclamation> create(@Valid @RequestBody Reclamation reclamation) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .body(reclamationService.create(reclamation));
    }

    /**
     * Paginated list for back-office.
     * @param categorieMl optional filter (exact match on ML category); omit or empty = all
     * @param sort {@code priority} (default) or {@code mlCategory} (alphabetical by ML category)
     */
    @GetMapping("/admin")
    public ResponseEntity<ReclamationAdminPageDto> getAdminPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String categorieMl,
            @RequestParam(defaultValue = "priority") String sort) {
        return okNoStore(reclamationService.getAdminPage(page, size, categorieMl, sort));
    }

    /** Distinct ML categories for admin filter dropdown. */
    @GetMapping("/admin/ml-categories")
    public ResponseEntity<List<String>> getAdminMlCategories() {
        return okNoStore(reclamationService.getDistinctMlCategories());
    }

    /**
     * Recalcule {@code categorieMl} pour les réclamations qui n’en ont pas encore (jusqu’à {@code limit}).
     * Nécessite le service Python actif et {@code reclamation.ml.base-url} configuré.
     */
    @PostMapping("/admin/backfill-ml")
    public ResponseEntity<BackfillMlResponseDto> backfillMl(
            @RequestParam(defaultValue = "100") int limit) {
        return okNoStore(reclamationService.backfillMlCategories(limit));
    }

    /** Student: all own reclamations (no pagination). */
    @GetMapping
    public ResponseEntity<List<Reclamation>> listForStudent(@RequestParam Long studentId) {
        return okNoStore(reclamationService.getByStudentId(studentId));
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<Reclamation>> getUnreadNotifications(@RequestParam Long studentId) {
        return okNoStore(reclamationService.getUnreadNotifications(studentId));
    }

    @PostMapping("/chatbot/assist")
    public ResponseEntity<ChatbotAssistResponse> chatbotAssist(@Valid @RequestBody ChatbotAssistRequest request) {
        return ResponseEntity.ok(reclamationChatbotService.assist(request));
    }

    @GetMapping("/students/{studentId}/block-status")
    public ResponseEntity<StudentBlockStatusResponse> getStudentBlockStatus(@PathVariable Long studentId) {
        LocalDateTime now = LocalDateTime.now();
        return studentReclamationBlockRepository.findByStudentId(studentId)
                .map(block -> {
                    boolean blocked = block.getBlockedUntil() != null && block.getBlockedUntil().isAfter(now);
                    String reason = blocked
                            ? "You were reported because of your language and cannot create a new reclamation for 3 days. Please consult the administration."
                            : "";
                    return okNoStore(new StudentBlockStatusResponse(
                            blocked,
                            block.getBlockedUntil() == null ? null : block.getBlockedUntil().toString(),
                            reason
                    ));
                })
                .orElse(okNoStore(new StudentBlockStatusResponse(false, null, "")));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reclamation> getById(@PathVariable Long id) {
        return okNoStore(reclamationService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Reclamation> update(@PathVariable Long id, @Valid @RequestBody Reclamation reclamation) {
        return ResponseEntity.ok(reclamationService.update(id, reclamation));
    }

    @PutMapping("/{id}/traitement")
    public ResponseEntity<Reclamation> traiterParAdmin(@PathVariable Long id, @Valid @RequestBody AdminReponseRequest request) {
        return ResponseEntity.ok(reclamationService.traiterParAdmin(id, request.getStatut(), request.getReponseAdmin()));
    }

    @PutMapping("/{id}/notifications/read")
    public ResponseEntity<Reclamation> markNotificationAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(reclamationService.markNotificationAsRead(id));
    }

    @PutMapping("/{id}/report-student")
    public ResponseEntity<Reclamation> reportStudent(@PathVariable Long id, @Valid @RequestBody ReportStudentRequest request) {
        return ResponseEntity.ok(reclamationService.reportStudent(id, request.getReportReason()));
    }

    @PutMapping("/{id}/unblock-student")
    public ResponseEntity<Reclamation> unblockStudent(@PathVariable Long id) {
        return ResponseEntity.ok(reclamationService.unblockStudent(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reclamationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
