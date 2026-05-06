package esprit.inscription.service;

import esprit.inscription.entity.EmailCampaign;
import esprit.inscription.entity.EmailTracking;
import esprit.inscription.entity.Order;
import esprit.inscription.entity.User;
import esprit.inscription.repository.EmailTrackingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final EmailTrackingRepository trackingRepository;
    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username:}")
    private String mailFromAddress;

    @Transactional
    public void trackEmailOpen(String emailId) {
        log.debug("Tracking email open: {}", emailId);
        
        EmailTracking tracking = trackingRepository.findByEmailId(emailId)
                .orElse(null);
        
        if (tracking != null) {
            tracking.incrementOpenCount();
            trackingRepository.save(tracking);
            log.debug("Email open tracked: {}", emailId);
        }
    }

    @Transactional
    public void trackEmailClick(String emailId) {
        log.debug("Tracking email click: {}", emailId);
        
        EmailTracking tracking = trackingRepository.findByEmailId(emailId)
                .orElse(null);
        
        if (tracking != null) {
            tracking.incrementClickCount();
            trackingRepository.save(tracking);
            log.debug("Email click tracked: {}", emailId);
        }
    }

    @Transactional
    public void trackEmailConversion(String emailId, BigDecimal conversionValue) {
        log.info("Tracking email conversion: {} - Value: {}", emailId, conversionValue);
        
        EmailTracking tracking = trackingRepository.findByEmailId(emailId)
                .orElse(null);
        
        if (tracking != null) {
            tracking.markAsConverted(conversionValue);
            trackingRepository.save(tracking);
            log.info("Email conversion tracked: {}", emailId);
        }
    }

    @Transactional
    public void trackEmailBounce(String emailId, String reason) {
        log.info("Tracking email bounce: {} - {}", emailId, reason);
        
        EmailTracking tracking = trackingRepository.findByEmailId(emailId)
                .orElse(null);
        
        if (tracking != null) {
            tracking.setStatus(EmailTracking.EmailStatus.BOUNCED);
            tracking.setBouncedAt(LocalDateTime.now());
            trackingRepository.save(tracking);
            log.info("Email bounce tracked: {}", emailId);
        }
    }

    @Transactional
    public void trackEmailUnsubscribe(String emailId) {
        log.info("Tracking email unsubscribe: {}", emailId);
        
        EmailTracking tracking = trackingRepository.findByEmailId(emailId)
                .orElse(null);
        
        if (tracking != null) {
            tracking.setStatus(EmailTracking.EmailStatus.UNSUBSCRIBED);
            tracking.setUnsubscribedAt(LocalDateTime.now());
            trackingRepository.save(tracking);
            log.info("Email unsubscribe tracked: {}", emailId);
        }
    }

    @Transactional
    public void sendWelcomeEmail(User user) {
        log.info("Sending welcome email to: {}", user.getEmail());
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("Bienvenue chez English Academy!");
            message.setText("Bonjour " + user.getFirstName() + ",\n\n" +
                    "Bienvenue chez English Academy! Votre compte a été créé avec succès.\n\n" +
                    "Votre niveau: " + user.getEnglishLevel() + "\n" +
                    "Statut: " + user.getSubscriptionStatus() + "\n\n" +
                    "Cordialement,\n" +
                    "L'équipe English Academy");
            
            javaMailSender.send(message);
            log.info("Welcome email sent successfully to: {}", user.getEmail());
            
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", user.getEmail(), e);
            throw new RuntimeException("Failed to send welcome email", e);
        }
    }

    /**
     * Envoie un email HTML via JavaMailSender.
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent, String fromEmail, String fromName) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Email recipient (to) cannot be null or blank");
        }
        log.info("Sending HTML email to: {} - subject: {}", to, subject);
        try {
            // Gmail SMTP exige que From = adresse authentifiée (spring.mail.username)
            String effectiveFrom = (mailFromAddress != null && !mailFromAddress.isBlank())
                    ? mailFromAddress : (fromEmail != null ? fromEmail : "noreply@english-academy.com");
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom(effectiveFrom, fromName != null ? fromName : "English Academy");
            javaMailSender.send(message);
            log.info("HTML email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send HTML email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Envoie un email HTML avec tracking (crée EmailTracking avant envoi).
     */
    public String sendHtmlEmailWithTracking(String to, String subject, String htmlContent, String fromEmail,
                                            String fromName, Long campaignId, Long userId) {
        log.info("Sending HTML email with tracking to: {}", to);
        String emailId = java.util.UUID.randomUUID().toString();
        try {
            EmailTracking tracking = EmailTracking.builder()
                    .emailId(emailId)
                    .emailAddress(to)
                    .userId(userId)
                    .campaign(campaignId != null ? EmailCampaign.builder().id(campaignId).build() : null)
                    .status(EmailTracking.EmailStatus.PENDING)
                    .build();
            tracking = trackingRepository.save(tracking);

            sendHtmlEmail(to, subject, htmlContent, fromEmail, fromName);

            tracking.setStatus(EmailTracking.EmailStatus.SENT);
            tracking.setSentAt(LocalDateTime.now());
            trackingRepository.save(tracking);
            log.info("HTML email with tracking sent successfully to: {}", to);
            return emailId;
        } catch (Exception e) {
            log.error("Failed to send email with tracking to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Envoie l'email d'inscription à l'offre Slang English (après création commande ou paiement confirmé).
     */
    public void sendPurchaseConfirmationEmail(User user, Order order) {
        String toEmail = user.getEmail();
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Cannot send inscription email: user {} has no email", user.getId());
            throw new IllegalArgumentException("User has no email address");
        }
        log.info("Sending inscription (Slang English offer) email to: {}", toEmail);
        try {
            String firstName = user.getFirstName() != null ? user.getFirstName() : "Customer";
            String orderNumber = order != null && order.getOrderNumber() != null ? order.getOrderNumber() : "";
            String html = """
                <!DOCTYPE html><html><body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <h2 style="color: #10b981;">You're in! 🎉</h2>
                <p>Hi {{firstName}},</p>
                <p>You're now <strong>registered for the Slang English offer</strong>. Welcome aboard!</p>
                <p>Your subscription is active. Get ready to level up your English with real slang and everyday expressions.</p>
                {{orderRef}}
                <p>Cheers,<br><strong>English Academy</strong></p>
                </body></html>
                """
                .replace("{{firstName}}", firstName)
                .replace("{{orderRef}}", orderNumber.isBlank() ? "" : "<p>Order ref: <strong>" + orderNumber + "</strong></p>");
            String subject = "You're registered for Slang English – English Academy";
            sendHtmlEmail(toEmail, subject, html, null, "English Academy");
            log.info("Inscription (Slang English) email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send inscription email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send inscription email", e);
        }
    }

    // Send email with tracking
    public void sendEmailWithTracking(String to, String subject, String content, Long campaignId) {
        log.info("Sending email with tracking to: {}", to);
        try {
            sendHtmlEmailWithTracking(to, subject, content, null, null, campaignId, null);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
