package esprit.reclamation.service;

import esprit.reclamation.dto.ChatbotAssistRequest;
import esprit.reclamation.dto.ChatbotAssistResponse;

public interface ReclamationChatbotService {
    ChatbotAssistResponse assist(ChatbotAssistRequest request);
}
