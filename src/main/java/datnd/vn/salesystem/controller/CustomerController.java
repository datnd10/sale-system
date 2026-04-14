package datnd.vn.salesystem.controller;

import datnd.vn.salesystem.common.ApiResponse;
import datnd.vn.salesystem.common.PageResponse;
import datnd.vn.salesystem.common.SearchRequest;
import datnd.vn.salesystem.constant.Constants;
import datnd.vn.salesystem.dto.request.CustomerRequest;
import datnd.vn.salesystem.dto.response.CustomerResponse;
import datnd.vn.salesystem.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Customer", description = "Quản lý khách hàng")
@RestController
@RequestMapping(Constants.URI.CUSTOMER)
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "Tạo khách hàng mới")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest request) {
        return ApiResponse.success(
                CustomerResponse.from(customerService.createCustomer(
                        request.getName(), request.getPhone(), request.getAddress())),
                "Customer created successfully"
        );
    }

    @Operation(summary = "Lấy danh sách tất cả khách hàng")
    @GetMapping
    public ApiResponse<List<CustomerResponse>> getAllCustomers() {
        List<CustomerResponse> customers = customerService.getAllCustomers()
                .stream()
                .map(CustomerResponse::from)
                .toList();
        return ApiResponse.success(customers, "Customers retrieved successfully");
    }

    @Operation(summary = "Tìm kiếm khách hàng có phân trang")
    @GetMapping("/search")
    public ApiResponse<PageResponse<CustomerResponse>> searchCustomers(
            @Parameter(description = "Tên khách hàng (tìm kiếm gần đúng)") @RequestParam(required = false) String name,
            @Parameter(description = "Số điện thoại (tìm kiếm gần đúng)") @RequestParam(required = false) String phone,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        SearchRequest request = new SearchRequest();
        request.setPage(page);
        request.setSize(size);
        request.setSort(sort);
        request.setDirection(direction);
        request.filter("name", name);
        request.filter("phone", phone);

        PageResponse<CustomerResponse> result = PageResponse.from(
                customerService.searchCustomers(request).map(CustomerResponse::from)
        );
        return ApiResponse.success(result, "Customers retrieved successfully");
    }

    @Operation(summary = "Lấy chi tiết khách hàng theo ID", description = "Bao gồm tổng công nợ hiện tại")
    @GetMapping("/{id}")
    public ApiResponse<CustomerResponse> getCustomerById(
            @Parameter(description = "ID khách hàng") @PathVariable Long id) {
        return ApiResponse.success(
                CustomerResponse.from(customerService.getCustomerById(id)),
                "Customer retrieved successfully"
        );
    }

    @Operation(summary = "Cập nhật thông tin khách hàng")
    @PutMapping("/{id}")
    public ApiResponse<CustomerResponse> updateCustomer(
            @Parameter(description = "ID khách hàng") @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request) {
        return ApiResponse.success(
                CustomerResponse.from(customerService.updateCustomer(
                        id, request.getName(), request.getPhone(), request.getAddress())),
                "Customer updated successfully"
        );
    }
}
