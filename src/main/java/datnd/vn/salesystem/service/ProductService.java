package datnd.vn.salesystem.service;

import datnd.vn.salesystem.common.PageResponse;
import datnd.vn.salesystem.common.SearchRequest;
import datnd.vn.salesystem.common.SpecificationBuilder;
import datnd.vn.salesystem.constant.enums.ProductUnit;
import datnd.vn.salesystem.entity.Category;
import datnd.vn.salesystem.entity.Product;
import datnd.vn.salesystem.exception.EntityNotFoundException;
import datnd.vn.salesystem.repository.CategoryRepository;
import datnd.vn.salesystem.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public Product createProduct(String name, Long categoryId, ProductUnit unit, BigDecimal price,
                                 BigDecimal width, String description) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + categoryId));

        Product product = Product.builder()
                .name(name)
                .category(category)
                .unit(unit)
                .price(price)
                .width(width)
                .description(description)
                .active(true)
                .build();

        Product saved = productRepository.save(product);
        saved.setCode(String.format("SP%07d", saved.getId()));
        return productRepository.save(saved);
    }

    @Transactional(readOnly = true)
    public Page<Product> searchProducts(SearchRequest request) {
        Specification<Product> spec = SpecificationBuilder.<Product>builder()
                .like("name", (String) request.getFilters().get("name"))
                .eq("active", request.getFilters().getOrDefault("active", true))
                .eq("category.id", request.getFilters().get("categoryId"))
                .build();
        return productRepository.findAll(spec, request.toPageable());
    }

    @Transactional(readOnly = true)
    public List<Product> getProducts(Long categoryId) {
        if (categoryId != null) {
            return productRepository.findAllByCategoryIdAndActiveTrue(categoryId);
        }
        return productRepository.findAllByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));
    }

    @Transactional
    public Product updateProduct(Long id, String name, Long categoryId, ProductUnit unit,
                                 BigDecimal price, BigDecimal width, String description) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + categoryId));

        product.setName(name);
        product.setCategory(category);
        product.setUnit(unit);
        product.setPrice(price);
        product.setWidth(width);
        product.setDescription(description);
        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));
        product.setActive(false);
        productRepository.save(product);
    }
}
