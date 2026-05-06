package org.example.club.service;

import org.example.club.dto.PostAssistantRequestDto;
import org.example.club.dto.PostAssistantResponseDto;

public interface PostClubAssistantService {

    PostAssistantResponseDto suggest(PostAssistantRequestDto request);
}
