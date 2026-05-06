package org.example.club.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.club.dto.PostAssistantRequestDto;
import org.example.club.dto.PostAssistantResponseDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PostClubAssistantServiceImpl implements PostClubAssistantService {

    private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*}");

    private final RestTemplate ollamaRestTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.ollama.enabled:true}")
    private boolean ollamaEnabled;

    @Value("${app.ollama.base-url:http://127.0.0.1:11434}")
    private String ollamaBaseUrl;

    @Value("${app.ollama.model:llama3}")
    private String ollamaModel;

    public PostClubAssistantServiceImpl(
            @Qualifier("ollamaRestTemplate") RestTemplate ollamaRestTemplate,
            ObjectMapper objectMapper) {
        this.ollamaRestTemplate = ollamaRestTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public PostAssistantResponseDto suggest(PostAssistantRequestDto request) {
        if (!ollamaEnabled) {
            throw new IllegalStateException("Assistant IA désactivé (app.ollama.enabled=false).");
        }
        if (request == null || request.getKeywords() == null) {
            throw new IllegalArgumentException("Indiquez des mots-clés ou une idée de post.");
        }
        String raw = request.getKeywords().trim().replaceAll("\\s+", " ");
        if (raw.length() < 3) {
            throw new IllegalArgumentException("Au moins 3 caractères (titre ou mots-clés).");
        }
        if (raw.length() > 500) {
            raw = raw.substring(0, 500);
        }
        String club = request.getClubNom() != null && !request.getClubNom().isBlank()
                ? request.getClubNom().trim()
                : "club étudiant";

        String prompt = buildPrompt(raw, club);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", ollamaModel);
        body.put("stream", false);
        body.put("format", "json");
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("temperature", 0.65);
        body.put("options", options);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        final String url = ollamaBaseUrl.replaceAll("/$", "") + "/api/chat";
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = ollamaRestTemplate.postForObject(url, entity, Map.class);
            if (resp == null) {
                throw new IllegalStateException("Réponse Ollama vide.");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) resp.get("message");
            if (message == null) {
                throw new IllegalStateException("Réponse Ollama invalide (pas de message).");
            }
            String content = String.valueOf(message.getOrDefault("content", "")).trim();
            return parseAssistantJson(content);
        } catch (ResourceAccessException e) {
            throw new IllegalStateException(
                    "Impossible de joindre Ollama sur " + ollamaBaseUrl
                            + ". Lancez « ollama serve » et vérifiez le modèle « " + ollamaModel + " ».",
                    e);
        } catch (HttpStatusCodeException e) {
            String details = e.getResponseBodyAsString();
            if (details != null && !details.isBlank()) {
                throw new IllegalStateException("Erreur Ollama: " + details, e);
            }
            throw new IllegalStateException("Erreur Ollama HTTP " + e.getStatusCode().value() + ".", e);
        } catch (RestClientException e) {
            throw new IllegalStateException("Erreur HTTP vers Ollama: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Analyse de la réponse IA impossible: " + e.getMessage(), e);
        }
    }

    private static String buildPrompt(String keywords, String clubNom) {
        String jsonShape = "{\"titre\":\"...\",\"description\":\"...\",\"hashtags\":[\"...\",\"...\"]}";
        return "Tu es redacteur ou redactrice pour la vie associative etudiante (France). "
                + "L'admin donne des mots-cles ou un titre bref pour un post sur le mur d'un club.\n\n"
                + "Reponds UNIQUEMENT avec un objet JSON valide UTF-8, sans markdown ni texte avant ou apres, exactement cette structure :\n"
                + jsonShape
                + "\n\nContraintes :\n"
                + "- titre : une ligne percutante, max 90 caracteres, en francais.\n"
                + "- description : 2 a 4 phrases, ton chaleureux et clair, annonce concrete pour les membres.\n"
                + "- hashtags : tableau de 5 a 8 chaines sans symbole diese, courts (un ou deux mots), pertinents pour etudiants, clubs, ecole.\n\n"
                + "Contexte club : "
                + clubNom
                + "\nIdee / mots-cles : "
                + keywords;
    }

    private PostAssistantResponseDto parseAssistantJson(String content) {
        String json = extractJsonObject(content);
        final JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON renvoyé par le modèle invalide.", e);
        }
        PostAssistantResponseDto dto = new PostAssistantResponseDto();
        if (root.hasNonNull("titre")) {
            dto.setTitre(root.get("titre").asText().trim());
        }
        if (root.hasNonNull("description")) {
            dto.setDescription(root.get("description").asText().trim());
        }
        List<String> tags = new ArrayList<>();
        if (root.has("hashtags") && root.get("hashtags").isArray()) {
            for (JsonNode n : root.get("hashtags")) {
                if (n.isTextual()) {
                    String t = n.asText().trim().replaceFirst("^#+", "");
                    if (!t.isEmpty() && tags.size() < 12) {
                        tags.add(t);
                    }
                }
            }
        }
        dto.setHashtags(tags);

        if (dto.getTitre() == null || dto.getTitre().isEmpty()) {
            dto.setTitre("Annonce club");
        }
        if (dto.getDescription() == null || dto.getDescription().isEmpty()) {
            dto.setDescription(content.length() > 2000 ? content.substring(0, 2000) : content);
        }
        if (dto.getHashtags().isEmpty()) {
            dto.getHashtags().addAll(List.of("vieetudiante", "club", "campus"));
        }
        return dto;
    }

    private static String extractJsonObject(String content) {
        if (content == null || content.isEmpty()) {
            return "{}";
        }
        String c = content.trim();
        if (c.startsWith("{")) {
            return c;
        }
        Matcher m = JSON_BLOCK.matcher(c);
        if (m.find()) {
            return m.group();
        }
        return c;
    }
}
