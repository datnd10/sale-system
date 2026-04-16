package datnd.vn.salesystem.service;

import datnd.vn.salesystem.constant.enums.ProductUnit;
import datnd.vn.salesystem.entity.*;
import datnd.vn.salesystem.exception.EntityNotFoundException;
import datnd.vn.salesystem.exception.InvalidRequestException;
import datnd.vn.salesystem.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DebtRepository debtRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;

    /**
     * Request DTO for a single order item line.
     */
    public record OrderItemRequest(
            Long productId,
            BigDecimal count,
            BigDecimal length,
            BigDecimal width,
            BigDecimal height
    ) {}

    /**
     * Result DTO returned from createOrder / getOrderById.
     */
    public record OrderWithItems(Order order, List<OrderItem> items) {}

    // -------------------------------------------------------------------------
    // createOrder
    // -------------------------------------------------------------------------

    /**
     * Creates a new Order with its OrderItems and a corresponding Debt record.
     *
     * Flow:
     * 1. Validate customer exists → 404
     * 2. Validate each product in items exists → 404
     * 3. Snapshot unit_price from product at creation time
     * 4. Compute quantity per item based on product unit
     * 5. Compute subtotal = quantity × unit_price per item
     * 6. Compute total_amount = Σ subtotal
     * 7. Validate paid_immediately <= total_amount → 400 if violated
     * 8. Create Order entity (save → set code → save again)
     * 9. Create OrderItem entities
     * 10. Create Debt record with original_amount = total_amount - paid_immediately
     *     (save → set code → save again)
     */
    @Transactional
    public OrderWithItems createOrder(Long customerId,
                                      LocalDate orderDate,
                                      List<OrderItemRequest> itemRequests,
                                      BigDecimal paidImmediately) {

        // 1. Validate customer
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách hàng với mã: " + customerId));

        // Default values
        if (orderDate == null) {
            orderDate = LocalDate.now();
        }
        if (paidImmediately == null) {
            paidImmediately = BigDecimal.ZERO;
        }

        // 2 & 3. Validate products and snapshot prices
        List<Product> products = new ArrayList<>();
        for (OrderItemRequest req : itemRequests) {
            Product product = productRepository.findById(req.productId())
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm với mã: " + req.productId()));
            products.add(product);
        }

        // 4 & 5. Compute quantity and subtotal per item
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (int i = 0; i < itemRequests.size(); i++) {
            OrderItemRequest req = itemRequests.get(i);
            Product product = products.get(i);

            BigDecimal count = req.count();
            BigDecimal length = req.length() != null ? req.length() : BigDecimal.ZERO;
            BigDecimal width = req.width() != null ? req.width() : BigDecimal.ZERO;

            // Compute quantity based on product unit
            BigDecimal quantity = computeQuantity(product.getUnit(), count, length, width);

            // Snapshot unit_price from product
            BigDecimal unitPrice = product.getPrice();

            // subtotal = quantity × unit_price
            BigDecimal subtotal = quantity.multiply(unitPrice);
            totalAmount = totalAmount.add(subtotal);

            OrderItem item = OrderItem.builder()
                    .product(product)
                    .length(req.length())
                    .width(req.width())
                    .height(req.height())
                    .count(count)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .subtotal(subtotal)
                    .build();
            orderItems.add(item);
        }

        // 6. Validate paid_immediately <= total_amount
        if (paidImmediately.compareTo(totalAmount) > 0) {
            throw new InvalidRequestException(
                    "Số tiền thanh toán ngay (" + paidImmediately + ") không được vượt quá tổng tiền đơn hàng (" + totalAmount + ")");
        }

        // 7. Create and save Order
        Order order = Order.builder()
                .customer(customer)
                .orderDate(orderDate)
                .totalAmount(totalAmount)
                .paidImmediately(paidImmediately)
                .active(true)
                .build();

        Order savedOrder = orderRepository.save(order);
        savedOrder.setCode(String.format("HD%07d", savedOrder.getId()));
        savedOrder = orderRepository.save(savedOrder);

        // 8. Save OrderItems (link to saved order)
        List<OrderItem> savedItems = new ArrayList<>();
        for (OrderItem item : orderItems) {
            item.setOrder(savedOrder);
            savedItems.add(orderItemRepository.save(item));
        }

        // 9. Create Debt record
        BigDecimal originalAmount = totalAmount.subtract(paidImmediately);
        Debt debt = Debt.builder()
                .customer(customer)
                .order(savedOrder)
                .originalAmount(originalAmount)
                .remainingAmount(originalAmount)
                .build();

        Debt savedDebt = debtRepository.save(debt);
        savedDebt.setCode(String.format("CN%07d", savedDebt.getId()));
        debtRepository.save(savedDebt);

        // Update customer has_debt flag if there is remaining debt
        if (originalAmount.compareTo(BigDecimal.ZERO) > 0) {
            customer.setHasDebt(true);
            customerRepository.save(customer);
        }

        // Tạo bản ghi Payment nếu khách hàng thanh toán ngay
        if (paidImmediately.compareTo(BigDecimal.ZERO) > 0) {
            Payment payment = Payment.builder()
                    .customer(customer)
                    .amount(paidImmediately)
                    .paymentDate(orderDate)
                    .note("Thanh toán ngay khi tạo đơn " + savedOrder.getCode())
                    .build();
            Payment savedPayment = paymentRepository.save(payment);
            savedPayment.setCode(String.format("TT%07d", savedPayment.getId()));
            paymentRepository.save(savedPayment);
        }

        return new OrderWithItems(savedOrder, savedItems);
    }

    // -------------------------------------------------------------------------
    // updateOrder
    // -------------------------------------------------------------------------

    /**
     * Updates an existing Order: recalculates totals, replaces OrderItems, updates Debt.
     */
    @Transactional
    public OrderWithItems updateOrder(Long id,
                                      Long customerId,
                                      LocalDate orderDate,
                                      List<OrderItemRequest> itemRequests,
                                      BigDecimal paidImmediately,
                                      String note) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng với mã: " + id));

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách hàng với mã: " + customerId));

        if (orderDate == null) orderDate = order.getOrderDate();
        if (paidImmediately == null) paidImmediately = BigDecimal.ZERO;

        // Validate & build new items
        List<Product> products = new ArrayList<>();
        for (OrderItemRequest req : itemRequests) {
            Product product = productRepository.findById(req.productId())
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm với mã: " + req.productId()));
            products.add(product);
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

        // Update order fields
        order.setCustomer(customer);
        order.setOrderDate(orderDate);
        order.setTotalAmount(totalAmount);
        order.setPaidImmediately(paidImmediately);
        order.setNote(note);
        Order savedOrder = orderRepository.save(order);

        // Replace order items
        orderItemRepository.deleteAllByOrderId(savedOrder.getId());
        List<OrderItem> savedItems = new ArrayList<>();
        for (OrderItem item : newItems) {
            item.setOrder(savedOrder);
            savedItems.add(orderItemRepository.save(item));
        }

        // Update debt record
        BigDecimal newOriginalAmount = totalAmount.subtract(paidImmediately);
        List<Debt> debts = debtRepository.findAllByOrderId(savedOrder.getId());
        if (!debts.isEmpty()) {
            Debt debt = debts.get(0);
            debt.setOriginalAmount(newOriginalAmount);
            debt.setRemainingAmount(newOriginalAmount);
            debtRepository.save(debt);
        } else if (newOriginalAmount.compareTo(BigDecimal.ZERO) > 0) {
            Debt debt = Debt.builder()
                    .customer(customer).order(savedOrder)
                    .originalAmount(newOriginalAmount).remainingAmount(newOriginalAmount).build();
            Debt savedDebt = debtRepository.save(debt);
            savedDebt.setCode(String.format("CN%07d", savedDebt.getId()));
            debtRepository.save(savedDebt);
        }

        // Update customer has_debt flag
        boolean hasDebt = newOriginalAmount.compareTo(BigDecimal.ZERO) > 0;
        customer.setHasDebt(hasDebt);
        customerRepository.save(customer);

        return new OrderWithItems(savedOrder, savedItems);
    }

    // -------------------------------------------------------------------------
    // getOrders
    // -------------------------------------------------------------------------

    /**
     * Returns orders filtered by optional customerId and/or date range.
     */
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

    /**
     * Returns an order with its items. Throws 404 if not found.
     */
    @Transactional(readOnly = true)
    public OrderWithItems getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng với mã: " + id));
        List<OrderItem> items = orderItemRepository.findAllByOrderId(id);
        return new OrderWithItems(order, items);
    }

    // -------------------------------------------------------------------------
    // updateOrderNote
    // -------------------------------------------------------------------------

    /**
     * Updates the note field of an order. Throws 404 if not found.
     */
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

    /**
     * Soft-deletes an order by setting active = false. Throws 404 if not found.
     */
    @Transactional
    public void deleteOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng với mã: " + id));
        order.setActive(false);
        orderRepository.save(order);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /**
     * Computes the effective quantity based on the product's unit:
     * - M2  : count × length × width
     * - MET : count × length
     * - Others: count
     */
    private BigDecimal computeQuantity(ProductUnit unit, BigDecimal count, BigDecimal length, BigDecimal width) {
        return switch (unit) {
            case M2 -> count.multiply(length).multiply(width);
            case MET -> count.multiply(length);
            default -> count;
        };
    }
}
