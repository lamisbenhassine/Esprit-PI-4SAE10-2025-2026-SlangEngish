package esprit.inscription.repository;

import esprit.inscription.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);
    Optional<Payment> findByTransactionId(String transactionId);
    List<Payment> findByStatus(String status);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'completed'")
    Double sumCompletedAmount();

    @org.springframework.data.jpa.repository.Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'completed' AND p.date >= :date")
    Double sumCompletedAmountAfter(@Param("date") java.time.LocalDateTime date);

    long countByStatus(String status);
}
