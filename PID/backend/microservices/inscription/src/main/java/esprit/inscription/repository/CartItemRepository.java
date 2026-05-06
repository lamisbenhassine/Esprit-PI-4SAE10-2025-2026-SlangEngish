package esprit.inscription.repository;

import esprit.inscription.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    
    List<CartItem> findByCartId(Long cartId);
    
    Optional<CartItem> findByCartIdAndSubscriptionPlanId(Long cartId, Long subscriptionPlanId);
    
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.subscriptionPlanId = :subscriptionPlanId")
    Optional<CartItem> findByCartIdAndSubscriptionPlanIdWithCart(@Param("cartId") Long cartId, @Param("subscriptionPlanId") Long subscriptionPlanId);
    
    void deleteByCartId(Long cartId);
    
    @Query("SELECT COUNT(ci) FROM CartItem ci WHERE ci.cart.id = :cartId")
    Long countByCartId(@Param("cartId") Long cartId);
}
