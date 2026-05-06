package com.school.schoolservice.chatbot.controller;

import com.school.schoolservice.chatbot.dto.ChatMessageDto;
import com.school.schoolservice.chatbot.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/message")
    public ResponseEntity<ChatMessageDto> sendMessage(
            @RequestBody ChatMessageDto request) {
        return ResponseEntity.ok(
                chatbotService.processMessage(request.getUserMessage())
        );
    }
}