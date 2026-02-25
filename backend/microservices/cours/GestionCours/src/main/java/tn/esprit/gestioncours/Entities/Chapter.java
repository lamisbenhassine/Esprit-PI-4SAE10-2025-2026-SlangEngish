package tn.esprit.gestioncours.Entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idChapter;

    private String name;

    private String description;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String pdfSupport;     // nullable

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String videoSupport;   // nullable

    @ManyToOne
    @JoinColumn(name = "idCourse")
    @JsonBackReference
    private Course course;

}
