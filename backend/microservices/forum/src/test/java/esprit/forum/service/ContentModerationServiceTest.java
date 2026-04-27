package esprit.forum.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentModerationServiceTest {

    private ContentModerationService moderationService;

    @BeforeEach
    void setUp() {
        moderationService = new ContentModerationService();
    }

    @Test
    void containsProfanityReturnsTrueForKnownProfanityWord() {
        assertTrue(moderationService.containsProfanity("This is bullshit"));
    }

    @Test
    void containsProfanityReturnsFalseForCleanText() {
        assertFalse(moderationService.containsProfanity("Hello team, this topic is helpful."));
    }

    @Test
    void assertTextAcceptableThrowsForInappropriateText() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> moderationService.assertTextAcceptable("stfu please")
        );
        assertEquals(ContentModerationService.REJECT_MESSAGE, exception.getMessage());
    }

    @Test
    void assertTextAcceptablePassesForNullOrBlankText() {
        assertDoesNotThrow(() -> moderationService.assertTextAcceptable(null));
        assertDoesNotThrow(() -> moderationService.assertTextAcceptable("   "));
    }

    @Test
    void sanitizeTextReplacesProfanityWithStars() {
        String sanitized = moderationService.sanitizeText("This is damn bad");
        assertEquals("This is **** bad", sanitized);
    }
}
