package datnd.vn.salesystem.entity;

import datnd.vn.salesystem.constant.enums.ProductUnit;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    @Column(name = "code", length = 20, unique = true)
    private String code;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", length = 50, nullable = false)
    private ProductUnit unit;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "width", precision = 10, scale = 3)
    private BigDecimal width;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
