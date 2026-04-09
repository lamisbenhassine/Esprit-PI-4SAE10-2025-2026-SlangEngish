package esprit.reclamation.controller;

import esprit.reclamation.dto.AdminReponseRequest;
import esprit.reclamation.dto.ChatbotAssistRequest;
import esprit.reclamation.dto.ChatbotAssistResponse;
import esprit.reclamation.entity.Reclamation;
import esprit.reclamation.service.ReclamationChatbotService;
import esprit.reclamation.service.ReclamationService;
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

@RestController
@RequestMapping("/api/reclamations")
public class ReclamationController {

    private final ReclamationService reclamationService;
    private final ReclamationChatbotService reclamationChatbotService;

    public ReclamationController(ReclamationService reclamationService, ReclamationChatbotService reclamationChatbotService) {
        this.reclamationService = reclamationService;
        this.reclamationChatbotService = reclamationChatbotService;
    }

    @GetMapping("/health")
    public String health() {
        return "Reclamation service is running";
    }

    @PostMapping
    public ResponseEntity<Reclamation> create(@Valid @RequestBody Reclamation reclamation) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reclamationService.create(reclamation));
    }

    @GetMapping
    public ResponseEntity<List<Reclamation>> getAll(@RequestParam(required = false) Long studentId) {
        if (studentId != null) {
            return ResponseEntity.ok(reclamationService.getByStudentId(studentId));
        }
        return ResponseEntity.ok(reclamationService.getAll());
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<Reclamation>> getUnreadNotifications(@RequestParam Long studentId) {
        return ResponseEntity.ok(reclamationService.getUnreadNotifications(studentId));
    }

    @PostMapping("/chatbot/assist")
    public ResponseEntity<ChatbotAssistResponse> chatbotAssist(@Valid @RequestBody ChatbotAssistRequest request) {
        return ResponseEntity.ok(reclamationChatbotService.assist(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reclamation> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reclamationService.getById(id));
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reclamationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
