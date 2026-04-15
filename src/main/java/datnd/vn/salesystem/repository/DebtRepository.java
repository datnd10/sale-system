package datnd.vn.salesystem.repository;

import datnd.vn.salesystem.entity.Debt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface DebtRepository extends JpaRepository<Debt, Long> {

    List<Debt> findAllByCustomerIdAndRemainingAmountGreaterThanOrderByCreatedAtAsc(Long customerId, BigDecimal remainingAmount);
}
