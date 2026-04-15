package datnd.vn.salesystem.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class OrderRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    private LocalDate orderDate;

    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<OrderItemRequest> items;

    private BigDecimal paidImmediately;
}
