package esprit.inscription.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_tracking")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email_id", nullable = false, unique = true)
    @Builder.Default
    private String emailId = ""; // UUID for tracking

    @Column(name = "user_id")
    @Builder.Default
    private Long userId = 0L;

    @Column(name = "email_address", nullable = false)
    @Builder.Default
    private String emailAddress = "";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private EmailCampaign campaign;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private EmailStatus status = EmailStatus.PENDING;

    @Column(name = "sent_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deliveredAt;

    @Column(name = "opened_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime openedAt;

    @Column(name = "clicked_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime clickedAt;

    @Column(name = "converted_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime convertedAt;

    @Column(name = "bounced_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime bouncedAt;

    @Column(name = "unsubscribed_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime unsubscribedAt;

    @Column(name = "open_count", columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer openCount = 0;

    @Column(name = "click_count", columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer clickCount = 0;

    @Column(name = "conversion_value", precision = 12, scale = 2)
    private BigDecimal conversionValue;

    @Column(name = "error_message")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    public enum EmailStatus {
        PENDING, QUEUED, SENT, DELIVERED, OPENED, CLICKED, CONVERTED, 
        BOUNCED, UNSUBSCRIBED, FAILED, CANCELLED
    }

    // Business methods
    public boolean isDelivered() {
        return status == EmailStatus.DELIVERED || 
               status == EmailStatus.OPENED || 
               status == EmailStatus.CLICKED || 
               status == EmailStatus.CONVERTED;
    }

    public boolean isOpened() {
        return status == EmailStatus.OPENED || 
               status == EmailStatus.CLICKED || 
               status == EmailStatus.CONVERTED;
    }

    public boolean isClicked() {
        return status == EmailStatus.CLICKED || 
               status == EmailStatus.CONVERTED;
    }

    public boolean isBounced() {
        return status == EmailStatus.BOUNCED;
    }

    public boolean isConverted() {
        return status == EmailStatus.CONVERTED;
    }

    public void incrementOpenCount() {
        this.openCount = (this.openCount == null ? 0 : this.openCount) + 1;
        this.openedAt = LocalDateTime.now();
        if (status.ordinal() < EmailStatus.OPENED.ordinal()) {
            this.status = EmailStatus.OPENED;
        }
    }

    public void incrementClickCount() {
        this.clickCount = (this.clickCount == null ? 0 : this.clickCount) + 1;
        this.clickedAt = LocalDateTime.now();
        if (status.ordinal() < EmailStatus.CLICKED.ordinal()) {
            this.status = EmailStatus.CLICKED;
        }
    }

    public void markAsConverted(BigDecimal value) {
        this.convertedAt = LocalDateTime.now();
        this.conversionValue = value;
        this.status = EmailStatus.CONVERTED;
    }
}
