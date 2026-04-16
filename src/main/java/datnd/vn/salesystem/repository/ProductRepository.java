package datnd.vn.salesystem.repository;

import datnd.vn.salesystem.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    boolean existsByCategoryIdAndActiveTrue(Long categoryId);

    @EntityGraph(attributePaths = {"category"})
    List<Product> findAllByActiveTrue();

    @EntityGraph(attributePaths = {"category"})
    List<Product> findAllByCategoryIdAndActiveTrue(Long categoryId);
}
