package tn.esprit.gestioncours.Services;



import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.apache.pdfbox.Loader;

import org.apache.pdfbox.pdmodel.PDDocument;

import org.apache.pdfbox.text.PDFTextStripper;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.HttpEntity;

import org.springframework.http.HttpHeaders;

import org.springframework.http.HttpMethod;

import org.springframework.http.MediaType;

import org.springframework.http.ResponseEntity;

import org.springframework.stereotype.Service;

import org.springframework.web.client.HttpStatusCodeException;

import org.springframework.web.client.RestClientException;

import org.springframework.web.client.RestTemplate;

import org.springframework.web.multipart.MultipartFile;

import tn.esprit.gestioncours.DTO.ChapterPresentationResponseDto;

import tn.esprit.gestioncours.DTO.PresentationSlideDto;

import tn.esprit.gestioncours.DTO.openai.OpenAiChatCompletionResponse;

import tn.esprit.gestioncours.DTO.openai.PresentationAiPayload;

import tn.esprit.gestioncours.Entities.Chapter;

import tn.esprit.gestioncours.Entities.ChapterPresentation;

import tn.esprit.gestioncours.Repositories.ChapterPresentationRepository;

import tn.esprit.gestioncours.Repositories.ChapterRepository;



import java.io.IOException;

import java.nio.charset.StandardCharsets;

import java.nio.file.Files;

import java.nio.file.Path;

import java.nio.file.Paths;

import java.util.ArrayList;

import java.util.HashMap;

import java.util.List;

import java.util.Map;

import java.util.Objects;

import java.util.Optional;

import java.util.regex.Matcher;

import java.util.regex.Pattern;

import java.util.stream.Collectors;



@Service

@RequiredArgsConstructor

@Slf4j

public class PresentationService {



    private static final String PRESENTATION_SYSTEM_PROMPT = """

            You are an English learning assistant. From this chapter content generate a structured presentation with exactly 6 slides. Each slide has a title and 3 short bullet points maximum. Then write a natural narration script for the full presentation. Return a JSON with two fields : slides (array of objects with title and bullets array) and narration (full script as a single string).

            Return ONLY valid JSON. Do not wrap the JSON in markdown code fences.""";

    private final ChapterRepository chapterRepository;

    private final ChapterPresentationRepository presentationRepository;

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



    @Value("${chapter.presentation.storage-dir:./data/chapter-presentations}")

    private String storageDir;



    public Optional<ChapterPresentationResponseDto> getCached(Long chapterId) {

        if (!chapterRepository.existsById(chapterId)) {

            return Optional.empty();

        }

        return presentationRepository.findByChapter_IdChapter(chapterId)

                .map(row -> toDto(chapterId, row, true));

    }



    public ChapterPresentationResponseDto generateFromPdf(Long chapterId, MultipartFile pdf, boolean regenerate) {

        Chapter chapter = chapterRepository.findById(chapterId)

                .orElseThrow(() -> new IllegalArgumentException("Chapter not found: " + chapterId));



        if (!regenerate) {

            Optional<ChapterPresentation> existing = presentationRepository.findByChapter_IdChapter(chapterId);

            if (existing.isPresent()) {

                return toDto(chapterId, existing.get(), true);

            }

        }



        validatePdf(pdf);

        byte[] pdfBytes;

        try {

            pdfBytes = pdf.getBytes();

        } catch (IOException e) {

            throw new IllegalStateException("Could not read uploaded file", e);

        }



        String extracted = extractTextFromPdf(pdfBytes);

        if (extracted.isBlank()) {

            throw new IllegalArgumentException("No extractable text in PDF");

        }



        String forLlm = truncate(extracted, 16_000);

        PresentationAiPayload ai = runPresentationLlm(forLlm);

        List<PresentationSlideDto> slides = normalizeSlides(ai.getSlides());

        String narration = ai.getNarration() == null || ai.getNarration().isBlank()

                ? buildNarrationFallback(slides)

                : ai.getNarration().trim();



        Path root = Paths.get(storageDir).toAbsolutePath().normalize();

        Path chapterDir = root.resolve(String.valueOf(chapterId));

        try {

            Files.createDirectories(chapterDir);

        } catch (IOException e) {

            throw new IllegalStateException("Could not create storage directory", e);

        }



        // Voice feature removed: keep silent presentation (auto-advance on client).
        String audioPathSaved = null;



        ChapterPresentation row = presentationRepository.findByChapter_IdChapter(chapterId)

                .orElseGet(() -> {

                    ChapterPresentation n = new ChapterPresentation();

                    n.setChapter(chapter);

                    return n;

                });

        row.setChapter(chapter);

        try {

            row.setSlidesJson(objectMapper.writeValueAsString(slides));

        } catch (JsonProcessingException e) {

            throw new IllegalStateException("Could not serialize slides", e);

        }

        row.setAudioPath(audioPathSaved);

        presentationRepository.save(row);

        return toDto(chapterId, row, false);

    }



    private void validatePdf(MultipartFile pdf) {

        if (pdf == null || pdf.isEmpty()) {

            throw new IllegalArgumentException("PDF file is required");

        }

        String ct = pdf.getContentType();

        String name = pdf.getOriginalFilename() == null ? "" : pdf.getOriginalFilename().toLowerCase();

        boolean okType = ct != null && ct.toLowerCase().contains("pdf");

        boolean okName = name.endsWith(".pdf");

        if (!okType && !okName) {

            throw new IllegalArgumentException("File must be a PDF");

        }

    }



    private String extractTextFromPdf(byte[] pdfBytes) {

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {

            PDFTextStripper stripper = new PDFTextStripper();

            int pages = document.getNumberOfPages();

            stripper.setStartPage(1);

            stripper.setEndPage(Math.min(pages, 50));

            String text = stripper.getText(document);

            return text == null ? "" : text.trim();

        } catch (IOException e) {

            throw new IllegalStateException("PDF read error: " + e.getMessage(), e);

        }

    }



