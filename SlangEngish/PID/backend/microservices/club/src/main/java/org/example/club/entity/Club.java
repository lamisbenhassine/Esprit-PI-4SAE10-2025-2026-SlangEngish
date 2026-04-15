package org.example.club.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "clubs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    private String description;
    private String type;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String image;
    private String statut;

    @Column(name = "date_creation")
    private LocalDate dateCreation;

    @Column(name = "id_responsable")
    private Long idResponsable;

    /**
     * Départements du club (format CSV): RH,Marketing,Technique...
     */
    @Column(name = "departements", columnDefinition = "TEXT")
    private String departements;

    @OneToMany(mappedBy = "club", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<org.example.club.entity.ReunionClub> reunions;

    @OneToMany(mappedBy = "club", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<org.example.club.entity.PostClub> posts;

}
