package datnd.vn.salesystem.dto.response;

import datnd.vn.salesystem.entity.Customer;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CustomerResponse {

    private Long id;
    private String code;
    private String name;
    private String phone;
    private String address;
    private Boolean hasDebt;
    private BigDecimal totalDebt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CustomerResponse from(Customer customer) {
        return from(customer, null);
    }

    public static CustomerResponse from(Customer customer, BigDecimal totalDebt) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .code(customer.getCode())
                .name(customer.getName())
                .phone(customer.getPhone())
                .address(customer.getAddress())
                .hasDebt(customer.getHasDebt())
                .totalDebt(totalDebt)
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
