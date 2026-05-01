package datnd.vn.salesystem.repository;

import datnd.vn.salesystem.constant.enums.OrderType;
import datnd.vn.salesystem.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    @EntityGraph(attributePaths = {"customer"})
    List<Order> findAllByCustomerId(Long customerId);

    @EntityGraph(attributePaths = {"customer"})
    List<Order> findAllByOrderDateBetween(LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = {"customer"})
    List<Order> findAllByCustomerIdAndOrderDateBetween(Long customerId, LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = {"customer"})
    List<Order> findAll();

    @EntityGraph(attributePaths = {"customer"})
    Optional<Order> findById(Long id);

    // -------------------------------------------------------------------------
    // Statistics queries — chỉ tính đơn SALE active
    // -------------------------------------------------------------------------

    @Query("SELECT COUNT(o) FROM Order o WHERE o.active = true AND o.orderType = 'SALE' AND o.orderDate BETWEEN :from AND :to")
    long countActiveSaleOrdersBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.active = true AND o.orderType = 'SALE' AND o.orderDate BETWEEN :from AND :to")
    BigDecimal sumSaleTotalAmountBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(o.paidImmediately), 0) FROM Order o WHERE o.active = true AND o.orderType = 'SALE' AND o.orderDate BETWEEN :from AND :to")
    BigDecimal sumSalePaidImmediatelyBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.active = true AND o.orderType = 'SALE' AND YEAR(o.orderDate) = :year AND MONTH(o.orderDate) = :month")
    BigDecimal sumSaleTotalAmountByYearAndMonth(@Param("year") int year, @Param("month") int month);

    // -------------------------------------------------------------------------
    // Debt calculation queries
    // -------------------------------------------------------------------------

    /**
     * Tổng tiền hàng (SALE) của một khách hàng.
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.customer.id = :customerId AND o.active = true AND o.orderType = 'SALE'")
    BigDecimal sumSaleTotalByCustomer(@Param("customerId") Long customerId);

    /**
     * Tổng công nợ còn lại của một khách hàng:
     * = Σ(SALE.total_amount) - Σ(Payment.amount)
     */
    @Query("""
            SELECT COALESCE(SUM(o.totalAmount), 0)
            FROM Order o
            WHERE o.customer.id = :customerId
              AND o.active = true
              AND o.orderType = 'SALE'
            """)
    BigDecimal sumSaleTotalAmountByCustomerId(@Param("customerId") Long customerId);

    /**
     * Danh sách khách hàng có công nợ > 0, sắp xếp giảm dần.
     * Công nợ = Σ(SALE.total_amount) - Σ(Payment.amount)
     */
    @Query("""
            SELECT o.customer.id, o.customer.code, o.customer.name,
                   COALESCE(SUM(o.totalAmount), 0) - COALESCE(
                       (SELECT SUM(p.amount) FROM Payment p WHERE p.customer.id = o.customer.id), 0
                   )
            FROM Order o
            WHERE o.active = true AND o.orderType = 'SALE'
            GROUP BY o.customer.id, o.customer.code, o.customer.name
            HAVING COALESCE(SUM(o.totalAmount), 0) - COALESCE(
                       (SELECT SUM(p.amount) FROM Payment p WHERE p.customer.id = o.customer.id), 0
                   ) > 0
            ORDER BY 4 DESC
            """)
    List<Object[]> findCustomerDebtSummariesDescending();
}
