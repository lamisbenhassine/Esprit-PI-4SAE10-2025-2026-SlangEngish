package esprit.inscription.controller;

import esprit.inscription.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/inscription/email-tracking")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class EmailTrackingController {

    private final EmailService emailService;

    @GetMapping("/pixel/{emailId}")
    public void trackEmailOpen(@PathVariable String emailId, HttpServletResponse response) throws IOException {
        log.debug("Tracking email open: {}", emailId);
        
        try {
            emailService.trackEmailOpen(emailId);
            
            // Return 1x1 transparent pixel
            response.setContentType("image/png");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
            
            // 1x1 transparent PNG
            byte[] pixel = {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D,
                0x49, 0x48, 0x44, 0x52, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89, 0x00, 0x00, 0x00,
                0x0B, 0x49, 0x44, 0x41, 0x54, 0x78, (byte) 0x9C, (byte) 0xCF, (byte) 0xC8, (byte) 0xC0,
                0x00, 0x00, 0x00, 0x02, 0x00, 0x01, (byte) 0xE2, 0x21, (byte) 0xBC, 0x33, 0x00, 0x00,
                0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82
            };
            
            response.getOutputStream().write(pixel);
            response.getOutputStream().flush();
            
        } catch (Exception e) {
            log.error("Failed to track email open: {}", emailId, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/link/{emailId}")
    public ResponseEntity<String> trackEmailClick(@PathVariable String emailId, @RequestParam String url) {
        log.debug("Tracking email click: {} -> {}", emailId, url);
        
        try {
            emailService.trackEmailClick(emailId);
            
            // Redirect to original URL
            return ResponseEntity.status(302)
                    .header("Location", url)
                    .build();
            
        } catch (Exception e) {
            log.error("Failed to track email click: {}", emailId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/conversion/{emailId}")
    public ResponseEntity<Void> trackEmailConversion(@PathVariable String emailId, @RequestParam Double value) {
        log.info("Tracking email conversion: {} - Value: {}", emailId, value);
        
        try {
            emailService.trackEmailConversion(emailId, BigDecimal.valueOf(value));
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to track email conversion: {}", emailId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/bounce/{emailId}")
    public ResponseEntity<Void> trackEmailBounce(@PathVariable String emailId, @RequestParam String reason) {
        log.info("Tracking email bounce: {} - {}", emailId, reason);
        
        try {
            emailService.trackEmailBounce(emailId, reason);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to track email bounce: {}", emailId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/unsubscribe/{emailId}")
    public ResponseEntity<Void> trackEmailUnsubscribe(@PathVariable String emailId) {
        log.info("Tracking email unsubscribe: {}", emailId);
        
        try {
            emailService.trackEmailUnsubscribe(emailId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to track email unsubscribe: {}", emailId, e);
            return ResponseEntity.badRequest().build();
        }
    }
}
