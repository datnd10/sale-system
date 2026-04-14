package datnd.vn.salesystem.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {

    @Column(name = "code", length = 20, unique = true)
    private String code;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "phone", length = 20, unique = true)
    private String phone;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Builder.Default
    @Column(name = "has_debt", nullable = false)
    private Boolean hasDebt = false;
}
