package esprit.inscription.repository;

import esprit.inscription.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);
    Optional<Payment> findByTransactionId(String transactionId);
    List<Payment> findByStatus(String status);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'completed'")
    BigDecimal sumCompletedAmount();

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'completed' AND p.date >= :date")
    BigDecimal sumCompletedAmountAfter(@Param("date") LocalDateTime date);

    long countByStatus(String status);
}
