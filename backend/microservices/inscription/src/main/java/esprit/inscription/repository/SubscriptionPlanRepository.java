package esprit.inscription.repository;

import esprit.inscription.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    List<SubscriptionPlan> findByUserId(Long userId);
    List<SubscriptionPlan> findByPlanType(String planType);
    
    @Query("SELECT sp FROM SubscriptionPlan sp WHERE sp.userId = :userId AND sp.isActive = true")
    SubscriptionPlan findActiveSubscriptionByUserId(@Param("userId") Long userId);
    
    @Query("SELECT sp FROM SubscriptionPlan sp WHERE sp.trialEndsAt BETWEEN :now AND :futureDate")
    List<SubscriptionPlan> findExpiringTrials(@Param("now") LocalDateTime now, @Param("futureDate") LocalDateTime futureDate);
    
    @Query("SELECT COUNT(sp) FROM SubscriptionPlan sp WHERE sp.planType = :planType")
    Long countByPlanType(@Param("planType") String planType);
}
