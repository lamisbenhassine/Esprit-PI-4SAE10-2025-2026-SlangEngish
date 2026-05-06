package esprit.reclamation.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Rule-based sentiment / urgency detection (FR + EN) for reclamation triage.
 * No external API — deterministic and fast.
 */
@Component
public class ReclamationSentimentAnalyzer {

    private static final Pattern NON_WORD = Pattern.compile("[^a-z0-9\\s']+");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    /** Urgency / time pressure */
    private static final String[] URGENT = {
            "urgent", "urgence", "urgently", "immediate", "immediat", "immediately", "asap",
            "critical", "critique", "emergency", "now", "today", "deadline", "right now",
            "as soon", "within 24", "within 48", "ce jour", "aujourdhui", "aujourd hui",
            "vite", "rapidement", "tout de suite", "sans delai", "sans délai", "help",
            "aidez", "svp vite", "please hurry", "hurry", "priority", "priorite", "priorité"
    };

    /** Access / account blocked */
    private static final String[] BLOCKED = {
            "blocked", "bloque", "bloqué", "bloquee", "cannot access", "cant access",
            "can't access", "locked out", "locked", "access denied", "acces refuse",
            "accès refusé", "refused access", "connection refused", "login failed",
            "cannot log", "cant log", "impossible de me connecter", "pas acces", "pas d accès",
            "no access", "suspended", "suspendu", "banned", "banni"
    };

    /** Frustration / anger */
    private static final String[] FRUSTRATED = {
            "frustrated", "frustré", "frustre", "furious", "furieux", "angry", "fache",
            "fâché", "outraged", "unacceptable", "inacceptable", "ridiculous", "ridicule",
            "scandal", "scandaleux", "disgusted", "degoûté", "degoûte", "worst", "pire",
            "never again", "jamais vu", "honte", "shame"
    };

    /** Anxiety / stress */
    private static final String[] ANXIOUS = {
            "worried", "inquiet", "inquiète", "stress", "stressed", "anxious", "anxieux",
            "anxieuse", "afraid", "peur", "fear", "scared", "panicking", "panique"
    };

    public SentimentSnapshot analyze(String sujet, String description) {
        String raw = safe(sujet) + " " + safe(description);
        String normalized = normalizeForMatch(raw);

        int score = 22;
        Set<String> tags = new LinkedHashSet<>();

        int urgentHits = countKeywordHits(normalized, URGENT);
        int blockedHits = countKeywordHits(normalized, BLOCKED);
        int frustratedHits = countKeywordHits(normalized, FRUSTRATED);
        int anxiousHits = countKeywordHits(normalized, ANXIOUS);

        if (urgentHits > 0) {
            score += Math.min(40, 12 + urgentHits * 8);
            tags.add("URGENT");
        }
        if (blockedHits > 0) {
            score += Math.min(35, 10 + blockedHits * 10);
            tags.add("BLOCKED");
        }
        if (frustratedHits > 0) {
            score += Math.min(28, 8 + frustratedHits * 7);
            tags.add("FRUSTRATED");
        }
        if (anxiousHits > 0) {
            score += Math.min(20, 6 + anxiousHits * 5);
            tags.add("ANXIOUS");
        }

        long exclamations = raw.chars().filter(c -> c == '!').count();
        if (exclamations >= 5) {
            score += 14;
        } else if (exclamations >= 3) {
            score += 8;
        }

        score += capsBoost(raw);

        score = Math.min(100, Math.max(0, score));

        if (tags.isEmpty()) {
            tags.add("NEUTRAL");
        }

        int level = mapUrgencyLevel(score, urgentHits, blockedHits);
        String urgency = urgencyCode(level);
        String label = buildLabel(urgency, tags);
        String emotionCsv = String.join(",", tags);

        return new SentimentSnapshot(urgency, emotionCsv, score, label);
    }

    private static int mapUrgencyLevel(int score, int urgentHits, int blockedHits) {
        boolean criticalCombo = urgentHits >= 2 && blockedHits >= 1;
        if (score >= 78 || (criticalCombo && score >= 62)) {
            return 4; // CRITICAL
        }
        if (score >= 58) {
            return 3; // HIGH
        }
        if (score >= 40) {
            return 2; // MEDIUM
        }
        return 1; // LOW
    }

    private static String urgencyCode(int level) {
        switch (level) {
            case 4:
                return "CRITICAL";
            case 3:
                return "HIGH";
            case 2:
                return "MEDIUM";
            default:
                return "LOW";
        }
    }

    private static String buildLabel(String urgencyCode, Set<String> tags) {
        String head = switch (urgencyCode) {
            case "CRITICAL" -> "Critical priority";
            case "HIGH" -> "High priority";
            case "MEDIUM" -> "Medium priority";
            default -> "Low priority";
        };
        List<String> parts = new ArrayList<>();
        parts.add(head);
        for (String t : tags) {
            if (!"NEUTRAL".equals(t)) {
                String pretty = t.charAt(0) + t.substring(1).toLowerCase(Locale.ROOT).replace('_', ' ');
                parts.add(pretty);
            }
        }
        return String.join(" · ", parts);
    }

    private static int capsBoost(String raw) {
        int letters = 0;
        int upper = 0;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isLetter(c)) {
                letters++;
                if (Character.isUpperCase(c)) {
                    upper++;
                }
            }
        }
        if (letters < 12) {
            return 0;
        }
        double ratio = (double) upper / letters;
        if (ratio > 0.45) {
            return 12;
        }
        if (ratio > 0.28) {
            return 6;
        }
        return 0;
    }

    private static int countKeywordHits(String normalized, String[] keywords) {
        int n = 0;
        for (String kw : keywords) {
            String k = normalizeForMatch(kw);
            if (k.isBlank()) {
                continue;
            }
            if (normalized.contains(k)) {
                n++;
            }
        }
        return n;
    }

    private static String normalizeForMatch(String s) {
        String decomposed = Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String alnum = NON_WORD.matcher(decomposed).replaceAll(" ");
        return MULTI_SPACE.matcher(alnum).replaceAll(" ").trim();
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }

    public static final class SentimentSnapshot {
        private final String urgencyLevel;
        private final String emotionTagsCsv;
        private final int priorityScore;
        private final String sentimentLabel;

        public SentimentSnapshot(String urgencyLevel, String emotionTagsCsv, int priorityScore, String sentimentLabel) {
            this.urgencyLevel = urgencyLevel;
            this.emotionTagsCsv = emotionTagsCsv;
            this.priorityScore = priorityScore;
            this.sentimentLabel = sentimentLabel;
        }

        public String getUrgencyLevel() {
            return urgencyLevel;
        }

        public String getEmotionTagsCsv() {
            return emotionTagsCsv;
        }

        public int getPriorityScore() {
            return priorityScore;
        }

        public String getSentimentLabel() {
            return sentimentLabel;
        }
    }
}
