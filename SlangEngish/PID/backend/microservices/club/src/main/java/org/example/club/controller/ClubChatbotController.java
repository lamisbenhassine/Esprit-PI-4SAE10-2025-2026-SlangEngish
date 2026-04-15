package org.example.club.controller;

import org.example.club.dto.ClubChatbotAnswerDto;
import org.example.club.dto.ClubChatbotQuestionDto;
import org.example.club.service.ClubChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clubs/chatbot")
@CrossOrigin(origins = "*")
public class ClubChatbotController {

    @Autowired
    private ClubChatbotService chatbotService;

    @PostMapping("/ask")
    public ResponseEntity<?> ask(@RequestBody ClubChatbotQuestionDto body) {
        try {
            ClubChatbotAnswerDto answer = chatbotService.ask(body);
            return ResponseEntity.ok(answer);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage());
        }
    }
}
