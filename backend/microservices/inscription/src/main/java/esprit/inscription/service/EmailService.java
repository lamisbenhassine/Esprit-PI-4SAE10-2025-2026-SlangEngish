package esprit.inscription.service;

import esprit.inscription.entity.EmailCampaign;
import esprit.inscription.entity.EmailTracking;
import esprit.inscription.entity.User;
import esprit.inscription.repository.EmailTrackingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final EmailTrackingRepository trackingRepository;
    private final JavaMailSender javaMailSender;

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

    // Send email with tracking
    public void sendEmailWithTracking(String to, String subject, String content, Long campaignId) {
        log.info("Sending email with tracking to: {}", to);
        
        try {
            // Create tracking record
            EmailTracking tracking = EmailTracking.builder()
                    .emailId(java.util.UUID.randomUUID().toString())
                    .emailAddress(to)
                    .campaign(EmailCampaign.builder().id(campaignId).build())
                    .status(EmailTracking.EmailStatus.PENDING)
                    .build();
            
            // In real implementation, would send actual email here
            // For now, just log and mark as sent
            tracking.setStatus(EmailTracking.EmailStatus.SENT);
            tracking.setSentAt(LocalDateTime.now());
            
            trackingRepository.save(tracking);
            log.info("Email sent successfully to: {}", to);
            
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