    private PresentationAiPayload runPresentationLlm(String chapterText) {

        if (openAiApiKey == null || openAiApiKey.isBlank()) {

            log.warn("openai.api.key missing — mock presentation");

            return mockPresentationPayload(chapterText);

        }

        try {

            return callGroqPresentation(chapterText);

        } catch (Exception e) {

            log.error("Groq presentation generation failed", e);

            if (mockOnOpenAiError) {

                return mockPresentationPayload(chapterText);

            }

            throw new IllegalStateException(rootMessage(e), e);

        }

    }



    private PresentationAiPayload callGroqPresentation(String chapterText) {

        Map<String, Object> body = new HashMap<>();

        body.put("model", openAiModel);

        body.put("response_format", Map.of("type", "json_object"));

        body.put("temperature", 0.4);



        String userContent = "Chapter content extracted from PDF:\n\n" + chapterText;



        body.put("messages", List.of(

                Map.of("role", "system", "content", PRESENTATION_SYSTEM_PROMPT),

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

            return objectMapper.readValue(json, PresentationAiPayload.class);

        } catch (JsonProcessingException e) {

            throw new IllegalStateException("Could not parse AI JSON: " + truncate(json, 400), e);

        }

    }



    // Voice feature removed: no ElevenLabs call.



    private List<PresentationSlideDto> normalizeSlides(List<PresentationAiPayload.SlideAi> raw) {

        List<PresentationSlideDto> out = new ArrayList<>();

        if (raw != null) {

            for (PresentationAiPayload.SlideAi s : raw) {

                if (s == null || s.getTitle() == null || s.getTitle().isBlank()) {

                    continue;

                }

                List<String> bullets = s.getBullets() == null ? new ArrayList<>() : s.getBullets().stream()

                        .filter(Objects::nonNull)

                        .map(String::trim)

                        .filter(t -> !t.isEmpty())

                        .limit(3)

                        .collect(Collectors.toCollection(ArrayList::new));

                if (bullets.isEmpty()) {

                    bullets.add("Key point from this part of the chapter.");

                }

                while (bullets.size() < 2) {

                    bullets.add("Practice this idea with a short example.");

                }

                out.add(PresentationSlideDto.builder()

                        .title(s.getTitle().trim())

                        .bullets(bullets.stream().limit(3).collect(Collectors.toList()))

                        .build());

                if (out.size() >= 10) {

                    break;

                }

            }

        }

        while (out.size() < 6) {

            int n = out.size() + 1;

            out.add(PresentationSlideDto.builder()

                    .title("Slide " + n)

                    .bullets(List.of(

                            "Review the PDF section related to this topic.",

                            "Write one sentence summarizing what you learned."))

                    .build());

        }

        if (out.size() > 6) {

            return new ArrayList<>(out.subList(0, 6));

        }

        return out;

    }



    private static String buildNarrationFallback(List<PresentationSlideDto> slides) {

        StringBuilder sb = new StringBuilder();

        sb.append("Welcome to this chapter explainer. ");

        for (int i = 0; i < slides.size(); i++) {

            PresentationSlideDto s = slides.get(i);

            sb.append("Slide ").append(i + 1).append(": ").append(s.getTitle()).append(". ");

            if (s.getBullets() != null) {

                for (String b : s.getBullets()) {

                    sb.append(b).append(" ");

                }

            }

        }

        sb.append("That wraps up this short presentation. Keep practicing!");

        return sb.toString().trim();

    }



    private PresentationAiPayload mockPresentationPayload(String chapterText) {

        String snippet = truncate(chapterText.replaceAll("\\s+", " "), 400);

        List<PresentationSlideDto> slideDtos = new ArrayList<>();

        for (int i = 1; i <= 6; i++) {

            slideDtos.add(PresentationSlideDto.builder()

                    .title("Demo slide " + i)

                    .bullets(List.of(

                            "Placeholder content — configure openai.api.key for real AI.",

                            "Your PDF excerpt: " + (snippet.isEmpty() ? "…" : snippet),

                            "Upload again after setting API keys."))

                    .build());

        }

        PresentationAiPayload p = new PresentationAiPayload();

        List<PresentationAiPayload.SlideAi> raw = new ArrayList<>();

        for (PresentationSlideDto dto : slideDtos) {

            PresentationAiPayload.SlideAi ai = new PresentationAiPayload.SlideAi();

            ai.setTitle(dto.getTitle());

            ai.setBullets(new ArrayList<>(dto.getBullets()));

            raw.add(ai);

        }

        p.setSlides(raw);

        p.setNarration(buildNarrationFallback(slideDtos));

        return p;

    }



    private ChapterPresentationResponseDto toDto(Long chapterId, ChapterPresentation row, boolean fromCache) {

        List<PresentationSlideDto> slides;

        try {

            slides = objectMapper.readValue(row.getSlidesJson(), new TypeReference<>() {

            });

        } catch (JsonProcessingException e) {

            log.warn("Bad slides JSON for chapter {}", chapterId, e);

            slides = List.of();

        }

        return ChapterPresentationResponseDto.builder()

                .chapterId(chapterId)

                .slides(slides)

                .audioAvailable(false)

                .updatedAt(row.getUpdatedAt())

                .fromCache(fromCache)

                .build();

    }



    private static String rootMessage(Throwable e) {

        Throwable c = e;

        while (c.getCause() != null && c.getCause() != c) {

            c = c.getCause();

        }

        String m = c.getMessage();

        return m != null ? m : c.getClass().getSimpleName();

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

}

