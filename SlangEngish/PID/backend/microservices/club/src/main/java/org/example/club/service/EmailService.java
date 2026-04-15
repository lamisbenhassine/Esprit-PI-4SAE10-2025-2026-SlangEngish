package org.example.club.service;

import org.example.club.client.UserInfoDto;
import org.example.club.entity.Club;
import org.example.club.entity.ReunionClub;
import org.example.club.entity.TypeReunion;
import org.example.club.sentiment.FeedbackSentiment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:}")
    private String fromEmail;

    @Value("${app.admin.alert-email:azzouzo317@gmail.com}")
    private String adminAlertEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Alerte admin : avis club à sentiment négatif ou note très basse.
     */
    public void sendNegativeFeedbackAlertClub(
            String nomClub,
            Long clubId,
            Long idEtudiant,
            String nomCompletEtudiant,
            Integer note,
            String commentaire,
            FeedbackSentiment sentiment) {
        if (adminAlertEmail == null || adminAlertEmail.isBlank()) {
            return;
        }
        String subject = "[ALERTE AVIS — Club] Sentiment " + sentiment + " — " + safe(nomClub);
        String body = "Un avis nécessite votre attention.\n\n"
                + "Type : Club\n"
                + "Club : " + safe(nomClub) + " (id=" + clubId + ")\n"
                + "Étudiant : " + safe(nomCompletEtudiant) + " (id=" + idEtudiant + ")\n"
                + "Note : " + note + " / 5\n"
                + "Analyse sentiment : " + sentiment + "\n"
                + "Commentaire :\n" + safe(commentaire) + "\n\n"
                + "---\nPlateforme PID — analyse automatique (mots-clés + note).";
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            if (fromEmail != null && !fromEmail.isBlank()) {
                helper.setFrom(fromEmail);
            }
            helper.setTo(adminAlertEmail);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
        } catch (Exception ignored) {
            System.err.println("Erreur envoi alerte admin avis club: " + ignored.getMessage());
        }
    }

    public void sendAcceptanceBadgeEmail(UserInfoDto user, Club club) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank() || club == null) {
            return;
        }

        String subject = "Felicitation - Demande acceptee pour le club " + safe(club.getNom());
        String html = buildBadgeHtml(user, club);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            if (fromEmail != null && !fromEmail.isBlank()) {
                helper.setFrom(fromEmail);
            }
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception ignored) {
            // Ne pas bloquer l'acceptation si l'email echoue.
        }
    }

    public void sendReunionInvitationEmail(UserInfoDto user, Club club, ReunionClub reunion) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank() || club == null || reunion == null) {
            return;
        }

        String subject = "Invitation reunion - Club " + safe(club.getNom());
        String html = buildReunionHtml(user, club, reunion);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            if (fromEmail != null && !fromEmail.isBlank()) {
                helper.setFrom(fromEmail);
            }
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception ignored) {
            // L'envoi d'email ne doit pas bloquer la creation de reunion.
        }
    }

    private String buildBadgeHtml(UserInfoDto user, Club club) {
        String fullName = (safe(user.getFirstName()) + " " + safe(user.getLastName())).trim();
        return "<!doctype html>"
                + "<html><body style='font-family:Arial,sans-serif;background:#f5f7fb;padding:20px;'>"
                + "<div style='max-width:680px;margin:0 auto;background:#fff;border:1px solid #e6eaf2;border-radius:12px;padding:24px;'>"
                + "<h2 style='margin:0 0 8px;color:#1f3c88;'>Bienvenue dans le club !</h2>"
                + "<p style='margin:0 0 16px;color:#333;'>Votre demande a ete acceptee par l'administrateur.</p>"
                + "<div style='border:2px dashed #1f3c88;border-radius:12px;padding:16px;background:#f9fbff;'>"
                + "<h3 style='margin:0 0 12px;color:#1f3c88;'>Badge Membre</h3>"
                + "<p style='margin:6px 0;'><strong>Nom complet :</strong> " + escape(fullName) + "</p>"
                + "<p style='margin:6px 0;'><strong>ID Etudiant :</strong> " + user.getId() + "</p>"
                + "<p style='margin:6px 0;'><strong>Email :</strong> " + escape(safe(user.getEmail())) + "</p>"
                + "<p style='margin:6px 0;'><strong>Club :</strong> " + escape(safe(club.getNom())) + "</p>"
                + "<p style='margin:6px 0;'><strong>Date d'activation :</strong> " + LocalDate.now() + "</p>"
                + "</div>"
                + "<p style='margin:16px 0 0;color:#555;'>Vous pouvez maintenant acceder a la discussion interne du club.</p>"
                + "</div></body></html>";
    }

    private String buildReunionHtml(UserInfoDto user, Club club, ReunionClub reunion) {
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter hourFmt = DateTimeFormatter.ofPattern("HH:mm");
        String date = reunion.getDate() != null ? reunion.getDate().format(dateFmt) : "-";
        String heure = reunion.getHeure() != null ? reunion.getHeure().format(hourFmt) : "-";
        boolean online = reunion.getTypeReunion() == TypeReunion.EN_LIGNE;

        String details = online
                ? "<p style='margin:6px 0;'><strong>Lien reunion :</strong> <a href='" + escapeAttr(safe(reunion.getLienReunion()))
                + "'>" + escape(safe(reunion.getLienReunion())) + "</a></p>"
                : "<p style='margin:6px 0;'><strong>Lieu :</strong> " + escape(safe(reunion.getLieu())) + "</p>";

        return "<!doctype html>"
                + "<html><body style='font-family:Arial,sans-serif;background:#f5f7fb;padding:20px;'>"
                + "<div style='max-width:680px;margin:0 auto;background:#fff;border:1px solid #e6eaf2;border-radius:12px;padding:24px;'>"
                + "<h2 style='margin:0 0 8px;color:#1f3c88;'>Nouvelle reunion du club</h2>"
                + "<p style='margin:0 0 16px;color:#333;'>Bonjour " + escape(safe(user.getFirstName())) + ", une reunion est planifiee.</p>"
                + "<div style='border:1px solid #dbe4ff;border-radius:12px;padding:16px;background:#f9fbff;'>"
                + "<p style='margin:6px 0;'><strong>Club :</strong> " + escape(safe(club.getNom())) + "</p>"
                + "<p style='margin:6px 0;'><strong>Type :</strong> " + (online ? "En ligne" : "Presentielle") + "</p>"
                + "<p style='margin:6px 0;'><strong>Date :</strong> " + date + "</p>"
                + "<p style='margin:6px 0;'><strong>Heure :</strong> " + heure + "</p>"
                + details
                + "</div>"
                + "<p style='margin:16px 0 0;color:#555;'>Merci de votre participation.</p>"
                + "</div></body></html>";
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String escape(String s) {
        return safe(s)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String escapeAttr(String s) {
        return escape(s);
    }
}
