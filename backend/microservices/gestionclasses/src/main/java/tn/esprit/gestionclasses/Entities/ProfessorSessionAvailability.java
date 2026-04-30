package tn.esprit.gestionclasses.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "professor_session_availability",
        uniqueConstraints = @UniqueConstraint(columnNames = {"professor_user_id", "slot_code"})
)
@Getter
@Setter
@NoArgsConstructor
public class ProfessorSessionAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "professor_user_id", nullable = false)
    private Long professorUserId;

    @Column(name = "slot_code", nullable = false, length = 24)
    private String slotCode;

    /** Plan / contenu prévu pour la séance (optionnel). */
    @Column(name = "description", length = 2000)
    private String description;
}
