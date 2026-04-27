package esprit.inscription.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailValidatorTest {

    private EmailValidator emailValidator;

    @BeforeEach
    void setUp() {
        emailValidator = new EmailValidator();
    }

    @Test
    void isValidAcceptsWellFormedEmail() {
        assertTrue(emailValidator.isValid("student.test@example.com"));
    }

    @Test
    void isValidRejectsNullOrBlankOrMalformedEmails() {
        assertFalse(emailValidator.isValid(null));
        assertFalse(emailValidator.isValid("   "));
        assertFalse(emailValidator.isValid("invalid-email"));
    }

    @Test
    void isValidListReturnsTrueOnlyWhenAllEmailsAreValid() {
        assertTrue(emailValidator.isValidList("a@company.com", "b@company.com"));
        assertFalse(emailValidator.isValidList("a@company.com", "invalid"));
    }

    @Test
    void normalizeTrimsAndLowercasesInput() {
        assertEquals("user@mail.com", emailValidator.normalize("  User@Mail.com "));
        assertNull(emailValidator.normalize(null));
    }

    @Test
    void disposableAndCorporateChecksWorkAsExpected() {
        assertTrue(emailValidator.isDisposableEmail("temp@10minutemail.com"));
        assertFalse(emailValidator.isDisposableEmail("john@company.com"));
        assertFalse(emailValidator.isCorporateEmail("john@gmail.com"));
        assertTrue(emailValidator.isCorporateEmail("john@company.com"));
    }

    @Test
    void maskEmailMasksLocalPartAndKeepsDomain() {
        assertEquals("jo***st@example.com", emailValidator.maskEmail("johntest@example.com"));
        assertEquals("a***c@example.com", emailValidator.maskEmail("abc@example.com"));
    }
}
