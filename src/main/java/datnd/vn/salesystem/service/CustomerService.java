package datnd.vn.salesystem.service;

import datnd.vn.salesystem.common.SearchRequest;
import datnd.vn.salesystem.common.SpecificationBuilder;
import datnd.vn.salesystem.entity.Customer;
import datnd.vn.salesystem.exception.DuplicateResourceException;
import datnd.vn.salesystem.exception.EntityNotFoundException;
import datnd.vn.salesystem.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public Customer createCustomer(String name, String phone, String address) {
        if (phone != null && !phone.isBlank()) {
            customerRepository.findByPhone(phone).ifPresent(existing -> {
                throw new DuplicateResourceException("Số điện thoại '" + phone + "' đã được sử dụng");
            });
        }

        Customer customer = Customer.builder()
                .name(name)
                .phone(phone)
                .address(address)
                .hasDebt(false)
                .active(true)
                .build();

        Customer saved = customerRepository.save(customer);
        saved.setCode(String.format("KH%07d", saved.getId()));
        return customerRepository.save(saved);
    }

    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Customer> searchCustomers(SearchRequest request) {
        Specification<Customer> spec = SpecificationBuilder.<Customer>builder()
                .like("name", (String) request.getFilters().get("name"))
                .like("phone", (String) request.getFilters().get("phone"))
                .build();
        return customerRepository.findAll(spec, request.toPageable());
    }

    @Transactional(readOnly = true)
    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách hàng với mã: " + id));
    }

    @Transactional
    public Customer updateCustomer(Long id, String name, String phone, String address) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách hàng với mã: " + id));

        if (phone != null && !phone.isBlank()) {
            if (customerRepository.existsByPhoneAndIdNot(phone, id)) {
                throw new DuplicateResourceException("Số điện thoại '" + phone + "' đã được sử dụng");
            }
        }

        customer.setName(name);
        customer.setPhone(phone);
        customer.setAddress(address);
        return customerRepository.save(customer);
    }
}
