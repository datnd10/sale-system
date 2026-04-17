package datnd.vn.salesystem.service;

import datnd.vn.salesystem.common.PageResponse;
import datnd.vn.salesystem.dto.request.PaymentRequest;
import datnd.vn.salesystem.dto.response.PaymentResponse;
import datnd.vn.salesystem.entity.Customer;
import datnd.vn.salesystem.entity.Debt;
import datnd.vn.salesystem.entity.Payment;
import datnd.vn.salesystem.exception.EntityNotFoundException;
import datnd.vn.salesystem.exception.InvalidRequestException;
import datnd.vn.salesystem.repository.CustomerRepository;
import datnd.vn.salesystem.repository.DebtRepository;
import datnd.vn.salesystem.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final DebtRepository debtRepository;
    private final DebtService debtService;

    /**
     * Creates a new Payment and deducts the amount from the customer's debts using FIFO order.
     *
     * Requirements: 6.1, 6.2, 6.3, 6.4, 9.1, 9.2
     */
    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) {
        // Validate customer exists
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy khách hàng với mã: " + request.getCustomerId()));

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Số tiền thanh toán phải lớn hơn 0");
        }

        // Determine payment date — default to today if not provided
        LocalDate paymentDate = request.getPaymentDate() != null
                ? request.getPaymentDate()
                : LocalDate.now();

        // FIFO debt deduction
        List<Debt> debts = debtRepository
                .findAllByCustomerIdAndRemainingAmountGreaterThanOrderByCreatedAtAsc(
                        customer.getId(), BigDecimal.ZERO);

        BigDecimal remaining = request.getAmount();
        for (Debt debt : debts) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal deduction = remaining.min(debt.getRemainingAmount());
            debt.setRemainingAmount(debt.getRemainingAmount().subtract(deduction));
            remaining = remaining.subtract(deduction);
            debtRepository.save(debt);
        }

        // Save Payment record
        Payment payment = Payment.builder()
                .customer(customer)
                .amount(request.getAmount())
                .paymentDate(paymentDate)
                .note(request.getNote())
                .build();

        Payment saved = paymentRepository.save(payment);

        // Generate display code after save (id is now available)
        saved.setCode(String.format("TT%07d", saved.getId()));
        saved = paymentRepository.save(saved);

        // Update customer has_debt flag
        debtService.updateCustomerDebtFlag(customer.getId());

        return PaymentResponse.from(saved);
    }

    /**
     * Returns the payment history for a specific customer, sorted by created_at DESC.
     *
     * Requirements: 6.5
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
     * Returns all payments, optionally filtered by payment date range.
     *
     * Requirements: 6.6
     */
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> searchPayments(
            String customerName, LocalDate from, LocalDate to,
            int page, int size, String sort, Sort.Direction direction) {

        Specification<Payment> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (query != null && Long.class != query.getResultType()) {
                root.fetch("customer", jakarta.persistence.criteria.JoinType.LEFT);
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

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments(LocalDate from, LocalDate to) {
        List<Payment> payments;

        if (from != null && to != null) {
            payments = paymentRepository.findByPaymentDateBetween(from, to);
        } else {
            payments = paymentRepository.findAll();
        }

        return payments.stream()
                .map(PaymentResponse::from)
                .toList();
    }
}
