package esprit.reclamation.dto;

public class ChatbotAssistResponse {

    private String reply;
    private String suggestedSubject;
    private String suggestedDescription;
    private boolean aiUsed;

    public ChatbotAssistResponse() {
    }

    public ChatbotAssistResponse(String reply, String suggestedSubject, String suggestedDescription, boolean aiUsed) {
        this.reply = reply;
        this.suggestedSubject = suggestedSubject;
        this.suggestedDescription = suggestedDescription;
        this.aiUsed = aiUsed;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getSuggestedSubject() {
        return suggestedSubject;
    }

    public void setSuggestedSubject(String suggestedSubject) {
        this.suggestedSubject = suggestedSubject;
    }

    public String getSuggestedDescription() {
        return suggestedDescription;
    }

    public void setSuggestedDescription(String suggestedDescription) {
        this.suggestedDescription = suggestedDescription;
    }

    public boolean isAiUsed() {
        return aiUsed;
    }

    public void setAiUsed(boolean aiUsed) {
        this.aiUsed = aiUsed;
    }
}
