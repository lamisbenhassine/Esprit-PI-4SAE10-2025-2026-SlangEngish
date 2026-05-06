package org.example.club.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "post_club_reactions",
        uniqueConstraints = @UniqueConstraint(name = "uk_post_reaction_user", columnNames = {"post_id", "id_etudiant"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private PostClub post;

    @Column(name = "id_etudiant", nullable = false)
    private Long idEtudiant;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false, length = 24)
    private PostReactionType reactionType;
}
