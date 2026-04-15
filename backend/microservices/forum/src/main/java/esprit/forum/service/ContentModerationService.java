package esprit.forum.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Détection de vocabulaire grossier (mots entiers, insensible à la casse).
 */
@Service
public class ContentModerationService {

    public static final String REJECT_MESSAGE =
            "Ce message ne peut pas être envoyé : vocabulaire inapproprié détecté.";

    private static final Pattern PROFANITY =
            Pattern.compile(
                    "\\b(?:"
                            + "fuck|fucking|fucked|fucker|fucks|"
                            + "shit|bullshit|bitch|bastard|asshole|"
                            + "wtf|wth|stfu|lmfao|"
                            + "damn|crap|piss|pissed|cunt|slut|whore|dick|cock|"
                            + "putain|merde|connard|salope|enculé|encule"
                            + ")\\b",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public void assertTextAcceptable(String content) {
        if (!StringUtils.hasText(content)) {
            return;
        }
        if (PROFANITY.matcher(content).find()) {
            throw new IllegalArgumentException(REJECT_MESSAGE);
        }
    }

    public boolean containsProfanity(String content) {
        return StringUtils.hasText(content) && PROFANITY.matcher(content).find();
    }

    /**
     * Remplace les mots inappropriés par des étoiles, sans bloquer l'envoi.
     */
    public String sanitizeText(String content) {
        if (!StringUtils.hasText(content)) {
            return content;
        }
        Matcher matcher = PROFANITY.matcher(content);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String found = matcher.group();
            String stars = "*".repeat(Math.max(found.length(), 3));
            matcher.appendReplacement(out, Matcher.quoteReplacement(stars));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
