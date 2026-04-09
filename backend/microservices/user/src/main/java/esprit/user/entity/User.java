package esprit.user.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** STUDENT (défaut), TUTOR (formateur), ou ADMIN (équipe / publication officielle). */
    @Column(name = "account_role", nullable = false, length = 20)
    @Builder.Default
    private String accountRole = "STUDENT";

    @Column(name = "english_level")
    @Builder.Default
    private String englishLevel = "A1"; // A1, A2, B1, B2, C1, C2

    @Column(name = "subscription_status")
    @Builder.Default
    private String subscriptionStatus = "TRIAL"; // TRIAL, ACTIVE, EXPIRED, CANCELLED

    @PrePersist
    public void prePersist() {
        if (accountRole == null || accountRole.isBlank()) {
            accountRole = "STUDENT";
        }
    }
}

