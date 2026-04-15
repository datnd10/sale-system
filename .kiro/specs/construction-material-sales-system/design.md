# Design Document: Construction Material Sales System

## Overview

Hệ thống backend REST API quản lý bán vật liệu xây dựng (tôn, sắt, thép) dành cho nội bộ gia đình. Được xây dựng trên Spring Boot 4.0.5 + Java 21 + PostgreSQL, hệ thống cung cấp các endpoint để quản lý danh mục, sản phẩm, khách hàng, đơn hàng, công nợ, thanh toán và thống kê cơ bản.

Phạm vi hệ thống:
- Không có authentication/authorization phức tạp
- Không quản lý tồn kho
- Tất cả response theo định dạng JSON thống nhất
- Soft delete cho Category, Product và Order
- Mỗi bản ghi có mã hiển thị tự sinh (`code`) dạng `PREFIX + 7 chữ số` để nhận diện ra ngoài

## Architecture

Kiến trúc 3 lớp chuẩn Spring Boot:

```
┌─────────────────────────────────────────────────────┐
│                   REST Controllers                   │
│  (CategoryController, ProductController, ...)        │
├─────────────────────────────────────────────────────┤
│                   Service Layer                      │
│  (CategoryService, ProductService, OrderService,...) │
├─────────────────────────────────────────────────────┤
│                 Repository Layer                     │
│  (Spring Data JPA Repositories)                      │
├─────────────────────────────────────────────────────┤
│                   PostgreSQL DB                      │
└─────────────────────────────────────────────────────┘
```

Cross-cutting concerns:
- `GlobalExceptionHandler` (@ControllerAdvice) — xử lý lỗi tập trung
- `ApiResponse<T>` — wrapper response thống nhất
- Bean Validation (Jakarta Validation) — validate request DTO

## Components and Interfaces

### REST Controllers

| Controller | Base Path | Trách nhiệm |
|---|---|---|
| `CategoryController` | `/api/categories` | CRUD danh mục |
| `ProductController` | `/api/products` | CRUD sản phẩm |
| `CustomerController` | `/api/customers` | CRUD khách hàng |
| `OrderController` | `/api/orders` | Tạo & xem đơn hàng |
| `DebtController` | `/api/debts` | Xem công nợ |
| `PaymentController` | `/api/payments` | Ghi nhận thanh toán |
| `StatisticsController` | `/api/statistics` | Thống kê |

### API Endpoints

**Category**
- `POST /api/categories` — tạo mới
- `GET /api/categories` — danh sách (chỉ active)
- `PUT /api/categories/{id}` — cập nhật
- `DELETE /api/categories/{id}` — soft delete

**Product**
- `POST /api/products` — tạo mới
- `GET /api/products?categoryId=` — danh sách, lọc theo category
- `GET /api/products/{id}` — chi tiết
- `PUT /api/products/{id}` — cập nhật
- `DELETE /api/products/{id}` — soft delete

**Customer**
- `POST /api/customers` — tạo mới
- `GET /api/customers` — danh sách
- `GET /api/customers/{id}` — chi tiết + tổng công nợ
- `PUT /api/customers/{id}` — cập nhật

**Order**
- `POST /api/orders` — tạo đơn hàng
- `GET /api/orders?customerId=&from=&to=` — danh sách, lọc
- `GET /api/orders/{id}` — chi tiết + OrderItems
- `PATCH /api/orders/{id}/note` — cập nhật ghi chú

**Debt**
- `GET /api/debts` — tổng nợ tất cả khách hàng
- `GET /api/debts/customer/{customerId}` — chi tiết nợ theo khách hàng

**Payment**
- `POST /api/payments` — ghi nhận thanh toán
- `GET /api/payments/customer/{customerId}` — lịch sử thanh toán
- `GET /api/payments?from=&to=` — tất cả payment, lọc theo thời gian

**Statistics**
- `GET /api/statistics/revenue?from=&to=` — doanh thu theo khoảng thời gian
- `GET /api/statistics/debts` — danh sách khách hàng còn nợ
- `GET /api/statistics/revenue/monthly?year=` — doanh thu theo tháng

### Service Layer

- `CategoryService` — validate tên trùng, kiểm tra product liên kết trước khi xóa
- `ProductService` — validate category tồn tại, giá > 0
- `CustomerService` — validate phone trùng, tính tổng công nợ
- `OrderService` — tính total_amount, tạo Debt record, validate payment_amount
- `DebtService` — tính remaining_debt, cập nhật has_debt flag
- `PaymentService` — trừ nợ, cho phép số dư âm
- `StatisticsService` — aggregate queries

### Response Wrapper

```java
public record ApiResponse<T>(
    boolean success,
    T data,
    String message,
    String timestamp  // ISO 8601
) {}
```

