package datnd.vn.salesystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerRequest {

    @NotBlank(message = "Customer name is required")
    private String name;

    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phone;

    private String address;
}
