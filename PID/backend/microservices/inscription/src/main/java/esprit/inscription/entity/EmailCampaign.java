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
import java.util.List;

@Entity
@Table(name = "email_campaigns")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    @Builder.Default
    private String name = "";

    @Column(name = "description", columnDefinition = "TEXT")
    @Builder.Default
    private String description = "";

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    @Builder.Default
    private CampaignCategory category = CampaignCategory.WELCOME;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Column(name = "target_level")
    @Builder.Default
    private String targetLevel = "A1"; // A1, A2, B1, B2, C1, C2

    @Column(name = "subject")
    @Builder.Default
    private String subject = "";

    @Column(name = "from_email")
    @Builder.Default
    private String fromEmail = "";

    @Column(name = "from_name")
    @Builder.Default
    private String fromName = "";

    @Column(name = "scheduled_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledAt;

    @Column(name = "sent_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sentAt;

    @Column(name = "completed_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;

    @Column(name = "total_recipients")
    @Builder.Default
    private Integer totalRecipients = 0;

    @Column(name = "sent_count")
    @Builder.Default
    private Integer sentCount = 0;

    @Column(name = "opened_count")
    @Builder.Default
    private Integer openedCount = 0;

    @Column(name = "clicked_count")
    @Builder.Default
    private Integer clickedCount = 0;

    @Column(name = "converted_count")
    @Builder.Default
    private Integer convertedCount = 0;

    @Column(name = "bounced_count")
    @Builder.Default
    private Integer bouncedCount = 0;

    @Column(name = "unsubscribed_count")
    @Builder.Default
    private Integer unsubscribedCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EmailTracking> tracking = new java.util.ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private EmailTemplate template;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = CampaignStatus.DRAFT;
        }
    }

    public enum CampaignCategory {
        WELCOME, COURSE_COMPLETION, REACTIVATION, LEVEL_PROGRESSION, TRIAL_EXPIRATION, PROMOTIONAL
    }

    public enum CampaignStatus {
        DRAFT, SCHEDULED, SENDING, SENT, COMPLETED, PAUSED, CANCELLED, FAILED
    }

    // Business methods
    public double getOpenRate() {
        if (sentCount == 0) return 0.0;
        return (double) openedCount / sentCount * 100;
    }

    public double getClickRate() {
        if (sentCount == 0) return 0.0;
        return (double) clickedCount / sentCount * 100;
    }

    public double getConversionRate() {
        if (sentCount == 0) return 0.0;
        return (double) convertedCount / sentCount * 100;
    }

    public boolean isCompleted() {
        return status == CampaignStatus.COMPLETED || 
               status == CampaignStatus.CANCELLED || 
               status == CampaignStatus.FAILED;
    }

    public boolean isActive() {
        return status == CampaignStatus.SCHEDULED || 
               status == CampaignStatus.SENDING;
    }
}
