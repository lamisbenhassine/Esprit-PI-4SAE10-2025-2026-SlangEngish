package com.school.schoolservice.chatbot.service;

import com.school.schoolservice.chatbot.dto.ChatMessageDto;

public interface ChatbotService {
    ChatMessageDto processMessage(String message);
}