package datnd.vn.salesystem.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class MonthlyRevenueResponse {

    private int year;

    /**
     * 12-element list: index 0 = January, index 11 = December.
     */
    private List<BigDecimal> monthlyRevenue;
}
