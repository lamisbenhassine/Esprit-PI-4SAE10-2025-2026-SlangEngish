package esprit.inscription.controller;

import esprit.inscription.dto.LoyaltyRedemptionPreviewDTO;
import esprit.inscription.dto.LoyaltySummaryDTO;
import esprit.inscription.service.LoyaltyService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/inscription/loyalty")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    /**
     * Retourne un résumé du compte de fidélité pour un utilisateur donné.
     * Optionnellement, prend en compte un montant de commande pour calculer la réduction max possible.
     */
    @GetMapping("/{userId}/summary")
    public ResponseEntity<LoyaltySummaryDTO> getSummary(
            @PathVariable Long userId,
            @RequestParam(value = "orderTotal", required = false) BigDecimal orderTotal) {
        LoyaltySummaryDTO dto = loyaltyService.getSummary(userId, orderTotal);
        return ResponseEntity.ok(dto);
    }

    /**
     * Prévisualise l'utilisation de points pour une commande (ne modifie pas le solde).
     */
    @PostMapping("/preview-redemption")
    public ResponseEntity<LoyaltyRedemptionPreviewDTO> previewRedemption(
            @RequestBody LoyaltyRedemptionRequest request) {
        LoyaltyRedemptionPreviewDTO dto = loyaltyService.previewRedemption(
                request.getUserId(),
                request.getOrderTotal(),
                request.getRequestedPoints());
        return ResponseEntity.ok(dto);
    }

    /**
     * Vue admin : retourne la liste de tous les comptes de fidélité
     * (userId, solde, lifetimePoints, tier).
     */
    @GetMapping("/admin/accounts")
    public ResponseEntity<List<LoyaltySummaryDTO>> getAllAccounts() {
        return ResponseEntity.ok(loyaltyService.getAllAccountsSummary());
    }

    /**
     * Seed de comptes de fidélité de démonstration pour le backoffice.
     */
    @PostMapping("/admin/seed-demo")
    public ResponseEntity<List<LoyaltySummaryDTO>> seedDemoAccounts() {
        return ResponseEntity.ok(loyaltyService.seedDemoAccounts());
    }

    @Data
    public static class LoyaltyRedemptionRequest {
        private Long userId;
        private BigDecimal orderTotal;
        private Long requestedPoints;
    }
}

