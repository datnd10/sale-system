package datnd.vn.salesystem.repository;

import datnd.vn.salesystem.entity.OrderItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @EntityGraph(attributePaths = {"product"})
    List<OrderItem> findAllByOrderId(Long orderId);

    boolean existsByProductId(Long productId);

    void deleteAllByOrderId(Long orderId);
}
