package com.school.schoolservice.chatbot.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private String userMessage;
    private String botResponse;
    private List<JobOfferSuggestion> suggestions;
    private String intent; // SEARCH, APPLY, INFO, GREETING, UNKNOWN
    private String language; // fr, en

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JobOfferSuggestion {
        private Long id;
        private String title;
        private String company;
        private String location;
        private String contractType;
        private String salary;
        private int matchPercent;
    }
}