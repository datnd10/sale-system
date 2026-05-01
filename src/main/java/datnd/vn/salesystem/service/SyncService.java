package datnd.vn.salesystem.service;

import datnd.vn.salesystem.entity.*;
import datnd.vn.salesystem.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;

    @Qualifier("localJdbcTemplate")
    private final JdbcTemplate localJdbc;

    // -------------------------------------------------------------------------
    // Scheduled — chạy lúc 23:00 mỗi ngày
    // -------------------------------------------------------------------------

    @Scheduled(cron = "${app.sync.cron:0 0 23 * * *}")
    public void scheduledSync() {
        log.info("[SYNC] Bắt đầu đồng bộ dữ liệu cuối ngày...");
        syncAll();
        log.info("[SYNC] Hoàn thành đồng bộ dữ liệu.");
    }

    // -------------------------------------------------------------------------
    // syncAll — có thể gọi thủ công qua API
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public SyncResult syncAll() {
        long start = System.currentTimeMillis();

        try {
            ensureSchema();

            int categories = syncCategories();
            int products    = syncProducts();
            int customers   = syncCustomers();
            int orders      = syncOrders();
            int orderItems  = syncOrderItems();
            int payments    = syncPayments();

            long elapsed = System.currentTimeMillis() - start;
            log.info("[SYNC] categories={}, products={}, customers={}, orders={}, orderItems={}, payments={} — {}ms",
                    categories, products, customers, orders, orderItems, payments, elapsed);

            return new SyncResult(true, categories, products, customers, orders, orderItems, payments, elapsed, null);

        } catch (Exception e) {
            log.error("[SYNC] Lỗi khi đồng bộ: {}", e.getMessage(), e);
            return new SyncResult(false, 0, 0, 0, 0, 0, 0, System.currentTimeMillis() - start, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Schema creation
    // -------------------------------------------------------------------------

    private void ensureSchema() {
        log.info("[SYNC] Kiểm tra và tạo schema nếu chưa có...");

        localJdbc.execute("""
            CREATE TABLE IF NOT EXISTS categories (
                id          BIGINT PRIMARY KEY,
                code        VARCHAR(20),
                name        VARCHAR(255) NOT NULL,
                description TEXT,
                active      BOOLEAN NOT NULL DEFAULT TRUE,
                created_at  TIMESTAMP,
                updated_at  TIMESTAMP,
                deleted_at  TIMESTAMP
            )
        """);

        localJdbc.execute("""
            CREATE TABLE IF NOT EXISTS products (
                id          BIGINT PRIMARY KEY,
                code        VARCHAR(20),
                name        VARCHAR(255) NOT NULL,
                category_id BIGINT,
                unit        VARCHAR(50) NOT NULL,
                price       NUMERIC(15,2) NOT NULL,
                width       NUMERIC(10,3),
                description TEXT,
                active      BOOLEAN NOT NULL DEFAULT TRUE,
                created_at  TIMESTAMP,
                updated_at  TIMESTAMP,
                deleted_at  TIMESTAMP
            )
        """);

        localJdbc.execute("""
            CREATE TABLE IF NOT EXISTS customers (
                id         BIGINT PRIMARY KEY,
                code       VARCHAR(20),
                name       VARCHAR(255) NOT NULL,
                phone      VARCHAR(20),
                address    TEXT,
                has_debt   BOOLEAN NOT NULL DEFAULT FALSE,
                active     BOOLEAN NOT NULL DEFAULT TRUE,
                created_at TIMESTAMP,
                updated_at TIMESTAMP,
                deleted_at TIMESTAMP
            )
        """);

        localJdbc.execute("""
            CREATE TABLE IF NOT EXISTS orders (
                id               BIGINT PRIMARY KEY,
                code             VARCHAR(20),
                customer_id      BIGINT,
                order_type       VARCHAR(20) NOT NULL,
                order_date       DATE NOT NULL,
                total_amount     NUMERIC(15,2) NOT NULL,
                paid_immediately NUMERIC(15,2) NOT NULL DEFAULT 0,
                note             TEXT,
                active           BOOLEAN NOT NULL DEFAULT TRUE,
                created_at       TIMESTAMP,
                updated_at       TIMESTAMP,
                deleted_at       TIMESTAMP
            )
        """);

        localJdbc.execute("""
            CREATE TABLE IF NOT EXISTS order_items (
                id         BIGINT PRIMARY KEY,
                order_id   BIGINT,
                product_id BIGINT,
                length     NUMERIC(10,3),
                width      NUMERIC(10,3),
                height     NUMERIC(10,3),
                count      NUMERIC(15,3) NOT NULL,
                quantity   NUMERIC(15,3) NOT NULL,
                unit_price NUMERIC(15,2) NOT NULL,
                subtotal   NUMERIC(15,2) NOT NULL
            )
        """);

        localJdbc.execute("""
            CREATE TABLE IF NOT EXISTS payments (
                id           BIGINT PRIMARY KEY,
                code         VARCHAR(20),
                customer_id  BIGINT,
                order_id     BIGINT,
                amount       NUMERIC(15,2) NOT NULL,
                payment_date DATE NOT NULL,
                note         TEXT,
                created_at   TIMESTAMP
            )
        """);

        log.info("[SYNC] Schema sẵn sàng.");
    }

    // -------------------------------------------------------------------------
    // Sync từng bảng — dùng UPSERT (INSERT ... ON CONFLICT DO UPDATE)
    // -------------------------------------------------------------------------

    private int syncCategories() {
        List<Category> list = categoryRepository.findAll();
        String sql = """
            INSERT INTO categories (id, code, name, description, active, created_at, updated_at, deleted_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                code = EXCLUDED.code,
                name = EXCLUDED.name,
                description = EXCLUDED.description,
                active = EXCLUDED.active,
                updated_at = EXCLUDED.updated_at,
                deleted_at = EXCLUDED.deleted_at
        """;
        for (Category c : list) {
            localJdbc.update(sql, c.getId(), c.getCode(), c.getName(), c.getDescription(),
                    c.getActive(), c.getCreatedAt(), c.getUpdatedAt(), c.getDeletedAt());
        }
        return list.size();
    }

    private int syncProducts() {
        List<Product> list = productRepository.findAll();
        String sql = """
            INSERT INTO products (id, code, name, category_id, unit, price, width, description, active, created_at, updated_at, deleted_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                code = EXCLUDED.code,
                name = EXCLUDED.name,
                category_id = EXCLUDED.category_id,
                unit = EXCLUDED.unit,
                price = EXCLUDED.price,
                width = EXCLUDED.width,
                description = EXCLUDED.description,
                active = EXCLUDED.active,
                updated_at = EXCLUDED.updated_at,
                deleted_at = EXCLUDED.deleted_at
        """;
        for (Product p : list) {
            localJdbc.update(sql, p.getId(), p.getCode(), p.getName(),
                    p.getCategory().getId(), p.getUnit().name(), p.getPrice(),
                    p.getWidth(), p.getDescription(), p.getActive(),
                    p.getCreatedAt(), p.getUpdatedAt(), p.getDeletedAt());
        }
        return list.size();
    }

    private int syncCustomers() {
        List<Customer> list = customerRepository.findAll();
        String sql = """
            INSERT INTO customers (id, code, name, phone, address, has_debt, active, created_at, updated_at, deleted_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                code = EXCLUDED.code,
                name = EXCLUDED.name,
                phone = EXCLUDED.phone,
                address = EXCLUDED.address,
                has_debt = EXCLUDED.has_debt,
                active = EXCLUDED.active,
                updated_at = EXCLUDED.updated_at,
                deleted_at = EXCLUDED.deleted_at
        """;
        for (Customer c : list) {
            localJdbc.update(sql, c.getId(), c.getCode(), c.getName(), c.getPhone(),
                    c.getAddress(), c.getHasDebt(), c.getActive(),
                    c.getCreatedAt(), c.getUpdatedAt(), c.getDeletedAt());
        }
        return list.size();
    }

    private int syncOrders() {
        List<Order> list = orderRepository.findAll();
        String sql = """
            INSERT INTO orders (id, code, customer_id, order_type, order_date, total_amount, paid_immediately, note, active, created_at, updated_at, deleted_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                code = EXCLUDED.code,
                customer_id = EXCLUDED.customer_id,
                order_type = EXCLUDED.order_type,
                order_date = EXCLUDED.order_date,
                total_amount = EXCLUDED.total_amount,
                paid_immediately = EXCLUDED.paid_immediately,
                note = EXCLUDED.note,
                active = EXCLUDED.active,
                updated_at = EXCLUDED.updated_at,
                deleted_at = EXCLUDED.deleted_at
        """;
        for (Order o : list) {
            localJdbc.update(sql, o.getId(), o.getCode(), o.getCustomer().getId(),
                    o.getOrderType().name(), o.getOrderDate(), o.getTotalAmount(),
                    o.getPaidImmediately(), o.getNote(), o.getActive(),
                    o.getCreatedAt(), o.getUpdatedAt(), o.getDeletedAt());
        }
        return list.size();
    }

    private int syncOrderItems() {
        List<OrderItem> list = orderItemRepository.findAll();
        String sql = """
            INSERT INTO order_items (id, order_id, product_id, length, width, height, count, quantity, unit_price, subtotal)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                order_id = EXCLUDED.order_id,
                product_id = EXCLUDED.product_id,
                length = EXCLUDED.length,
                width = EXCLUDED.width,
                height = EXCLUDED.height,
                count = EXCLUDED.count,
                quantity = EXCLUDED.quantity,
                unit_price = EXCLUDED.unit_price,
                subtotal = EXCLUDED.subtotal
        """;
        for (OrderItem item : list) {
            localJdbc.update(sql, item.getId(), item.getOrder().getId(), item.getProduct().getId(),
                    item.getLength(), item.getWidth(), item.getHeight(),
                    item.getCount(), item.getQuantity(), item.getUnitPrice(), item.getSubtotal());
        }
        return list.size();
    }

    private int syncPayments() {
        List<Payment> list = paymentRepository.findAll();
        String sql = """
            INSERT INTO payments (id, code, customer_id, order_id, amount, payment_date, note, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                code = EXCLUDED.code,
                customer_id = EXCLUDED.customer_id,
                order_id = EXCLUDED.order_id,
                amount = EXCLUDED.amount,
                payment_date = EXCLUDED.payment_date,
                note = EXCLUDED.note
        """;
        for (Payment p : list) {
            localJdbc.update(sql, p.getId(), p.getCode(), p.getCustomer().getId(),
                    p.getOrder() != null ? p.getOrder().getId() : null,
                    p.getAmount(), p.getPaymentDate(), p.getNote(), p.getCreatedAt());
        }
        return list.size();
    }

    // -------------------------------------------------------------------------
    // Result DTO
    // -------------------------------------------------------------------------

    public record SyncResult(
            boolean success,
            int categories,
            int products,
            int customers,
            int orders,
            int orderItems,
            int payments,
            long elapsedMs,
            String errorMessage
    ) {}
}
