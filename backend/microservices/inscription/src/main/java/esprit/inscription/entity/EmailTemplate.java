package esprit.inscription.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    @Builder.Default
    private String name = "";

    @Column(name = "display_name", nullable = false)
    @Builder.Default
    private String displayName = "";

    @Column(name = "description", columnDefinition = "TEXT")
    @Builder.Default
    private String description = "";

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    @Builder.Default
    private TemplateCategory category = TemplateCategory.WELCOME;

    @Column(name = "subject_template", nullable = false)
    @Builder.Default
    private String subjectTemplate = "";

    @Column(name = "html_content", columnDefinition = "LONGTEXT", nullable = false)
    @Builder.Default
    private String htmlContent = "";

    @Column(name = "text_content", columnDefinition = "TEXT")
    @Builder.Default
    private String textContent = "";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TemplateStatus status = TemplateStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    public enum TemplateCategory {
        WELCOME, COURSE_COMPLETION, REACTIVATION, LEVEL_PROGRESSION, TRIAL_EXPIRATION, PROMOTIONAL
    }

    public enum TemplateStatus {
        ACTIVE, INACTIVE, DRAFT, ARCHIVED
    }

    // Business methods
    public boolean isActive() {
        return status == TemplateStatus.ACTIVE;
    }
}
