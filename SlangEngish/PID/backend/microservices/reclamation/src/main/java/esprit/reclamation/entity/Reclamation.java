package esprit.reclamation.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "reclamations", indexes = @Index(name = "idx_reclamation_issue_key", columnList = "issueKey"))
public class Reclamation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le sujet est obligatoire")
    @Size(max = 100, message = "Le sujet ne doit pas depasser 100 caracteres")
    @Column(nullable = false, length = 100)
    private String sujet;

    /** Normalized subject for grouping "same issue" across students (lowercase, trimmed, single spaces). */
    @Column(length = 100)
    private String issueKey;

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

    @Column(nullable = false)
    private Boolean studentReported;

    @Column(nullable = false)
    private Boolean containsBadWords;

    @Size(max = 500, message = "La raison du signalement ne doit pas depasser 500 caracteres")
    @Column(length = 500)
    private String reportReason;

    private LocalDateTime reportedAt;

    /** LOW, MEDIUM, HIGH, CRITICAL — computed at save time */
    @Column(length = 20)
    private String urgencyLevel;

    /** Comma-separated tags: URGENT, BLOCKED, FRUSTRATED, ANXIOUS, NEUTRAL */
    @Column(length = 255)
    private String emotionTags;

    /** 0–100 — higher = treat first */
    private Integer priorityScore;

    /** Short human-readable summary for admin triage */
    @Column(length = 300)
    private String sentimentLabel;

    /** Catégorie prédite par le modèle RF (TF-IDF + SVD + Random Forest) — optionnel si service ML désactivé */
    @Column(length = 40)
    private String categorieMl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void prePersist() {
        if (this.statut == null || this.statut.isBlank()) {
            this.statut = "EN_ATTENTE";
        }
        this.notificationRead = true;
        this.studentReported = false;
        this.containsBadWords = false;
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

    public String getIssueKey() {
        return issueKey;
    }

    public void setIssueKey(String issueKey) {
        this.issueKey = issueKey;
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

    public Boolean getStudentReported() {
        return studentReported;
    }

    public void setStudentReported(Boolean studentReported) {
        this.studentReported = studentReported;
    }

    public Boolean getContainsBadWords() {
        return containsBadWords;
    }

    public void setContainsBadWords(Boolean containsBadWords) {
        this.containsBadWords = containsBadWords;
    }

    public String getReportReason() {
        return reportReason;
    }

    public void setReportReason(String reportReason) {
        this.reportReason = reportReason;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }

    public String getUrgencyLevel() {
        return urgencyLevel;
    }

    public void setUrgencyLevel(String urgencyLevel) {
        this.urgencyLevel = urgencyLevel;
    }

    public String getEmotionTags() {
        return emotionTags;
    }

    public void setEmotionTags(String emotionTags) {
        this.emotionTags = emotionTags;
    }

    public Integer getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(Integer priorityScore) {
        this.priorityScore = priorityScore;
    }

    public String getSentimentLabel() {
        return sentimentLabel;
    }

    public void setSentimentLabel(String sentimentLabel) {
        this.sentimentLabel = sentimentLabel;
    }

    public String getCategorieMl() {
        return categorieMl;
    }

    public void setCategorieMl(String categorieMl) {
        this.categorieMl = categorieMl;
    }
}
