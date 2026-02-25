package esprit.inscription.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "subscription_plan")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_type")
    private String planType;

    @Column(name = "name")
    private String name;

    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "currency", length = 8)
    private String currency;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "course_id")
    private Long courseId;

    // Use MEDIUMTEXT to safely store base64 images (up to 16MB)
    @Column(name = "image_url", columnDefinition = "MEDIUMTEXT")
    private String imageUrl;
}
