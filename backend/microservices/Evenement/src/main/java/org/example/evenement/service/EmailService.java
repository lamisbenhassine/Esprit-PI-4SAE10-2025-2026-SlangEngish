package org.example.evenement.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.example.evenement.sentiment.FeedbackSentiment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:azzouzo317@gmail.com}")
    private String fromEmail;

    @Value("${app.admin.alert-email:azzouzo317@gmail.com}")
    private String adminAlertEmail;

    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (Exception e) {
            // Log and ignore in dev if mail not configured
            System.err.println("Erreur envoi email: " + e.getMessage());
        }
    }

    /**
     * Alerte admin : avis événement à sentiment négatif ou note très basse.
     */
    public void sendNegativeFeedbackAlertEvenement(
            String titreEvenement,
            Long evenementId,
            Long idEtudiant,
            String nomEtudiant,
            Integer note,
            String commentaire,
            FeedbackSentiment sentiment) {
        if (adminAlertEmail == null || adminAlertEmail.isBlank()) {
            return;
        }
        String subject = "[ALERTE AVIS — Événement] Sentiment " + sentiment + " — " + safe(titreEvenement);
        String body = "Un avis nécessite votre attention.\n\n"
                + "Type : Événement\n"
                + "Événement : " + safe(titreEvenement) + " (id=" + evenementId + ")\n"
                + "Étudiant : " + safe(nomEtudiant) + " (id=" + idEtudiant + ")\n"
                + "Note : " + note + " / 5\n"
                + "Analyse sentiment : " + sentiment + "\n"
                + "Commentaire :\n" + safe(commentaire) + "\n\n"
                + "---\nPlateforme PID — analyse automatique (mots-clés + note).";
        sendSimpleEmail(adminAlertEmail, subject, body);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    public void sendInscriptionConfirmation(String toEmail, String nomEtudiant, String titreEvenement, String dateEvenement, String lieu, String invitationUrl) {
        String subject = "Confirmation d'inscription - " + titreEvenement;
        String text = "Bonjour " + nomEtudiant + ",\n\n"
                + "Votre inscription à l'événement \"" + titreEvenement + "\" est confirmée.\n\n"
                + "Date : " + dateEvenement + "\n"
                + (lieu != null && !lieu.isEmpty() ? "Lieu : " + lieu + "\n" : "")
                + "\nVotre invitation personnalisée (QR code) : " + invitationUrl + "\n\n"
                + "À bientôt !";
        sendSimpleEmail(toEmail, subject, text);
    }

    public void sendListeAttente(String toEmail, String nomEtudiant, String titreEvenement, int position) {
        String subject = "Liste d'attente - " + titreEvenement;
        String text = "Bonjour " + nomEtudiant + ",\n\n"
                + "L'événement \"" + titreEvenement + "\" est complet. Vous avez été ajouté(e) en liste d'attente (position " + position + ").\n\n"
                + "Vous serez informé(e) par email si une place se libère.\n\n"
                + "Cordialement.";
        sendSimpleEmail(toEmail, subject, text);
    }

    public void sendPromotionFromListeAttente(String toEmail, String nomEtudiant, String titreEvenement, String dateEvenement, String lieu, String invitationUrl) {
        String subject = "Place disponible - " + titreEvenement;
        String text = "Bonjour " + nomEtudiant + ",\n\n"
                + "Une place s'est libérée pour l'événement \"" + titreEvenement + "\". Vous êtes maintenant inscrit(e).\n\n"
                + "Date : " + dateEvenement + "\n"
                + (lieu != null && !lieu.isEmpty() ? "Lieu : " + lieu + "\n" : "")
                + "\nVotre invitation personnalisée : " + invitationUrl + "\n\n"
                + "À bientôt !";
        sendSimpleEmail(toEmail, subject, text);
    }
}
