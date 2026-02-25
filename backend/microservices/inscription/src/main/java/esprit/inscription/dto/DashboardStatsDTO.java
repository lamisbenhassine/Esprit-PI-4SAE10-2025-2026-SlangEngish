package esprit.inscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Statistics DTO — sent both as HTTP response and via WebSocket push.
 * Contains all the metrics for the backoffice dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {

    // ── Overview KPIs ──────────────────────────────────────────────
    private long totalOrders;
    private long totalPayments;
    private double totalRevenue;
    private double revenueToday;
    private double revenueThisMonth;
    private long newSubscriptionsToday;
    private long newSubscriptionsThisMonth;

    // ── Plan Popularity ────────────────────────────────────────────
    private List<PlanStatDTO> planStats;        // orders per plan type
    private String mostPopularPlan;

    // ── Revenue Over Time (last 7 days) ────────────────────────────
    private List<DailyRevenueDTO> revenueChart; // [{date, revenue}]

    // ── Conversion Metrics ─────────────────────────────────────────
    private long completedPayments;
    private long pendingPayments;
    private long failedPayments;
    private double conversionRate;              // % completed / total

    // ── WebSocket metadata ─────────────────────────────────────────
    private String lastUpdated;                 // ISO timestamp of last refresh

    // ─── Nested DTOs ───────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PlanStatDTO {
        private String planType;
        private long count;
        private double revenue;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DailyRevenueDTO {
        private String date;                    // "yyyy-MM-dd"
        private double revenue;
        private long orders;
    }
}
