package datnd.vn.salesystem.controller;

import datnd.vn.salesystem.common.ApiResponse;
import datnd.vn.salesystem.common.PageResponse;
import datnd.vn.salesystem.constant.Constants;
import datnd.vn.salesystem.dto.response.CustomerDebtDetailResponse;
import datnd.vn.salesystem.dto.response.DebtSummaryResponse;
import datnd.vn.salesystem.service.DebtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Debt", description = "Quản lý công nợ khách hàng")
@RestController
@RequestMapping(Constants.URI.DEBT)
@RequiredArgsConstructor
public class DebtController {

    private final DebtService debtService;

    @Operation(summary = "Tìm kiếm công nợ có phân trang")
    @GetMapping("/search")
    public ApiResponse<PageResponse<DebtSummaryResponse>> searchDebts(
            @Parameter(description = "Tên khách hàng (tìm kiếm gần đúng)") @RequestParam(required = false) String customerName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "totalRemaining") String sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        return ApiResponse.success(
                debtService.searchDebts(customerName, page, size, sort, direction),
                "Customer debts retrieved successfully");
    }

    @Operation(summary = "Lấy danh sách công nợ tất cả khách hàng")
    @GetMapping
    public ApiResponse<List<DebtSummaryResponse>> getAllCustomerDebts() {
        List<DebtSummaryResponse> result = debtService.getAllCustomerDebts()
                .stream()
                .map(DebtSummaryResponse::from)
                .toList();
        return ApiResponse.success(result, "Customer debts retrieved successfully");
    }

    @Operation(summary = "Lấy chi tiết công nợ của một khách hàng")
    @GetMapping("/customer/{customerId:\\d+}")
    public ApiResponse<CustomerDebtDetailResponse> getCustomerDebtDetail(
            @Parameter(description = "ID khách hàng") @PathVariable Long customerId) {
        return ApiResponse.success(
                CustomerDebtDetailResponse.from(debtService.getCustomerDebtDetail(customerId)),
                "Customer debt detail retrieved successfully");
    }
}
