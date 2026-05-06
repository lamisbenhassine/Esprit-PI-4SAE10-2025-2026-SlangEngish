package org.example.club.service;

import org.example.club.dto.ClubChatbotAnswerDto;
import org.example.club.dto.ClubChatbotQuestionDto;

public interface ClubChatbotService {

    ClubChatbotAnswerDto ask(ClubChatbotQuestionDto question);
}
