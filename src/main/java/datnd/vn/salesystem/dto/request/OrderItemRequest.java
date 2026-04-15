package datnd.vn.salesystem.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Count is required")
    @Positive(message = "Count must be greater than 0")
    private BigDecimal count;

    private BigDecimal length;

    private BigDecimal width;

    private BigDecimal height;
}
