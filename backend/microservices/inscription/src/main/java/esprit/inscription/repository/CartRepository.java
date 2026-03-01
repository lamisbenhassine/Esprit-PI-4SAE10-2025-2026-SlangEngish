package esprit.inscription.repository;

import esprit.inscription.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);

    /** When multiple carts exist (e.g. before unique constraint), use the latest one (most recently used). */
    Optional<Cart> findFirstByUserIdOrderByIdDesc(Long userId);
    
    @Query("SELECT c FROM Cart c WHERE c.userId = :userId AND c.cartItems IS NOT EMPTY")
    Optional<Cart> findActiveCartByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(c) FROM Cart c WHERE c.userId = :userId")
    Long countCartsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT SUM(c.totalAmount) FROM Cart c WHERE c.userId = :userId")
    BigDecimal sumTotalAmountByUserId(@Param("userId") Long userId);
    
    @Query("SELECT c FROM Cart c WHERE c.totalAmount > :minAmount")
    List<Cart> findCartsWithTotalGreaterThan(@Param("minAmount") BigDecimal minAmount);
}
