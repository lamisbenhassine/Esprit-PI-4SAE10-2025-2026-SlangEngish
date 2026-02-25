package esprit.inscription.repository;

import esprit.inscription.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    Optional<Order> findByOrderNumber(String orderNumber);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :date")
    long countByCreatedAtAfter(@Param("date") java.time.LocalDateTime date);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(o.totalAmount) FROM Order o")
    Double sumTotalAmount();
}
