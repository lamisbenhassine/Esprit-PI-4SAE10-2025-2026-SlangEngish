package esprit.inscription.controller;

import esprit.inscription.entity.PromoCode;
import esprit.inscription.service.PromoCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inscription/promo")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PromoCodeController {

    private final PromoCodeService promoCodeService;

    @GetMapping("/validate")
    public ResponseEntity<PromoCodeService.PromoCodeValidationResult> validate(
            @RequestParam("code") String code,
            @RequestParam("amount") Double amount) {
        return promoCodeService.validate(code, amount)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(PromoCodeService.PromoCodeValidationResult.invalid("Code invalide.")));
    }

    /** Create a promo code (admin / dev). In production, secure this endpoint. */
    @PostMapping
    public ResponseEntity<PromoCode> create(@RequestBody PromoCode promo) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(promoCodeService.create(promo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
