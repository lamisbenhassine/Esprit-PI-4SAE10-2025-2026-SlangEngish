package esprit.inscription.service;

import esprit.inscription.entity.Order;
import esprit.inscription.entity.Payment;
import esprit.inscription.repository.OrderRepository;
import esprit.inscription.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }

    public Optional<Payment> getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    public List<Payment> getPaymentsByStatus(String status) {
        return paymentRepository.findByStatus(status);
    }

    @Transactional
    public Payment processPayment(Long orderId, Double amount, String method) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        // Check if payment already exists for this order
        Optional<Payment> existingPayment = paymentRepository.findByOrderId(orderId);
        if (existingPayment.isPresent()) {
            throw new RuntimeException("Payment already exists for order: " + orderId);
        }

        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setAmount(BigDecimal.valueOf(amount));
        payment.setMethod(method);
        payment.setStatus("pending");
        payment.setTransactionId(generateTransactionId());

        // Simulate payment processing
        // In a real application, this would integrate with a payment gateway
        payment.setStatus("completed");

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment updatePaymentStatus(Long paymentId, String status) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));
        
        payment.setStatus(status);
        return paymentRepository.save(payment);
    }

    public Boolean isUserPaid(Long userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        
        return orders.stream()
                .anyMatch(order -> {
                    Optional<Payment> payment = paymentRepository.findByOrderId(order.getId());
                    return payment.isPresent() && "completed".equals(payment.get().getStatus());
                });
    }

    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
