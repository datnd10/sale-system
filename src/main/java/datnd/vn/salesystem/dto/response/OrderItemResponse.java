package datnd.vn.salesystem.dto.response;

import datnd.vn.salesystem.entity.OrderItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal height;
    private BigDecimal count;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    public static OrderItemResponse from(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .length(item.getLength())
                .width(item.getWidth())
                .height(item.getHeight())
                .count(item.getCount())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .build();
    }
}
