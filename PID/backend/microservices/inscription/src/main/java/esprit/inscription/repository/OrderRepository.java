package esprit.inscription.repository;

import esprit.inscription.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    Optional<Order> findByOrderNumber(String orderNumber);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.id = :id")
    Optional<Order> findByIdWithOrderItems(@Param("id") Long id);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :date")
    long countByCreatedAtAfter(@Param("date") LocalDateTime date);

    @Query("SELECT SUM(o.totalAmount) FROM Order o")
    BigDecimal sumTotalAmount();

    /** Montant total des commandes créées à partir de cette date (repli stats si aucun paiement « completed »). */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.createdAt >= :date")
    BigDecimal sumTotalAmountCreatedOnOrAfter(@Param("date") LocalDateTime date);

    /** Commandes sans aucune ligne dans la table payment (ex. Stripe sans webhook / verify). */
    @Query("SELECT COUNT(o) FROM Order o WHERE NOT EXISTS (SELECT p FROM Payment p WHERE p.orderId = o.id)")
    long countOrdersWithNoPaymentRecord();

    /** Somme des total_amount par mois civil pour une année (MySQL). */
    @Query(value = """
            SELECT MONTH(o.created_at) AS m, COALESCE(SUM(o.total_amount), 0) AS amt
            FROM orders o
            WHERE YEAR(o.created_at) = :year
            GROUP BY MONTH(o.created_at)
            """, nativeQuery = true)
    List<Object[]> sumOrderTotalsGroupedByMonth(@Param("year") int year);
    
    @Query("SELECT o FROM Order o WHERE o.totalAmount >= :minAmount")
    List<Order> findHighValueOrders(@Param("minAmount") BigDecimal minAmount);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId AND o.totalAmount >= :minAmount")
    Long countHighValueOrdersByUser(@Param("userId") Long userId, @Param("minAmount") BigDecimal minAmount);
    
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :start AND :end")
    List<Order> findOrdersBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
