package datnd.vn.salesystem.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic search + pagination request.
 * <p>
 * Usage in controller:
 * <pre>
 *   GET /api/categories?page=0&size=10&sort=name&direction=ASC&name=ton
 * </pre>
 * Extend this class or use {@code filters} map for module-specific filters.
 */
@Data
public class SearchRequest {

    @Min(0)
    private int page = 0;

    @Min(1)
    @Max(100)
    private int size = 10;

    /** Field name to sort by (must match entity field name). */
    private String sort = "createdAt";

    private Sort.Direction direction = Sort.Direction.DESC;

    /**
     * Dynamic key-value filters passed as query params.
     * Populated manually in controller or via subclass fields.
     */
    private Map<String, Object> filters = new HashMap<>();

    public Pageable toPageable() {
        return PageRequest.of(page, size, Sort.by(direction, sort));
    }

    /** Convenience: add a filter only when value is non-null and non-blank. */
    public SearchRequest filter(String key, Object value) {
        if (value != null && !value.toString().isBlank()) {
            filters.put(key, value);
        }
        return this;
    }
}
