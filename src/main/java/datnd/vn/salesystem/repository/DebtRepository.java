package datnd.vn.salesystem.repository;

import datnd.vn.salesystem.entity.Debt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface DebtRepository extends JpaRepository<Debt, Long> {

    List<Debt> findAllByCustomerIdAndRemainingAmountGreaterThanOrderByCreatedAtAsc(Long customerId, BigDecimal remainingAmount);

    List<Debt> findAllByCustomerId(Long customerId);

    /**
     * Returns all Debt records that have remaining_amount > 0, eagerly fetching the customer.
     * Used by DebtService.getAllCustomerDebts() to aggregate per-customer totals.
     */
    @Query("SELECT d FROM Debt d JOIN FETCH d.customer WHERE d.remainingAmount > 0")
    List<Debt> findAllWithPositiveRemainingAmount();

    /**
     * Returns aggregated debt per customer: [customerId, customerCode, customerName, totalRemaining].
     * Only includes customers with total remaining > 0, sorted descending by total.
     */
    @Query("""
            SELECT d.customer.id, d.customer.code, d.customer.name, SUM(d.remainingAmount)
            FROM Debt d
            GROUP BY d.customer.id, d.customer.code, d.customer.name
            HAVING SUM(d.remainingAmount) > 0
            ORDER BY SUM(d.remainingAmount) DESC
            """)
    List<Object[]> findCustomerDebtSummariesDescending();
}
