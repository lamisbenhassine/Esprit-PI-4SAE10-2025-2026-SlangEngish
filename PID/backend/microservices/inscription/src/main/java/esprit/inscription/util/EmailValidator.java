package esprit.inscription.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
@Slf4j
public class EmailValidator {

    // RFC 5322 compliant email regex
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    public boolean isValid(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public boolean isValidList(String... emails) {
        if (emails == null || emails.length == 0) {
            return false;
        }
        
        for (String email : emails) {
            if (!isValid(email)) {
                log.debug("Invalid email found: {}", email);
                return false;
            }
        }
        
        return true;
    }

    public String normalize(String email) {
        if (email == null) {
            return null;
        }
        
        return email.trim().toLowerCase();
    }

    public boolean isDisposableEmail(String email) {
        if (email == null || !isValid(email)) {
            return false;
        }
        
        String domain = email.substring(email.lastIndexOf('@') + 1).toLowerCase();
        
        // List of common disposable email domains
        String[] disposableDomains = {
                "10minutemail.com", "20minutemail.com", "guerrillamail.com", "mailinator.com",
                "tempmail.org", "yopmail.com", "maildrop.cc", "temp-mail.org",
                "throwaway.email", "maildrop.cc", "10mail.org", "tempmailaddress.com"
        };
        
        for (String disposableDomain : disposableDomains) {
            if (domain.contains(disposableDomain)) {
                return true;
            }
        }
        
        return false;
    }

    public String extractDomain(String email) {
        if (email == null || !isValid(email)) {
            return null;
        }
        
        return email.substring(email.lastIndexOf('@') + 1).toLowerCase();
    }

    public boolean isCorporateEmail(String email) {
        if (email == null || !isValid(email)) {
            return false;
        }
        
        String domain = extractDomain(email);
        
        // Common personal email domains
        String[] personalDomains = {
                "gmail.com", "yahoo.com", "hotmail.com", "outlook.com", "aol.com",
                "icloud.com", "protonmail.com", "tutanota.com", "mail.com"
        };
        
        for (String personalDomain : personalDomains) {
            if (domain.equals(personalDomain)) {
                return false;
            }
        }
        
        return true;
    }

    public String maskEmail(String email) {
        if (email == null || !isValid(email)) {
            return email;
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email;
        }
        
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        // Mask local part, keeping first 2 and last 2 characters
        if (localPart.length() <= 4) {
            localPart = localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1);
        } else {
            localPart = localPart.substring(0, 2) + "***" + localPart.substring(localPart.length() - 2);
        }
        
        return localPart + domain;
    }
}
