package datnd.vn.salesystem.controller;

import datnd.vn.salesystem.common.ApiResponse;
import datnd.vn.salesystem.common.PageResponse;
import datnd.vn.salesystem.common.SearchRequest;
import datnd.vn.salesystem.constant.Constants;
import datnd.vn.salesystem.dto.request.ProductRequest;
import datnd.vn.salesystem.dto.response.ProductResponse;
import datnd.vn.salesystem.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Product", description = "Quản lý sản phẩm vật liệu xây dựng")
@RestController
@RequestMapping(Constants.URI.PRODUCT)
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Tạo sản phẩm mới")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = ProductResponse.from(
                productService.createProduct(
                        request.getName(),
                        request.getCategoryId(),
                        request.getUnit(),
                        request.getPrice(),
                        request.getHeight(),
                        request.getWidth(),
                        request.getDescription()
                )
        );
        return ApiResponse.success(response, "Product created successfully");
    }

    @Operation(summary = "Tìm kiếm sản phẩm có phân trang", description = "Tìm theo tên, lọc theo categoryId với hỗ trợ phân trang")
    @GetMapping("/search")
    public ApiResponse<PageResponse<ProductResponse>> searchProducts(
            @Parameter(description = "Tên sản phẩm (tìm kiếm gần đúng)") @RequestParam(required = false) String name,
            @Parameter(description = "Lọc theo ID danh mục") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Số trang (bắt đầu từ 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số bản ghi mỗi trang") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Trường sắp xếp") @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "Chiều sắp xếp: ASC hoặc DESC") @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        SearchRequest request = new SearchRequest();
        request.setPage(page);
        request.setSize(size);
        request.setSort(sort);
        request.setDirection(direction);
        request.filter("name", name);
        request.filter("categoryId", categoryId);

        PageResponse<ProductResponse> result = PageResponse.from(
                productService.searchProducts(request).map(ProductResponse::from)
        );
        return ApiResponse.success(result, "Products retrieved successfully");
    }

    @Operation(summary = "Lấy danh sách sản phẩm", description = "Trả về sản phẩm đang active, hỗ trợ lọc theo categoryId")
    @GetMapping
    public ApiResponse<List<ProductResponse>> getProducts(
            @Parameter(description = "Lọc theo ID danh mục (tùy chọn)")
            @RequestParam(required = false) Long categoryId) {
        List<ProductResponse> products = productService.getProducts(categoryId)
                .stream()
                .map(ProductResponse::from)
                .toList();
        return ApiResponse.success(products, "Products retrieved successfully");
    }

    @Operation(summary = "Lấy chi tiết sản phẩm theo ID")
    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProductById(
            @Parameter(description = "ID sản phẩm") @PathVariable Long id) {
        return ApiResponse.success(
                ProductResponse.from(productService.getProductById(id)),
                "Product retrieved successfully"
        );
    }

    @Operation(summary = "Cập nhật sản phẩm")
    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> updateProduct(
            @Parameter(description = "ID sản phẩm") @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = ProductResponse.from(
                productService.updateProduct(
                        id,
                        request.getName(),
                        request.getCategoryId(),
                        request.getUnit(),
                        request.getPrice(),
                        request.getHeight(),
                        request.getWidth(),
                        request.getDescription()
                )
        );
        return ApiResponse.success(response, "Product updated successfully");
    }

    @Operation(summary = "Xóa sản phẩm (soft delete)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProduct(
            @Parameter(description = "ID sản phẩm") @PathVariable Long id) {
        productService.deleteProduct(id);
        return ApiResponse.success(null, "Product deleted successfully");
    }
}
