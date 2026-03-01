package esprit.inscription.controller;

import esprit.inscription.entity.EmailCampaign;
import esprit.inscription.entity.User;
import esprit.inscription.service.EmailCampaignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/inscription/email-campaigns")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class EmailCampaignController {

    private final EmailCampaignService campaignService;

    @GetMapping
    public ResponseEntity<Page<EmailCampaign>> getAllCampaigns(Pageable pageable) {
        log.info("Getting all email campaigns with pagination");
        Page<EmailCampaign> campaigns = campaignService.getAllCampaigns(pageable);
        return ResponseEntity.ok(campaigns);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailCampaign> getCampaignById(@PathVariable Long id) {
        log.info("Getting email campaign by ID: {}", id);
        EmailCampaign campaign = campaignService.getCampaignById(id);
        return ResponseEntity.ok(campaign);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<EmailCampaign>> getCampaignsByStatus(@PathVariable EmailCampaign.CampaignStatus status) {
        log.info("Getting campaigns by status: {}", status);
        List<EmailCampaign> campaigns = campaignService.getCampaignsByStatus(status);
        return ResponseEntity.ok(campaigns);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<EmailCampaign>> getCampaignsByCategory(@PathVariable EmailCampaign.CampaignCategory category) {
        log.info("Getting campaigns by category: {}", category);
        List<EmailCampaign> campaigns = campaignService.getCampaignsByCategory(category);
        return ResponseEntity.ok(campaigns);
    }

    @GetMapping("/level/{level}")
    public ResponseEntity<List<EmailCampaign>> getCampaignsByTargetLevel(@PathVariable String level) {
        log.info("Getting campaigns by target level: {}", level);
        List<EmailCampaign> campaigns = campaignService.getCampaignsByTargetLevel(level);
        return ResponseEntity.ok(campaigns);
    }

    @PostMapping
    public ResponseEntity<EmailCampaign> createCampaign(@Valid @RequestBody EmailCampaign campaign) {
        log.info("Creating new email campaign: {}", campaign.getName());
        try {
            EmailCampaign createdCampaign = campaignService.createCampaign(campaign);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCampaign);
        } catch (Exception e) {
            log.error("Failed to create email campaign", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmailCampaign> updateCampaign(@PathVariable Long id, @Valid @RequestBody EmailCampaign campaign) {
        log.info("Updating email campaign: {}", id);
        try {
            EmailCampaign updatedCampaign = campaignService.updateCampaign(id, campaign);
            return ResponseEntity.ok(updatedCampaign);
        } catch (Exception e) {
            log.error("Failed to update email campaign", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCampaign(@PathVariable Long id) {
        log.info("Deleting email campaign: {}", id);
        try {
            campaignService.deleteCampaign(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Failed to delete email campaign", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/schedule")
    public ResponseEntity<EmailCampaign> scheduleCampaign(@PathVariable Long id, @RequestParam LocalDateTime scheduledAt) {
        log.info("Scheduling email campaign {} for: {}", id, scheduledAt);
        try {
            EmailCampaign scheduledCampaign = campaignService.scheduleCampaign(id, scheduledAt);
            return ResponseEntity.ok(scheduledCampaign);
        } catch (Exception e) {
            log.error("Failed to schedule email campaign", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/launch")
    public ResponseEntity<EmailCampaign> launchCampaign(@PathVariable Long id) {
        log.info("Launching email campaign: {}", id);
        try {
            EmailCampaign launchedCampaign = campaignService.launchCampaign(id);
            return ResponseEntity.ok(launchedCampaign);
        } catch (Exception e) {
            log.error("Failed to launch email campaign", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<EmailCampaign> pauseCampaign(@PathVariable Long id) {
        log.info("Pausing email campaign: {}", id);
        try {
            EmailCampaign pausedCampaign = campaignService.pauseCampaign(id);
            return ResponseEntity.ok(pausedCampaign);
        } catch (Exception e) {
            log.error("Failed to pause email campaign", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<EmailCampaign> resumeCampaign(@PathVariable Long id) {
        log.info("Resuming email campaign: {}", id);
        try {
            EmailCampaign resumedCampaign = campaignService.resumeCampaign(id);
            return ResponseEntity.ok(resumedCampaign);
        } catch (Exception e) {
            log.error("Failed to resume email campaign", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<EmailCampaign> cancelCampaign(@PathVariable Long id) {
        log.info("Cancelling email campaign: {}", id);
        try {
            EmailCampaign cancelledCampaign = campaignService.cancelCampaign(id);
            return ResponseEntity.ok(cancelledCampaign);
        } catch (Exception e) {
            log.error("Failed to cancel email campaign", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/stats/update")
    public ResponseEntity<Void> updateCampaignStats(@PathVariable Long id) {
        log.info("Updating email campaign stats: {}", id);
        try {
            campaignService.updateCampaignStats(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to update email campaign stats", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/ready-to-send")
    public ResponseEntity<List<EmailCampaign>> getCampaignsReadyToSend() {
        log.info("Getting email campaigns ready to send");
        List<EmailCampaign> campaigns = campaignService.getCampaignsReadyToSend();
        return ResponseEntity.ok(campaigns);
    }

    @GetMapping("/active")
    public ResponseEntity<List<EmailCampaign>> getActiveCampaigns() {
        log.info("Getting active email campaigns");
        List<EmailCampaign> campaigns = campaignService.getActiveCampaigns();
        return ResponseEntity.ok(campaigns);
    }

    // Automated campaign endpoints
    @PostMapping("/welcome/{userId}")
    public ResponseEntity<EmailCampaign> createWelcomeCampaign(@PathVariable Long userId) {
        log.info("Creating welcome campaign for user: {}", userId);
        try {
            // This would need a UserService to get user details
            // For now, return success
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to create welcome campaign", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/course-completion/{userId}")
    public ResponseEntity<EmailCampaign> createCourseCompletionCampaign(@PathVariable Long userId, @RequestParam String completedLevel) {
        log.info("Creating course completion campaign for user: {} - Level: {}", userId, completedLevel);
        try {
            // This would need a UserService to get user details
            // For now, return success
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to create course completion campaign", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/reactivation")
    public ResponseEntity<EmailCampaign> createReactivationCampaign() {
        log.info("Creating reactivation campaign for inactive users");
        try {
            // This would identify inactive users and create campaign
            // For now, return success
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to create reactivation campaign", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
