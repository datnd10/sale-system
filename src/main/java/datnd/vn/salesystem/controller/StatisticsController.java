package datnd.vn.salesystem.controller;

import datnd.vn.salesystem.common.ApiResponse;
import datnd.vn.salesystem.constant.Constants;
import datnd.vn.salesystem.dto.response.DebtStatsResponse;
import datnd.vn.salesystem.dto.response.MonthlyRevenueResponse;
import datnd.vn.salesystem.dto.response.RevenueStatsResponse;
import datnd.vn.salesystem.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Statistics", description = "Thống kê doanh thu và công nợ")
@RestController
@RequestMapping(Constants.URI.STATISTICS)
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(
            summary = "Thống kê doanh thu theo khoảng thời gian",
            description = "Trả về tổng số đơn hàng, tổng doanh thu, tổng đã thu và tổng còn nợ trong khoảng thời gian chỉ định."
    )
    @GetMapping("/revenue")
    public ApiResponse<RevenueStatsResponse> getRevenueStats(
            @Parameter(description = "Ngày bắt đầu (yyyy-MM-dd)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Ngày kết thúc (yyyy-MM-dd)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        RevenueStatsResponse response = statisticsService.getRevenueStats(from, to);
        return ApiResponse.success(response, "Revenue statistics retrieved successfully");
    }

    @Operation(
            summary = "Thống kê công nợ khách hàng",
            description = "Trả về danh sách khách hàng có remaining_debt > 0, sắp xếp theo remaining_debt giảm dần."
    )
    @GetMapping("/debts")
    public ApiResponse<List<DebtStatsResponse>> getDebtStats() {
        List<DebtStatsResponse> response = statisticsService.getDebtStats();
        return ApiResponse.success(response, "Debt statistics retrieved successfully");
    }

    @Operation(
            summary = "Thống kê doanh thu theo tháng trong năm",
            description = "Trả về mảng 12 phần tử với tổng doanh thu từng tháng của năm chỉ định (index 0 = tháng 1, index 11 = tháng 12)."
    )
    @GetMapping("/revenue/monthly")
    public ApiResponse<MonthlyRevenueResponse> getMonthlyRevenue(
            @Parameter(description = "Năm cần thống kê (ví dụ: 2024)", required = true)
            @RequestParam int year) {
        MonthlyRevenueResponse response = statisticsService.getMonthlyRevenue(year);
        return ApiResponse.success(response, "Monthly revenue statistics retrieved successfully");
    }
}
