package datnd.vn.salesystem.controller;

import datnd.vn.salesystem.common.ApiResponse;
import datnd.vn.salesystem.common.PageResponse;
import datnd.vn.salesystem.common.SearchRequest;
import datnd.vn.salesystem.constant.Constants;
import datnd.vn.salesystem.constant.enums.OrderType;
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

@Tag(name = "Order", description = "Quản lý đơn hàng (bán hàng và trả nợ)")
@RestController
@RequestMapping(Constants.URI.ORDER)
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(
            summary = "Tạo đơn hàng mới",
            description = """
                    Tạo đơn hàng theo loại:
                    - **SALE**: Đơn bán hàng — cần truyền `items` và `paidImmediately` (tùy chọn).
                    - **PAYMENT**: Đơn trả nợ — chỉ cần truyền `amount`, không cần `items`.
                    """
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderDetailResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        List<OrderService.OrderItemRequest> itemRequests = toItemRequests(request);

        OrderService.OrderWithItems result = orderService.createOrder(
                request.getCustomerId(),
                request.getOrderType(),
                request.getOrderDate(),
                itemRequests,
                request.getPaidImmediately(),
                request.getAmount(),
                request.getNote()
        );

        return ApiResponse.success(
                OrderDetailResponse.from(result.order(), result.items()),
                "Order created successfully"
        );
    }

    @Operation(
            summary = "Tìm kiếm đơn hàng có phân trang",
            description = "Lọc theo customerId, orderType, khoảng thời gian với hỗ trợ phân trang."
    )
    @GetMapping("/search")
    public ApiResponse<PageResponse<OrderResponse>> searchOrders(
            @Parameter(description = "Lọc theo Customer ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "Lọc theo loại đơn: SALE hoặc PAYMENT") @RequestParam(required = false) OrderType orderType,
            @Parameter(description = "Ngày bắt đầu (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Ngày kết thúc (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "orderDate") String sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        SearchRequest request = new SearchRequest();
        request.setPage(page);
        request.setSize(size);
        request.setSort(sort);
        request.setDirection(direction);
        if (customerId != null) request.filter("customerId", customerId);
        if (orderType != null) request.filter("orderType", orderType);
        if (from != null) request.filter("from", from);
        if (to != null) request.filter("to", to);

        return ApiResponse.success(
                PageResponse.from(orderService.searchOrders(request).map(OrderResponse::from)),
                "Orders retrieved successfully"
        );
    }

    @Operation(summary = "Lấy danh sách đơn hàng", description = "Hỗ trợ lọc theo Customer ID và khoảng thời gian.")
    @GetMapping
    public ApiResponse<List<OrderResponse>> getOrders(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ApiResponse.success(
                orderService.getOrders(customerId, from, to).stream().map(OrderResponse::from).toList(),
                "Orders retrieved successfully"
        );
    }

    @Operation(summary = "Lấy chi tiết đơn hàng theo ID", description = "Trả về thông tin đơn hàng kèm OrderItems (nếu là đơn SALE).")
    @GetMapping("/{id:\\d+}")
    public ApiResponse<OrderDetailResponse> getOrderById(@PathVariable Long id) {
        OrderService.OrderWithItems result = orderService.getOrderById(id);
        return ApiResponse.success(
                OrderDetailResponse.from(result.order(), result.items()),
                "Order retrieved successfully"
        );
    }

    @Operation(
            summary = "Cập nhật đơn hàng",
            description = "Cập nhật toàn bộ thông tin đơn hàng. Tự động tính lại Payment."
    )
    @PutMapping("/{id:\\d+}")
    public ApiResponse<OrderDetailResponse> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderRequest request) {

        List<OrderService.OrderItemRequest> itemRequests = toItemRequests(request);

        OrderService.OrderWithItems result = orderService.updateOrder(
                id,
                request.getCustomerId(),
                request.getOrderType(),
                request.getOrderDate(),
                itemRequests,
                request.getPaidImmediately(),
                request.getAmount(),
                request.getNote()
        );

        return ApiResponse.success(
                OrderDetailResponse.from(result.order(), result.items()),
                "Order updated successfully"
        );
    }

    @Operation(summary = "Cập nhật ghi chú đơn hàng")
    @PatchMapping("/{id:\\d+}/note")
    public ApiResponse<OrderResponse> updateOrderNote(
            @PathVariable Long id,
            @RequestBody UpdateNoteRequest request) {
        return ApiResponse.success(
                OrderResponse.from(orderService.updateOrderNote(id, request.getNote())),
                "Order note updated successfully"
        );
    }

    @Operation(summary = "Xóa đơn hàng (soft delete)")
    @DeleteMapping("/{id:\\d+}")
    public ApiResponse<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ApiResponse.success(null, "Order deleted successfully");
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private List<OrderService.OrderItemRequest> toItemRequests(OrderRequest request) {
        if (request.getItems() == null) return List.of();
        return request.getItems().stream()
                .map(item -> new OrderService.OrderItemRequest(
                        item.getProductId(),
                        item.getCount(),
                        item.getLength(),
                        item.getWidth(),
                        item.getHeight()
                ))
                .toList();
    }
}
