package datnd.vn.salesystem.controller;

import datnd.vn.salesystem.common.ApiResponse;
import datnd.vn.salesystem.service.SyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Sync", description = "Đồng bộ dữ liệu về local DB")
@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService syncService;

    @Operation(
            summary = "Trigger đồng bộ thủ công",
            description = "Đồng bộ toàn bộ dữ liệu từ Supabase về local DB ngay lập tức. " +
                    "Tự động chạy lúc 23:00 mỗi ngày."
    )
    @PostMapping("/trigger")
    public ApiResponse<SyncService.SyncResult> triggerSync() {
        SyncService.SyncResult result = syncService.syncAll();
        String message = result.success()
                ? String.format("Đồng bộ thành công: %d categories, %d products, %d customers, %d orders, %d orderItems, %d payments (%dms)",
                result.categories(), result.products(), result.customers(),
                result.orders(), result.orderItems(), result.payments(), result.elapsedMs())
                : "Đồng bộ thất bại: " + result.errorMessage();
        return ApiResponse.success(result, message);
    }
}
