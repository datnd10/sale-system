package datnd.vn.salesystem.repository;

import datnd.vn.salesystem.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    Optional<Customer> findByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);
}
