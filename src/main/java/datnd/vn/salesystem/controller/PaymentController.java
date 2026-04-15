package datnd.vn.salesystem.controller;

import datnd.vn.salesystem.common.ApiResponse;
import datnd.vn.salesystem.constant.Constants;
import datnd.vn.salesystem.dto.request.PaymentRequest;
import datnd.vn.salesystem.dto.response.PaymentResponse;
import datnd.vn.salesystem.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @Operation(summary = "Ghi nhận thanh toán",
               description = "Tạo một giao dịch thanh toán và trừ nợ theo thứ tự FIFO.")
    @PostMapping
    public ApiResponse<PaymentResponse> createPayment(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.createPayment(request);
        return ApiResponse.success(response, "Payment created successfully");
    }

    @Operation(summary = "Lấy lịch sử thanh toán của khách hàng",
               description = "Trả về danh sách các giao dịch thanh toán của khách hàng theo ID, sắp xếp mới nhất trước.")
    @GetMapping("/customer/{customerId}")
    public ApiResponse<List<PaymentResponse>> getPaymentsByCustomer(
            @Parameter(description = "ID khách hàng") @PathVariable Long customerId) {
        List<PaymentResponse> response = paymentService.getPaymentsByCustomer(customerId);
        return ApiResponse.success(response, "Customer payments retrieved successfully");
    }

    @Operation(summary = "Lấy danh sách tất cả thanh toán",
               description = "Trả về tất cả giao dịch thanh toán, hỗ trợ lọc theo khoảng thời gian.")
    @GetMapping
    public ApiResponse<List<PaymentResponse>> getAllPayments(
            @Parameter(description = "Ngày bắt đầu (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Ngày kết thúc (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<PaymentResponse> response = paymentService.getAllPayments(from, to);
        return ApiResponse.success(response, "Payments retrieved successfully");
    }
}
