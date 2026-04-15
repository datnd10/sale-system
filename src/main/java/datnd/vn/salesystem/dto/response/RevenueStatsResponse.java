package datnd.vn.salesystem.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RevenueStatsResponse {

    private long totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal totalCollected;
    private BigDecimal totalDebt;
}
