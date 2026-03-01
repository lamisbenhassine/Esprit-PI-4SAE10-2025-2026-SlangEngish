package esprit.inscription.controller;

import esprit.inscription.entity.EmailTemplate;
import esprit.inscription.entity.User;
import esprit.inscription.service.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/inscription/email-templates")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class EmailTemplateController {

    private final EmailTemplateService templateService;

    @GetMapping
    public ResponseEntity<List<EmailTemplate>> getAllTemplates() {
        log.info("Getting all email templates");
        List<EmailTemplate> templates = templateService.getAllTemplates();
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/active")
    public ResponseEntity<List<EmailTemplate>> getActiveTemplates() {
        log.info("Getting active email templates");
        List<EmailTemplate> templates = templateService.getActiveTemplates();
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailTemplate> getTemplateById(@PathVariable Long id) {
        log.info("Getting template by ID: {}", id);
        EmailTemplate template = templateService.getTemplateById(id);
        return ResponseEntity.ok(template);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<EmailTemplate>> getTemplatesByCategory(@PathVariable EmailTemplate.TemplateCategory category) {
        log.info("Getting templates by category: {}", category);
        List<EmailTemplate> templates = templateService.getTemplatesByCategory(category);
        return ResponseEntity.ok(templates);
    }

    @PostMapping
    public ResponseEntity<EmailTemplate> createTemplate(@Valid @RequestBody EmailTemplate template) {
        log.info("Creating new email template: {}", template.getName());
        try {
            EmailTemplate createdTemplate = templateService.createTemplate(template);
            return ResponseEntity.status(201).body(createdTemplate);
        } catch (Exception e) {
            log.error("Failed to create email template", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmailTemplate> updateTemplate(@PathVariable Long id, @Valid @RequestBody EmailTemplate template) {
        log.info("Updating email template: {}", id);
        try {
            EmailTemplate updatedTemplate = templateService.updateTemplate(id, template);
            return ResponseEntity.ok(updatedTemplate);
        } catch (Exception e) {
            log.error("Failed to update email template", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        log.info("Deleting email template: {}", id);
        try {
            templateService.deleteTemplate(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Failed to delete email template", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/create-defaults")
    public ResponseEntity<List<EmailTemplate>> createDefaultTemplates() {
        log.info("Creating default email templates");
        try {
            List<EmailTemplate> templates = templateService.createDefaultTemplates();
            return ResponseEntity.status(201).body(templates);
        } catch (Exception e) {
            log.error("Failed to create default templates", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/preview/{id}")
    public ResponseEntity<String> previewTemplate(@PathVariable Long id, @RequestBody PreviewRequest request) {
        log.info("Previewing template: {}", id);
        try {
            EmailTemplate template = templateService.getTemplateById(id);
            
            // Create a mock user for preview
            User mockUser = User.builder()
                    .firstName(request.getFirstName() != null ? request.getFirstName() : "John")
                    .lastName(request.getLastName() != null ? request.getLastName() : "Doe")
                    .email(request.getEmail() != null ? request.getEmail() : "john@example.com")
                    .englishLevel(request.getEnglishLevel() != null ? request.getEnglishLevel() : "B1")
                    .build();
            
            String personalizedContent = templateService.personalizeContent(template, mockUser);
            return ResponseEntity.ok(personalizedContent);
        } catch (Exception e) {
            log.error("Failed to preview template", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // DTOs
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class PreviewRequest {
        private String firstName;
        private String lastName;
        private String email;
        private String englishLevel;
    }
}
