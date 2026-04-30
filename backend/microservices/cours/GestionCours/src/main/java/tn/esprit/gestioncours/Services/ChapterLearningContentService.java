package tn.esprit.gestioncours.Services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tn.esprit.gestioncours.DTO.ChapterLearningContentResponseDto;
import tn.esprit.gestioncours.DTO.FlashcardItemDto;
import tn.esprit.gestioncours.DTO.openai.ChapterLearningAiPayload;
import tn.esprit.gestioncours.DTO.openai.OpenAiChatCompletionResponse;
import tn.esprit.gestioncours.Entities.Chapter;
import tn.esprit.gestioncours.Entities.ChapterLearningContent;
import tn.esprit.gestioncours.Repositories.ChapterLearningContentRepository;
import tn.esprit.gestioncours.Repositories.ChapterRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChapterLearningContentService {

    private static final String SYSTEM_PROMPT = """
            You are a friendly English teacher assistant for language learners (levels A2–B2).
            Given a chapter title and description, produce learning aids.

            Return ONLY valid JSON with exactly these keys:
            - summary (string): a short summary of what the chapter covers, in SIMPLE English, 5 to 8 sentences max, warm encouraging tone, no jargon unless explained.
            - flashcards (array of 8 to 10 objects): each object has "front" and "back" (strings).
              For grammar chapters: front = a clear question (e.g. "When do we use the Present Perfect?"), back = a concise answer with one short example if helpful.
              For vocabulary chapters: front = the English word or phrase, back = a clear definition in simple English plus one example sentence using the word.

            Do not wrap the JSON in markdown fences.""";

    private final ChapterRepository chapterRepository;
    private final ChapterLearningContentRepository learningContentRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key:}")
    private String openAiApiKey;

    @Value("${openai.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String openAiUrl;

    @Value("${openai.model:${openai.api.model:llama-3.1-8b-instant}}")
    private String openAiModel;

    @Value("${openai.fallback.mock-on-error:true}")
    private boolean mockOnOpenAiError;

    /** Vide si chapitre inconnu ou aucun contenu généré encore. */
    public Optional<ChapterLearningContentResponseDto> getCached(Long chapterId) {
        if (!chapterRepository.existsById(chapterId)) {
            return Optional.empty();
        }
        return learningContentRepository.findByChapter_IdChapter(chapterId)
                .map(row -> toDto(chapterId, row, true));
    }

    @Transactional
    public ChapterLearningContentResponseDto generateOrGet(Long chapterId, boolean regenerate) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new IllegalArgumentException("Chapter not found: " + chapterId));

        if (!regenerate) {
            var existing = learningContentRepository.findByChapter_IdChapter(chapterId);
            if (existing.isPresent()) {
                return toDto(chapterId, existing.get(), true);
            }
        }

        String title = chapter.getName() == null ? "" : chapter.getName();
        String description = chapter.getDescription() == null ? "" : chapter.getDescription();

        ChapterLearningAiPayload ai = runLlm(title, description);
        List<FlashcardItemDto> cards = normalizeFlashcards(ai.getFlashcards());
        String summary = ai.getSummary() == null ? "" : ai.getSummary().trim();

        ChapterLearningContent row = learningContentRepository.findByChapter_IdChapter(chapterId)
                .orElseGet(() -> {
                    ChapterLearningContent n = new ChapterLearningContent();
                    n.setChapter(chapter);
                    return n;
                });
        row.setChapter(chapter);
        row.setSummaryEnglish(summary);
        try {
            row.setFlashcardsJson(objectMapper.writeValueAsString(cards));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize flashcards", e);
        }
        ChapterLearningContent saved = learningContentRepository.save(row);
        return toDto(chapterId, saved, false);
    }

    private ChapterLearningContentResponseDto toDto(Long chapterId, ChapterLearningContent row, boolean fromCache) {
        List<FlashcardItemDto> cards;
        try {
            cards = objectMapper.readValue(row.getFlashcardsJson(), new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("Bad flashcards JSON for chapter {}", chapterId, e);
            cards = List.of();
        }
        return ChapterLearningContentResponseDto.builder()
                .chapterId(chapterId)
                .summaryEnglish(row.getSummaryEnglish() == null ? "" : row.getSummaryEnglish())
                .flashcards(cards)
                .updatedAt(row.getUpdatedAt())
                .fromCache(fromCache)
                .build();
    }

    private List<FlashcardItemDto> normalizeFlashcards(List<ChapterLearningAiPayload.FlashcardAi> raw) {
        if (raw == null) {
            return List.of();
        }
        return raw.stream()
                .filter(f -> f != null && f.getFront() != null && f.getBack() != null)
                .map(f -> FlashcardItemDto.builder()
                        .front(f.getFront().trim())
                        .back(f.getBack().trim())
                        .build())
                .filter(f -> !f.getFront().isEmpty() && !f.getBack().isEmpty())
                .limit(12)
                .collect(Collectors.toList());
    }

    private ChapterLearningAiPayload runLlm(String title, String description) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            log.warn("openai.api.key is not set — returning mock chapter learning content");
            return mockPayload(title, description);
        }
        try {
            return callGroq(title, description);
        } catch (Exception e) {
            log.error("Groq chapter learning failed", e);
            if (mockOnOpenAiError) {
                return mockPayload(title, description);
            }
            throw new IllegalStateException(rootMessage(e), e);
        }
    }

    private static String rootMessage(Throwable e) {
        Throwable c = e;
        while (c.getCause() != null && c.getCause() != c) {
            c = c.getCause();
        }
        String m = c.getMessage();
        return m != null ? m : c.getClass().getSimpleName();
    }

    private ChapterLearningAiPayload callGroq(String title, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", openAiModel);
        body.put("response_format", Map.of("type", "json_object"));
        body.put("temperature", 0.45);

        String userContent = "Chapter title: " + title + "\n\nChapter description:\n" + description;

        body.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", userContent)
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey.trim());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<OpenAiChatCompletionResponse> response;
        try {
            response = restTemplate.postForEntity(openAiUrl, entity, OpenAiChatCompletionResponse.class);
        } catch (HttpStatusCodeException e) {
            String snippet = truncate(e.getResponseBodyAsString(), 800);
            throw new IllegalStateException(
                    "Groq HTTP " + e.getStatusCode().value() + (snippet.isBlank() ? "" : ": " + snippet), e);
        } catch (RestClientException e) {
            throw new IllegalStateException("Groq network error: " + e.getMessage(), e);
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null
                || response.getBody().getChoices() == null
                || response.getBody().getChoices().isEmpty()
                || response.getBody().getChoices().get(0).getMessage() == null) {
            throw new IllegalStateException("Invalid Groq response");
        }

        String content = response.getBody().getChoices().get(0).getMessage().getContent();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("Empty Groq message content");
        }
        String json = extractJson(content);
        try {
            return objectMapper.readValue(json, ChapterLearningAiPayload.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not parse AI JSON: " + truncate(json, 400), e);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim().replaceAll("\\s+", " ");
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }

    private static String extractJson(String content) {
        String trimmed = content.trim();
        Pattern fence = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
        Matcher m = fence.matcher(trimmed);
        if (m.find()) {
            return m.group(1).trim();
        }
        return trimmed;
    }

    private ChapterLearningAiPayload mockPayload(String title, String description) {
        ChapterLearningAiPayload p = new ChapterLearningAiPayload();
        p.setSummary(
                "This chapter is called \"" + (title.isBlank() ? "this lesson" : title) + "\". "
                        + "It helps you build your English step by step. "
                        + (description.isBlank()
                        ? "Read the materials carefully and note new words."
                        : "Here is the main idea in simple words: " + truncate(description, 400))
                        + " Take your time, review the flashcards, and try to use one new phrase today.");
        List<ChapterLearningAiPayload.FlashcardAi> cards = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            ChapterLearningAiPayload.FlashcardAi c = new ChapterLearningAiPayload.FlashcardAi();
            c.setFront("Demo question " + i + " about \"" + truncate(title, 40) + "\"?");
            c.setBack("This is a placeholder answer (no API key). Add openai.api.key for real AI flashcards.");
            cards.add(c);
        }
        p.setFlashcards(cards);
        return p;
    }
}
