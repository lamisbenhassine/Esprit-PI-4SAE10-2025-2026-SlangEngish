package esprit.inscription.service;

import esprit.inscription.entity.*;
import esprit.inscription.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailingAutomationService {

    private final EmailCampaignService campaignService;
    private final EmailTemplateService templateService;
    private final UserRepository userRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final OrderRepository orderRepository;

    // Automated welcome campaign for new users
    @Transactional
    public void sendWelcomeCampaign(User user) {
        log.info("Sending welcome campaign for new user: {}", user.getId());
        
        try {
            EmailCampaign campaign = EmailCampaign.builder()
                    .name("Welcome - " + user.getEmail())
                    .category(EmailCampaign.CampaignCategory.WELCOME)
                    .subject("Welcome to English Academy, " + user.getFirstName() + "!")
                    .fromEmail("welcome@english-academy.com")
                    .fromName("English Academy")
                    .targetLevel(user.getEnglishLevel())
                    .build();

            EmailCampaign createdCampaign = campaignService.createCampaign(campaign);
            campaignService.launchCampaign(createdCampaign.getId());
            
            log.info("Welcome campaign sent successfully to user: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to send welcome campaign to user: {}", user.getId(), e);
        }
    }

    // Automated course completion campaign
    @Transactional
    public void sendCourseCompletionCampaign(User user, String completedLevel) {
        log.info("Sending course completion campaign for user: {} - Level: {}", user.getId(), completedLevel);
        
        try {
            EmailCampaign campaign = EmailCampaign.builder()
                    .name("Course Completion - " + user.getEmail())
                    .category(EmailCampaign.CampaignCategory.COURSE_COMPLETION)
                    .subject("Congratulations on completing " + completedLevel + "!")
                    .fromEmail("success@english-academy.com")
                    .fromName("English Academy")
                    .targetLevel(completedLevel)
                    .build();

            EmailCampaign createdCampaign = campaignService.createCampaign(campaign);
            campaignService.launchCampaign(createdCampaign.getId());
            
            log.info("Course completion campaign sent successfully to user: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to send course completion campaign to user: {}", user.getId(), e);
        }
    }

    // Automated level progression campaign
    @Transactional
    public void sendLevelProgressionCampaign(User user, String nextLevel) {
        log.info("Sending level progression campaign for user: {} - Next level: {}", user.getId(), nextLevel);
        
        try {
            EmailCampaign campaign = EmailCampaign.builder()
                    .name("Level Progression - " + user.getEmail())
                    .category(EmailCampaign.CampaignCategory.LEVEL_PROGRESSION)
                    .subject("Ready for " + nextLevel + ", " + user.getFirstName() + "?")
                    .fromEmail("progress@english-academy.com")
                    .fromName("English Academy")
                    .targetLevel(nextLevel)
                    .build();

            EmailCampaign createdCampaign = campaignService.createCampaign(campaign);
            campaignService.launchCampaign(createdCampaign.getId());
            
            log.info("Level progression campaign sent successfully to user: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to send level progression campaign to user: {}", user.getId(), e);
        }
    }

    // Automated trial expiration campaign
    @Transactional
    public void sendTrialExpirationCampaign(User user, int daysLeft) {
        log.info("Sending trial expiration campaign for user: {} - Days left: {}", user.getId(), daysLeft);
        
        try {
            EmailCampaign campaign = EmailCampaign.builder()
                    .name("Trial Expiration - " + user.getEmail())
                    .category(EmailCampaign.CampaignCategory.TRIAL_EXPIRATION)
                    .subject("Your trial expires in " + daysLeft + " days, " + user.getFirstName() + "!")
                    .fromEmail("trial@english-academy.com")
                    .fromName("English Academy")
                    .targetLevel(user.getEnglishLevel())
                    .build();

            EmailCampaign createdCampaign = campaignService.createCampaign(campaign);
            campaignService.launchCampaign(createdCampaign.getId());
            
            log.info("Trial expiration campaign sent successfully to user: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to send trial expiration campaign to user: {}", user.getId(), e);
        }
    }

    // Automated reactivation campaign for inactive users
    @Transactional
    public void sendReactivationCampaigns() {
        log.info("Sending reactivation campaigns to inactive users");
        
        try {
            // Find users inactive for 30+ days
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
            List<User> inactiveUsers = userRepository.findInactiveUsersSince(cutoffDate);
            
            if (!inactiveUsers.isEmpty()) {
                EmailCampaign campaign = EmailCampaign.builder()
                        .name("Reactivation Campaign - " + LocalDateTime.now().toString())
                        .category(EmailCampaign.CampaignCategory.REACTIVATION)
                        .subject("We miss you at English Academy!")
                        .fromEmail("reactivation@english-academy.com")
                        .fromName("English Academy")
                        .build();

                EmailCampaign createdCampaign = campaignService.createCampaign(campaign);
                campaignService.launchCampaign(createdCampaign.getId());
                
                log.info("Reactivation campaign sent to {} inactive users", inactiveUsers.size());
            }
        } catch (Exception e) {
            log.error("Failed to send reactivation campaigns", e);
        }
    }

    // Automated promotional campaigns for high-value users
    @Transactional
    public void sendPromotionalCampaigns() {
        log.info("Sending promotional campaigns to high-value users");
        
        try {
            // Find users with high revenue
            List<User> highValueUsers = userRepository.findHighValueUsers(BigDecimal.valueOf(500.0));
            
            if (!highValueUsers.isEmpty()) {
                EmailCampaign campaign = EmailCampaign.builder()
                        .name("Promotional Campaign - High Value Users")
                        .category(EmailCampaign.CampaignCategory.PROMOTIONAL)
                        .subject("Exclusive offer for our valued students!")
                        .fromEmail("promo@english-academy.com")
                        .fromName("English Academy")
                        .build();

                EmailCampaign createdCampaign = campaignService.createCampaign(campaign);
                campaignService.launchCampaign(createdCampaign.getId());
                
                log.info("Promotional campaign sent to {} high-value users", highValueUsers.size());
            }
        } catch (Exception e) {
            log.error("Failed to send promotional campaigns", e);
        }
    }

    // Scheduled tasks for automated campaigns
    @Scheduled(cron = "0 0 9 * * *") // Every day at 9 AM
    public void dailyReactivationCampaign() {
        log.info("Running daily reactivation campaign");
        sendReactivationCampaigns();
    }

    @Scheduled(cron = "0 0 10 * * MON") // Every Monday at 10 AM
    public void weeklyPromotionalCampaign() {
        log.info("Running weekly promotional campaign");
        sendPromotionalCampaigns();
    }

    @Scheduled(cron = "0 0 */12 * * *") // Every 12 hours
    public void checkTrialExpirations() {
        log.info("Checking for trial expirations");
        
        try {
            // Find users whose trials expire in the next 3 days
            LocalDateTime threeDaysFromNow = LocalDateTime.now().plusDays(3);
            List<User> usersWithExpiringTrials = userRepository.findUsersWithExpiringTrials(threeDaysFromNow);
            
            for (User user : usersWithExpiringTrials) {
                int daysLeft = (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), user.getTrialEndsAt());
                if (daysLeft <= 3 && daysLeft > 0) {
                    sendTrialExpirationCampaign(user, daysLeft);
                }
            }
        } catch (Exception e) {
            log.error("Failed to check trial expirations", e);
        }
    }

    @Scheduled(cron = "0 0 8 * * *") // Every day at 8 AM
    public void processScheduledCampaigns() {
        log.info("Processing scheduled campaigns");
        
        try {
            List<EmailCampaign> readyCampaigns = campaignService.getCampaignsReadyToSend();
            
            for (EmailCampaign campaign : readyCampaigns) {
                campaignService.launchCampaign(campaign.getId());
                log.info("Launched scheduled campaign: {}", campaign.getId());
            }
        } catch (Exception e) {
            log.error("Failed to process scheduled campaigns", e);
        }
    }

    @Scheduled(cron = "0 */30 * * * *") // Every 30 minutes
    public void updateCampaignStatistics() {
        log.debug("Updating campaign statistics");
        
        try {
            List<EmailCampaign> activeCampaigns = campaignService.getActiveCampaigns();
            
            for (EmailCampaign campaign : activeCampaigns) {
                campaignService.updateCampaignStats(campaign.getId());
            }
        } catch (Exception e) {
            log.error("Failed to update campaign statistics", e);
        }
    }

    // Event-driven methods (to be called from other services)
    @Transactional
    public void handleUserRegistered(User user) {
        log.info("Handling user registration event for user: {}", user.getId());
        sendWelcomeCampaign(user);
    }

    @Transactional
    public void handleCourseCompleted(User user, String completedLevel) {
        log.info("Handling course completion event for user: {} - Level: {}", user.getId(), completedLevel);
        sendCourseCompletionCampaign(user, completedLevel);
        
        // Also suggest next level progression
        String nextLevel = getNextLevel(completedLevel);
        if (nextLevel != null) {
            sendLevelProgressionCampaign(user, nextLevel);
        }
    }

    @Transactional
    public void handleSubscriptionCreated(User user, SubscriptionPlan plan) {
        log.info("Handling subscription creation event for user: {} - Plan: {}", user.getId(), plan.getPlanType());
        
        // Send welcome campaign if user is new
        if (user.getCreatedAt().isAfter(LocalDateTime.now().minusDays(1))) {
            sendWelcomeCampaign(user);
        }
    }

    @Transactional
    public void handlePaymentCompleted(User user, Order order) {
        log.info("Handling payment completion event for user: {} - Order: {}", user.getId(), order.getId());
        
        // Could trigger promotional campaigns based on payment amount
        if (false) {
            // Send special offer for high-value customers
            // This could be implemented as a separate campaign type
        }
    }

    private String getNextLevel(String currentLevel) {
        switch (currentLevel.toUpperCase()) {
            case "A1": return "A2";
            case "A2": return "B1";
            case "B1": return "B2";
            case "B2": return "C1";
            case "C1": return "C2";
            case "C2": return null; // Highest level
            default: return "B1"; // Default to B1
        }
    }
}
