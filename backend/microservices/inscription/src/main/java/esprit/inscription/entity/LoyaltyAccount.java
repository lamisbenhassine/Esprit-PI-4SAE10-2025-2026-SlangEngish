package esprit.inscription.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "loyalty_account")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true, nullable = false)
    private Long userId;

    /**
     * Points actuellement disponibles (non utilisés).
     */
    @Column(name = "balance_points")
    private Long balancePoints;

    /**
     * Total de points gagnés depuis la création du compte (sert aux paliers silver / gold).
     */
    @Column(name = "lifetime_points")
    private Long lifetimePoints;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (balancePoints == null) balancePoints = 0L;
        if (lifetimePoints == null) lifetimePoints = 0L;
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}

