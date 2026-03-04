package esprit.inscription.config;

import esprit.inscription.entity.SubscriptionPlan;
import esprit.inscription.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Crée des offres d'abonnement par défaut au démarrage si la table est vide,
 * pour que le backoffice affiche des offres et que le front puisse proposer des plans (C1, C2, etc.).
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class DefaultSubscriptionPlanInitializer implements ApplicationRunner {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (subscriptionPlanRepository.count() > 0) {
            return;
        }
        try {
            List<SubscriptionPlan> defaults = List.of(
                    plan("Monthly", "Mensuel", "29.99", 30, "Abonnement mensuel"),
                    plan("Quarterly", "Trimestriel", "79.99", 90, "Abonnement 3 mois"),
                    plan("Yearly", "Annuel", "249.99", 365, "Abonnement 1 an"),
                    plan("C1", "Niveau C1", "199.99", 180, "Plan niveau C1"),
                    plan("C2", "Niveau C2", "299.99", 365, "Plan niveau C2"),
                    plan("Premium", "Premium", "399.99", 365, "Offre premium")
            );
            for (SubscriptionPlan p : defaults) {
                subscriptionPlanRepository.save(p);
            }
            log.info("{} offres d'abonnement par défaut créées (Monthly, Quarterly, Yearly, C1, C2, Premium)", defaults.size());
        } catch (Exception e) {
            log.error("Impossible de créer les offres par défaut", e);
        }
    }

    private static SubscriptionPlan plan(String planType, String name, String price, int durationDays, String description) {
        return SubscriptionPlan.builder()
                .planType(planType)
                .name(name)
                .price(new BigDecimal(price))
                .currency("TND")
                .durationDays(durationDays)
                .description(description)
                .date(LocalDate.now())
                .build();
    }
}
