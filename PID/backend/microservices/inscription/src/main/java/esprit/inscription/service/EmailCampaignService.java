package esprit.inscription.service;

import esprit.inscription.entity.*;
import esprit.inscription.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailCampaignService {

    private final EmailCampaignRepository campaignRepository;
    private final EmailTemplateRepository templateRepository;
    private final EmailTrackingRepository trackingRepository;
    private final UserRepository userRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final OrderRepository orderRepository;
    private final EmailService emailService;

    @Transactional
    public EmailCampaign createCampaign(EmailCampaign campaign) {
        log.info("Creating new email campaign: {}", campaign.getName());
        
        // Validate template
        if (campaign.getTemplate() != null && campaign.getTemplate().getId() != null) {
            EmailTemplate template = templateRepository.findById(campaign.getTemplate().getId())
                    .orElseThrow(() -> new RuntimeException("Template not found: " + campaign.getTemplate().getId()));
            campaign.setTemplate(template);
        }

        // Set defaults
        if (campaign.getStatus() == null) {
            campaign.setStatus(EmailCampaign.CampaignStatus.DRAFT);
        }

        EmailCampaign savedCampaign = campaignRepository.save(campaign);
        log.info("Email campaign created successfully with ID: {}", savedCampaign.getId());
        return savedCampaign;
    }

    @Transactional
    public EmailCampaign updateCampaign(Long id, EmailCampaign campaign) {
        log.info("Updating email campaign: {}", id);
        
        EmailCampaign existingCampaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + id));

        // Don't allow updates if campaign is already sending or completed
        if (existingCampaign.getStatus() == EmailCampaign.CampaignStatus.SENDING || 
            existingCampaign.getStatus() == EmailCampaign.CampaignStatus.COMPLETED) {
            throw new RuntimeException("Cannot update campaign that is sending or completed");
        }

        // Update fields
        existingCampaign.setName(campaign.getName());
        existingCampaign.setDescription(campaign.getDescription());
        existingCampaign.setCategory(campaign.getCategory());
        existingCampaign.setTargetLevel(campaign.getTargetLevel());
        existingCampaign.setSubject(campaign.getSubject());
        existingCampaign.setFromEmail(campaign.getFromEmail());
        existingCampaign.setFromName(campaign.getFromName());
        existingCampaign.setScheduledAt(campaign.getScheduledAt());

        EmailCampaign updatedCampaign = campaignRepository.save(existingCampaign);
        log.info("Email campaign updated successfully: {}", updatedCampaign.getId());
        return updatedCampaign;
    }

    @Transactional
    public void deleteCampaign(Long id) {
        log.info("Deleting email campaign: {}", id);
        
        EmailCampaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + id));

        // Don't allow deletion if campaign is active
        if (campaign.isActive()) {
            throw new RuntimeException("Cannot delete active campaign");
        }

        campaignRepository.deleteById(id);
        log.info("Email campaign deleted successfully: {}", id);
    }

    public EmailCampaign getCampaignById(Long id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + id));
    }

    public Page<EmailCampaign> getAllCampaigns(Pageable pageable) {
        return campaignRepository.findAll(pageable);
    }

    public List<EmailCampaign> getCampaignsByStatus(EmailCampaign.CampaignStatus status) {
        return campaignRepository.findByStatus(status);
    }

    public List<EmailCampaign> getCampaignsByCategory(EmailCampaign.CampaignCategory category) {
        return campaignRepository.findByCategory(category);
    }

    public List<EmailCampaign> getCampaignsByTargetLevel(String targetLevel) {
        return campaignRepository.findByTargetLevel(targetLevel);
    }

    @Transactional
    public EmailCampaign scheduleCampaign(Long id, LocalDateTime scheduledAt) {
        log.info("Scheduling email campaign {} for: {}", id, scheduledAt);
        
        EmailCampaign campaign = getCampaignById(id);
        
        if (campaign.getStatus() != EmailCampaign.CampaignStatus.DRAFT) {
            throw new RuntimeException("Only draft campaigns can be scheduled");
        }

        if (scheduledAt.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Scheduled time must be in the future");
        }

        campaign.setScheduledAt(scheduledAt);
        campaign.setStatus(EmailCampaign.CampaignStatus.SCHEDULED);
        
        EmailCampaign savedCampaign = campaignRepository.save(campaign);
        log.info("Email campaign scheduled successfully: {}", savedCampaign.getId());
        return savedCampaign;
    }

    @Transactional
    public EmailCampaign launchCampaign(Long id) {
        log.info("Launching email campaign: {}", id);
        
        EmailCampaign campaign = getCampaignById(id);
        
        if (campaign.getStatus() != EmailCampaign.CampaignStatus.DRAFT && 
            campaign.getStatus() != EmailCampaign.CampaignStatus.SCHEDULED) {
            throw new RuntimeException("Only draft or scheduled campaigns can be launched");
        }

        // Validate campaign has all required fields
        validateCampaignForLaunch(campaign);

        // Get recipients based on target level
        List<User> recipients = getRecipientsForCampaign(campaign);
        campaign.setTotalRecipients(recipients.size());
        campaign.setSentCount(0);
        campaign.setStatus(EmailCampaign.CampaignStatus.SENDING);
        campaign.setSentAt(LocalDateTime.now());

        EmailCampaign savedCampaign = campaignRepository.save(campaign);
        
        // Start sending process
        sendCampaignToRecipients(savedCampaign, recipients);
        
        log.info("Email campaign launched successfully: {} with {} recipients", 
                savedCampaign.getId(), recipients.size());
        return savedCampaign;
    }

    @Transactional
    public EmailCampaign pauseCampaign(Long id) {
        log.info("Pausing email campaign: {}", id);
        
        EmailCampaign campaign = getCampaignById(id);
        
        if (campaign.getStatus() != EmailCampaign.CampaignStatus.SENDING) {
            throw new RuntimeException("Only sending campaigns can be paused");
        }

        campaign.setStatus(EmailCampaign.CampaignStatus.PAUSED);
        
        EmailCampaign savedCampaign = campaignRepository.save(campaign);
        log.info("Email campaign paused successfully: {}", savedCampaign.getId());
        return savedCampaign;
    }

    @Transactional
    public EmailCampaign resumeCampaign(Long id) {
        log.info("Resuming email campaign: {}", id);
        
        EmailCampaign campaign = getCampaignById(id);
        
        if (campaign.getStatus() != EmailCampaign.CampaignStatus.PAUSED) {
            throw new RuntimeException("Only paused campaigns can be resumed");
        }

        campaign.setStatus(EmailCampaign.CampaignStatus.SENDING);
        
        EmailCampaign savedCampaign = campaignRepository.save(campaign);
        log.info("Email campaign resumed successfully: {}", savedCampaign.getId());
        return savedCampaign;
    }

    @Transactional
    public EmailCampaign cancelCampaign(Long id) {
        log.info("Cancelling email campaign: {}", id);
        
        EmailCampaign campaign = getCampaignById(id);
        
        if (campaign.getStatus() == EmailCampaign.CampaignStatus.COMPLETED || 
            campaign.getStatus() == EmailCampaign.CampaignStatus.CANCELLED) {
            throw new RuntimeException("Cannot cancel completed or already cancelled campaign");
        }

        campaign.setStatus(EmailCampaign.CampaignStatus.CANCELLED);
        
        EmailCampaign savedCampaign = campaignRepository.save(campaign);
        log.info("Email campaign cancelled successfully: {}", savedCampaign.getId());
        return savedCampaign;
    }

    @Transactional
    public void updateCampaignStats(Long id) {
        EmailCampaign campaign = getCampaignById(id);
        
        // Update counts from tracking data
        campaign.setSentCount(trackingRepository.countTotalEmailsByCampaign(id).intValue());
        campaign.setOpenedCount(trackingRepository.countOpenedEmailsByCampaign(id).intValue());
        campaign.setClickedCount(trackingRepository.countClickedEmailsByCampaign(id).intValue());
        campaign.setConvertedCount(trackingRepository.countConvertedEmailsByCampaign(id).intValue());

        // Check if campaign is completed
        if (campaign.getSentCount() >= campaign.getTotalRecipients()) {
            campaign.setStatus(EmailCampaign.CampaignStatus.COMPLETED);
            campaign.setCompletedAt(LocalDateTime.now());
        }

        campaignRepository.save(campaign);
    }

    public List<EmailCampaign> getCampaignsReadyToSend() {
        return campaignRepository.findCampaignsReadyToSend(LocalDateTime.now());
    }

    public List<EmailCampaign> getActiveCampaigns() {
        return campaignRepository.findByStatus(EmailCampaign.CampaignStatus.SENDING);
    }

    public List<EmailCampaign> getIncompleteSendingCampaigns() {
        return campaignRepository.findIncompleteSendingCampaigns();
    }

    // Automated campaign creation for specific events
    @Transactional
    public EmailCampaign createWelcomeCampaign(User user) {
        log.info("Creating welcome campaign for user: {}", user.getId());
        
        EmailTemplate template = templateRepository.findByName("WELCOME_TEMPLATE")
                .orElseThrow(() -> new RuntimeException("Welcome template not found"));

        EmailCampaign campaign = EmailCampaign.builder()
                .name("Welcome Campaign for " + user.getEmail())
                .category(EmailCampaign.CampaignCategory.WELCOME)
                .subject("Welcome to English Academy, " + user.getFirstName() + "!")
                .fromEmail("welcome@english-academy.com")
                .fromName("English Academy")
                .targetLevel(user.getEnglishLevel())
                .template(template)
                .build();

        EmailCampaign savedCampaign = createCampaign(campaign);
        
        // Send immediately
        return launchCampaign(savedCampaign.getId());
    }

    @Transactional
    public EmailCampaign createCourseCompletionCampaign(User user, String completedLevel) {
        log.info("Creating course completion campaign for user: {} - Level: {}", user.getId(), completedLevel);
        
        EmailTemplate template = templateRepository.findByName("COURSE_COMPLETION_TEMPLATE")
                .orElseThrow(() -> new RuntimeException("Course completion template not found"));

        EmailCampaign campaign = EmailCampaign.builder()
                .name("Course Completion - " + user.getEmail())
                .category(EmailCampaign.CampaignCategory.COURSE_COMPLETION)
                .subject("Congratulations on completing " + completedLevel + "!")
                .fromEmail("success@english-academy.com")
                .fromName("English Academy")
                .targetLevel(completedLevel)
                .template(template)
                .build();

        EmailCampaign savedCampaign = createCampaign(campaign);
        return launchCampaign(savedCampaign.getId());
    }

    @Transactional
    public EmailCampaign createReactivationCampaign(List<User> inactiveUsers) {
        log.info("Creating reactivation campaign for {} inactive users", inactiveUsers.size());
        
        EmailTemplate template = templateRepository.findByName("REACTIVATION_TEMPLATE")
                .orElseThrow(() -> new RuntimeException("Reactivation template not found"));

        EmailCampaign campaign = EmailCampaign.builder()
                .name("Reactivation Campaign - " + LocalDateTime.now().toString())
                .category(EmailCampaign.CampaignCategory.REACTIVATION)
                .subject("We miss you at English Academy!")
                .fromEmail("reactivation@english-academy.com")
                .fromName("English Academy")
                .template(template)
                .build();

        EmailCampaign savedCampaign = createCampaign(campaign);
        return launchCampaign(savedCampaign.getId());
    }

    private void validateCampaignForLaunch(EmailCampaign campaign) {
        if (campaign.getSubject() == null || campaign.getSubject().trim().isEmpty()) {
            throw new RuntimeException("Campaign must have a subject");
        }
        
        if (campaign.getFromEmail() == null || campaign.getFromEmail().trim().isEmpty()) {
            throw new RuntimeException("Campaign must have a from email");
        }
    }

    private List<User> getRecipientsForCampaign(EmailCampaign campaign) {
        if (campaign.getTargetLevel() != null) {
            return userRepository.findByEnglishLevel(campaign.getTargetLevel());
        }
        return userRepository.findAll();
    }

    private void sendCampaignToRecipients(EmailCampaign campaign, List<User> recipients) {
        int sentCount = 0;
        
        for (User recipient : recipients) {
            if (recipient.getEmail() == null || recipient.getEmail().isBlank()) {
                log.warn("Skipping recipient {} (id={}): no email address", recipient.getFirstName(), recipient.getId());
                continue;
            }
            try {
                sendEmailToRecipient(campaign, recipient);
                sentCount++;
            } catch (Exception e) {
                log.error("Failed to send email to recipient: {}", recipient.getEmail(), e);
            }
        }
        
        // Update campaign status
        campaign.setSentCount(sentCount);
        campaign.setStatus(EmailCampaign.CampaignStatus.COMPLETED);
        campaign.setCompletedAt(LocalDateTime.now());
        campaignRepository.save(campaign);
    }

    private void sendEmailToRecipient(EmailCampaign campaign, User recipient) {
        // Create tracking record
        EmailTracking tracking = EmailTracking.builder()
                .emailId(UUID.randomUUID().toString())
                .userId(recipient.getId())
                .emailAddress(recipient.getEmail())
                .campaign(campaign)
                .status(EmailTracking.EmailStatus.PENDING)
                .build();

        tracking = trackingRepository.save(tracking);

        try {
            String personalizedSubject = personalizeSubject(campaign.getSubject(), recipient);
            String personalizedContent = personalizeContent(campaign.getTemplate(), recipient);
            if (personalizedContent == null || personalizedContent.isBlank()) {
                personalizedContent = buildFallbackHtmlContent(campaign, recipient);
            }

            // Envoi réel via JavaMailSender
            emailService.sendHtmlEmail(
                    recipient.getEmail(),
                    personalizedSubject,
                    personalizedContent,
                    campaign.getFromEmail(),
                    campaign.getFromName()
            );

            tracking.setStatus(EmailTracking.EmailStatus.SENT);
            tracking.setSentAt(LocalDateTime.now());
            trackingRepository.save(tracking);

        } catch (Exception e) {
            tracking.setStatus(EmailTracking.EmailStatus.FAILED);
            tracking.setErrorMessage(e.getMessage());
            trackingRepository.save(tracking);
            throw e;
        }
    }

    private String buildFallbackHtmlContent(EmailCampaign campaign, User recipient) {
        String name = recipient.getFirstName() != null ? recipient.getFirstName() : "Friend";
        return """
                <!DOCTYPE html><html><body style="font-family: Arial, sans-serif;">
                <h2>%s</h2>
                <p>Hello %s,</p>
                <p>This is a message from English Academy.</p>
                <p>Best regards,<br>%s</p>
                </body></html>
                """.formatted(campaign.getSubject(), name,
                campaign.getFromName() != null ? campaign.getFromName() : "English Academy");
    }

    private String personalizeSubject(String subjectTemplate, User user) {
        if (subjectTemplate == null || user == null) {
            return subjectTemplate;
        }

        return subjectTemplate.replace("{{firstName}}", user.getFirstName() != null ? user.getFirstName() : "Friend")
                           .replace("{{lastName}}", user.getLastName() != null ? user.getLastName() : "")
                           .replace("{{englishLevel}}", user.getEnglishLevel() != null ? user.getEnglishLevel() : "");
    }

    private String personalizeContent(EmailTemplate template, User user) {
        if (template == null || user == null) {
            return "";
        }

        String content = template.getHtmlContent();
        
        // Replace common variables
        content = content.replace("{{firstName}}", user.getFirstName() != null ? user.getFirstName() : "Friend");
        content = content.replace("{{lastName}}", user.getLastName() != null ? user.getLastName() : "");
        content = content.replace("{{email}}", user.getEmail() != null ? user.getEmail() : "");
        content = content.replace("{{englishLevel}}", user.getEnglishLevel() != null ? user.getEnglishLevel() : "");
        content = content.replace("{{id}}", user.getId() != null ? user.getId().toString() : "");
        
        return content;
    }
}
