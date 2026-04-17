package datnd.vn.salesystem.service;

import datnd.vn.salesystem.common.PageResponse;
import datnd.vn.salesystem.entity.Customer;
import datnd.vn.salesystem.entity.Debt;
import datnd.vn.salesystem.dto.response.DebtSummaryResponse;
import datnd.vn.salesystem.exception.EntityNotFoundException;
import datnd.vn.salesystem.repository.CustomerRepository;
import datnd.vn.salesystem.repository.DebtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DebtService {

    private final DebtRepository debtRepository;
    private final CustomerRepository customerRepository;

    /**
     * Result DTO for a single customer's aggregated debt summary.
     */
    public record CustomerDebtSummary(Customer customer, BigDecimal totalRemaining) {}

    /**
     * Result DTO for a customer's detailed debt list.
     */
    public record CustomerDebtDetail(Customer customer, List<Debt> debts, BigDecimal totalRemaining) {}

    // -------------------------------------------------------------------------
    // getAllCustomerDebts
    // -------------------------------------------------------------------------

    /**
     * Returns a list of all customers that have remaining debt (remaining_amount > 0),
     * with the total remaining amount per customer.
     *
     * Requirements: 5.1
     */
    @Transactional(readOnly = true)
    public PageResponse<DebtSummaryResponse> searchDebts(
            String customerName, int page, int size, String sort, Sort.Direction direction) {

        List<CustomerDebtSummary> all = getAllCustomerDebts();

        // Filter by customer name
        List<CustomerDebtSummary> filtered = all.stream()
                .filter(s -> customerName == null || customerName.isBlank() ||
                        s.customer().getName().toLowerCase().contains(customerName.toLowerCase()))
                .collect(Collectors.toList());

        // Sort
        Comparator<CustomerDebtSummary> comparator = Comparator.comparing(
                s -> s.totalRemaining(), Comparator.reverseOrder());
        if (direction == Sort.Direction.ASC) {
            comparator = Comparator.comparing(s -> s.totalRemaining());
        }
        filtered.sort(comparator);

        // Paginate
        int total = filtered.size();
        int fromIdx = Math.min(page * size, total);
        int toIdx = Math.min(fromIdx + size, total);
        List<DebtSummaryResponse> content = filtered.subList(fromIdx, toIdx)
                .stream().map(DebtSummaryResponse::from).toList();

        return new PageResponse<>(content, page, size, total,
                (int) Math.ceil((double) total / size), fromIdx + size >= total);
    }

    @Transactional(readOnly = true)
    public List<CustomerDebtSummary> getAllCustomerDebts() {
        // Fetch all debts with remaining_amount > 0 (customer eagerly loaded)
        List<Debt> allDebts = debtRepository.findAllWithPositiveRemainingAmount();

        // Group by customer and sum remaining_amount
        Map<Customer, BigDecimal> debtByCustomer = allDebts.stream()
                .collect(Collectors.groupingBy(
                        Debt::getCustomer,
                        Collectors.reducing(BigDecimal.ZERO, Debt::getRemainingAmount, BigDecimal::add)
                ));

        // Build result list — only customers with remaining > 0
        return debtByCustomer.entrySet().stream()
                .filter(e -> e.getValue().compareTo(BigDecimal.ZERO) > 0)
                .map(e -> new CustomerDebtSummary(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // getCustomerDebtDetail
    // -------------------------------------------------------------------------

    /**
     * Returns the list of Debt records for a specific customer (remaining_amount > 0),
     * plus the total remaining amount.
     *
     * Throws EntityNotFoundException (404) if customer does not exist.
     *
     * Requirements: 5.2
     */
    @Transactional(readOnly = true)
    public CustomerDebtDetail getCustomerDebtDetail(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách hàng với mã: " + customerId));

        List<Debt> debts = debtRepository
                .findAllByCustomerIdAndRemainingAmountGreaterThanOrderByCreatedAtAsc(customerId, BigDecimal.ZERO);

        BigDecimal totalRemaining = debts.stream()
                .map(Debt::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CustomerDebtDetail(customer, debts, totalRemaining);
    }

    // -------------------------------------------------------------------------
    // updateCustomerDebtFlag
    // -------------------------------------------------------------------------

    /**
     * Recalculates the total remaining_amount for a customer across all their Debt records
     * and updates the customer's has_debt flag accordingly:
     * - has_debt = true  if total remaining > 0  (Req 5.3)
     * - has_debt = false if total remaining <= 0  (Req 5.4)
     *
     * Throws EntityNotFoundException (404) if customer does not exist.
     *
     * Requirements: 5.3, 5.4
     */
    @Transactional
    public Customer updateCustomerDebtFlag(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách hàng với mã: " + customerId));

        // Sum all remaining_amount for this customer (including zero/negative entries)
        List<Debt> allDebts = debtRepository.findAllByCustomerId(customerId);

        BigDecimal totalRemaining = allDebts.stream()
                .map(Debt::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        customer.setHasDebt(totalRemaining.compareTo(BigDecimal.ZERO) > 0);
        return customerRepository.save(customer);
    }
}
