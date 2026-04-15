package datnd.vn.salesystem.repository;

import datnd.vn.salesystem.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    List<Order> findAllByCustomerId(Long customerId);

    List<Order> findAllByOrderDateBetween(LocalDate from, LocalDate to);

    List<Order> findAllByCustomerIdAndOrderDateBetween(Long customerId, LocalDate from, LocalDate to);
}
