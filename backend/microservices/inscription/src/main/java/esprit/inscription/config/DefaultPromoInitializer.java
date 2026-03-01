package esprit.inscription.config;

import esprit.inscription.entity.PromoCode;
import esprit.inscription.repository.PromoCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Crée un code promo par défaut au démarrage si aucun n'existe (pour que le code soit "applicable" côté client).
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DefaultPromoInitializer implements ApplicationRunner {

    private final PromoCodeRepository promoCodeRepository;

    private static final String DEFAULT_CODE = "BIENVENUE10";

    @Override
    public void run(ApplicationArguments args) {
        if (promoCodeRepository.findByCodeIgnoreCaseAndActiveTrue(DEFAULT_CODE).isPresent()) {
            return;
        }
        try {
            PromoCode promo = PromoCode.builder()
                    .code(DEFAULT_CODE)
                    .discountType("PERCENTAGE")
                    .discountValue(new BigDecimal("10"))
                    .minPurchaseAmount(BigDecimal.ZERO)
                    .validFrom(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().plusYears(1))
                    .maxUses(10000)
                    .usedCount(0)
                    .active(true)
                    .build();
            promoCodeRepository.save(promo);
            log.info("Code promo par défaut créé : {} (10 % de réduction, sans minimum d'achat)", DEFAULT_CODE);
        } catch (Exception e) {
            log.warn("Impossible de créer le code promo par défaut: {}", e.getMessage());
        }
    }
}
