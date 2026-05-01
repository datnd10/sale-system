package datnd.vn.salesystem.service;

import datnd.vn.salesystem.common.SearchRequest;
import datnd.vn.salesystem.constant.enums.OrderType;
import datnd.vn.salesystem.constant.enums.ProductUnit;
import datnd.vn.salesystem.entity.*;
import datnd.vn.salesystem.exception.EntityNotFoundException;
import datnd.vn.salesystem.exception.InvalidRequestException;
import datnd.vn.salesystem.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;

    public record OrderItemRequest(
            Long productId,
            BigDecimal count,
            BigDecimal length,
            BigDecimal width,
            BigDecimal height
    ) {}

    public record OrderWithItems(Order order, List<OrderItem> items) {}

    // -------------------------------------------------------------------------
    // createOrder — phân nhánh theo orderType
    // -------------------------------------------------------------------------

    @Transactional
    public OrderWithItems createOrder(Long customerId,
                                      OrderType orderType,
                                      LocalDate orderDate,
                                      List<OrderItemRequest> itemRequests,
                                      BigDecimal paidImmediately,
                                      BigDecimal amount,
                                      String note) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách hàng với mã: " + customerId));

        if (orderDate == null) orderDate = LocalDate.now();

        return switch (orderType) {
            case SALE -> createSaleOrder(customer, orderDate, itemRequests, paidImmediately, note);
            case PAYMENT -> createPaymentOrder(customer, orderDate, amount, note);
        };
    }

    // -------------------------------------------------------------------------
    // SALE order
    // -------------------------------------------------------------------------

    private OrderWithItems createSaleOrder(Customer customer,
                                            LocalDate orderDate,
                                            List<OrderItemRequest> itemRequests,
                                            BigDecimal paidImmediately,
                                            String note) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new InvalidRequestException("Đơn bán hàng phải có ít nhất 1 sản phẩm");
        }
        if (paidImmediately == null) paidImmediately = BigDecimal.ZERO;

        // Validate & load products
        List<Product> products = new ArrayList<>();
        for (OrderItemRequest req : itemRequests) {
            products.add(productRepository.findById(req.productId())
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm với mã: " + req.productId())));
        }

        // Tính total_amount và build OrderItems
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (int i = 0; i < itemRequests.size(); i++) {
            OrderItemRequest req = itemRequests.get(i);
            Product product = products.get(i);

            BigDecimal count = req.count();
            BigDecimal length = req.length() != null ? req.length() : BigDecimal.ZERO;
            BigDecimal width = req.width() != null ? req.width() : BigDecimal.ZERO;
            BigDecimal quantity = computeQuantity(product.getUnit(), count, length, width);
            BigDecimal unitPrice = product.getPrice();
            BigDecimal subtotal = quantity.multiply(unitPrice);
            totalAmount = totalAmount.add(subtotal);

            orderItems.add(OrderItem.builder()
                    .product(product)
                    .length(req.length())
                    .width(req.width())
                    .height(req.height())
                    .count(count)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build());
        }

        if (paidImmediately.compareTo(totalAmount) > 0) {
            throw new InvalidRequestException(
                    "Số tiền thanh toán ngay (" + paidImmediately + ") không được vượt quá tổng tiền đơn hàng (" + totalAmount + ")");
        }

        // Lưu Order
        Order order = Order.builder()
                .customer(customer)
                .orderType(OrderType.SALE)
                .orderDate(orderDate)
                .totalAmount(totalAmount)
                .paidImmediately(paidImmediately)
                .note(note)
                .active(true)
                .build();

        Order savedOrder = orderRepository.save(order);
        savedOrder.setCode(String.format("HD%07d", savedOrder.getId()));
        savedOrder = orderRepository.save(savedOrder);

        // Lưu OrderItems
        List<OrderItem> savedItems = new ArrayList<>();
        for (OrderItem item : orderItems) {
            item.setOrder(savedOrder);
            savedItems.add(orderItemRepository.save(item));
        }

        // Tạo Payment nếu có trả ngay
        if (paidImmediately.compareTo(BigDecimal.ZERO) > 0) {
            savePayment(customer, savedOrder, paidImmediately, orderDate,
                    "Thanh toán ngay khi tạo đơn " + savedOrder.getCode());
        }

        // Cập nhật has_debt
        updateCustomerDebtFlag(customer);

        return new OrderWithItems(savedOrder, savedItems);
    }

    // -------------------------------------------------------------------------
    // PAYMENT order
    // -------------------------------------------------------------------------

    private OrderWithItems createPaymentOrder(Customer customer,
                                               LocalDate orderDate,
                                               BigDecimal amount,
                                               String note) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Số tiền trả nợ phải lớn hơn 0");
        }

        // Lưu Order loại PAYMENT — total_amount = paid_immediately = amount
        Order order = Order.builder()
                .customer(customer)
                .orderType(OrderType.PAYMENT)
                .orderDate(orderDate)
                .totalAmount(amount)
                .paidImmediately(amount)
                .note(note)
                .active(true)
                .build();

        Order savedOrder = orderRepository.save(order);
        savedOrder.setCode(String.format("HD%07d", savedOrder.getId()));
        savedOrder = orderRepository.save(savedOrder);

        // Tạo Payment record
        savePayment(customer, savedOrder, amount, orderDate,
                note != null ? note : "Trả nợ theo đơn " + savedOrder.getCode());

        // Cập nhật has_debt
        updateCustomerDebtFlag(customer);

        return new OrderWithItems(savedOrder, Collections.emptyList());
    }

    // -------------------------------------------------------------------------
    // updateOrder
    // -------------------------------------------------------------------------

    @Transactional
    public OrderWithItems updateOrder(Long id,
                                      Long customerId,
                                      OrderType orderType,
                                      LocalDate orderDate,
                                      List<OrderItemRequest> itemRequests,
                                      BigDecimal paidImmediately,
                                      BigDecimal amount,
                                      String note) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng với mã: " + id));

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách hàng với mã: " + customerId));

        if (orderDate == null) orderDate = order.getOrderDate();

        if (orderType == OrderType.SALE) {
            return updateSaleOrder(order, customer, orderDate, itemRequests, paidImmediately, note);
        } else {
            return updatePaymentOrder(order, customer, orderDate, amount, note);
        }
    }

    private OrderWithItems updateSaleOrder(Order order, Customer customer, LocalDate orderDate,
                                            List<OrderItemRequest> itemRequests,
                                            BigDecimal paidImmediately, String note) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new InvalidRequestException("Đơn bán hàng phải có ít nhất 1 sản phẩm");
        }
        if (paidImmediately == null) paidImmediately = BigDecimal.ZERO;

        List<Product> products = new ArrayList<>();
        for (OrderItemRequest req : itemRequests) {
            products.add(productRepository.findById(req.productId())
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm với mã: " + req.productId())));
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> newItems = new ArrayList<>();

        for (int i = 0; i < itemRequests.size(); i++) {
            OrderItemRequest req = itemRequests.get(i);
            Product product = products.get(i);
            BigDecimal count = req.count();
            BigDecimal length = req.length() != null ? req.length() : BigDecimal.ZERO;
            BigDecimal width = req.width() != null ? req.width() : BigDecimal.ZERO;
            BigDecimal quantity = computeQuantity(product.getUnit(), count, length, width);
            BigDecimal unitPrice = product.getPrice();
            BigDecimal subtotal = quantity.multiply(unitPrice);
            totalAmount = totalAmount.add(subtotal);

            newItems.add(OrderItem.builder()
                    .product(product).length(req.length()).width(req.width())
                    .height(req.height()).count(count).quantity(quantity)
                    .unitPrice(unitPrice).subtotal(subtotal).build());
        }

        if (paidImmediately.compareTo(totalAmount) > 0) {
            throw new InvalidRequestException(
                    "Số tiền thanh toán ngay không được vượt quá tổng tiền đơn hàng (" + totalAmount + ")");
        }

        order.setCustomer(customer);
        order.setOrderType(OrderType.SALE);
        order.setOrderDate(orderDate);
        order.setTotalAmount(totalAmount);
        order.setPaidImmediately(paidImmediately);
        order.setNote(note);
        Order savedOrder = orderRepository.save(order);

        // Thay thế OrderItems
        orderItemRepository.deleteAllByOrderId(savedOrder.getId());
        List<OrderItem> savedItems = new ArrayList<>();
        for (OrderItem item : newItems) {
            item.setOrder(savedOrder);
            savedItems.add(orderItemRepository.save(item));
        }

        // Cập nhật Payment liên kết với đơn này
        List<Payment> existingPayments = paymentRepository.findByOrderId(savedOrder.getId());
        for (Payment p : existingPayments) {
            paymentRepository.delete(p);
        }
        if (paidImmediately.compareTo(BigDecimal.ZERO) > 0) {
            savePayment(customer, savedOrder, paidImmediately, orderDate,
                    "Thanh toán ngay khi tạo đơn " + savedOrder.getCode());
        }

        updateCustomerDebtFlag(customer);
        return new OrderWithItems(savedOrder, savedItems);
    }

    private OrderWithItems updatePaymentOrder(Order order, Customer customer, LocalDate orderDate,
                                               BigDecimal amount, String note) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Số tiền trả nợ phải lớn hơn 0");
        }

        order.setCustomer(customer);
        order.setOrderType(OrderType.PAYMENT);
        order.setOrderDate(orderDate);
        order.setTotalAmount(amount);
        order.setPaidImmediately(amount);
        order.setNote(note);
        Order savedOrder = orderRepository.save(order);

        // Cập nhật Payment liên kết
        List<Payment> existingPayments = paymentRepository.findByOrderId(savedOrder.getId());
        for (Payment p : existingPayments) {
            paymentRepository.delete(p);
        }
        savePayment(customer, savedOrder, amount, orderDate,
                note != null ? note : "Trả nợ theo đơn " + savedOrder.getCode());

        updateCustomerDebtFlag(customer);
        return new OrderWithItems(savedOrder, Collections.emptyList());
    }

    // -------------------------------------------------------------------------
    // searchOrders
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<Order> searchOrders(SearchRequest request) {
        Specification<Order> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (query != null && Long.class != query.getResultType()) {
                root.fetch("customer", jakarta.persistence.criteria.JoinType.LEFT);
            }

            Object customerId = request.getFilters().get("customerId");
            if (customerId != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), customerId));
            }

            Object orderType = request.getFilters().get("orderType");
            if (orderType != null) {
                predicates.add(cb.equal(root.get("orderType"), orderType));
            }

            Object from = request.getFilters().get("from");
            Object to = request.getFilters().get("to");
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("orderDate"), (java.time.LocalDate) from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("orderDate"), (java.time.LocalDate) to));
            }

            predicates.add(cb.isTrue(root.get("active")));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return orderRepository.findAll(spec, request.toPageable());
    }

    // -------------------------------------------------------------------------
    // getOrders
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Order> getOrders(Long customerId, LocalDate from, LocalDate to) {
        if (customerId != null && from != null && to != null) {
            return orderRepository.findAllByCustomerIdAndOrderDateBetween(customerId, from, to);
        } else if (customerId != null) {
            return orderRepository.findAllByCustomerId(customerId);
        } else if (from != null && to != null) {
            return orderRepository.findAllByOrderDateBetween(from, to);
        } else {
            return orderRepository.findAll();
        }
    }

    // -------------------------------------------------------------------------
    // getOrderById
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public OrderWithItems getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng với mã: " + id));
        List<OrderItem> items = order.getOrderType() == OrderType.SALE
                ? orderItemRepository.findAllByOrderId(id)
                : Collections.emptyList();
        return new OrderWithItems(order, items);
    }

    // -------------------------------------------------------------------------
    // updateOrderNote
    // -------------------------------------------------------------------------

    @Transactional
    public Order updateOrderNote(Long id, String note) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng với mã: " + id));
        order.setNote(note);
        return orderRepository.save(order);
    }

    // -------------------------------------------------------------------------
    // deleteOrder
    // -------------------------------------------------------------------------

    @Transactional
    public void deleteOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng với mã: " + id));
        order.setActive(false);
        orderRepository.save(order);

        // Cập nhật lại has_debt sau khi xóa đơn
        updateCustomerDebtFlag(order.getCustomer());
    }

    // -------------------------------------------------------------------------
    // Tính công nợ
    // -------------------------------------------------------------------------

    /**
     * Tổng công nợ của khách hàng:
     * = Σ(SALE.total_amount) - Σ(Payment.amount)
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateCustomerDebt(Long customerId) {
        BigDecimal totalSale = orderRepository.sumSaleTotalAmountByCustomerId(customerId);
        BigDecimal totalPaid = paymentRepository.sumAmountByCustomerId(customerId);
        return totalSale.subtract(totalPaid);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void savePayment(Customer customer, Order order, BigDecimal amount,
                              LocalDate paymentDate, String note) {
        Payment payment = Payment.builder()
                .customer(customer)
                .order(order)
                .amount(amount)
                .paymentDate(paymentDate)
                .note(note)
                .build();
        Payment saved = paymentRepository.save(payment);
        saved.setCode(String.format("TT%07d", saved.getId()));
        paymentRepository.save(saved);
    }

    private void updateCustomerDebtFlag(Customer customer) {
        BigDecimal totalDebt = calculateCustomerDebt(customer.getId());
        customer.setHasDebt(totalDebt.compareTo(BigDecimal.ZERO) > 0);
        customerRepository.save(customer);
    }

    private BigDecimal computeQuantity(ProductUnit unit, BigDecimal count, BigDecimal length, BigDecimal width) {
        return switch (unit) {
            case M2 -> count.multiply(length).multiply(width);
            case MET -> count.multiply(length);
            default -> count;
        };
    }
}
