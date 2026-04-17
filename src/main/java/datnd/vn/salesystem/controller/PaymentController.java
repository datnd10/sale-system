package datnd.vn.salesystem.controller;

import datnd.vn.salesystem.common.ApiResponse;
import datnd.vn.salesystem.common.PageResponse;
import datnd.vn.salesystem.constant.Constants;
import datnd.vn.salesystem.dto.request.PaymentRequest;
import datnd.vn.salesystem.dto.response.PaymentResponse;
import datnd.vn.salesystem.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Payment", description = "Quản lý thanh toán công nợ")
@RestController
@RequestMapping(Constants.URI.PAYMENT)
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Ghi nhận thanh toán")
    @PostMapping
    public ApiResponse<PaymentResponse> createPayment(@Valid @RequestBody PaymentRequest request) {
        return ApiResponse.success(paymentService.createPayment(request), "Payment created successfully");
    }

    @Operation(summary = "Tìm kiếm thanh toán có phân trang")
    @GetMapping("/search")
    public ApiResponse<PageResponse<PaymentResponse>> searchPayments(
            @Parameter(description = "Lọc theo tên khách hàng") @RequestParam(required = false) String customerName,
            @Parameter(description = "Ngày bắt đầu (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Ngày kết thúc (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "paymentDate") String sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        return ApiResponse.success(
                paymentService.searchPayments(customerName, from, to, page, size, sort, direction),
                "Payments retrieved successfully");
    }

    @Operation(summary = "Lấy lịch sử thanh toán của khách hàng")
    @GetMapping("/customer/{customerId:\\d+}")
    public ApiResponse<List<PaymentResponse>> getPaymentsByCustomer(
            @Parameter(description = "ID khách hàng") @PathVariable Long customerId) {
        return ApiResponse.success(paymentService.getPaymentsByCustomer(customerId), "Customer payments retrieved successfully");
    }

    @Operation(summary = "Lấy danh sách tất cả thanh toán")
    @GetMapping
    public ApiResponse<List<PaymentResponse>> getAllPayments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(paymentService.getAllPayments(from, to), "Payments retrieved successfully");
    }
}
