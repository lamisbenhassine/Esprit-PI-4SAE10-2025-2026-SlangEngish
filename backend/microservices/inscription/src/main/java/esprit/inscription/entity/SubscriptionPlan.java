package esprit.inscription.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_plan")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_type")
    @Builder.Default
    private String planType = "BASIC";

    @Column(name = "name")
    @Builder.Default
    private String name = "Basic Plan";

    @Column(name = "price", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "currency", length = 8)
    @Builder.Default
    private String currency = "EUR";

    @Column(name = "duration_days")
    @Builder.Default
    private Integer durationDays = 30;

    @Column(name = "description", columnDefinition = "TEXT")
    @Builder.Default
    private String description = "";

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Builder.Default
    private LocalDate date = LocalDate.now();

    @Column(name = "user_id")
    @Builder.Default
    private Long userId = 0L;

    @Column(name = "course_id")
    @Builder.Default
    private Long courseId = 0L;

    // Use MEDIUMTEXT to safely store base64 images (up to 16MB)
    @Column(name = "image_url", columnDefinition = "MEDIUMTEXT")
    @Builder.Default
    private String imageUrl = "";
    
    @Column(name = "is_active", columnDefinition = "BOOLEAN DEFAULT TRUE")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "trial_ends_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime trialEndsAt;
    
    @Column(name = "created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @JsonIgnore
    private User user;
}
