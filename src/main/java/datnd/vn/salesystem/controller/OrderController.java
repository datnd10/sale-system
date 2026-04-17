package datnd.vn.salesystem.controller;

import datnd.vn.salesystem.common.ApiResponse;
import datnd.vn.salesystem.common.PageResponse;
import datnd.vn.salesystem.common.SearchRequest;
import datnd.vn.salesystem.constant.Constants;
import datnd.vn.salesystem.dto.request.OrderRequest;
import datnd.vn.salesystem.dto.request.UpdateNoteRequest;
import datnd.vn.salesystem.dto.response.OrderDetailResponse;
import datnd.vn.salesystem.dto.response.OrderResponse;
import datnd.vn.salesystem.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Order", description = "Quản lý đơn hàng")
@RestController
@RequestMapping(Constants.URI.ORDER)
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Tạo đơn hàng mới",
            description = "Tạo đơn hàng với danh sách sản phẩm. Tự động tính total_amount và tạo bản ghi Debt.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderDetailResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        List<OrderService.OrderItemRequest> itemRequests = request.getItems().stream()
                .map(item -> new OrderService.OrderItemRequest(
                        item.getProductId(),
                        item.getCount(),
                        item.getLength(),
                        item.getWidth(),
                        item.getHeight()
                ))
                .toList();

        OrderService.OrderWithItems result = orderService.createOrder(
                request.getCustomerId(),
                request.getOrderDate(),
                itemRequests,
                request.getPaidImmediately()
        );

        return ApiResponse.success(
                OrderDetailResponse.from(result.order(), result.items()),
                "Order created successfully"
        );
    }

    @Operation(summary = "Tìm kiếm đơn hàng có phân trang",
            description = "Lọc theo customerId, khoảng thời gian với hỗ trợ phân trang.")
    @GetMapping("/search")
    public ApiResponse<PageResponse<OrderResponse>> searchOrders(
            @Parameter(description = "Lọc theo Customer ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "Ngày bắt đầu (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Ngày kết thúc (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Số trang (bắt đầu từ 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số bản ghi mỗi trang") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Trường sắp xếp") @RequestParam(defaultValue = "orderDate") String sort,
            @Parameter(description = "Chiều sắp xếp: ASC hoặc DESC") @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        SearchRequest request = new SearchRequest();
        request.setPage(page);
        request.setSize(size);
        request.setSort(sort);
        request.setDirection(direction);
        if (customerId != null) request.filter("customerId", customerId);
        if (from != null) request.filter("from", from);
        if (to != null) request.filter("to", to);

        PageResponse<OrderResponse> result = PageResponse.from(
                orderService.searchOrders(request).map(OrderResponse::from)
        );
        return ApiResponse.success(result, "Orders retrieved successfully");
    }

    @Operation(summary = "Lấy danh sách đơn hàng",
            description = "Trả về danh sách đơn hàng, hỗ trợ lọc theo Customer ID và khoảng thời gian.")
    @GetMapping
    public ApiResponse<List<OrderResponse>> getOrders(
            @Parameter(description = "Lọc theo Customer ID (tùy chọn)")
            @RequestParam(required = false) Long customerId,
            @Parameter(description = "Ngày bắt đầu (yyyy-MM-dd, tùy chọn)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Ngày kết thúc (yyyy-MM-dd, tùy chọn)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<OrderResponse> orders = orderService.getOrders(customerId, from, to)
                .stream()
                .map(OrderResponse::from)
                .toList();

        return ApiResponse.success(orders, "Orders retrieved successfully");
    }

    @Operation(summary = "Lấy chi tiết đơn hàng theo ID",
            description = "Trả về thông tin đơn hàng kèm danh sách OrderItem.")
    @GetMapping("/{id:\\d+}")
    public ApiResponse<OrderDetailResponse> getOrderById(
            @Parameter(description = "ID đơn hàng") @PathVariable Long id) {

        OrderService.OrderWithItems result = orderService.getOrderById(id);
        return ApiResponse.success(
                OrderDetailResponse.from(result.order(), result.items()),
                "Order retrieved successfully"
        );
    }

    @Operation(summary = "Cập nhật đơn hàng",
            description = "Cập nhật toàn bộ thông tin đơn hàng: khách hàng, ngày, sản phẩm, thanh toán. Tự động tính lại Debt.")
    @PutMapping("/{id:\\d+}")
    public ApiResponse<OrderDetailResponse> updateOrder(
            @Parameter(description = "ID đơn hàng") @PathVariable Long id,
            @Valid @RequestBody OrderRequest request) {

        List<OrderService.OrderItemRequest> itemRequests = request.getItems().stream()
                .map(item -> new OrderService.OrderItemRequest(
                        item.getProductId(),
                        item.getCount(),
                        item.getLength(),
                        item.getWidth(),
                        item.getHeight()
                ))
                .toList();

        OrderService.OrderWithItems result = orderService.updateOrder(
                id,
                request.getCustomerId(),
                request.getOrderDate(),
                itemRequests,
                request.getPaidImmediately(),
                request.getNote()
        );

        return ApiResponse.success(
                OrderDetailResponse.from(result.order(), result.items()),
                "Order updated successfully"
        );
    }

    @Operation(summary = "Cập nhật ghi chú đơn hàng",
            description = "Cập nhật trường note của đơn hàng theo ID.")
    @PatchMapping("/{id:\\d+}/note")
    public ApiResponse<OrderResponse> updateOrderNote(
            @Parameter(description = "ID đơn hàng") @PathVariable Long id,
            @RequestBody UpdateNoteRequest request) {

        return ApiResponse.success(
                OrderResponse.from(orderService.updateOrderNote(id, request.getNote())),
                "Order note updated successfully"
        );
    }

    @Operation(summary = "Xóa đơn hàng (soft delete)",
            description = "Đánh dấu đơn hàng là không hoạt động (active = false).")
    @DeleteMapping("/{id:\\d+}")
    public ApiResponse<Void> deleteOrder(
            @Parameter(description = "ID đơn hàng") @PathVariable Long id) {

        orderService.deleteOrder(id);
        return ApiResponse.success(null, "Order deleted successfully");
    }
}
