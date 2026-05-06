package esprit.inscription.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request for prorata (upgrade/downgrade) calculation.
 * Current subscription: start date + duration + price paid; change date = when user switches.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProrataCalculateRequest {
    private Long currentPlanId;
    private Long newPlanId;
    private BigDecimal pricePaid;
    private Integer durationDays;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate subscriptionStartDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate changeDate; // optional; if null, use today
}
