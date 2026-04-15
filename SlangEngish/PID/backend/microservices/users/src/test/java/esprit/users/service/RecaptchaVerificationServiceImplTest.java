package esprit.users.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecaptchaVerificationServiceImplTest {

    @Test
    void verify_blankOrNullToken_returnsFalse() {
        RecaptchaVerificationServiceImpl svc = new RecaptchaVerificationServiceImpl();
        ReflectionTestUtils.setField(svc, "secretKey", "anySecret");

        assertFalse(svc.verify(null));
        assertFalse(svc.verify(""));
        assertFalse(svc.verify("   "));
    }

    @Test
    void verify_blankSecretKey_skipsAndReturnsTrue() {
        RecaptchaVerificationServiceImpl svc = new RecaptchaVerificationServiceImpl();
        ReflectionTestUtils.setField(svc, "secretKey", "");

        assertTrue(svc.verify("some-token"));
    }
}
