package org.example.club.service;

import org.example.club.dto.ClubFeedbackSummaryDto;
import org.example.club.dto.FeedbackClubDto;
import org.example.club.dto.FeedbackClubRequestDto;
import org.example.club.dto.FeedbackClubStatsDto;

import java.util.List;

public interface FeedbackClubService {
    FeedbackClubDto createOrUpdate(FeedbackClubRequestDto request);
    List<FeedbackClubDto> getByClub(Long clubId);
    List<FeedbackClubDto> getAll();
    FeedbackClubStatsDto getAdminStats();

    /** Moyenne et nombre d'avis par club (affichage public liste des clubs). */
    List<ClubFeedbackSummaryDto> getPublicSummaries();
}

