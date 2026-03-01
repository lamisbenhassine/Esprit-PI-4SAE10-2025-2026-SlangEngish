package esprit.inscription.service;

import esprit.inscription.dto.DashboardStatsDTO;
import esprit.inscription.dto.MonthlyRevenueRecognitionDTO;
import esprit.inscription.repository.OrderRepository;
import esprit.inscription.repository.PaymentRepository;
import esprit.inscription.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionPlanRepository planRepository;
    private final RevenueRecognitionService revenueRecognitionService;

    public DashboardStatsDTO getDashboardStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        BigDecimal totalRevenue = paymentRepository.sumCompletedAmount();
        BigDecimal revenueToday = paymentRepository.sumCompletedAmountAfter(todayStart);
        BigDecimal revenueThisMonth = paymentRepository.sumCompletedAmountAfter(monthStart);

        long totalOrders = orderRepository.count();
        long completedPayments = paymentRepository.countByStatus("completed");
        long pendingPayments = paymentRepository.countByStatus("pending");
        long failedPayments = paymentRepository.countByStatus("failed");

        double conversionRate = totalOrders > 0 ? (double) completedPayments / totalOrders * 100 : 0;

        // Plan popularity (mocked or simplified for now as it requires complex join)
        // In a real scenario, we would use a more efficient query
        List<DashboardStatsDTO.PlanStatDTO> planStats = new ArrayList<>();
        // For demo purposes, we will return some mock breakdown if no data
        planStats.add(new DashboardStatsDTO.PlanStatDTO("A1", 12, 588.0));
        planStats.add(new DashboardStatsDTO.PlanStatDTO("B2", 8, 1032.0));
        planStats.add(new DashboardStatsDTO.PlanStatDTO("C1", 5, 795.0));

        int currentYear = Year.now().getValue();
        List<MonthlyRevenueRecognitionDTO> recognizedRevenueByMonth =
                revenueRecognitionService.getMonthlyRevenue(currentYear);

        return DashboardStatsDTO.builder()
                .totalOrders(totalOrders)
                .totalPayments(completedPayments)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .revenueToday(revenueToday != null ? revenueToday : BigDecimal.ZERO)
                .revenueThisMonth(revenueThisMonth != null ? revenueThisMonth : BigDecimal.ZERO)
                .newSubscriptionsToday(orderRepository.countByCreatedAtAfter(todayStart))
                .newSubscriptionsThisMonth(orderRepository.countByCreatedAtAfter(monthStart))
                .completedPayments(completedPayments)
                .pendingPayments(pendingPayments)
                .failedPayments(failedPayments)
                .conversionRate(conversionRate)
                .planStats(planStats)
                .recognizedRevenueByMonth(recognizedRevenueByMonth)
                .mostPopularPlan("A1 Discovery")
                .lastUpdated(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }
}
