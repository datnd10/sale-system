package datnd.vn.salesystem.dto.response;

import datnd.vn.salesystem.entity.Debt;
import datnd.vn.salesystem.service.DebtService.CustomerDebtDetail;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CustomerDebtDetailResponse {

    private Long customerId;
    private String customerCode;
    private String customerName;
    private BigDecimal totalRemaining;
    private List<DebtItemResponse> debts;

    @Data
    @Builder
    public static class DebtItemResponse {
        private Long debtId;
        private String debtCode;
        private Long orderId;
        private String orderCode;
        private BigDecimal originalAmount;
        private BigDecimal remainingAmount;
        private LocalDateTime createdAt;

        public static DebtItemResponse from(Debt debt) {
            return DebtItemResponse.builder()
                    .debtId(debt.getId())
                    .debtCode(debt.getCode())
                    .orderId(debt.getOrder() != null ? debt.getOrder().getId() : null)
                    .orderCode(debt.getOrder() != null ? debt.getOrder().getCode() : null)
                    .originalAmount(debt.getOriginalAmount())
                    .remainingAmount(debt.getRemainingAmount())
                    .createdAt(debt.getCreatedAt())
                    .build();
        }
    }

    public static CustomerDebtDetailResponse from(CustomerDebtDetail detail) {
        return CustomerDebtDetailResponse.builder()
                .customerId(detail.customer().getId())
                .customerCode(detail.customer().getCode())
                .customerName(detail.customer().getName())
                .totalRemaining(detail.totalRemaining())
                .debts(detail.debts().stream()
                        .map(DebtItemResponse::from)
                        .toList())
                .build();
    }
}
