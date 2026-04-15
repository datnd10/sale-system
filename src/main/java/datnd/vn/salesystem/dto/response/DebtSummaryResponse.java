package datnd.vn.salesystem.dto.response;

import datnd.vn.salesystem.service.DebtService.CustomerDebtSummary;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DebtSummaryResponse {

    private Long customerId;
    private String customerCode;
    private String customerName;
    private BigDecimal totalRemaining;

    public static DebtSummaryResponse from(CustomerDebtSummary summary) {
        return DebtSummaryResponse.builder()
                .customerId(summary.customer().getId())
                .customerCode(summary.customer().getCode())
                .customerName(summary.customer().getName())
                .totalRemaining(summary.totalRemaining())
                .build();
    }
}
