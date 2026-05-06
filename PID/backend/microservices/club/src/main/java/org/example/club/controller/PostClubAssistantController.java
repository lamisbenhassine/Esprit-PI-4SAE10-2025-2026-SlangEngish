package org.example.club.controller;

import org.example.club.dto.PostAssistantRequestDto;
import org.example.club.dto.PostAssistantResponseDto;
import org.example.club.service.PostClubAssistantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts-club/assistant")
@CrossOrigin(origins = "*")
public class PostClubAssistantController {

    @Autowired
    private PostClubAssistantService assistantService;

    @PostMapping("/suggest")
    public ResponseEntity<?> suggest(@RequestBody PostAssistantRequestDto request) {
        try {
            PostAssistantResponseDto dto = assistantService.suggest(request);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage());
        }
    }
}
