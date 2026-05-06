package esprit.inscription.service;

import esprit.inscription.dto.MonthlyRevenueRecognitionDTO;
import esprit.inscription.entity.Order;
import esprit.inscription.entity.OrderItem;
import esprit.inscription.entity.Payment;
import esprit.inscription.entity.SubscriptionPlan;
import esprit.inscription.repository.OrderRepository;
import esprit.inscription.repository.PaymentRepository;
import esprit.inscription.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Métier avancé 3 : répartition du revenu d'abonnement dans le temps.
 * On répartit le montant de chaque OrderItem sur la durée du plan (en mois)
 * et on agrège le revenu reconnu par mois civil pour une année donnée.
 */
@Service
@RequiredArgsConstructor
public class RevenueRecognitionService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    @Transactional(readOnly = true)
    public List<MonthlyRevenueRecognitionDTO> getMonthlyRevenue(int year) {
        Map<Integer, BigDecimal> amountByMonth = new HashMap<>();

        List<Payment> payments = paymentRepository.findAllCompletedPayments();
        for (Payment payment : payments) {
            Order order = orderRepository.findByIdWithOrderItems(payment.getOrderId()).orElse(null);
            if (order == null) {
                continue;
            }
            LocalDateTime createdAt = order.getCreatedAt() != null ? order.getCreatedAt() : payment.getDate();
            if (createdAt == null) {
                continue;
            }
            LocalDate startDate = createdAt.toLocalDate();

            for (OrderItem item : order.getOrderItems()) {
                BigDecimal totalAmount = item.getTotalPrice();
                if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    // fallback: unitPrice * quantity
                    if (item.getUnitPrice() != null && item.getQuantity() != null) {
                        totalAmount = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    } else {
                        continue;
                    }
                }

                SubscriptionPlan plan = null;
                if (item.getSubscriptionPlanId() != null) {
                    plan = subscriptionPlanRepository.findById(item.getSubscriptionPlanId()).orElse(null);
                }
                int durationDays = plan != null && plan.getDurationDays() != null && plan.getDurationDays() > 0
                        ? plan.getDurationDays()
                        : 30;
                int months = Math.max(1, (durationDays + 29) / 30); // ceil(durationDays / 30)

                BigDecimal monthlyAmount = totalAmount
                        .divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);

                for (int i = 0; i < months; i++) {
                    LocalDate monthDate = startDate.plusMonths(i);
                    if (monthDate.getYear() != year) {
                        continue;
                    }
                    int month = monthDate.getMonthValue();
                    amountByMonth.merge(month, monthlyAmount, BigDecimal::add);
                }
            }
        }

        List<MonthlyRevenueRecognitionDTO> result = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            BigDecimal value = amountByMonth.getOrDefault(month, BigDecimal.ZERO);
            result.add(MonthlyRevenueRecognitionDTO.builder()
                    .year(year)
                    .month(month)
                    .recognizedAmount(value)
                    .orders(0L)
                    .build());
        }
        return result;
    }
}

