package esprit.reclamation.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "reclamations")
public class Reclamation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le sujet est obligatoire")
    @Size(max = 100, message = "Le sujet ne doit pas depasser 100 caracteres")
    @Column(nullable = false, length = 100)
    private String sujet;

    @NotBlank(message = "La description est obligatoire")
    @Size(max = 1000, message = "La description ne doit pas depasser 1000 caracteres")
    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false, length = 30)
    private String statut;

    @Column(nullable = false)
    private Long studentId;

    @Size(max = 1000, message = "La reponse admin ne doit pas depasser 1000 caracteres")
    @Column(length = 1000)
    private String reponseAdmin;

    @Column(nullable = false)
    private Boolean notificationRead;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void prePersist() {
        if (this.statut == null || this.statut.isBlank()) {
            this.statut = "EN_ATTENTE";
        }
        this.notificationRead = true;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSujet() {
        return sujet;
    }

    public void setSujet(String sujet) {
        this.sujet = sujet;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getReponseAdmin() {
        return reponseAdmin;
    }

    public void setReponseAdmin(String reponseAdmin) {
        this.reponseAdmin = reponseAdmin;
    }

    public Boolean getNotificationRead() {
        return notificationRead;
    }

    public void setNotificationRead(Boolean notificationRead) {
        this.notificationRead = notificationRead;
    }
}
