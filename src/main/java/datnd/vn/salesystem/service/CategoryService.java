package datnd.vn.salesystem.service;

import datnd.vn.salesystem.common.SearchRequest;
import datnd.vn.salesystem.common.SpecificationBuilder;
import datnd.vn.salesystem.entity.Category;
import datnd.vn.salesystem.exception.BusinessRuleException;
import datnd.vn.salesystem.exception.DuplicateResourceException;
import datnd.vn.salesystem.exception.EntityNotFoundException;
import datnd.vn.salesystem.repository.CategoryRepository;
import datnd.vn.salesystem.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional
    public Category createCategory(String name, String description) {
        categoryRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            throw new DuplicateResourceException("Tên danh mục '" + name + "' đã tồn tại");
        });

        Category category = Category.builder()
                .name(name)
                .description(description)
                .active(true)
                .build();

        Category saved = categoryRepository.save(category);
        saved.setCode(String.format("DM%07d", saved.getId()));
        return categoryRepository.save(saved);
    }

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAllByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Page<Category> searchCategories(SearchRequest request) {
        Specification<Category> spec = SpecificationBuilder.<Category>builder()
                .like("name", (String) request.getFilters().get("name"))
                .eq("active", request.getFilters().getOrDefault("active", true))
                .build();
        return categoryRepository.findAll(spec, request.toPageable());
    }

    @Transactional(readOnly = true)
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy danh mục với mã: " + id));
    }

    @Transactional
    public Category updateCategory(Long id, String name, String description) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy danh mục với mã: " + id));

        categoryRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException("Tên danh mục '" + name + "' đã tồn tại");
            }
        });

        category.setName(name);
        category.setDescription(description);
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy danh mục với mã: " + id));

        if (productRepository.existsByCategoryIdAndActiveTrue(id)) {
            throw new BusinessRuleException("Không thể xóa danh mục vì đang có sản phẩm liên kết");
        }

        category.setActive(false);
        categoryRepository.save(category);
    }
}
