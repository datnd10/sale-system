package datnd.vn.salesystem.repository;

import datnd.vn.salesystem.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Payment> findByPaymentDateBetween(LocalDate from, LocalDate to);

    List<Payment> findByCustomerIdAndPaymentDateBetween(Long customerId, LocalDate from, LocalDate to);
}
