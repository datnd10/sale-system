package datnd.vn.salesystem.service;

import datnd.vn.salesystem.common.PageResponse;
import datnd.vn.salesystem.dto.response.PaymentResponse;
import datnd.vn.salesystem.entity.Payment;
import datnd.vn.salesystem.exception.EntityNotFoundException;
import datnd.vn.salesystem.repository.CustomerRepository;
import datnd.vn.salesystem.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;

    /**
     * Lịch sử thanh toán của một khách hàng, sắp xếp mới nhất trước.
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByCustomer(Long customerId) {
        customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy khách hàng với mã: " + customerId));

        return paymentRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(PaymentResponse::from)
                .toList();
    }

    /**
     * Tìm kiếm thanh toán có phân trang, lọc theo tên khách hàng và khoảng ngày.
     */
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> searchPayments(
            String customerName, LocalDate from, LocalDate to,
            int page, int size, String sort, Sort.Direction direction) {

        Specification<Payment> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (query != null && Long.class != query.getResultType()) {
                root.fetch("customer", jakarta.persistence.criteria.JoinType.LEFT);
                root.fetch("order", jakarta.persistence.criteria.JoinType.LEFT);
            }
            if (customerName != null && !customerName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("customer").get("name")),
                        "%" + customerName.toLowerCase() + "%"));
            }
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("paymentDate"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("paymentDate"), to));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        var pageable = PageRequest.of(page, size, Sort.by(direction, sort));
        return PageResponse.from(paymentRepository.findAll(spec, pageable).map(PaymentResponse::from));
    }

    /**
     * Tất cả thanh toán, lọc theo khoảng ngày.
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments(LocalDate from, LocalDate to) {
        List<Payment> payments = (from != null && to != null)
                ? paymentRepository.findByPaymentDateBetween(from, to)
                : paymentRepository.findAll();

        return payments.stream().map(PaymentResponse::from).toList();
    }
}
