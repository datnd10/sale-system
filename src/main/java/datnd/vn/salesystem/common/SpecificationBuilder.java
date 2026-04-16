package datnd.vn.salesystem.common;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generic JPA Specification builder for dynamic search queries.
 *
 * <p>Supports:
 * <ul>
 *   <li>Exact match — {@code eq(field, value)}</li>
 *   <li>Case-insensitive LIKE — {@code like(field, value)}</li>
 *   <li>Boolean match — {@code eq(field, value)}</li>
 *   <li>Bulk filter from map — {@code fromFilters(filters)}</li>
 * </ul>
 *
 * <p>Example:
 * <pre>
 *   Specification&lt;Category&gt; spec = SpecificationBuilder.&lt;Category&gt;builder()
 *       .like("name", request.getName())
 *       .eq("active", true)
 *       .build();
 *   Page&lt;Category&gt; page = categoryRepository.findAll(spec, request.toPageable());
 * </pre>
 *
 * @param <T> the entity type
 */
public class SpecificationBuilder<T> {

    private final List<Specification<T>> specs = new ArrayList<>();

    public static <T> SpecificationBuilder<T> builder() {
        return new SpecificationBuilder<>();
    }

    /** Exact match predicate. Skipped when value is null. */
    public SpecificationBuilder<T> eq(String field, Object value) {
        if (value != null) {
            specs.add((root, query, cb) -> cb.equal(root.get(field), value));
        }
        return this;
    }

    /**
     * Case-insensitive, accent-insensitive LIKE predicate dùng PostgreSQL unaccent().
     * Tìm "ton" sẽ khớp "tôn", "tón", "tòn"...
     * Skipped when value is null or blank.
     */
    public SpecificationBuilder<T> like(String field, String value) {
        if (value != null && !value.isBlank()) {
            specs.add((root, query, cb) -> {
                // unaccent(lower(field)) LIKE unaccent(lower('%keyword%'))
                var unaccentField = cb.function("unaccent", String.class, cb.lower(root.get(field)));
                var unaccentValue = cb.function("unaccent", String.class,
                        cb.literal("%" + value.toLowerCase() + "%"));
                return cb.like(unaccentField, unaccentValue);
            });
        }
        return this;
    }

    /**
     * Bulk-add exact-match predicates from a filter map.
     * Keys are entity field names; values are the expected values.
     */
    public SpecificationBuilder<T> fromFilters(Map<String, Object> filters) {
        if (filters != null) {
            filters.forEach(this::eq);
        }
        return this;
    }

    /** Combine all predicates with AND and return the final Specification. */
    public Specification<T> build() {
        return (root, query, cb) -> {
            List<Predicate> predicates = specs.stream()
                    .map(s -> s.toPredicate(root, query, cb))
                    .toList();
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
