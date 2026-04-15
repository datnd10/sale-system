package datnd.vn.salesystem.dto.response;

import datnd.vn.salesystem.constant.enums.ProductUnit;
import datnd.vn.salesystem.entity.Product;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProductResponse {

    private Long id;
    private String code;
    private String name;
    private Long categoryId;
    private String categoryName;
    private ProductUnit unit;
    private BigDecimal price;
    private BigDecimal width;
    private String description;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductResponse from(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .code(product.getCode())
                .name(product.getName())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .unit(product.getUnit())
                .price(product.getPrice())
                .width(product.getWidth())
                .description(product.getDescription())
                .active(product.getActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
