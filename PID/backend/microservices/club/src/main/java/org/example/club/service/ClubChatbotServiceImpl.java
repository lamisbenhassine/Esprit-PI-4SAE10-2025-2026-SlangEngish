package org.example.club.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.club.client.EvenementApiClient;
import org.example.club.dto.ClubChatbotAnswerDto;
import org.example.club.dto.ClubChatbotQuestionDto;
import org.example.club.entity.Club;
import org.example.club.entity.StatutParticipation;
import org.example.club.repository.ClubRepository;
import org.example.club.repository.FeedbackClubRepository;
import org.example.club.repository.ParticipationClubRepository;
import org.example.club.repository.PostClubRepository;
import org.example.club.repository.ReunionClubRepository;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClubChatbotServiceImpl implements ClubChatbotService {

    private final ClubRepository clubRepository;
    private final PostClubRepository postClubRepository;
    private final ReunionClubRepository reunionClubRepository;
    private final ParticipationClubRepository participationClubRepository;
    private final FeedbackClubRepository feedbackClubRepository;
    private final EvenementApiClient evenementApiClient;
    private final RestTemplate ollamaRestTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.ollama.enabled:true}")
    private boolean ollamaEnabled;

    @Value("${app.ollama.base-url:http://127.0.0.1:11434}")
    private String ollamaBaseUrl;

    @Value("${app.ollama.model:llama3}")
    private String ollamaModel;

    public ClubChatbotServiceImpl(
            ClubRepository clubRepository,
            PostClubRepository postClubRepository,
            ReunionClubRepository reunionClubRepository,
            ParticipationClubRepository participationClubRepository,
            FeedbackClubRepository feedbackClubRepository,
            EvenementApiClient evenementApiClient,
            @Qualifier("ollamaRestTemplate") RestTemplate ollamaRestTemplate,
            ObjectMapper objectMapper) {
        this.clubRepository = clubRepository;
        this.postClubRepository = postClubRepository;
        this.reunionClubRepository = reunionClubRepository;
        this.participationClubRepository = participationClubRepository;
        this.feedbackClubRepository = feedbackClubRepository;
        this.evenementApiClient = evenementApiClient;
        this.ollamaRestTemplate = ollamaRestTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ClubChatbotAnswerDto ask(ClubChatbotQuestionDto dto) {
        if (!ollamaEnabled) {
            throw new IllegalStateException("Assistant IA désactivé.");
        }
        if (dto == null || dto.getQuestion() == null) {
            throw new IllegalArgumentException("Posez une question.");
        }
        String q = dto.getQuestion().trim().replaceAll("\\s+", " ");
        if (q.length() < 3) {
            throw new IllegalArgumentException("Question trop courte.");
        }
        if (q.length() > 500) {
            q = q.substring(0, 500);
        }

        String dataJson = buildClubsDataJson();
        String prompt = buildPrompt(dataJson, q);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", ollamaModel);
        body.put("stream", false);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("temperature", 0.4);
        body.put("options", options);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String url = ollamaBaseUrl.replaceAll("/$", "") + "/api/chat";

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = ollamaRestTemplate.postForObject(url, entity, Map.class);
            if (resp == null) {
                throw new IllegalStateException("Réponse Ollama vide.");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) resp.get("message");
            if (message == null) {
                throw new IllegalStateException("Réponse invalide.");
            }
            String content = String.valueOf(message.getOrDefault("content", "")).trim();
            if (content.isEmpty()) {
                throw new IllegalStateException("Réponse vide du modèle.");
            }
            return new ClubChatbotAnswerDto(content);
        } catch (ResourceAccessException e) {
            throw new IllegalStateException(
                    "Ollama injoignable. Lancez « ollama serve » sur cette machine.", e);
        } catch (HttpStatusCodeException e) {
            String details = e.getResponseBodyAsString();
            if (details != null && !details.isBlank()) {
                throw new IllegalStateException("Erreur Ollama: " + details, e);
            }
            throw new IllegalStateException("Erreur Ollama HTTP " + e.getStatusCode().value() + ".", e);
        } catch (RestClientException e) {
            throw new IllegalStateException("Erreur vers Ollama: " + e.getMessage(), e);
        }
    }

    private String buildClubsDataJson() {
        Map<Long, double[]> avisByClub = new LinkedHashMap<>();
        for (Object[] row : feedbackClubRepository.summarizeByClub()) {
            if (row == null || row.length < 3 || row[0] == null || row[1] == null || row[2] == null) {
                continue;
            }
            long cid = ((Number) row[0]).longValue();
            double avg = ((Number) row[1]).doubleValue();
            double cnt = ((Number) row[2]).doubleValue();
            avisByClub.put(cid, new double[] {avg, cnt});
        }
        Map<Long, Long> eventsByClub = evenementApiClient.countEvenementsParClub();

        List<Map<String, Object>> clubs = new ArrayList<>();
        for (Club c : clubRepository.findAll()) {
            if (c.getId() == null) {
                continue;
            }
            Long id = c.getId();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", id);
            row.put("nom", c.getNom());
            row.put("type", c.getType());
            row.put("statut", c.getStatut());
            row.put("description", truncate(c.getDescription(), 180));
            row.put("membresAcceptes", participationClubRepository.countByClub_IdAndStatut(id, StatutParticipation.ACCEPTED));
            long posts = postClubRepository.countByClub_Id(id);
            long reunions = reunionClubRepository.countByClub_Id(id);
            long evts = eventsByClub.getOrDefault(id, 0L);
            row.put("postsMur", posts);
            row.put("reunions", reunions);
            row.put("evenements", evts);
            double[] av = avisByClub.get(id);
            if (av != null) {
                row.put("noteMoyenneAvis", Math.round(av[0] * 10.0) / 10.0);
                row.put("nombreAvis", (int) Math.round(av[1]));
            } else {
                row.put("noteMoyenneAvis", null);
                row.put("nombreAvis", 0);
            }
            row.put("scoreActiviteApprox", posts + reunions + evts);
            clubs.add(row);
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("clubs", clubs);
        root.put(
                "note",
                "scoreActiviteApprox = postsMur + reunions + evenements (indicateur simple, pas officiel).");
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            return "{\"clubs\":[]}";
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.trim().replaceAll("\\s+", " ");
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }

    private static String buildPrompt(String dataJson, String question) {
        return "Tu es l'assistant Campus Clubs pour etudiants. Voici des donnees factuelles agregees (JSON). "
                + "Reponds a la question UNIQUEMENT a partir de ces donnees. Si une information absente du JSON, dis-le clairement. "
                + "Reponse en francais, 2 a 8 phrases, ton amical et clair. Pas de markdown. "
                + "Pour comparer l'activite des clubs, tu peux t'appuyer sur scoreActiviteApprox, postsMur, reunions, evenements, membresAcceptes. "
                + "Ne invente aucun chiffre hors JSON.\n\nDONNEES:\n"
                + dataJson
                + "\n\nQUESTION:\n"
                + question;
    }
}
