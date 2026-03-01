package esprit.inscription;

import esprit.inscription.entity.*;
import esprit.inscription.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class MailingIntegrationTest {

    @Autowired
    private EmailCampaignService campaignService;

    @Autowired
    private EmailTemplateService templateService;

    @Autowired
    private MailingAutomationService automationService;

    @Test
    void testEmailCampaignCreation() {
        // Create a template first
        EmailTemplate template = EmailTemplate.builder()
                .name("TEST_TEMPLATE")
                .displayName("Test Template")
                .category(EmailTemplate.TemplateCategory.WELCOME)
                .subjectTemplate("Test Subject")
                .htmlContent("<p>Hello {{firstName}}!</p>")
                .build();

        EmailTemplate createdTemplate = templateService.createTemplate(template);
        assertNotNull(createdTemplate.getId());

        // Create campaign
        EmailCampaign campaign = EmailCampaign.builder()
                .name("Test Campaign")
                .category(EmailCampaign.CampaignCategory.WELCOME)
                .subject("Test Subject")
                .fromEmail("test@example.com")
                .targetLevel("B1")
                .template(createdTemplate)
                .build();

        EmailCampaign createdCampaign = campaignService.createCampaign(campaign);
        assertNotNull(createdCampaign.getId());
        assertEquals(EmailCampaign.CampaignStatus.DRAFT, createdCampaign.getStatus());
    }

    @Test
    void testEmailTemplatePersonalization() {
        EmailTemplate template = EmailTemplate.builder()
                .name("PERSONALIZATION_TEST")
                .displayName("Personalization Test")
                .category(EmailTemplate.TemplateCategory.WELCOME)
                .subjectTemplate("Hello {{firstName}}!")
                .htmlContent("<p>Welcome {{firstName}} {{lastName}}!</p>")
                .build();

        EmailTemplate createdTemplate = templateService.createTemplate(template);
        
        // Create mock user
        User user = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .englishLevel("B1")
                .build();

        String personalizedSubject = templateService.personalizeSubject(
                createdTemplate.getSubjectTemplate(), 
                user
        );
        
        assertEquals("Hello John!", personalizedSubject);
    }

    @Test
    void testCampaignStatusTransitions() {
        EmailCampaign campaign = EmailCampaign.builder()
                .name("Status Test Campaign")
                .category(EmailCampaign.CampaignCategory.WELCOME)
                .subject("Test Subject")
                .fromEmail("test@example.com")
                .targetLevel("B1")
                .build();

        EmailCampaign createdCampaign = campaignService.createCampaign(campaign);
        assertEquals(EmailCampaign.CampaignStatus.DRAFT, createdCampaign.getStatus());

        // Test scheduling
        EmailCampaign scheduledCampaign = campaignService.scheduleCampaign(
                createdCampaign.getId(), 
                java.time.LocalDateTime.now().plusDays(1)
        );
        assertEquals(EmailCampaign.CampaignStatus.SCHEDULED, scheduledCampaign.getStatus());
    }

    @Test
    void testDefaultTemplatesCreation() {
        List<EmailTemplate> templates = templateService.createDefaultTemplates();
        
        assertFalse(templates.isEmpty());
        
        // Check that all expected template types are created
        boolean hasWelcome = templates.stream()
                .anyMatch(t -> t.getName().equals("WELCOME_TEMPLATE"));
        assertTrue(hasWelcome);
        
        boolean hasCourseCompletion = templates.stream()
                .anyMatch(t -> t.getName().equals("COURSE_COMPLETION_TEMPLATE"));
        assertTrue(hasCourseCompletion);
        
        boolean hasReactivation = templates.stream()
                .anyMatch(t -> t.getName().equals("REACTIVATION_TEMPLATE"));
        assertTrue(hasReactivation);
    }

    @Test
    void testCampaignMetrics() {
        EmailCampaign campaign = EmailCampaign.builder()
                .name("Metrics Test Campaign")
                .category(EmailCampaign.CampaignCategory.WELCOME)
                .subject("Test Subject")
                .fromEmail("test@example.com")
                .targetLevel("B1")
                .totalRecipients(100)
                .sentCount(80)
                .openedCount(40)
                .clickedCount(20)
                .convertedCount(5)
                .build();

        assertEquals(40.0, campaign.getOpenRate(), 0.01);
        assertEquals(20.0, campaign.getClickRate(), 0.01);
        assertEquals(5.0, campaign.getConversionRate(), 0.01);
    }

    @Test
    void testUserSegmentation() {
        // This would test user segmentation logic
        // For now, just verify the service exists
        assertNotNull(automationService);
    }

    @Test
    void testTemplateCategories() {
        EmailTemplate[] templates = new EmailTemplate[] {
            EmailTemplate.builder()
                    .name("WELCOME_TEST")
                    .category(EmailTemplate.TemplateCategory.WELCOME)
                    .build(),
            EmailTemplate.builder()
                    .name("COMPLETION_TEST")
                    .category(EmailTemplate.TemplateCategory.COURSE_COMPLETION)
                    .build(),
            EmailTemplate.builder()
                    .name("REACTIVATION_TEST")
                    .category(EmailTemplate.TemplateCategory.REACTIVATION)
                    .build(),
            EmailTemplate.builder()
                    .name("PROGRESSION_TEST")
                    .category(EmailTemplate.TemplateCategory.LEVEL_PROGRESSION)
                    .build(),
            EmailTemplate.builder()
                    .name("TRIAL_TEST")
                    .category(EmailTemplate.TemplateCategory.TRIAL_EXPIRATION)
                    .build(),
            EmailTemplate.builder()
                    .name("PROMOTIONAL_TEST")
                    .category(EmailTemplate.TemplateCategory.PROMOTIONAL)
                    .build()
        };

        for (EmailTemplate template : templates) {
            assertNotNull(template.getCategory());
        }
    }

    @Test
    void testCampaignCategories() {
        EmailCampaign[] campaigns = new EmailCampaign[] {
            EmailCampaign.builder()
                    .name("WELCOME_CAMPAIGN")
                    .category(EmailCampaign.CampaignCategory.WELCOME)
                    .build(),
            EmailCampaign.builder()
                    .name("COMPLETION_CAMPAIGN")
                    .category(EmailCampaign.CampaignCategory.COURSE_COMPLETION)
                    .build(),
            EmailCampaign.builder()
                    .name("REACTIVATION_CAMPAIGN")
                    .category(EmailCampaign.CampaignCategory.REACTIVATION)
                    .build(),
            EmailCampaign.builder()
                    .name("PROGRESSION_CAMPAIGN")
                    .category(EmailCampaign.CampaignCategory.LEVEL_PROGRESSION)
                    .build(),
            EmailCampaign.builder()
                    .name("TRIAL_CAMPAIGN")
                    .category(EmailCampaign.CampaignCategory.TRIAL_EXPIRATION)
                    .build(),
            EmailCampaign.builder()
                    .name("PROMOTIONAL_CAMPAIGN")
                    .category(EmailCampaign.CampaignCategory.PROMOTIONAL)
                    .build()
        };

        for (EmailCampaign campaign : campaigns) {
            assertNotNull(campaign.getCategory());
        }
    }
}
