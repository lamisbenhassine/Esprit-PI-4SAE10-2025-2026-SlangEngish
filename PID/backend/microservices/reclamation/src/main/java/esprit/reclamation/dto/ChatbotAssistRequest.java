package esprit.reclamation.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class ChatbotAssistRequest {

    @NotBlank(message = "Message is required")
    @Size(max = 2000, message = "Message is too long")
    private String message;

    @Size(max = 200, message = "Subject is too long")
    private String sujet;

    @Size(max = 2000, message = "Description is too long")
    private String description;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSujet() {
        return sujet;
    }

    public void setSujet(String sujet) {
        this.sujet = sujet;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
