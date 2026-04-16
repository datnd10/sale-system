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

    @NotNull(message = "Vui lòng chọn khách hàng")
    private Long customerId;

    private LocalDate orderDate;

    @NotEmpty(message = "Đơn hàng phải có ít nhất 1 sản phẩm")
    @Valid
    private List<OrderItemRequest> items;

    private BigDecimal paidImmediately;

    private String note;
}
