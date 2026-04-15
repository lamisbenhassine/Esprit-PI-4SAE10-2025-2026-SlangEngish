package esprit.reclamation.service;

import esprit.reclamation.dto.BackfillMlResponseDto;
import esprit.reclamation.dto.ReclamationAdminPageDto;
import esprit.reclamation.entity.Reclamation;
import esprit.reclamation.entity.StudentReclamationBlock;
import esprit.reclamation.repository.ReclamationRepository;
import esprit.reclamation.repository.StudentReclamationBlockRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ReclamationServiceImpl implements ReclamationService {

    private final ReclamationRepository reclamationRepository;
    private final StudentReclamationBlockRepository studentReclamationBlockRepository;
    private final ReclamationSentimentAnalyzer sentimentAnalyzer;
    private final ReclamationMlClassifierClient mlClassifierClient;
    private final List<String> badWords;
    private final List<Pattern> badWordPatterns;
    private final int autoResolveMinDistinctStudents;
    private final String autoResolveMessage;

    public ReclamationServiceImpl(
            ReclamationRepository reclamationRepository,
            StudentReclamationBlockRepository studentReclamationBlockRepository,
            ReclamationSentimentAnalyzer sentimentAnalyzer,
            ReclamationMlClassifierClient mlClassifierClient,
            @Value("${app.moderation.bad-words:insulte,idiot,stupid,fuck,shit}") String badWordsConfig,
            @Value("${app.reclamation.auto-resolve.min-distinct-students:3}") int autoResolveMinDistinctStudents,
            @Value("${app.reclamation.auto-resolve.message:This request was automatically closed after three or more different students reported the same subject.}") String autoResolveMessage
    ) {
        this.reclamationRepository = reclamationRepository;
        this.studentReclamationBlockRepository = studentReclamationBlockRepository;
        this.sentimentAnalyzer = sentimentAnalyzer;
        this.mlClassifierClient = mlClassifierClient;
        this.badWords = Arrays.stream(badWordsConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());
        this.badWordPatterns = this.badWords.stream()
                .map(word -> Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE))
                .collect(Collectors.toList());
        this.autoResolveMinDistinctStudents = autoResolveMinDistinctStudents;
        this.autoResolveMessage = autoResolveMessage == null || autoResolveMessage.isBlank()
                ? "This request was automatically closed after three or more different students reported the same subject."
                : autoResolveMessage.trim();
    }

    @Override
    @Transactional
    public Reclamation create(Reclamation reclamation) {
        if (reclamation.getStudentId() == null) {
            throw new RuntimeException("studentId est obligatoire");
        }
        reclamation.setUrgencyLevel(null);
        reclamation.setEmotionTags(null);
        reclamation.setPriorityScore(null);
        reclamation.setSentimentLabel(null);
        reclamation.setCategorieMl(null);
        enforceStudentCooldownIfReported(reclamation.getStudentId());

        String rawSujet = reclamation.getSujet();
        String rawDesc = reclamation.getDescription();

        applySentiment(reclamation, rawSujet, rawDesc);
        applyMlCategory(reclamation, rawSujet, rawDesc);
        ModerationResult moderation = moderateText(rawSujet, rawDesc);
        reclamation.setSujet(moderation.sanitizedSujet);
        reclamation.setDescription(moderation.sanitizedDescription);
        reclamation.setContainsBadWords(moderation.containsBadWords);
        reclamation.setIssueKey(normalizeIssueKey(reclamation.getSujet()));
        Reclamation saved = reclamationRepository.save(reclamation);
        applyAutoResolveForSameIssue(saved.getIssueKey());
        return reclamationRepository.findById(saved.getId()).orElse(saved);
    }

    @Override
    public ReclamationAdminPageDto getAdminPage(int page, int size, String categorieMl, String sort) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        String ml = normalizeMlFilter(categorieMl);
        boolean sortByMl = sort != null && "mlCategory".equalsIgnoreCase(sort.trim());
        Page<Reclamation> result = sortByMl
                ? reclamationRepository.findAdminByMlFilterOrderByMlCategory(ml, pageable)
                : reclamationRepository.findAdminByMlFilterOrderByPriority(ml, pageable);
        long pending = reclamationRepository.countByStatut("EN_ATTENTE");
        long inProgress = reclamationRepository.countInProgressStatuses();
        long processed = reclamationRepository.countByStatut("RESOLUE");
        return new ReclamationAdminPageDto(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize(),
                pending,
                inProgress,
                processed
        );
    }

    @Override
    public List<String> getDistinctMlCategories() {
        return reclamationRepository.findDistinctCategorieMlOrdered();
    }

    @Override
    @Transactional
    public BackfillMlResponseDto backfillMlCategories(int limit) {
        int cap = Math.min(Math.max(limit, 1), 500);
        Pageable p = PageRequest.of(0, cap);
        List<Reclamation> rows = reclamationRepository.findByCategorieMlIsNullOrderByCreatedAtDesc(p).getContent();
        int updated = 0;
        int skipped = 0;
        for (Reclamation r : rows) {
            Optional<String> cat = mlClassifierClient.predictCategory(r.getSujet(), r.getDescription());
            if (cat.isPresent()) {
                r.setCategorieMl(cat.get());
                reclamationRepository.save(r);
                updated++;
            } else {
                skipped++;
            }
        }
        return new BackfillMlResponseDto(updated, skipped);
    }

    private static String normalizeMlFilter(String categorieMl) {
        if (categorieMl == null) {
            return "";
        }
        String t = categorieMl.trim();
        return t;
    }

    @Override
    public List<Reclamation> getByStudentId(Long studentId) {
        return reclamationRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
    }

    @Override
    public List<Reclamation> getUnreadNotifications(Long studentId) {
        return reclamationRepository.findByStudentIdAndNotificationReadFalseAndReponseAdminIsNotNullOrderByCreatedAtDesc(studentId);
    }

    @Override
    public Reclamation getById(Long id) {
        return reclamationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reclamation introuvable avec l'id : " + id));
    }

    @Override
    @Transactional
    public Reclamation update(Long id, Reclamation reclamation) {
        Reclamation existing = getById(id);
        String rawSujet = reclamation.getSujet();
        String rawDesc = reclamation.getDescription();
        applySentiment(existing, rawSujet, rawDesc);
        applyMlCategory(existing, rawSujet, rawDesc);
        ModerationResult moderation = moderateText(rawSujet, rawDesc);
        existing.setSujet(moderation.sanitizedSujet);
        existing.setDescription(moderation.sanitizedDescription);
        existing.setContainsBadWords(moderation.containsBadWords);
        existing.setIssueKey(normalizeIssueKey(existing.getSujet()));
        if (reclamation.getStudentId() != null) {
            existing.setStudentId(reclamation.getStudentId());
        }
        Reclamation saved = reclamationRepository.save(existing);
        applyAutoResolveForSameIssue(saved.getIssueKey());
        return reclamationRepository.findById(saved.getId()).orElse(saved);
    }

    @Override
    public Reclamation traiterParAdmin(Long id, String statut, String reponseAdmin) {
        Reclamation existing = getById(id);
        if (statut == null || statut.isBlank()) {
            throw new RuntimeException("Le statut est obligatoire");
        }
        if (reponseAdmin == null || reponseAdmin.isBlank()) {
            throw new RuntimeException("La reponse admin est obligatoire");
        }
        existing.setStatut(statut);
        existing.setReponseAdmin(reponseAdmin.trim());
        existing.setNotificationRead(false);
        return reclamationRepository.save(existing);
    }

    @Override
    public Reclamation markNotificationAsRead(Long id) {
        Reclamation existing = getById(id);
        existing.setNotificationRead(true);
        return reclamationRepository.save(existing);
    }

    @Override
    public Reclamation reportStudent(Long id, String reportReason) {
        Reclamation existing = getById(id);
        if (reportReason == null || reportReason.isBlank()) {
            throw new RuntimeException("Report reason is required");
        }
        existing.setStudentReported(true);
        existing.setReportReason(reportReason.trim());
        existing.setReportedAt(LocalDateTime.now());
        Reclamation saved = reclamationRepository.save(existing);

        LocalDateTime blockedUntil = LocalDateTime.now().plusDays(3);
        StudentReclamationBlock block = studentReclamationBlockRepository.findByStudentId(existing.getStudentId())
                .orElseGet(StudentReclamationBlock::new);
        block.setStudentId(existing.getStudentId());
        block.setBlockedUntil(blockedUntil);
        studentReclamationBlockRepository.save(block);

        return saved;
    }

    @Override
    public Reclamation unblockStudent(Long id) {
        Reclamation existing = getById(id);
        Long studentId = existing.getStudentId();

        studentReclamationBlockRepository.findByStudentId(studentId)
                .ifPresent(studentReclamationBlockRepository::delete);

        // Also clear report flags on all reported reclamations for this student
        List<Reclamation> studentReclamations = reclamationRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
        for (Reclamation rec : studentReclamations) {
            if (Boolean.TRUE.equals(rec.getStudentReported())) {
                rec.setStudentReported(false);
                rec.setReportReason(null);
                rec.setReportedAt(null);
                reclamationRepository.save(rec);
            }
        }

        return getById(id);
    }

    @Override
    public void delete(Long id) {
        Reclamation existing = getById(id);
        reclamationRepository.delete(existing);
    }

    private ModerationResult moderateText(String sujet, String description) {
        String sanitizedSujet = safe(sujet);
        String sanitizedDescription = safe(description);
        boolean containsBadWords = false;

        for (Pattern pattern : badWordPatterns) {
            if (pattern.matcher(sanitizedSujet).find() || pattern.matcher(sanitizedDescription).find()) {
                containsBadWords = true;
            }
            sanitizedSujet = pattern.matcher(sanitizedSujet).replaceAll("*****");
            sanitizedDescription = pattern.matcher(sanitizedDescription).replaceAll("*****");
        }

        return new ModerationResult(sanitizedSujet, sanitizedDescription, containsBadWords);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    /**
     * Same issue = same normalized subject line (case and spacing ignored).
     */
    static String normalizeIssueKey(String sujet) {
        if (sujet == null) {
            return "";
        }
        return sujet.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    /**
     * When the number of distinct students with an open reclamation for the same issue reaches the
     * configured minimum or exceeds it (e.g. 3, 4, …), resolve all open tickets for that issue.
     */
    private void applyAutoResolveForSameIssue(String issueKey) {
        if (autoResolveMinDistinctStudents <= 0) {
            return;
        }
        if (issueKey == null || issueKey.isBlank()) {
            return;
        }
        long distinct = reclamationRepository.countDistinctStudentsOpenByIssueKey(issueKey);
        if (distinct < autoResolveMinDistinctStudents) {
            return;
        }
        List<Reclamation> open = reclamationRepository.findAllOpenByIssueKey(issueKey);
        for (Reclamation r : open) {
            r.setStatut("RESOLUE");
            r.setReponseAdmin(autoResolveMessage);
            r.setNotificationRead(false);
            reclamationRepository.save(r);
        }
        reclamationRepository.flush();
    }

    private void applySentiment(Reclamation target, String sujet, String description) {
        ReclamationSentimentAnalyzer.SentimentSnapshot snap = sentimentAnalyzer.analyze(sujet, description);
        target.setUrgencyLevel(snap.getUrgencyLevel());
        target.setEmotionTags(snap.getEmotionTagsCsv());
        target.setPriorityScore(snap.getPriorityScore());
        target.setSentimentLabel(snap.getSentimentLabel());
    }

    private void applyMlCategory(Reclamation target, String sujet, String description) {
        mlClassifierClient.predictCategory(sujet, description).ifPresent(target::setCategorieMl);
    }

    private void enforceStudentCooldownIfReported(Long studentId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusDays(3);

        // Fallback safety: if any report exists in last 3 days, block too.
        boolean reportedInLast3Days = reclamationRepository
                .existsByStudentIdAndStudentReportedTrueAndReportedAtAfter(studentId, threshold);
        if (reportedInLast3Days) {
            throw new RuntimeException("You were reported because of your language and cannot create a new reclamation for 3 days. Please consult the administration.");
        }

        studentReclamationBlockRepository.findByStudentId(studentId)
                .ifPresent(block -> {
                    LocalDateTime blockedUntil = block.getBlockedUntil();
                    if (blockedUntil != null && blockedUntil.isAfter(now)) {
                        throw new RuntimeException("You were reported because of your language and cannot create a new reclamation for 3 days. Please consult the administration. Blocked until: " + blockedUntil);
                    }
                });
    }

    private static class ModerationResult {
        private final String sanitizedSujet;
        private final String sanitizedDescription;
        private final boolean containsBadWords;

        private ModerationResult(String sanitizedSujet, String sanitizedDescription, boolean containsBadWords) {
            this.sanitizedSujet = sanitizedSujet;
            this.sanitizedDescription = sanitizedDescription;
            this.containsBadWords = containsBadWords;
        }
    }
}
