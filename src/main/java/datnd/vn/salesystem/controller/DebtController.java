package datnd.vn.salesystem.controller;

import datnd.vn.salesystem.common.ApiResponse;
import datnd.vn.salesystem.constant.Constants;
import datnd.vn.salesystem.dto.response.CustomerDebtDetailResponse;
import datnd.vn.salesystem.dto.response.DebtSummaryResponse;
import datnd.vn.salesystem.service.DebtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Debt", description = "Quản lý công nợ khách hàng")
@RestController
@RequestMapping(Constants.URI.DEBT)
@RequiredArgsConstructor
public class DebtController {

    private final DebtService debtService;

    @Operation(summary = "Lấy danh sách công nợ tất cả khách hàng",
               description = "Trả về tổng nợ còn lại (remaining_debt) của từng khách hàng có công nợ chưa thanh toán đủ.")
    @GetMapping
    public ApiResponse<List<DebtSummaryResponse>> getAllCustomerDebts() {
        List<DebtSummaryResponse> result = debtService.getAllCustomerDebts()
                .stream()
                .map(DebtSummaryResponse::from)
                .toList();
        return ApiResponse.success(result, "Customer debts retrieved successfully");
    }

    @Operation(summary = "Lấy chi tiết công nợ của một khách hàng",
               description = "Trả về danh sách các Order chưa thanh toán đủ và tổng nợ còn lại của khách hàng theo ID.")
    @GetMapping("/customer/{customerId}")
    public ApiResponse<CustomerDebtDetailResponse> getCustomerDebtDetail(
            @Parameter(description = "ID khách hàng") @PathVariable Long customerId) {
        CustomerDebtDetailResponse response =
                CustomerDebtDetailResponse.from(debtService.getCustomerDebtDetail(customerId));
        return ApiResponse.success(response, "Customer debt detail retrieved successfully");
    }
}
