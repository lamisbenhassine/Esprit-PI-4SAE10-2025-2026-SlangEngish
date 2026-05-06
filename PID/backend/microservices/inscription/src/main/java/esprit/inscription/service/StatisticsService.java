package esprit.inscription.service;

import esprit.inscription.dto.DashboardStatsDTO;
import esprit.inscription.dto.MonthlyRevenueRecognitionDTO;
import esprit.inscription.repository.OrderItemRepository;
import esprit.inscription.repository.OrderRepository;
import esprit.inscription.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderItemRepository orderItemRepository;
    private final RevenueRecognitionService revenueRecognitionService;

    public DashboardStatsDTO getDashboardStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        BigDecimal payTotal = nz(paymentRepository.sumCompletedAmount());
        BigDecimal orderBookTotal = nz(orderRepository.sumTotalAmount());
        /*
         * Souvent en dev : commandes créées mais aucune ligne payment « completed » (Stripe sans webhook).
         * Dans ce cas on affiche les montants commandes pour que le back-office ne reste pas à zéro.
         */
        boolean useOrderFallback = payTotal.signum() == 0 && orderBookTotal.signum() > 0;

        BigDecimal totalRevenue = useOrderFallback ? orderBookTotal : payTotal;
        BigDecimal revenueToday = useOrderFallback
                ? nz(orderRepository.sumTotalAmountCreatedOnOrAfter(todayStart))
                : nz(paymentRepository.sumCompletedAmountAfter(todayStart));
        BigDecimal revenueThisMonth = useOrderFallback
                ? nz(orderRepository.sumTotalAmountCreatedOnOrAfter(monthStart))
                : nz(paymentRepository.sumCompletedAmountAfter(monthStart));

        long totalOrders = orderRepository.count();
        long completedPayments = paymentRepository.countByStatusIgnoreCase("completed");
        long pendingPayments = paymentRepository.countByStatusIgnoreCase("pending");
        long failedPayments = paymentRepository.countByStatusIgnoreCase("failed");

        long ordersSansPaiement = orderRepository.countOrdersWithNoPaymentRecord();

        /*
         * Cohérence avec le CA affiché : si on utilise le repli « montants commandes » (aucun paiement
         * completed en base alors que les commandes portent le CA), on considère ces commandes comme
         * encaissées pour l’affichage — sinon « En attente » contredit « déjà payé » côté utilisateur.
         * Si au moins un paiement completed existe en base, on reste strict : commandes sans ligne
         * payment = encore à synchroniser (webhook / verify-session) → « En attente ».
         */
        if (useOrderFallback) {
            completedPayments = completedPayments + ordersSansPaiement;
        } else {
            pendingPayments = pendingPayments + ordersSansPaiement;
        }

        double conversionRate = totalOrders > 0 ? (double) completedPayments / totalOrders * 100 : 0;

        List<DashboardStatsDTO.PlanStatDTO> planStats = new ArrayList<>();
        String mostPopularPlan = "—";
        List<Object[]> planRows = orderItemRepository.aggregatePaidOrdersByPlanLabel();
        if (planRows == null || planRows.isEmpty()) {
            planRows = orderItemRepository.aggregateAllOrdersByPlanLabel();
        }
        if (planRows != null) {
            for (Object[] row : planRows) {
                String label = row[0] != null ? String.valueOf(row[0]) : "Autre";
                long cnt = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
                double rev = row[2] instanceof Number ? ((Number) row[2]).doubleValue() : 0.0;
                planStats.add(new DashboardStatsDTO.PlanStatDTO(label, cnt, rev));
            }
            if (!planStats.isEmpty()) {
                mostPopularPlan = planStats.get(0).getPlanType();
            }
        }

        int currentYear = Year.now().getValue();
        List<MonthlyRevenueRecognitionDTO> recognizedRevenueByMonth = useOrderFallback
                ? monthlyTotalsFromOrders(currentYear)
                : revenueRecognitionService.getMonthlyRevenue(currentYear);

        return DashboardStatsDTO.builder()
                .totalOrders(totalOrders)
                .totalPayments(completedPayments)
                .totalRevenue(totalRevenue)
                .revenueToday(revenueToday)
                .revenueThisMonth(revenueThisMonth)
                .newSubscriptionsToday(orderRepository.countByCreatedAtAfter(todayStart))
                .newSubscriptionsThisMonth(orderRepository.countByCreatedAtAfter(monthStart))
                .completedPayments(completedPayments)
                .pendingPayments(pendingPayments)
                .failedPayments(failedPayments)
                .conversionRate(conversionRate)
                .planStats(planStats)
                .recognizedRevenueByMonth(recognizedRevenueByMonth)
                .mostPopularPlan(mostPopularPlan)
                .lastUpdated(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    /** CA par mois basé sur la date de commande (repli si aucun paiement encaissé en base). */
    private List<MonthlyRevenueRecognitionDTO> monthlyTotalsFromOrders(int year) {
        Map<Integer, BigDecimal> byMonth = new HashMap<>();
        List<Object[]> rows = orderRepository.sumOrderTotalsGroupedByMonth(year);
        if (rows != null) {
            for (Object[] row : rows) {
                if (row == null || row.length < 2) {
                    continue;
                }
                int m = ((Number) row[0]).intValue();
                BigDecimal amt;
                if (row[1] instanceof BigDecimal) {
                    amt = (BigDecimal) row[1];
                } else if (row[1] instanceof Number) {
                    amt = BigDecimal.valueOf(((Number) row[1]).doubleValue());
                } else {
                    amt = BigDecimal.ZERO;
                }
                byMonth.put(m, amt);
            }
        }
        List<MonthlyRevenueRecognitionDTO> out = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            out.add(MonthlyRevenueRecognitionDTO.builder()
                    .year(year)
                    .month(month)
                    .recognizedAmount(byMonth.getOrDefault(month, BigDecimal.ZERO))
                    .orders(0L)
                    .build());
        }
        return out;
    }
}