### GlobalExceptionHandler

Xử lý tập trung các exception:
- `EntityNotFoundException` → 404
- `DuplicateResourceException` → 409
- `BusinessRuleException` → 409
- `MethodArgumentNotValidException` → 400 với danh sách field errors
- `Exception` (catch-all) → 500, log lỗi, không lộ stack trace

## Data Models

### Entity Diagram

```
Category (1) ──── (N) Product (1) ──── (N) OrderItem (N) ──── (1) Order (N) ──── (1) Customer
                                                                                        │
                                                                                   (1) ──── (N) Payment
                                                                                   (1) ──── (N) Debt
```

### Category

```sql
CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(20) NOT NULL UNIQUE,  -- DM0000001, DM0000002, ...
    name        VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### Product

```sql
CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(20) NOT NULL UNIQUE,  -- SP0000001, SP0000002, ...
    name        VARCHAR(255) NOT NULL,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    unit        VARCHAR(50) NOT NULL,       -- kg, tấm, cây, ...
    price       NUMERIC(15,2) NOT NULL CHECK (price > 0),
    description TEXT,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### Customer

```sql
CREATE TABLE customers (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(20) NOT NULL UNIQUE,  -- KH0000001, KH0000002, ...
    name         VARCHAR(255) NOT NULL,
    phone        VARCHAR(20) UNIQUE,
    address      TEXT,
    has_debt     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### Order

```sql
CREATE TABLE orders (
    id               BIGSERIAL PRIMARY KEY,
    code             VARCHAR(20) NOT NULL UNIQUE,  -- HD0000001, HD0000002, ...
    customer_id      BIGINT NOT NULL REFERENCES customers(id),
    order_date       DATE NOT NULL DEFAULT CURRENT_DATE,
    total_amount     NUMERIC(15,2) NOT NULL,
    paid_immediately NUMERIC(15,2) NOT NULL DEFAULT 0,
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    note             TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### OrderItem

```sql
CREATE TABLE order_items (
    id         BIGSERIAL PRIMARY KEY,
    order_id   BIGINT NOT NULL REFERENCES orders(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    -- Kích thước gốc (tùy unit của product)
    length     NUMERIC(10,3),            -- mét dài (MET, M2)
    width      NUMERIC(10,3),            -- mét rộng (M2)
    height     NUMERIC(10,3),            -- chiều cao nếu cần
    count      NUMERIC(15,3) NOT NULL CHECK (count > 0),  -- số lượng tờ/cây/kg/...
    -- Số lượng quy đổi theo unit: M2 = count×length×width, MET = count×length, còn lại = count
    quantity   NUMERIC(15,3) NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(15,2) NOT NULL,   -- snapshot tại thời điểm tạo đơn
    subtotal   NUMERIC(15,2) NOT NULL    -- quantity * unit_price
);
```

### Debt

```sql
CREATE TABLE debts (
    id               BIGSERIAL PRIMARY KEY,
    code             VARCHAR(20) NOT NULL UNIQUE,  -- CN0000001, CN0000002, ...
    customer_id      BIGINT NOT NULL REFERENCES customers(id),
    order_id         BIGINT NOT NULL REFERENCES orders(id),
    original_amount  NUMERIC(15,2) NOT NULL,   -- total_amount - paid_immediately
    remaining_amount NUMERIC(15,2) NOT NULL,   -- còn lại sau các payment
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### Payment

```sql
CREATE TABLE payments (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(20) NOT NULL UNIQUE,  -- TT0000001, TT0000002, ...
    customer_id  BIGINT NOT NULL REFERENCES customers(id),
    amount       NUMERIC(15,2) NOT NULL CHECK (amount > 0),
    payment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    note         TEXT,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### Indexes

```sql
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_order_date ON orders(order_date);
CREATE INDEX idx_debts_customer_id ON debts(customer_id);
CREATE INDEX idx_payments_customer_id ON payments(customer_id);
CREATE INDEX idx_payments_payment_date ON payments(payment_date);
```

### Display Code Generation

Mỗi bảng có cột `code` VARCHAR(20) UNIQUE, tự sinh khi insert. Format: `PREFIX + số thứ tự 7 chữ số`.

| Bảng | Prefix | Ví dụ |
|---|---|---|
| categories | `DM` | `DM0000001` |
| products | `SP` | `SP0000001` |
| customers | `KH` | `KH0000001` |
| orders | `HD` | `HD0000001` |
| debts | `CN` | `CN0000001` |
| payments | `TT` | `TT0000001` |

**Cơ chế sinh code trong Service layer:**

```java
// Ví dụ cho Customer
String nextCode = String.format("KH%07d", customerRepository.count() + 1);
```

Hoặc dùng `SELECT MAX(id)` để tránh race condition trong môi trường concurrent:

```java
String nextCode = String.format("KH%07d", entity.getId()); // sinh sau khi save() trả về id
```

Cách khuyến nghị: **sinh code sau khi `save()` trả về entity với `id`**, đảm bảo không bao giờ trùng:

```java
Category saved = categoryRepository.save(entity);
saved.setCode(String.format("DM%07d", saved.getId()));
categoryRepository.save(saved);
```

`code` chỉ dùng để hiển thị ra ngoài — tất cả URL path và foreign key vẫn dùng `id` số nguyên.

### Logic thanh toán công nợ

Khi tạo Payment với số tiền `P` cho Customer:
1. Lấy danh sách Debt của Customer có `remaining_amount > 0`, sắp xếp theo `created_at` (FIFO)
2. Trừ dần `P` vào từng Debt cho đến khi hết tiền hoặc hết nợ
3. Nếu `P` còn dư sau khi trừ hết nợ → ghi nhận Payment bình thường (remaining_amount có thể âm ở mức tổng)
4. Cập nhật `has_debt` của Customer dựa trên tổng `remaining_amount` còn lại


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Duplicate category name returns 409

*For any* valid category name, if a category with that name already exists (active or not), attempting to create another category with the same name SHALL return HTTP 409.

**Validates: Requirements 1.2**

---

### Property 2: Category list only contains active records

*For any* state of the category table (mix of active and soft-deleted categories), the GET list API SHALL only return categories where `active = true`.

**Validates: Requirements 1.3**

---

### Property 3: Category with linked products cannot be deleted

*For any* category that has at least one active product linked to it, attempting to soft-delete that category SHALL return HTTP 409.

**Validates: Requirements 1.7**

---

### Property 4: Product price must be positive

*For any* product creation or update request where `price <= 0`, the system SHALL return HTTP 400.

**Validates: Requirements 2.2**

---

### Property 5: Product filter by category returns only matching products

*For any* category ID used as a filter on the product list API, all returned products SHALL have `category_id` equal to that filter value.

**Validates: Requirements 2.4**

---

### Property 6: Soft-deleted product preserves order history

*For any* product that appears in at least one OrderItem, soft-deleting that product SHALL NOT remove or alter any existing OrderItem records.

**Validates: Requirements 2.8**

---

### Property 7: Duplicate phone number returns 409

*For any* phone number, if a customer with that phone already exists, attempting to create another customer with the same phone SHALL return HTTP 409.

**Validates: Requirements 3.2**

---

### Property 8: Order creation invariants (total amount and debt)

*For any* order creation request with a list of N order items, the system SHALL:
1. Store `unit_price` for each item as a snapshot of the product's price at creation time (not affected by future price changes)
2. Compute `quantity` per item based on product unit: `M2 → count × length × width`, `MET → count × length`, others → `count`
3. Compute `subtotal = quantity × unit_price` for each item
4. Compute `total_amount = Σ subtotal` for all items
5. Create a Debt record with `original_amount = total_amount - paid_immediately`

**Validates: Requirements 4.2, 4.3, 4.4**

---

### Property 9: Overpayment at order creation returns 400

*For any* order creation request where `paid_immediately > total_amount`, the system SHALL return HTTP 400 and not create the order or any debt record.

**Validates: Requirements 4.5**

---

### Property 10: Order filter returns only matching records

*For any* combination of `customerId` and/or date range filters on the order list API, all returned orders SHALL satisfy all applied filter conditions.

**Validates: Requirements 4.6**

---

### Property 12: Customer debt flag reflects actual debt state

*For any* customer, the `has_debt` flag SHALL be `true` if and only if the sum of all their `remaining_amount` across all Debt records is greater than 0.

**Validates: Requirements 5.3, 5.4**

---

### Property 13: Payment amount must be positive

*For any* payment creation request where `amount <= 0`, the system SHALL return HTTP 400.

**Validates: Requirements 6.2**

---

### Property 14: Payment reduces customer debt correctly

*For any* customer with total remaining debt `D` and a valid payment of amount `P`, after the payment is recorded the total remaining debt SHALL equal `D - P` (which may be negative if P > D).

**Validates: Requirements 6.3, 6.4**

---

### Property 15: Revenue statistics match order data

*For any* valid date range `[from, to]`, the `total_revenue` returned by the statistics API SHALL equal the sum of `total_amount` of all orders whose `order_date` falls within that range.

**Validates: Requirements 7.1**

---

### Property 16: Debt statistics only includes customers with outstanding debt

*For any* state of the system, the debt statistics API SHALL only return customers where `remaining_debt > 0`, and they SHALL be sorted by `remaining_debt` in descending order.

**Validates: Requirements 7.2**

---

### Property 17: Monthly revenue array is complete and accurate

*For any* year Y, the monthly revenue API SHALL return exactly 12 elements where element `i` equals the sum of `total_amount` of all orders in month `i` of year `Y`.

**Validates: Requirements 7.3**

---

### Property 18: Invalid date range returns 400

*For any* statistics query where `start_date > end_date`, the system SHALL return HTTP 400.

**Validates: Requirements 7.4**

---

### Property 19: All API responses follow unified format

*For any* API call (successful or error), the response body SHALL be a JSON object containing the fields: `success` (boolean), `data` (object/array/null), `message` (string), `timestamp` (ISO 8601 string).

**Validates: Requirements 8.1**

---

### Property 20: Validation errors include field-level details

*For any* request that fails bean validation, the HTTP 400 response SHALL include a list of specific field errors identifying which fields are invalid.

**Validates: Requirements 8.2**

---

## Error Handling

### Exception Hierarchy

```
RuntimeException
├── EntityNotFoundException       → HTTP 404
├── DuplicateResourceException    → HTTP 409
├── BusinessRuleException         → HTTP 409
└── InvalidRequestException       → HTTP 400
```

### GlobalExceptionHandler (@ControllerAdvice)

| Exception | HTTP Status | Response |
|---|---|---|
| `EntityNotFoundException` | 404 | `{success: false, data: null, message: "...", timestamp}` |
| `DuplicateResourceException` | 409 | `{success: false, data: null, message: "...", timestamp}` |
| `BusinessRuleException` | 409 | `{success: false, data: null, message: "...", timestamp}` |
| `InvalidRequestException` | 400 | `{success: false, data: null, message: "...", timestamp}` |
| `MethodArgumentNotValidException` | 400 | `{success: false, data: {fieldErrors: [...]}, message: "Validation failed", timestamp}` |
| `Exception` (catch-all) | 500 | `{success: false, data: null, message: "Internal server error", timestamp}` — log đầy đủ, không lộ stack trace |

### Validation Rules

- `@NotBlank` cho các trường bắt buộc dạng string
- `@NotNull` cho các trường bắt buộc dạng số/object
- `@Positive` cho price, amount, quantity
- `@Size` cho phone (tối đa 20 ký tự)
- Custom validator cho date range (start <= end)

## Testing Strategy

### Dual Testing Approach

Hệ thống sử dụng kết hợp:
1. **Unit tests** — kiểm tra service layer với mock repository, các ví dụ cụ thể và edge cases
2. **Property-based tests** — kiểm tra các invariant và universal properties với dữ liệu sinh ngẫu nhiên
3. **Integration tests** — kiểm tra end-to-end với database thật (H2 in-memory hoặc Testcontainers PostgreSQL)

### Property-Based Testing

Thư viện: **[jqwik](https://jqwik.net/)** — PBT library cho Java, tích hợp tốt với JUnit 5.

Cấu hình: mỗi property test chạy tối thiểu **100 iterations**.

Mỗi property test phải có comment tag theo format:
```
// Feature: construction-material-sales-system, Property {N}: {property_text}
```

Các property cần implement (tham chiếu theo số ở trên):
- **P1** — Duplicate category name → 409
- **P2** — Category list chỉ trả về active
- **P3** — Category có product không xóa được
- **P4** — Product price phải dương
- **P5** — Product filter theo category trả về đúng
- **P6** — Soft delete product không xóa order history
- **P7** — Duplicate phone → 409
- **P8** — Order creation invariants (snapshot price, total_amount, debt)
- **P9** — Overpayment at creation → 400
- **P10** — Order filter trả về đúng records
- **P12** — has_debt flag phản ánh đúng trạng thái nợ
- **P13** — Payment amount phải dương
- **P14** — Payment trừ nợ đúng (kể cả âm)
- **P15** — Revenue statistics khớp order data
- **P16** — Debt statistics chỉ trả về customer có nợ, sắp xếp đúng
- **P17** — Monthly revenue array đủ 12 phần tử và chính xác
- **P18** — Invalid date range → 400
- **P19** — Tất cả response theo unified format
- **P20** — Validation errors có field-level details

### Unit Tests (Service Layer)

Tập trung vào:
- Happy path cho từng service method
- Edge cases: empty list, null optional fields
- Error cases: entity not found, duplicate, business rule violations

### Integration Tests

Sử dụng `@SpringBootTest` + Testcontainers (PostgreSQL):
- Kiểm tra end-to-end flow: tạo order → tạo debt → thanh toán → cập nhật has_debt
- Kiểm tra cascade behavior khi soft delete
- Kiểm tra statistics queries với dữ liệu thật

### Test Coverage Target

- Service layer: ≥ 80% line coverage
- Controller layer: kiểm tra HTTP status codes và response format
- Property tests: 100 iterations mỗi property
