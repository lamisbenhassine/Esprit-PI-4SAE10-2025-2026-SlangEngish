package org.example.club.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackClubStatsDto {
    private long totalFeedbacks;
    private double moyenneGlobale;
    private List<NoteCountDto> distributionNotes;
    private List<ClubMoyenneDto> topClubs;
    private List<TendanceMensuelleDto> tendanceMensuelle;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NoteCountDto {
        private int note;
        private long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClubMoyenneDto {
        private Long clubId;
        private String nomClub;
        private double moyenne;
        private long totalAvis;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TendanceMensuelleDto {
        private String mois; // yyyy-MM
        private long totalAvis;
        private double moyenne;
    }
}

