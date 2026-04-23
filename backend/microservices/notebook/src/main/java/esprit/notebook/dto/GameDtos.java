package esprit.notebook.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

public class GameDtos {

    @Data
    public static class CreateGameRequest {
        private Long teacherId;
        private String title;
        private String description;
        private String type; // "CROSSWORD"
        private Boolean published;
        private List<CreateGameEntryRequest> entries;
    }

    @Data
    public static class CreateGameEntryRequest {
        private Integer order;
        private String clue;
        private String answer;
        private String teacherHint;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GameSummary {
        private Long id;
        private String title;
        private String description;
        private String type;
        private boolean published;
        private int entryCount;
        private Instant createdAt;
        private Instant updatedAt;
        private Long teacherId;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GameEntryView {
        private Long id;
        private Integer order;
        private String clue;
        private Integer answerLength;
        private Integer row;
        private Integer col;
        private String dir; // ACROSS | DOWN
        private Integer number;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GameDetail {
        private Long id;
        private String title;
        private String description;
        private String type;
        private Instant createdAt;
        private Long teacherId;
        /** Crossword grid metadata (no answers). */
        private Integer gridRows;
        private Integer gridCols;
        /** Each string is length gridCols with '1' for a playable cell, '0' for a black cell. */
        private List<String> cellMaskRows;
        private List<GameEntryView> entries;
    }

    @Data
    public static class SubmitAnswerRequest {
        private Long userId;
        private String answer;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SubmitAnswerResponse {
        private boolean correct;
        private boolean solved;
        private int attempts;
        private int hintLevel;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HintResponse {
        private int level;
        private String hint;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProgressRow {
        private Long entryId;
        private boolean solved;
        private int attempts;
        private int hintLevel;
        private String lastAnswer;
    }
}

