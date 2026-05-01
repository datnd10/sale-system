package datnd.vn.salesystem.dto.request;

import datnd.vn.salesystem.constant.enums.OrderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class OrderRequest {

    @NotNull(message = "Vui lòng chọn khách hàng")
    private Long customerId;

    @NotNull(message = "Vui lòng chọn loại đơn hàng")
    private OrderType orderType;

    private LocalDate orderDate;

    /**
     * Chỉ dùng cho orderType = SALE.
     * Bắt buộc khi SALE, bỏ qua khi PAYMENT.
     */
    @Valid
    private List<OrderItemRequest> items;

    /**
     * Chỉ dùng cho orderType = SALE.
     * Số tiền khách trả ngay khi mua hàng.
     */
    private BigDecimal paidImmediately;

    /**
     * Chỉ dùng cho orderType = PAYMENT.
     * Số tiền khách trả nợ.
     */
    private BigDecimal amount;

    private String note;
}
