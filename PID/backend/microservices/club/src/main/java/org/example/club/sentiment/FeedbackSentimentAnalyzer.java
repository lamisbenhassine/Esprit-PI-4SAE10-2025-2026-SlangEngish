package org.example.club.sentiment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FeedbackSentimentAnalyzer {

    private static final List<String> NEGATIVE_PHRASES = Arrays.asList(
            "mauvaise organisation", "mauvaise orga", "mal organise", "mal organisé", "mal organisee",
            "nul", "nulle", "horrible", "catastroph", "désastre", "decevant", "décevant", "déçu", "decu",
            "honte", "inadmissible", "scandale", "n importe quoi", "n'importe quoi", "perte de temps",
            "trop cher", "arnaque", "incompetent", "incompétent", "desagreable", "désagréable",
            "sale", "salete", "proprete", "propreté", "bruyant", "froid", "mauvais accueil",
            "personnel rude", "mal informe", "mal informé", "retard", "en retard", "annule", "annulé",
            "pas content", "pas satisfait", "insatisfait", "deplorable", "déplorable", "mediocre", "médiocre",
            "aucun interet", "aucun intérêt", "je deconseille", "je déconseille", "a eviter", "à éviter"
    );

    private static final List<String> POSITIVE_PHRASES = Arrays.asList(
            "super", "excellent", "genial", "génial", "merci", "parfait", "top", "ravie", "ravi",
            "content", "satisfait", "recommande", "recommandé", "bravo", "felicitation", "félicitation",
            "tres bien", "très bien", "bonne organisation", "bien organise", "bien organisé", "agreable", "agréable"
    );

    private final RestTemplate ollamaRestTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.ollama.enabled:true}")
    private boolean ollamaEnabled;

    @Value("${app.ollama.base-url:http://127.0.0.1:11434}")
    private String ollamaBaseUrl;

    @Value("${app.ollama.model:llama3}")
    private String ollamaModel;

    public FeedbackSentimentAnalyzer(
            @Qualifier("ollamaRestTemplate") RestTemplate ollamaRestTemplate,
            ObjectMapper objectMapper) {
        this.ollamaRestTemplate = ollamaRestTemplate;
        this.objectMapper = objectMapper;
    }

    public FeedbackSentiment analyze(String commentaire, Integer note) {
        FeedbackSentiment aiSentiment = analyzeWithOllama(commentaire, note);
        if (aiSentiment != null) {
            return aiSentiment;
        }
        return analyzeWithRules(commentaire, note);
    }

    private FeedbackSentiment analyzeWithRules(String commentaire, Integer note) {
        String t = normalize(commentaire);
        int neg = 0;
        int pos = 0;
        for (String phrase : NEGATIVE_PHRASES) {
            String pn = normalize(phrase);
            if (!pn.isEmpty() && t.contains(pn)) {
                neg += pn.split(" ").length >= 2 ? 3 : 2;
            }
        }
        for (String phrase : POSITIVE_PHRASES) {
            String pn = normalize(phrase);
            if (!pn.isEmpty() && t.contains(pn)) {
                pos += pn.split(" ").length >= 2 ? 2 : 1;
            }
        }
        if (note != null) {
            if (note <= 1) neg += 4;
            else if (note == 2) neg += 2;
            else if (note == 3) neg += 1;
            if (note >= 4) pos += 2;
            if (note == 5) pos += 1;
        }
        if (t.isBlank() && note != null) {
            if (note <= 2) return FeedbackSentiment.NEGATIVE;
            if (note >= 4) return FeedbackSentiment.POSITIVE;
            return FeedbackSentiment.NEUTRAL;
        }
        if (neg > pos) return FeedbackSentiment.NEGATIVE;
        if (pos > neg) return FeedbackSentiment.POSITIVE;
        return FeedbackSentiment.NEUTRAL;
    }

    private FeedbackSentiment analyzeWithOllama(String commentaire, Integer note) {
        if (!ollamaEnabled) {
            return null;
        }
        String text = commentaire == null ? "" : commentaire.trim();
        if (text.isEmpty() && note == null) {
            return null;
        }
        try {
            String prompt = buildPrompt(text, note);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", ollamaModel);
            body.put("stream", false);
            body.put("format", "json");
            body.put("messages", List.of(Map.of("role", "user", "content", prompt)));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = ollamaRestTemplate.postForObject(
                    ollamaBaseUrl.replaceAll("/$", "") + "/api/chat",
                    entity,
                    Map.class);
            if (response == null || !(response.get("message") instanceof Map<?, ?> msg)) {
                return null;
            }
            Object contentRaw = msg.get("content");
            String content = contentRaw == null ? "" : String.valueOf(contentRaw).trim();
            if (content.isEmpty()) {
                return null;
            }
            JsonNode root = objectMapper.readTree(content);
            String sentiment = root.path("sentiment").asText("").trim().toUpperCase();
            if ("NEGATIVE".equals(sentiment)) {
                return FeedbackSentiment.NEGATIVE;
            }
            if ("POSITIVE".equals(sentiment)) {
                return FeedbackSentiment.POSITIVE;
            }
            if ("NEUTRAL".equals(sentiment)) {
                return FeedbackSentiment.NEUTRAL;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String buildPrompt(String commentaire, Integer note) {
        return "Analyse ce feedback étudiant et renvoie UNIQUEMENT un JSON valide : "
                + "{\"sentiment\":\"POSITIVE|NEUTRAL|NEGATIVE\"}. "
                + "Utilise le texte + la note.\n"
                + "Commentaire: " + commentaire + "\n"
                + "Note: " + (note == null ? "null" : note);
    }

    public boolean shouldNotifyAdmin(FeedbackSentiment sentiment, String commentaire, Integer note) {
        if (sentiment == FeedbackSentiment.NEGATIVE) return true;
        if (note != null && note <= 2) return true;
        String t = normalize(commentaire);
        if (!t.isEmpty()) {
            for (String phrase : NEGATIVE_PHRASES) {
                String pn = normalize(phrase);
                if (!pn.isEmpty() && t.contains(pn)) return true;
            }
        }
        return false;
    }

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String lower = raw.toLowerCase().trim();
        String decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "");
    }
}
