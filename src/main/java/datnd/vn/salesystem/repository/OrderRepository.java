package datnd.vn.salesystem.repository;

import datnd.vn.salesystem.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    List<Order> findAllByCustomerId(Long customerId);

    List<Order> findAllByOrderDateBetween(LocalDate from, LocalDate to);

    List<Order> findAllByCustomerIdAndOrderDateBetween(Long customerId, LocalDate from, LocalDate to);

    /**
     * Count active orders within a date range.
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.active = true AND o.orderDate BETWEEN :from AND :to")
    long countActiveOrdersBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Sum total_amount of active orders within a date range.
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.active = true AND o.orderDate BETWEEN :from AND :to")
    BigDecimal sumTotalAmountBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Sum paid_immediately of active orders within a date range.
     */
    @Query("SELECT COALESCE(SUM(o.paidImmediately), 0) FROM Order o WHERE o.active = true AND o.orderDate BETWEEN :from AND :to")
    BigDecimal sumPaidImmediatelyBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Sum total_amount of active orders for a specific month and year.
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.active = true AND YEAR(o.orderDate) = :year AND MONTH(o.orderDate) = :month")
    BigDecimal sumTotalAmountByYearAndMonth(@Param("year") int year, @Param("month") int month);
}
