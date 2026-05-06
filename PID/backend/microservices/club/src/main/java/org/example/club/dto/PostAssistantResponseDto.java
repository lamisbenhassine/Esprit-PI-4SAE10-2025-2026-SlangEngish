package org.example.club.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PostAssistantResponseDto {
    private String titre;
    private String description;
    private List<String> hashtags = new ArrayList<>();
}
