package org.example.club.service;

import org.example.club.client.UserApiClient;
import org.example.club.client.UserInfoDto;
import org.example.club.dto.AdminPostEngagementRowDto;
import org.example.club.dto.PostCommentViewDto;
import org.example.club.dto.PostEngagementSummaryDto;
import org.example.club.entity.PostClub;
import org.example.club.entity.PostComment;
import org.example.club.entity.PostReaction;
import org.example.club.entity.PostReactionType;
import org.example.club.entity.StatutParticipation;
import org.example.club.repository.ParticipationClubRepository;
import org.example.club.repository.PostClubRepository;
import org.example.club.repository.PostCommentRepository;
import org.example.club.repository.PostReactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class PostClubEngagementServiceImpl implements PostClubEngagementService {

    private static final DateTimeFormatter ISO_DT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final PostClubRepository postClubRepository;
    private final PostReactionRepository postReactionRepository;
    private final PostCommentRepository postCommentRepository;
    private final ParticipationClubRepository participationClubRepository;
    private final UserApiClient userApiClient;
    private final PostCommentAiService postCommentAiService;

    public PostClubEngagementServiceImpl(
            PostClubRepository postClubRepository,
            PostReactionRepository postReactionRepository,
            PostCommentRepository postCommentRepository,
            ParticipationClubRepository participationClubRepository,
            UserApiClient userApiClient,
            PostCommentAiService postCommentAiService) {
        this.postClubRepository = postClubRepository;
        this.postReactionRepository = postReactionRepository;
        this.postCommentRepository = postCommentRepository;
        this.participationClubRepository = participationClubRepository;
        this.userApiClient = userApiClient;
        this.postCommentAiService = postCommentAiService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostEngagementSummaryDto> batchSummaries(List<Long> postIds, Long viewerId) {
        if (postIds == null || postIds.isEmpty()) {
            return List.of();
        }
        List<Long> ids = postIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return List.of();
        }

        List<PostReaction> reactions = postReactionRepository.findByPost_IdIn(ids);
        Map<Long, EnumMap<PostReactionType, Long>> countsByPost = new LinkedHashMap<>();
        Map<Long, PostReactionType> viewerByPost = new LinkedHashMap<>();

        for (Long pid : ids) {
            countsByPost.put(pid, new EnumMap<>(PostReactionType.class));
            for (PostReactionType t : PostReactionType.values()) {
                countsByPost.get(pid).put(t, 0L);
            }
        }

        for (PostReaction r : reactions) {
            Long pid = r.getPost().getId();
            EnumMap<PostReactionType, Long> m = countsByPost.get(pid);
            if (m != null) {
                m.merge(r.getReactionType(), 1L, Long::sum);
            }
            if (viewerId != null && viewerId.equals(r.getIdEtudiant())) {
                viewerByPost.put(pid, r.getReactionType());
            }
        }

        List<PostEngagementSummaryDto> out = new ArrayList<>();
        for (Long pid : ids) {
            PostEngagementSummaryDto dto = new PostEngagementSummaryDto();
            dto.setPostId(pid);
            dto.setCommentCount(postCommentRepository.countByPost_Id(pid));
            EnumMap<PostReactionType, Long> m = countsByPost.get(pid);
            Map<String, Long> rc = new LinkedHashMap<>();
            if (m != null) {
                for (PostReactionType t : PostReactionType.values()) {
                    long c = m.getOrDefault(t, 0L);
                    if (c > 0) {
                        rc.put(t.name(), c);
                    }
                }
            }
            dto.setReactionCounts(rc);
            PostReactionType mine = viewerByPost.get(pid);
            dto.setMyReaction(mine != null ? mine.name() : null);
            out.add(dto);
        }
        return out;
    }

    @Override
    public void setReaction(Long postId, Long userId, PostReactionType type) {
        if (postId == null || userId == null || type == null) {
            throw new IllegalArgumentException("Paramètres invalides");
        }
        assertActiveMember(postId, userId);

        PostClub post = postClubRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post introuvable"));

        Optional<PostReaction> existing = postReactionRepository.findByPost_IdAndIdEtudiant(postId, userId);
        if (existing.isPresent() && existing.get().getReactionType() == type) {
            postReactionRepository.delete(existing.get());
            return;
        }
        if (existing.isPresent()) {
            PostReaction r = existing.get();
            r.setReactionType(type);
            postReactionRepository.save(r);
            return;
        }
        PostReaction r = new PostReaction();
        r.setPost(post);
        r.setIdEtudiant(userId);
        r.setReactionType(type);
        postReactionRepository.save(r);
    }

    @Override
    public void clearReaction(Long postId, Long userId) {
        if (postId == null || userId == null) {
            throw new IllegalArgumentException("Paramètres invalides");
        }
        assertActiveMember(postId, userId);
        postReactionRepository.deleteByPost_IdAndIdEtudiant(postId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostCommentViewDto> listComments(Long postId) {
        if (postId == null) {
            return List.of();
        }
        if (!postClubRepository.existsById(postId)) {
            throw new IllegalArgumentException("Post introuvable");
        }
        return postCommentRepository.findByPost_IdOrderByDateCreationAsc(postId).stream()
                .map(this::toCommentView)
                .toList();
    }

    @Override
    public PostCommentViewDto addComment(Long postId, Long userId, String contenu, boolean translateToEnglish) {
        if (postId == null || userId == null) {
            throw new IllegalArgumentException("Paramètres invalides");
        }
        String text = contenu == null ? "" : contenu.trim();
        if (text.isEmpty() || text.length() > 2000) {
            throw new IllegalArgumentException("Le commentaire doit contenir entre 1 et 2000 caractères");
        }
        assertActiveMember(postId, userId);
        PostClub post = postClubRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post introuvable"));
        PostCommentAiService.ReviewResult ai = postCommentAiService.reviewComment(text, translateToEnglish);
        PostComment c = new PostComment();
        c.setPost(post);
        c.setIdAuteur(userId);
        c.setContenu(ai.correctedText());
        PostComment saved = postCommentRepository.save(c);
        return toCommentView(saved, ai.sentiment(), ai.corrected(), ai.translatedToEnglish());
    }

    @Override
    public void deleteOwnComment(Long commentId, Long userId) {
        if (commentId == null || userId == null) {
            throw new IllegalArgumentException("Paramètres invalides");
        }
        PostComment c = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Commentaire introuvable"));
        if (!userId.equals(c.getIdAuteur())) {
            throw new IllegalArgumentException("Vous ne pouvez supprimer que vos propres commentaires");
        }
        postCommentRepository.delete(c);
    }

    @Override
    public void deleteCommentAsAdmin(Long commentId) {
        if (commentId == null) {
            throw new IllegalArgumentException("Identifiant requis");
        }
        PostComment c = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Commentaire introuvable"));
        postCommentRepository.delete(c);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminPostEngagementRowDto> adminEngagementPage(Long clubIdFilter, Pageable pageable) {
        Page<PostClub> page = clubIdFilter == null
                ? postClubRepository.findAllPagedRecent(pageable)
                : postClubRepository.findPagedByClub(clubIdFilter, pageable);

        List<Long> ids = page.getContent().stream().map(PostClub::getId).toList();
        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, page.getTotalElements());
        }

        List<PostReaction> allReactions = postReactionRepository.findByPost_IdIn(ids);
        Map<Long, Map<PostReactionType, Long>> reactionAgg = new LinkedHashMap<>();
        for (Long id : ids) {
            reactionAgg.put(id, new EnumMap<>(PostReactionType.class));
        }
        for (PostReaction r : allReactions) {
            Long pid = r.getPost().getId();
            reactionAgg.computeIfAbsent(pid, k -> new EnumMap<>(PostReactionType.class))
                    .merge(r.getReactionType(), 1L, Long::sum);
        }

        List<PostComment> allComments = postCommentRepository.findAllForPosts(ids);
        Map<Long, List<PostComment>> commentsByPost = allComments.stream()
                .collect(Collectors.groupingBy(c -> c.getPost().getId(), LinkedHashMap::new, Collectors.toList()));

        List<AdminPostEngagementRowDto> rows = new ArrayList<>();
        for (PostClub p : page.getContent()) {
            AdminPostEngagementRowDto row = new AdminPostEngagementRowDto();
            row.setPostId(p.getId());
            row.setClubId(p.getClub() != null ? p.getClub().getId() : null);
            row.setClubNom(p.getClub() != null ? p.getClub().getNom() : "");
            String contenu = p.getContenu() != null ? p.getContenu() : "";
            row.setContenuApercu(contenu.length() > 200 ? contenu.substring(0, 200) + "…" : contenu);
            row.setDatePost(p.getDatePost() != null ? p.getDatePost().toString() : "");

            Map<PostReactionType, Long> rm = reactionAgg.getOrDefault(p.getId(), new EnumMap<>(PostReactionType.class));
            Map<String, Long> rc = new LinkedHashMap<>();
            long totalR = 0;
            for (PostReactionType t : PostReactionType.values()) {
                long c = rm.getOrDefault(t, 0L);
                if (c > 0) {
                    rc.put(t.name(), c);
                }
                totalR += c;
            }
            row.setReactionCounts(rc);
            row.setTotalReactions(totalR);

            List<PostComment> clist = commentsByPost.getOrDefault(p.getId(), List.of());
            row.setTotalComments(clist.size());
            row.setComments(clist.stream().map(this::toCommentView).toList());
            rows.add(row);
        }
        return new PageImpl<>(rows, pageable, page.getTotalElements());
    }

    private void assertActiveMember(Long postId, Long userId) {
        PostClub post = postClubRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post introuvable"));
        Long clubId = post.getClub().getId();
        if (!participationClubRepository.existsByIdEtudiantAndClubIdAndStatut(
                userId, clubId, StatutParticipation.ACCEPTED)) {
            throw new IllegalArgumentException(
                    "Seuls les membres actifs du club peuvent réagir ou commenter ce post.");
        }
    }

    private PostCommentViewDto toCommentView(PostComment c) {
        return toCommentView(c, null, false, false);
    }

    private PostCommentViewDto toCommentView(
            PostComment c,
            String aiSentiment,
            boolean aiCorrectionApplied,
            boolean aiTranslatedToEnglish) {
        String nom = displayName(userApiClient.getUserById(c.getIdAuteur()));
        return new PostCommentViewDto(
                c.getId(),
                c.getIdAuteur(),
                nom,
                c.getContenu(),
                c.getDateCreation() != null ? ISO_DT.format(c.getDateCreation()) : "",
                aiSentiment,
                aiCorrectionApplied,
                aiTranslatedToEnglish
        );
    }

    private static String displayName(UserInfoDto u) {
        if (u == null) {
            return "Utilisateur";
        }
        String n = (u.getFirstName() != null ? u.getFirstName() : "").trim();
        String ln = (u.getLastName() != null ? u.getLastName() : "").trim();
        String full = (n + " " + ln).trim();
        if (!full.isEmpty()) {
            return full;
        }
        return u.getEmail() != null ? u.getEmail() : "Utilisateur #" + u.getId();
    }
}
