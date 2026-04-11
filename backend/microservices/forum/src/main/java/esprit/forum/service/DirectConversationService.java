package esprit.forum.service;

import esprit.forum.client.UserClient;
import esprit.forum.client.dto.UserProfileDto;
import esprit.forum.dto.DirectConversationSummaryDto;
import esprit.forum.entity.DirectConversation;
import esprit.forum.entity.DirectMessage;
import esprit.forum.repository.DirectConversationRepository;
import esprit.forum.repository.DirectMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DirectConversationService {

    private final DirectConversationRepository conversationRepository;
    private final DirectMessageRepository messageRepository;
    private final UserClient userClient;

    private static boolean isTutor(UserProfileDto u) {
        return u != null && u.getAccountRole() != null && "TUTOR".equalsIgnoreCase(u.getAccountRole().trim());
    }

    private static long low(long a, long b) {
        return Math.min(a, b);
    }

    private static long high(long a, long b) {
        return Math.max(a, b);
    }

    public void assertParticipant(Long conversationId, Long userId) {
        DirectConversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation introuvable"));
        if (!Objects.equals(c.getParticipantLowId(), userId) && !Objects.equals(c.getParticipantHighId(), userId)) {
            throw new IllegalArgumentException("Accès refusé à cette conversation");
        }
    }

    @Transactional
    public DirectConversation openOrGet(OpenParams params) {
        Long uid = params.userId();
        Long other = params.withUserId();
        DirectConversation.DirectKind requested = params.kind();
        if (uid == null || other == null || Objects.equals(uid, other)) {
            throw new IllegalArgumentException("Utilisateurs invalides");
        }
        if (requested == null) {
            throw new IllegalArgumentException("Type de discussion requis (PEER ou WITH_TUTOR)");
        }

        UserProfileDto uSelf = userClient.getUser(uid);
        UserProfileDto uOther = userClient.getUser(other);
        if (uSelf == null || uOther == null) {
            throw new IllegalArgumentException("Utilisateur introuvable");
        }

        boolean selfTutor = isTutor(uSelf);
        boolean otherTutor = isTutor(uOther);

        DirectConversation.DirectKind stored;
        if (requested == DirectConversation.DirectKind.PEER) {
            if (selfTutor != otherTutor) {
                throw new IllegalArgumentException(
                        "Pour contacter un tuteur, utilisez l’onglet « Avec un tuteur ».");
            }
            stored = DirectConversation.DirectKind.PEER;
        } else {
            if (selfTutor == otherTutor) {
                throw new IllegalArgumentException(
                        "Cette discussion doit réunir exactement un tuteur et un apprenant.");
            }
            stored = DirectConversation.DirectKind.WITH_TUTOR;
        }

        long lo = low(uid, other);
        long hi = high(uid, other);

        return conversationRepository.findByParticipantLowIdAndParticipantHighId(lo, hi)
                .map(existing -> {
                    if (existing.getKind() != stored) {
                        throw new IllegalArgumentException("Une conversation existe déjà avec un autre type");
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    DirectConversation c = new DirectConversation();
                    c.setParticipantLowId(lo);
                    c.setParticipantHighId(hi);
                    c.setKind(stored);
                    return conversationRepository.save(c);
                });
    }

    public List<DirectConversationSummaryDto> inbox(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId requis");
        }
        return conversationRepository.findForUserOrderByUpdated(userId).stream()
                .map(c -> toSummary(c, userId))
                .collect(Collectors.toList());
    }

    private DirectConversationSummaryDto toSummary(DirectConversation c, Long viewerId) {
        DirectConversationSummaryDto d = new DirectConversationSummaryDto();
        d.setId(c.getId());
        d.setOtherUserId(Objects.equals(c.getParticipantLowId(), viewerId)
                ? c.getParticipantHighId()
                : c.getParticipantLowId());
        d.setKind(c.getKind());
        d.setUpdatedAt(c.getUpdatedAt());
        messageRepository.findTopByConversation_IdOrderByCreatedAtDesc(c.getId())
                .ifPresent(m -> {
                    d.setLastMessagePreview(trimPreview(m.getContent()));
                    d.setLastMessageId(m.getId());
                    d.setLastMessageSenderId(m.getSenderId());
                });
        return d;
    }

    private static String trimPreview(String content) {
        if (content == null) {
            return "";
        }
        String t = content.trim();
        return t.length() > 120 ? t.substring(0, 117) + "…" : t;
    }

    public List<DirectMessage> messages(Long conversationId, Long viewerUserId) {
        assertParticipant(conversationId, viewerUserId);
        return messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId);
    }

    @Transactional
    public DirectMessage send(Long conversationId, Long senderId, String content) {
        assertParticipant(conversationId, senderId);
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message vide");
        }
        DirectConversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation introuvable"));
        DirectMessage m = new DirectMessage();
        m.setConversation(c);
        m.setSenderId(senderId);
        m.setContent(content.trim());
        DirectMessage saved = messageRepository.save(m);
        c.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(c);
        return saved;
    }

    public record OpenParams(Long userId, Long withUserId, DirectConversation.DirectKind kind) {}
}
