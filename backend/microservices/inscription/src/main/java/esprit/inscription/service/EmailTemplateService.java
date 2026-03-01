package esprit.inscription.service;

import esprit.inscription.entity.EmailTemplate;
import esprit.inscription.entity.User;
import esprit.inscription.repository.EmailTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateService {

    private final EmailTemplateRepository templateRepository;

    @Transactional
    public EmailTemplate createTemplate(EmailTemplate template) {
        log.info("Creating new email template: {}", template.getName());
        
        // Validate template name uniqueness
        if (templateRepository.existsByName(template.getName())) {
            throw new RuntimeException("Template name already exists: " + template.getName());
        }

        // Set defaults
        if (template.getStatus() == null) {
            template.setStatus(EmailTemplate.TemplateStatus.ACTIVE);
        }

        EmailTemplate savedTemplate = templateRepository.save(template);
        log.info("Email template created successfully: {}", savedTemplate.getId());
        return savedTemplate;
    }

    @Transactional
    public EmailTemplate updateTemplate(Long id, EmailTemplate template) {
        log.info("Updating email template: {}", id);
        
        EmailTemplate existingTemplate = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found: " + id));

        // Update fields
        existingTemplate.setDisplayName(template.getDisplayName());
        existingTemplate.setDescription(template.getDescription());
        existingTemplate.setCategory(template.getCategory());
        existingTemplate.setSubjectTemplate(template.getSubjectTemplate());
        existingTemplate.setHtmlContent(template.getHtmlContent());
        existingTemplate.setTextContent(template.getTextContent());
        existingTemplate.setStatus(template.getStatus());

        EmailTemplate savedTemplate = templateRepository.save(existingTemplate);
        log.info("Email template updated successfully: {}", savedTemplate.getId());
        return savedTemplate;
    }

    @Transactional
    public void deleteTemplate(Long id) {
        log.info("Deleting email template: {}", id);
        
        EmailTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found: " + id));

        templateRepository.deleteById(id);
        log.info("Email template deleted successfully: {}", id);
    }

    public EmailTemplate getTemplateById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found: " + id));
    }

    public List<EmailTemplate> getAllTemplates() {
        return templateRepository.findAll();
    }

    public List<EmailTemplate> getActiveTemplates() {
        return templateRepository.findByStatus(EmailTemplate.TemplateStatus.ACTIVE);
    }

    public List<EmailTemplate> getTemplatesByCategory(EmailTemplate.TemplateCategory category) {
        return templateRepository.findByCategory(category);
    }

    public String personalizeSubject(String subjectTemplate, User user) {
        if (subjectTemplate == null || user == null) {
            return subjectTemplate;
        }

        String personalized = subjectTemplate;
        
        // Replace common variables
        personalized = personalized.replace("{{firstName}}", 
                user.getFirstName() != null ? user.getFirstName() : "Friend");
        personalized = personalized.replace("{{lastName}}", 
                user.getLastName() != null ? user.getLastName() : "");
        personalized = personalized.replace("{{email}}", 
                user.getEmail() != null ? user.getEmail() : "");
        personalized = personalized.replace("{{englishLevel}}", 
                user.getEnglishLevel() != null ? user.getEnglishLevel() : "");
        
        return personalized;
    }

    public String personalizeContent(EmailTemplate template, User user) {
        if (template == null || user == null) {
            return "";
        }

        String content = template.getHtmlContent();
        
        // Replace template-level variables
        content = content.replace("{{templateName}}", template.getDisplayName());
        content = content.replace("{{templateCategory}}", template.getCategory().toString());
        
        // Replace user variables
        content = content.replace("{{firstName}}", 
                user.getFirstName() != null ? user.getFirstName() : "Friend");
        content = content.replace("{{lastName}}", 
                user.getLastName() != null ? user.getLastName() : "");
        content = content.replace("{{email}}", 
                user.getEmail() != null ? user.getEmail() : "");
        content = content.replace("{{englishLevel}}", 
                user.getEnglishLevel() != null ? user.getEnglishLevel() : "");
        
        return content;
    }

    public List<EmailTemplate> createDefaultTemplates() {
        log.info("Creating default email templates");
        
        // Welcome template
        createTemplate(EmailTemplate.builder()
                .name("WELCOME_TEMPLATE")
                .displayName("Welcome to English Academy")
                .description("Welcome email for new users")
                .category(EmailTemplate.TemplateCategory.WELCOME)
                .subjectTemplate("Welcome to English Academy, {{firstName}}!")
                .htmlContent(buildWelcomeTemplateHtml())
                .textContent(buildWelcomeTemplateText())
                .status(EmailTemplate.TemplateStatus.ACTIVE)
                .build());

        // Course completion template
        createTemplate(EmailTemplate.builder()
                .name("COURSE_COMPLETION_TEMPLATE")
                .displayName("Course Completion")
                .description("Congratulations on completing a course")
                .category(EmailTemplate.TemplateCategory.COURSE_COMPLETION)
                .subjectTemplate("Congratulations, {{firstName}}! You've completed your course!")
                .htmlContent(buildCourseCompletionTemplateHtml())
                .textContent(buildCourseCompletionTemplateText())
                .status(EmailTemplate.TemplateStatus.ACTIVE)
                .build());

        // Reactivation template
        createTemplate(EmailTemplate.builder()
                .name("REACTIVATION_TEMPLATE")
                .displayName("We Miss You!")
                .description("Reactivation email for inactive users")
                .category(EmailTemplate.TemplateCategory.REACTIVATION)
                .subjectTemplate("We miss you, {{firstName}}! Here's what's new...")
                .htmlContent(buildReactivationTemplateHtml())
                .textContent(buildReactivationTemplateText())
                .status(EmailTemplate.TemplateStatus.ACTIVE)
                .build());

        // Level progression template
        createTemplate(EmailTemplate.builder()
                .name("LEVEL_PROGRESSION_TEMPLATE")
                .displayName("Ready for Next Level!")
                .description("Encourage progression to next English level")
                .category(EmailTemplate.TemplateCategory.LEVEL_PROGRESSION)
                .subjectTemplate("Ready for {{nextLevel}}, {{firstName}}?")
                .htmlContent(buildLevelProgressionTemplateHtml())
                .textContent(buildLevelProgressionTemplateText())
                .status(EmailTemplate.TemplateStatus.ACTIVE)
                .build());

        // Trial expiration template
        createTemplate(EmailTemplate.builder()
                .name("TRIAL_EXPIRATION_TEMPLATE")
                .displayName("Your Trial is Expiring")
                .description("Notify users about trial expiration")
                .category(EmailTemplate.TemplateCategory.TRIAL_EXPIRATION)
                .subjectTemplate("Your trial expires soon, {{firstName}}!")
                .htmlContent(buildTrialExpirationTemplateHtml())
                .textContent(buildTrialExpirationTemplateText())
                .status(EmailTemplate.TemplateStatus.ACTIVE)
                .build());

        List<EmailTemplate> templates = getActiveTemplates();
        log.info("Default templates created: {}", templates.size());
        return templates;
    }

    private String buildWelcomeTemplateHtml() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Welcome to English Academy</title>
                </head>
                <body style="font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 20px;">
                        <h1 style="color: #667eea;">Welcome to English Academy, {{firstName}}!</h1>
                        <p>We're excited to have you join our community of English learners.</p>
                        <p>Your journey to mastering English starts here. Based on your profile, we recommend starting with our {{englishLevel}} level courses.</p>
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="#" style="background-color: #667eea; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">Start Learning</a>
                        </div>
                        <p>Best regards,<br>The English Academy Team</p>
                    </div>
                </body>
                </html>
                """;
    }

    private String buildWelcomeTemplateText() {
        return """
                Welcome to English Academy, {{firstName}}!
                
                We're excited to have you join our community of English learners.
                
                Your journey to mastering English starts here. Based on your profile, we recommend starting with our {{englishLevel}} level courses.
                
                Start Learning: [Link]
                
                Best regards,
                The English Academy Team
                """;
    }

    private String buildCourseCompletionTemplateHtml() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Course Completion</title>
                </head>
                <body style="font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 20px;">
                        <h1 style="color: #10b981;">Congratulations, {{firstName}}!</h1>
                        <p>You've successfully completed your course! This is a fantastic achievement.</p>
                        <p>Ready for the next challenge? Check out our recommended next steps for your {{englishLevel}} level.</p>
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="#" style="background-color: #10b981; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">Continue Learning</a>
                        </div>
                        <p>Keep up the great work!<br>The English Academy Team</p>
                    </div>
                </body>
                </html>
                """;
    }

    private String buildCourseCompletionTemplateText() {
        return """
                Congratulations, {{firstName}}!
                
                You've successfully completed your course! This is a fantastic achievement.
                
                Ready for the next challenge? Check out our recommended next steps for your {{englishLevel}} level.
                
                Continue Learning: [Link]
                
                Keep up the great work!
                The English Academy Team
                """;
    }

    private String buildReactivationTemplateHtml() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>We Miss You</title>
                </head>
                <body style="font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 20px;">
                        <h1 style="color: #f59e0b;">We miss you, {{firstName}}!</h1>
                        <p>It's been a while since we've seen you. Here's what's new at English Academy:</p>
                        <ul>
                            <li>New {{englishLevel}} level courses</li>
                            <li>Interactive speaking exercises</li>
                            <li>Weekly conversation clubs</li>
                        </ul>
                        <p>Come back and continue your English journey!</p>
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="#" style="background-color: #f59e0b; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">Get Back to Learning</a>
                        </div>
                        <p>We're here to support you!<br>The English Academy Team</p>
                    </div>
                </body>
                </html>
                """;
    }

    private String buildReactivationTemplateText() {
        return """
                We miss you, {{firstName}}!
                
                It's been a while since we've seen you. Here's what's new at English Academy:
                - New {{englishLevel}} level courses
                - Interactive speaking exercises
                - Weekly conversation clubs
                
                Come back and continue your English journey!
                
                Get Back to Learning: [Link]
                
                We're here to support you!
                The English Academy Team
                """;
    }

    private String buildLevelProgressionTemplateHtml() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Level Progression</title>
                </head>
                <body style="font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 20px;">
                        <h1 style="color: #8b5cf6;">Ready for {{nextLevel}}, {{firstName}}?</h1>
                        <p>You've made amazing progress in {{englishLevel}}! It's time to take your skills to the next level.</p>
                        <p>Our {{nextLevel}} courses will help you:</p>
                        <ul>
                            <li>Expand your vocabulary</li>
                            <li>Improve your grammar</li>
                            <li>Practice advanced conversations</li>
                        </ul>
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="#" style="background-color: #8b5cf6; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">Start {{nextLevel}} Level</a>
                        </div>
                        <p>Continue your success story!<br>The English Academy Team</p>
                    </div>
                </body>
                </html>
                """;
    }

    private String buildLevelProgressionTemplateText() {
        return """
                Ready for {{nextLevel}}, {{firstName}}?
                
                You've made amazing progress in {{englishLevel}}! It's time to take your skills to the next level.
                
                Our {{nextLevel}} courses will help you:
                - Expand your vocabulary
                - Improve your grammar
                - Practice advanced conversations
                
                Start {{nextLevel}} Level: [Link]
                
                Continue your success story!
                The English Academy Team
                """;
    }

    private String buildTrialExpirationTemplateHtml() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Trial Expiration</title>
                </head>
                <body style="font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 20px;">
                        <h1 style="color: #ef4444;">Your trial expires soon, {{firstName}}!</h1>
                        <p>Your free trial ends in {{daysLeft}} days. Don't lose access to your {{englishLevel}} course progress!</p>
                        <p>Subscribe now and get:</p>
                        <ul>
                            <li>Unlimited access to all courses</li>
                            <li>Personalized learning paths</li>
                            <li>Certificate of completion</li>
                        </ul>
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="#" style="background-color: #ef4444; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">Subscribe Now</a>
                        </div>
                        <p>Keep learning without interruption!<br>The English Academy Team</p>
                    </div>
                </body>
                </html>
                """;
    }

    private String buildTrialExpirationTemplateText() {
        return """
                Your trial expires soon, {{firstName}}!
                
                Your free trial ends in {{daysLeft}} days. Don't lose access to your {{englishLevel}} course progress!
                
                Subscribe now and get:
                - Unlimited access to all courses
                - Personalized learning paths
                - Certificate of completion
                
                Subscribe Now: [Link]
                
                Keep learning without interruption!
                The English Academy Team
                """;
    }
}
