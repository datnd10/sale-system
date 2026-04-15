package datnd.vn.salesystem.service;

import datnd.vn.salesystem.dto.response.DebtStatsResponse;
import datnd.vn.salesystem.dto.response.MonthlyRevenueResponse;
import datnd.vn.salesystem.dto.response.RevenueStatsResponse;
import datnd.vn.salesystem.exception.InvalidRequestException;
import datnd.vn.salesystem.repository.DebtRepository;
import datnd.vn.salesystem.repository.OrderRepository;
import datnd.vn.salesystem.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final DebtRepository debtRepository;

    // -------------------------------------------------------------------------
    // getRevenueStats
    // -------------------------------------------------------------------------

    /**
     * Returns revenue statistics for the given date range.
     *
     * - total_orders: count of active orders whose order_date is in [from, to]
     * - total_revenue: sum of total_amount of those orders
     * - total_collected: sum of paid_immediately from those orders
     *                  + sum of payment amounts whose payment_date is in [from, to]
     * - total_debt: total_revenue - total_collected
     *
     * Throws InvalidRequestException (400) if from > to.
     *
     * Requirements: 7.1, 7.4
     */
    @Transactional(readOnly = true)
    public RevenueStatsResponse getRevenueStats(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new InvalidRequestException("Start date must not be after end date");
        }

        long totalOrders = orderRepository.countActiveOrdersBetween(from, to);
        BigDecimal totalRevenue = orderRepository.sumTotalAmountBetween(from, to);
        BigDecimal paidImmediately = orderRepository.sumPaidImmediatelyBetween(from, to);
        BigDecimal paymentsInRange = paymentRepository.sumAmountBetween(from, to);

        BigDecimal totalCollected = paidImmediately.add(paymentsInRange);
        BigDecimal totalDebt = totalRevenue.subtract(totalCollected);

        return RevenueStatsResponse.builder()
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .totalCollected(totalCollected)
                .totalDebt(totalDebt)
                .build();
    }

    // -------------------------------------------------------------------------
    // getDebtStats
    // -------------------------------------------------------------------------

    /**
     * Returns a list of customers with remaining_debt > 0, sorted descending by remaining debt.
     *
     * Requirements: 7.2
     */
    @Transactional(readOnly = true)
    public List<DebtStatsResponse> getDebtStats() {
        List<Object[]> rows = debtRepository.findCustomerDebtSummariesDescending();

        List<DebtStatsResponse> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(DebtStatsResponse.builder()
                    .customerId((Long) row[0])
                    .customerCode((String) row[1])
                    .customerName((String) row[2])
                    .remainingDebt((BigDecimal) row[3])
                    .build());
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // getMonthlyRevenue
    // -------------------------------------------------------------------------

    /**
     * Returns an array of 12 BigDecimal values representing total revenue per month
     * for the given year. Index 0 = January, index 11 = December.
     *
     * Requirements: 7.3
     */
    @Transactional(readOnly = true)
    public MonthlyRevenueResponse getMonthlyRevenue(int year) {
        List<BigDecimal> monthly = new ArrayList<>(12);
        for (int month = 1; month <= 12; month++) {
            BigDecimal revenue = orderRepository.sumTotalAmountByYearAndMonth(year, month);
            monthly.add(revenue);
        }
        return MonthlyRevenueResponse.builder()
                .year(year)
                .monthlyRevenue(monthly)
                .build();
    }
}
