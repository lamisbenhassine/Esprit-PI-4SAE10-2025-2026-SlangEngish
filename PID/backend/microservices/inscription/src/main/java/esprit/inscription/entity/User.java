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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "english_level")
    @Builder.Default
    private String englishLevel = "A1"; // A1, A2, B1, B2, C1, C2

    @Column(name = "subscription_status")
    @Builder.Default
    private String subscriptionStatus = "TRIAL"; // TRIAL, ACTIVE, EXPIRED, CANCELLED

    @Column(name = "trial_ends_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime trialEndsAt;

    @Column(name = "total_revenue", precision = 12, scale = 2)
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(name = "completed_lessons")
    @Builder.Default
    private Integer completedLessons = 0;

    @Column(name = "total_lessons")
    @Builder.Default
    private Integer totalLessons = 0;

    @Column(name = "is_active", columnDefinition = "BOOLEAN DEFAULT TRUE")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_login_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginAt;

    @Column(name = "phone")
    @Builder.Default
    private String phone = "";

    @Column(name = "country")
    @Builder.Default
    private String country = "";

    @Column(name = "city")
    @Builder.Default
    private String city = "";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SubscriptionPlan> subscriptionPlans = new java.util.ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Cart> carts = new java.util.ArrayList<>();

    // Business methods
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isTrialActive() {
        if (trialEndsAt == null) return false;
        return LocalDateTime.now().isBefore(trialEndsAt);
    }

    public boolean isSubscriptionActive() {
        return "ACTIVE".equals(subscriptionStatus);
    }

    public boolean isPremium() {
        return isSubscriptionActive() || isTrialActive();
    }

    public int getCompletionRate() {
        if (totalLessons == 0) return 0;
        return (completedLessons * 100) / totalLessons;
    }

    public boolean isRecentlyActive() {
        if (lastLoginAt == null) return false;
        return lastLoginAt.isAfter(LocalDateTime.now().minusDays(7));
    }

    public boolean isInactive() {
        if (lastLoginAt == null) return true;
        return lastLoginAt.isBefore(LocalDateTime.now().minusDays(30));
    }

    public boolean isBeginner() {
        return "A1".equals(englishLevel) || "A2".equals(englishLevel);
    }

    public boolean isIntermediate() {
        return "B1".equals(englishLevel) || "B2".equals(englishLevel);
    }

    public boolean isAdvanced() {
        return "C1".equals(englishLevel) || "C2".equals(englishLevel);
    }

    public int getDaysUntilTrialExpiry() {
        if (trialEndsAt == null) return -1;
        return (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), trialEndsAt);
    }

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = true;
        }
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }
        if (completedLessons == null) {
            completedLessons = 0;
        }
        if (totalLessons == null) {
            totalLessons = 0;
        }
    }
}
