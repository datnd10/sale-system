package datnd.vn.salesystem.controller;

import datnd.vn.salesystem.common.ApiResponse;
import datnd.vn.salesystem.common.PageResponse;
import datnd.vn.salesystem.common.SearchRequest;
import datnd.vn.salesystem.constant.Constants;
import datnd.vn.salesystem.dto.request.CategoryRequest;
import datnd.vn.salesystem.dto.response.CategoryResponse;
import datnd.vn.salesystem.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Category", description = "Quản lý danh mục mặt hàng (tôn, sắt, thép,...)")
@RestController
@RequestMapping(Constants.URI.CATEGORY)
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Tạo danh mục mới", description = "Tạo một danh mục mặt hàng mới. Tên danh mục phải là duy nhất.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tạo thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Tên danh mục đã tồn tại", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = CategoryResponse.from(
                categoryService.createCategory(request.getName(), request.getDescription())
        );
        return ApiResponse.success(response, "Category created successfully");
    }

    @Operation(summary = "Lấy danh sách danh mục", description = "Trả về tất cả danh mục đang hoạt động (active = true).")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Thành công")
    })
    @GetMapping
    public ApiResponse<List<CategoryResponse>> getAllCategories() {
        List<CategoryResponse> categories = categoryService.getAllCategories()
                .stream()
                .map(CategoryResponse::from)
                .toList();
        return ApiResponse.success(categories, "Categories retrieved successfully");
    }

    @Operation(summary = "Tìm kiếm danh mục có phân trang", description = "Tìm kiếm danh mục theo tên (LIKE, không phân biệt hoa thường) với hỗ trợ phân trang và sắp xếp.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Thành công")
    })
    @GetMapping("/search")
    public ApiResponse<PageResponse<CategoryResponse>> searchCategories(
            @Parameter(description = "Tên danh mục cần tìm (tìm kiếm gần đúng)") @RequestParam(required = false) String name,
            @Parameter(description = "Số trang (bắt đầu từ 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số bản ghi mỗi trang (tối đa 100)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Trường sắp xếp") @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "Chiều sắp xếp: ASC hoặc DESC") @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        SearchRequest request = new SearchRequest();
        request.setPage(page);
        request.setSize(size);
        request.setSort(sort);
        request.setDirection(direction);
        request.filter("name", name);

        PageResponse<CategoryResponse> result = PageResponse.from(
                categoryService.searchCategories(request).map(CategoryResponse::from)
        );
        return ApiResponse.success(result, "Categories retrieved successfully");
    }

    @Operation(summary = "Lấy chi tiết danh mục", description = "Lấy thông tin chi tiết của một danh mục theo ID.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy danh mục", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> getCategoryById(
            @Parameter(description = "ID của danh mục") @PathVariable Long id) {
        CategoryResponse response = CategoryResponse.from(categoryService.getCategoryById(id));
        return ApiResponse.success(response, "Category retrieved successfully");
    }

    @Operation(summary = "Cập nhật danh mục", description = "Cập nhật tên và mô tả của danh mục theo ID.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy danh mục", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Tên danh mục đã tồn tại", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> updateCategory(
            @Parameter(description = "ID của danh mục") @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = CategoryResponse.from(
                categoryService.updateCategory(id, request.getName(), request.getDescription())
        );
        return ApiResponse.success(response, "Category updated successfully");
    }

    @Operation(summary = "Xóa danh mục (soft delete)", description = "Đánh dấu danh mục là không hoạt động. Không thể xóa nếu còn sản phẩm liên kết.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xóa thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy danh mục", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Danh mục đang có sản phẩm liên kết", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCategory(
            @Parameter(description = "ID của danh mục") @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ApiResponse.success(null, "Category deleted successfully");
    }
}
