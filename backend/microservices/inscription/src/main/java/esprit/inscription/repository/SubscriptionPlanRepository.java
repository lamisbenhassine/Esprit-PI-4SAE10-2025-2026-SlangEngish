package esprit.inscription.repository;

import esprit.inscription.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    List<SubscriptionPlan> findByUserId(Long userId);
    List<SubscriptionPlan> findByPlanType(String planType);
}
