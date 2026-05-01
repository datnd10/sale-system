package datnd.vn.salesystem.service;

import datnd.vn.salesystem.dto.response.DebtStatsResponse;
import datnd.vn.salesystem.dto.response.MonthlyRevenueResponse;
import datnd.vn.salesystem.dto.response.RevenueStatsResponse;
import datnd.vn.salesystem.exception.InvalidRequestException;
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

    /**
     * Thống kê doanh thu theo khoảng thời gian.
     * Chỉ tính đơn SALE active.
     * total_collected = Σ(Payment.amount trong khoảng thời gian)
     */
    @Transactional(readOnly = true)
    public RevenueStatsResponse getRevenueStats(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new InvalidRequestException("Ngày bắt đầu không được sau ngày kết thúc");
        }

        long totalOrders = orderRepository.countActiveSaleOrdersBetween(from, to);
        BigDecimal totalRevenue = orderRepository.sumSaleTotalAmountBetween(from, to);
        BigDecimal totalCollected = paymentRepository.sumAmountBetween(from, to);
        BigDecimal totalDebt = totalRevenue.subtract(totalCollected);

        return RevenueStatsResponse.builder()
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .totalCollected(totalCollected)
                .totalDebt(totalDebt)
                .build();
    }

    /**
     * Danh sách khách hàng có công nợ > 0, sắp xếp giảm dần.
     * Công nợ = Σ(SALE.total_amount) - Σ(Payment.amount)
     */
    @Transactional(readOnly = true)
    public List<DebtStatsResponse> getDebtStats() {
        List<Object[]> rows = orderRepository.findCustomerDebtSummariesDescending();

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

    /**
     * Doanh thu theo tháng trong năm (chỉ đơn SALE).
     * Trả về mảng 12 phần tử, index 0 = tháng 1.
     */
    @Transactional(readOnly = true)
    public MonthlyRevenueResponse getMonthlyRevenue(int year) {
        List<BigDecimal> monthly = new ArrayList<>(12);
        for (int month = 1; month <= 12; month++) {
            monthly.add(orderRepository.sumSaleTotalAmountByYearAndMonth(year, month));
        }
        return MonthlyRevenueResponse.builder()
                .year(year)
                .monthlyRevenue(monthly)
                .build();
    }
}
