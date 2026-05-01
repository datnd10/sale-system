package datnd.vn.salesystem.controller;

import datnd.vn.salesystem.common.ApiResponse;
import datnd.vn.salesystem.constant.Constants;
import datnd.vn.salesystem.dto.response.CustomerResponse;
import datnd.vn.salesystem.dto.response.DebtStatsResponse;
import datnd.vn.salesystem.service.CustomerService;
import datnd.vn.salesystem.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Debt", description = "Xem công nợ khách hàng")
@RestController
@RequestMapping(Constants.URI.DEBT)
@RequiredArgsConstructor
public class DebtController {

    private final StatisticsService statisticsService;
    private final CustomerService customerService;

    @Operation(
            summary = "Danh sách khách hàng còn nợ",
            description = "Trả về tất cả khách hàng có công nợ > 0, sắp xếp giảm dần theo số nợ. " +
                    "Công nợ = Σ(SALE.total_amount) - Σ(Payment.amount)"
    )
    @GetMapping
    public ApiResponse<List<DebtStatsResponse>> getAllCustomerDebts() {
        return ApiResponse.success(
                statisticsService.getDebtStats(),
                "Customer debts retrieved successfully"
        );
    }

    @Operation(
            summary = "Công nợ chi tiết của một khách hàng",
            description = "Trả về tổng công nợ hiện tại của khách hàng."
    )
    @GetMapping("/customer/{customerId:\\d+}")
    public ApiResponse<CustomerResponse> getCustomerDebt(
            @Parameter(description = "ID khách hàng") @PathVariable Long customerId) {
        return ApiResponse.success(
                customerService.getCustomerWithDebtInfo(customerId),
                "Customer debt retrieved successfully"
        );
    }
}
