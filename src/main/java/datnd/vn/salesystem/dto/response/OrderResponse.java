package datnd.vn.salesystem.dto.response;

import datnd.vn.salesystem.entity.Order;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderResponse {

    private Long id;
    private String code;
    private Long customerId;
    private String customerName;
    private LocalDate orderDate;
    private BigDecimal totalAmount;
    private BigDecimal paidImmediately;
    private String note;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .code(order.getCode())
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getName())
                .orderDate(order.getOrderDate())
                .totalAmount(order.getTotalAmount())
                .paidImmediately(order.getPaidImmediately())
                .note(order.getNote())
                .active(order.getActive())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
