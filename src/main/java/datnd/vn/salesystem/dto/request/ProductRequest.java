package datnd.vn.salesystem.dto.request;

import datnd.vn.salesystem.constant.enums.ProductUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    @NotNull(message = "Vui lòng chọn danh mục")
    private Long categoryId;

    @NotNull(message = "Vui lòng chọn đơn vị tính")
    private ProductUnit unit;

    @NotNull(message = "Vui lòng nhập giá bán")
    @Positive(message = "Giá bán phải lớn hơn 0")
    private BigDecimal price;

    private BigDecimal width;

    private String description;
}
