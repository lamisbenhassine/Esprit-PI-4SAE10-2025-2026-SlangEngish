package esprit.inscription.exception;

public class MailingException extends RuntimeException {
    
    public MailingException(String message) {
        super(message);
    }
    
    public MailingException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static class CampaignNotFoundException extends MailingException {
        public CampaignNotFoundException(Long campaignId) {
            super("Campaign not found: " + campaignId);
        }
    }
    
    public static class TemplateNotFoundException extends MailingException {
        public TemplateNotFoundException(Long templateId) {
            super("Template not found: " + templateId);
        }
    }
    
    public static class EmailSendException extends MailingException {
        public EmailSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class TemplateProcessingException extends MailingException {
        public TemplateProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class ValidationException extends MailingException {
        public ValidationException(String message) {
            super(message);
        }
    }
    
    public static class RateLimitExceededException extends MailingException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }
}
