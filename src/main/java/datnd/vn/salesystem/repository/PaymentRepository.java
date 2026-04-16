package datnd.vn.salesystem.repository;

import datnd.vn.salesystem.entity.Payment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {"customer"})
    List<Payment> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    @EntityGraph(attributePaths = {"customer"})
    List<Payment> findByPaymentDateBetween(LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = {"customer"})
    List<Payment> findByCustomerIdAndPaymentDateBetween(Long customerId, LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = {"customer"})
    List<Payment> findAll();

    /**
     * Sum payment amounts whose payment_date falls within the given range.
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paymentDate BETWEEN :from AND :to")
    BigDecimal sumAmountBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
