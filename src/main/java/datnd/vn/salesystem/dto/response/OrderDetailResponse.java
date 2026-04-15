package datnd.vn.salesystem.dto.response;

import datnd.vn.salesystem.entity.Order;
import datnd.vn.salesystem.entity.OrderItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderDetailResponse {

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
    private List<OrderItemResponse> items;

    public static OrderDetailResponse from(Order order, List<OrderItem> items) {
        return OrderDetailResponse.builder()
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
                .items(items.stream().map(OrderItemResponse::from).toList())
                .build();
    }
}
