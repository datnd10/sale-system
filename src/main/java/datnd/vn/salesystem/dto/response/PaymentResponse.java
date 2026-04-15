package datnd.vn.salesystem.dto.response;

import datnd.vn.salesystem.entity.Payment;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {

    private Long id;
    private String code;
    private Long customerId;
    private String customerName;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String note;
    private LocalDateTime createdAt;

    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .code(payment.getCode())
                .customerId(payment.getCustomer().getId())
                .customerName(payment.getCustomer().getName())
                .amount(payment.getAmount())
                .paymentDate(payment.getPaymentDate())
                .note(payment.getNote())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
