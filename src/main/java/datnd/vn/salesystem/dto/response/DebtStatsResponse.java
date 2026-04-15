package datnd.vn.salesystem.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DebtStatsResponse {

    private Long customerId;
    private String customerCode;
    private String customerName;
    private BigDecimal remainingDebt;
}
