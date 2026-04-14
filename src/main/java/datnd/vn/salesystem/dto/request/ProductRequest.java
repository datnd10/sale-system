package datnd.vn.salesystem.dto.request;

import datnd.vn.salesystem.constant.enums.ProductUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "Unit is required")
    private ProductUnit unit;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than 0")
    private BigDecimal price;

    private BigDecimal height;

    private BigDecimal width;

    private String description;
}
