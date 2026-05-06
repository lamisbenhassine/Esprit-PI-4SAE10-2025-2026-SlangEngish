package tn.esprit.gestioncours.Services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import tn.esprit.gestioncours.DTO.RecordingHighlightDto;
import tn.esprit.gestioncours.DTO.openai.OpenAiChatCompletionResponse;
import tn.esprit.gestioncours.Entities.Recording;
import tn.esprit.gestioncours.Entities.RecordingAnalysisStatus;
import tn.esprit.gestioncours.Entities.RecordingHighlight;
import tn.esprit.gestioncours.Repositories.RecordingHighlightRepository;
import tn.esprit.gestioncours.Repositories.RecordingRepository;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecordingAnalysisService {

    private static final String HIGHLIGHT_PROMPT = """
            You are an English learning assistant. Analyze this transcription of an English lesson recording.
            Identify 5 to 8 key moments or topic changes and generate highlights.
            For each highlight provide the timestamp in seconds, a short title in English (maximum 5 words),
            and a one sentence description of what is covered.
            Return only a JSON array with objects containing : seconds (integer), title (string), description (string).
            Be precise with timestamps based on the transcription content.
            """;

    private final RecordingRepository recordingRepository;
    private final RecordingHighlightRepository recordingHighlightRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key:}")
    private String groqApiKey;

    @Value("${openai.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqChatUrl;

    @Value("${openai.model:llama-3.1-8b-instant}")
    private String groqModel;

    @Value("${groq.whisper.url:https://api.groq.com/openai/v1/audio/transcriptions}")
    private String groqWhisperUrl;

    @Transactional
    public boolean prepareForAnalysis(Long recordingId) {
        Optional<Recording> optionalRecording = recordingRepository.findById(recordingId);
        if (optionalRecording.isEmpty()) {
            return false;
        }

        Recording recording = optionalRecording.get();
        recording.setAnalysisStatus(RecordingAnalysisStatus.PROCESSING);
        recordingRepository.save(recording);
        recordingHighlightRepository.deleteByRecording_Id(recordingId);
        return true;
    }

    @Transactional
    public void analyzeRecording(Long recordingId) {
        Recording recording = recordingRepository.findById(recordingId)
                .orElseThrow(() -> new IllegalArgumentException("Recording not found: " + recordingId));

        Path videoPath = null;
        Path extractedAudioPath = null;
        Path downloadedVideoPath = null;

        try {
            recording.setAnalysisStatus(RecordingAnalysisStatus.PROCESSING);
            recordingRepository.save(recording);

            VideoSource videoSource = resolveVideoSource(recording.getRecordingLink());
            videoPath = videoSource.path();
            downloadedVideoPath = videoSource.temporaryDownloadPath();

            extractedAudioPath = extractAudioToMp3(videoPath);
            String transcriptionText = transcribeWithWhisper(extractedAudioPath);
            List<HighlightCandidate> generatedHighlights = generateHighlights(transcriptionText);

            List<RecordingHighlight> entities = generatedHighlights.stream()
                    .map(item -> {
                        RecordingHighlight highlight = new RecordingHighlight();
                        highlight.setRecording(recording);
                        highlight.setSeconds(item.seconds());
                        highlight.setTitle(item.title());
                        highlight.setDescription(item.description());
                        highlight.setCreatedAt(LocalDateTime.now());
                        return highlight;
                    })
                    .toList();

            recordingHighlightRepository.deleteByRecording_Id(recordingId);
            recordingHighlightRepository.saveAll(entities);

            recording.setAnalysisStatus(RecordingAnalysisStatus.COMPLETED);
            recordingRepository.save(recording);
        } catch (Exception e) {
            log.error("Recording analysis failed for id {}", recordingId, e);
            recording.setAnalysisStatus(RecordingAnalysisStatus.FAILED);
            recordingRepository.save(recording);
        } finally {
            deleteIfExists(extractedAudioPath);
            deleteIfExists(downloadedVideoPath);
        }
    }

    public List<RecordingHighlightDto> getHighlights(Long recordingId) {
        return recordingHighlightRepository.findByRecording_IdOrderBySecondsAsc(recordingId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public RecordingAnalysisStatus getStatus(Long recordingId) {
        return recordingRepository.findById(recordingId)
                .map(Recording::getAnalysisStatus)
                .orElse(RecordingAnalysisStatus.FAILED);
    }

    private RecordingHighlightDto toDto(RecordingHighlight entity) {
        return new RecordingHighlightDto(
                entity.getId(),
                entity.getRecording().getId(),
                entity.getSeconds(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCreatedAt()
        );
    }

    private Path extractAudioToMp3(Path videoPath) throws Exception {
        Path mp3Path = Files.createTempFile("recording_audio_", ".mp3");

        AudioAttributes audio = new AudioAttributes();
        audio.setCodec("libmp3lame");
        audio.setBitRate(128000);
        audio.setChannels(1);
        audio.setSamplingRate(44100);

        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setOutputFormat("mp3");
        attrs.setAudioAttributes(audio);

        Encoder encoder = new Encoder();
        encoder.encode(new MultimediaObject(videoPath.toFile()), mp3Path.toFile(), attrs);
        return mp3Path;
    }

    private String transcribeWithWhisper(Path audioPath) throws Exception {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(audioPath.toFile()));
        body.add("model", "whisper-large-v3");
        body.add("response_format", "verbose_json");
        body.add("timestamp_granularities[]", "segment");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(groqApiKey.trim());

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(groqWhisperUrl, request, String.class);

        JsonNode root = objectMapper.readTree(response.getBody());
        String text = root.path("text").asText("").trim();

        if (!text.isBlank()) {
            return text;
        }

        JsonNode segments = root.path("segments");
        List<String> merged = new ArrayList<>();
        if (segments.isArray()) {
            for (JsonNode segment : segments) {
                String segmentText = segment.path("text").asText("").trim();
                if (!segmentText.isBlank()) {
                    merged.add(segmentText);
                }
            }
        }
        return String.join(" ", merged);
    }

    private List<HighlightCandidate> generateHighlights(String transcriptionText) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", groqModel);
        body.put("temperature", 0.2);
        body.put("messages", List.of(
                Map.of("role", "system", "content", HIGHLIGHT_PROMPT),
                Map.of("role", "user", "content", transcriptionText)
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey.trim());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<OpenAiChatCompletionResponse> response =
                restTemplate.postForEntity(groqChatUrl, request, OpenAiChatCompletionResponse.class);

        if (response.getBody() == null
                || response.getBody().getChoices() == null
                || response.getBody().getChoices().isEmpty()
                || response.getBody().getChoices().get(0).getMessage() == null) {
            throw new IllegalStateException("Invalid Groq highlight response");
        }

        String content = response.getBody().getChoices().get(0).getMessage().getContent();
        JsonNode parsedNode = objectMapper.readTree(extractJson(content));

        List<HighlightCandidate> candidates = new ArrayList<>();
        if (parsedNode.isArray()) {
            for (JsonNode node : parsedNode) {
                candidates.add(toHighlightCandidate(node));
            }
        } else if (parsedNode.isObject()) {
            JsonNode arrayNode = parsedNode.path("highlights");
            if (arrayNode.isArray()) {
                for (JsonNode node : arrayNode) {
                    candidates.add(toHighlightCandidate(node));
                }
            }
        }

        return candidates.stream()
                .filter(item -> item.seconds() >= 0)
                .sorted(Comparator.comparingInt(HighlightCandidate::seconds))
                .limit(8)
                .toList();
    }

    private HighlightCandidate toHighlightCandidate(JsonNode node) {
        int seconds = node.path("seconds").asInt(0);
        String title = node.path("title").asText("Chapter").trim();
        String description = node.path("description").asText("").trim();
        return new HighlightCandidate(seconds, title, description);
    }

    private VideoSource resolveVideoSource(String recordingLink) throws Exception {
        if (recordingLink == null || recordingLink.isBlank()) {
            throw new IllegalArgumentException("Recording link is empty");
        }

        String mediaSegment = "/media/recordings/";
        int mediaIndex = recordingLink.indexOf(mediaSegment);
        if (mediaIndex >= 0) {
            String fileName = recordingLink.substring(mediaIndex + mediaSegment.length());
            fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);
            Path localPath = Paths.get(System.getProperty("user.dir"), "uploads", "recordings", fileName);
            if (Files.exists(localPath)) {
                return new VideoSource(localPath, null);
            }
        }

        Path asPath = Paths.get(recordingLink);
        if (Files.exists(asPath)) {
            return new VideoSource(asPath, null);
        }

        if (recordingLink.startsWith("http://") || recordingLink.startsWith("https://")) {
            Path tempVideo = Files.createTempFile("recording_video_", ".mp4");
            URL url = URI.create(recordingLink).toURL();
            try (InputStream inputStream = url.openStream()) {
                Files.copy(inputStream, tempVideo, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return new VideoSource(tempVideo, tempVideo);
        }

        throw new IllegalStateException("Could not resolve recording video source");
    }

    void markFailed(Long recordingId) {
        recordingRepository.findById(recordingId).ifPresent(recording -> {
            recording.setAnalysisStatus(RecordingAnalysisStatus.FAILED);
            recordingRepository.save(recording);
        });
    }

    private static String extractJson(String content) {
        String trimmed = content == null ? "" : content.trim();
        Pattern fence = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
        Matcher matcher = fence.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return trimmed;
    }

    private void deleteIfExists(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (Exception e) {
            log.warn("Could not delete temporary file {}", path, e);
        }
    }

    private record HighlightCandidate(int seconds, String title, String description) {}

    private record VideoSource(Path path, Path temporaryDownloadPath) {}
}
