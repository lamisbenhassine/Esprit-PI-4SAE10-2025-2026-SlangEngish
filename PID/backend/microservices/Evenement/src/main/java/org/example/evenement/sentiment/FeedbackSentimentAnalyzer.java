package org.example.evenement.sentiment;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;

/**
 * Analyse lexicale simple (français) — détecte des formulations négatives / positives.
 * Peut être remplacée plus tard par un appel API NLP / IA.
 */
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

    public FeedbackSentiment analyze(String commentaire, Integer note) {
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

    /**
     * Déclenche une alerte admin (email) : avis très bas ou sentiment négatif détecté dans le texte.
     */
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
