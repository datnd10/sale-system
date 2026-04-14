# Implementation Plan: Construction Material Sales System

## Overview

Triển khai hệ thống backend REST API quản lý bán vật liệu xây dựng theo kiến trúc 3 lớp chuẩn Spring Boot (Controller → Service → Repository) với PostgreSQL. Các task được sắp xếp theo thứ tự từ nền tảng đến tính năng cụ thể, đảm bảo mỗi bước đều có thể chạy và kiểm tra được.

## Tasks

- [ ] 1. Thiết lập nền tảng dự án (Foundation)
  - Thêm dependency jqwik vào `pom.xml` cho property-based testing
  - Thêm dependency Testcontainers PostgreSQL vào `pom.xml` cho integration test
  - Thêm dependency Lombok vào `pom.xml` để giảm boilerplate
  - Cấu hình `application.properties` với datasource PostgreSQL, JPA DDL auto, logging
  - Tạo `application-test.properties` cho môi trường test (H2 hoặc Testcontainers)
  - _Requirements: 8.1, 8.3_

- [ ] 2. Tạo infrastructure chung (Common Layer)
  - [x] 2.1 Tạo `ApiResponse<T>` record và các custom exception
    - Tạo `ApiResponse<T>` record với các trường: `success`, `data`, `message`, `timestamp`
    - Tạo exception hierarchy: `EntityNotFoundException`, `DuplicateResourceException`, `BusinessRuleException`, `InvalidRequestException`
    - _Requirements: 8.1_

  - [x] 2.2 Tạo `GlobalExceptionHandler`
    - Implement `@ControllerAdvice` xử lý tất cả exception types
    - Map từng exception type sang HTTP status code tương ứng
    - Xử lý `MethodArgumentNotValidException` trả về danh sách field errors
    - Catch-all handler cho `Exception` → 500, log lỗi, không lộ stack trace
    - _Requirements: 8.1, 8.2, 8.3_

  - [ ]* 2.3 Viết unit test cho `GlobalExceptionHandler`
    - Test từng exception type trả về đúng HTTP status và format response
    - Test `MethodArgumentNotValidException` có field errors trong response
    - _Requirements: 8.1, 8.2_

  - [ ]* 2.4 Viết property test cho unified response format (P19, P20)
    - **Property 19: All API responses follow unified format**
    - **Property 20: Validation errors include field-level details**
    - **Validates: Requirements 8.1, 8.2**

- [ ] 3. Implement Category module
  - [x] 3.1 Tạo `Category` entity và `CategoryRepository`
    - Tạo JPA entity `Category` với các trường theo schema (id, code, name, description, active, created_at, updated_at)
    - Sinh `code` tự động sau khi save theo format `DM%07d` dựa trên `id`
    - Tạo `CategoryRepository` extends `JpaRepository`
    - Thêm query method `findByNameIgnoreCase` và `findAllByActiveTrue`
    - _Requirements: 1.1, 1.3, 9.1, 9.2, 9.3_

  - [x] 3.2 Tạo `CategoryService` với đầy đủ business logic
    - Implement `createCategory`: validate tên trùng → 409, lưu entity
    - Implement `getAllCategories`: chỉ trả về active = true
    - Implement `updateCategory`: validate ID tồn tại → 404, validate tên trùng → 409
    - Implement `deleteCategory`: validate ID tồn tại → 404, kiểm tra product liên kết → 409, soft delete
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7_

  - [x] 3.3 Tạo `CategoryController` với các DTO request/response
    - Tạo `CategoryRequest` DTO với Bean Validation annotations
    - Tạo `CategoryResponse` DTO
    - Implement 4 endpoints: POST, GET, PUT `/{id}`, DELETE `/{id}`
    - _Requirements: 1.1, 1.3, 1.4, 1.6_

  - [ ]* 3.4 Viết unit test cho `CategoryService`
    - Test happy path: create, list, update, delete
    - Test error cases: duplicate name → 409, not found → 404, has products → 409
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7_

  - [ ]* 3.5 Viết property test cho Category (P1, P2, P3)
    - **Property 1: Duplicate category name returns 409**
    - **Property 2: Category list only contains active records**
    - **Property 3: Category with linked products cannot be deleted**
    - **Validates: Requirements 1.2, 1.3, 1.7**

- [ ] 4. Implement Product module
  - [x] 4.1 Tạo `Product` entity và `ProductRepository`
    - Tạo JPA entity `Product` với các trường theo schema (id, code, name, category_id, unit, price, description, active, timestamps), quan hệ ManyToOne với `Category`
    - Sinh `code` tự động sau khi save theo format `SP%07d` dựa trên `id`
    - Tạo `ProductRepository` với query method lọc theo `categoryId` và `active`
    - _Requirements: 2.1, 2.4, 9.1, 9.2, 9.3_

  - [ ] 4.2 Tạo `ProductService` với đầy đủ business logic
    - Implement `createProduct`: validate category tồn tại → 404, validate price > 0 → 400
    - Implement `getProducts`: lọc theo categoryId (optional), chỉ trả về active
    - Implement `getProductById`: validate tồn tại → 404
    - Implement `updateProduct`: validate tồn tại → 404, validate price > 0 → 400
    - Implement `deleteProduct`: soft delete, không xóa OrderItem liên quan
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8_

  - [ ] 4.3 Tạo `ProductController` với các DTO request/response
    - Tạo `ProductRequest` DTO với Bean Validation annotations (`@Positive` cho price)
    - Tạo `ProductResponse` DTO
    - Implement 5 endpoints: POST, GET, GET `/{id}`, PUT `/{id}`, DELETE `/{id}`
    - _Requirements: 2.1, 2.4, 2.5, 2.6, 2.7_

  - [ ]* 4.4 Viết unit test cho `ProductService`
    - Test happy path: create, list (with/without filter), get by id, update, delete
    - Test error cases: invalid price → 400, category not found → 404
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8_

  - [ ]* 4.5 Viết property test cho Product (P4, P5, P6)
    - **Property 4: Product price must be positive**
    - **Property 5: Product filter by category returns only matching products**
    - **Property 6: Soft-deleted product preserves order history**
    - **Validates: Requirements 2.2, 2.4, 2.8**

- [ ] 5. Implement Customer module
  - [ ] 5.1 Tạo `Customer` entity và `CustomerRepository`
    - Tạo JPA entity `Customer` với các trường theo schema (id, code, name, phone, address, has_debt, timestamps)
    - Sinh `code` tự động sau khi save theo format `KH%07d` dựa trên `id`
    - Tạo `CustomerRepository` với query method kiểm tra phone trùng
    - _Requirements: 3.1, 3.2, 9.1, 9.2, 9.3_

  - [ ] 5.2 Tạo `CustomerService` với đầy đủ business logic
    - Implement `createCustomer`: validate phone trùng → 409
    - Implement `getAllCustomers`: trả về danh sách tất cả
    - Implement `getCustomerById`: validate tồn tại → 404, tính tổng công nợ hiện tại
    - Implement `updateCustomer`: validate tồn tại → 404, validate phone trùng → 409
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

  - [ ] 5.3 Tạo `CustomerController` với các DTO request/response
    - Tạo `CustomerRequest` DTO với Bean Validation annotations
    - Tạo `CustomerResponse` DTO (bao gồm `totalDebt` cho GET by ID)
    - Implement 4 endpoints: POST, GET, GET `/{id}`, PUT `/{id}`
    - _Requirements: 3.1, 3.3, 3.4, 3.5_

  - [ ]* 5.4 Viết unit test cho `CustomerService`
    - Test happy path: create, list, get by id (với total debt), update
    - Test error cases: duplicate phone → 409, not found → 404
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

  - [ ]* 5.5 Viết property test cho Customer (P7)
    - **Property 7: Duplicate phone number returns 409**
    - **Validates: Requirements 3.2**

- [ ] 6. Checkpoint — Đảm bảo tất cả test pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 7. Implement Order module
  - [ ] 7.1 Tạo `Order`, `OrderItem` entity và các Repository
    - Tạo JPA entity `Order` với enum `OrderStatus` (PENDING, COMPLETED, CANCELLED), bao gồm trường `code` (HD0000001)
    - Sinh `code` tự động sau khi save theo format `HD%07d` dựa trên `id`
    - Tạo JPA entity `OrderItem` với quan hệ ManyToOne tới `Order` và `Product`
    - Tạo `OrderRepository` với query method lọc theo customerId và date range
    - Tạo `OrderItemRepository`
    - _Requirements: 4.1, 4.6, 9.1, 9.2, 9.3_

  - [ ] 7.2 Tạo `Debt` entity và `DebtRepository`
    - Tạo JPA entity `Debt` với các trường: code, customer_id, order_id, original_amount, remaining_amount, timestamps
    - Sinh `code` tự động sau khi save theo format `CN%07d` dựa trên `id`
    - Tạo `DebtRepository` với query method lấy debts theo customerId có remaining_amount > 0, sắp xếp FIFO
    - _Requirements: 4.4, 5.1, 5.2, 9.1, 9.2, 9.3_

  - [ ] 7.3 Tạo `OrderService` với đầy đủ business logic
    - Implement `createOrder`:
      - Validate customer tồn tại → 404
      - Validate từng product trong items tồn tại → 404
      - Snapshot unit_price từ product tại thời điểm tạo
      - Tính `total_amount = Σ (quantity × unit_price)`
      - Validate `paid_immediately <= total_amount` → 400 nếu vi phạm
      - Tạo Order, các OrderItem
      - Tạo Debt record với `original_amount = total_amount - paid_immediately`
    - Implement `getOrders`: lọc theo customerId, from, to
    - Implement `getOrderById`: validate tồn tại → 404, trả về kèm OrderItems
    - Implement `updateOrderNote`: validate tồn tại → 404, cập nhật note
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9_

  - [ ] 7.4 Tạo `OrderController` với các DTO request/response
    - Tạo `OrderRequest` DTO (customerId, orderDate, items list, paidImmediately)
    - Tạo `OrderItemRequest` DTO (productId, quantity)
    - Tạo `OrderResponse` và `OrderDetailResponse` DTO (bao gồm items)
    - Tạo `UpdateNoteRequest` DTO
    - Implement 4 endpoints: POST, GET, GET `/{id}`, PATCH `/{id}/note`
    - _Requirements: 4.1, 4.6, 4.7, 4.9_

  - [ ]* 7.5 Viết unit test cho `OrderService`
    - Test happy path: tạo order với nhiều items, lấy danh sách, lấy chi tiết, cập nhật note
    - Test error cases: overpayment → 400, customer not found → 404, product not found → 404
    - Test snapshot price: thay đổi giá product sau khi tạo order không ảnh hưởng OrderItem
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9_

  - [ ]* 7.6 Viết property test cho Order (P8, P9, P10, P11)
    - **Property 8: Order creation invariants (total amount and debt)**
    - **Property 9: Overpayment at order creation returns 400**
    - **Property 10: Order filter returns only matching records**
    - **Property 11: Terminal-status orders reject item updates**
    - **Validates: Requirements 4.2, 4.3, 4.4, 4.5, 4.6, 4.10**

- [ ] 8. Implement Debt module
  - [ ] 8.1 Tạo `DebtService` với logic tính công nợ
    - Implement `getAllCustomerDebts`: aggregate remaining_amount theo từng customer
    - Implement `getCustomerDebtDetail`: lấy danh sách Debt của customer, tính tổng remaining
    - Implement `updateCustomerDebtFlag`: cập nhật `has_debt` dựa trên tổng remaining_amount
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

  - [ ] 8.2 Tạo `DebtController`
    - Tạo `DebtSummaryResponse` DTO (customerId, customerName, totalRemaining)
    - Tạo `CustomerDebtDetailResponse` DTO (danh sách orders chưa thanh toán đủ)
    - Implement 2 endpoints: GET `/api/debts`, GET `/api/debts/customer/{customerId}`
    - _Requirements: 5.1, 5.2_

  - [ ]* 8.3 Viết unit test cho `DebtService`
    - Test tính tổng remaining_amount đúng
    - Test cập nhật has_debt flag khi nợ = 0 và nợ > 0
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

  - [ ]* 8.4 Viết property test cho Debt (P12)
    - **Property 12: Customer debt flag reflects actual debt state**
    - **Validates: Requirements 5.3, 5.4**

- [ ] 9. Implement Payment module
  - [ ] 9.1 Tạo `Payment` entity và `PaymentRepository`
    - Tạo JPA entity `Payment` với các trường theo schema, bao gồm trường `code` (TT0000001)
    - Sinh `code` tự động sau khi save theo format `TT%07d` dựa trên `id`
    - Tạo `PaymentRepository` với query method lọc theo customerId và date range
    - _Requirements: 6.1, 6.5, 6.6, 9.1, 9.2, 9.3_

  - [ ] 9.2 Tạo `PaymentService` với logic trừ nợ FIFO
    - Implement `createPayment`:
      - Validate customer tồn tại → 404
      - Validate amount > 0 → 400
      - Lấy danh sách Debt của customer có remaining_amount > 0, sắp xếp FIFO (created_at ASC)
      - Trừ dần amount vào từng Debt
      - Cho phép số dư âm nếu payment > tổng nợ
      - Lưu Payment record
      - Gọi `DebtService.updateCustomerDebtFlag` để cập nhật has_debt
    - Implement `getPaymentsByCustomer`: lấy lịch sử theo customerId
    - Implement `getAllPayments`: lọc theo date range
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

  - [ ] 9.3 Tạo `PaymentController` với các DTO request/response
    - Tạo `PaymentRequest` DTO với Bean Validation (`@Positive` cho amount)
    - Tạo `PaymentResponse` DTO
    - Implement 3 endpoints: POST, GET `/customer/{customerId}`, GET (với date filter)
    - _Requirements: 6.1, 6.5, 6.6_

  - [ ]* 9.4 Viết unit test cho `PaymentService`
    - Test FIFO debt reduction: nhiều debts, payment trừ theo thứ tự
    - Test overpayment: payment > tổng nợ → remaining âm, vẫn chấp nhận
    - Test invalid amount → 400
    - Test has_debt flag được cập nhật sau payment
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

  - [ ]* 9.5 Viết property test cho Payment (P13, P14)
    - **Property 13: Payment amount must be positive**
    - **Property 14: Payment reduces customer debt correctly**
    - **Validates: Requirements 6.2, 6.3, 6.4**

- [ ] 10. Checkpoint — Đảm bảo tất cả test pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 11. Implement Statistics module
  - [ ] 11.1 Tạo `StatisticsService` với các aggregate queries
    - Implement `getRevenueStats(from, to)`:
      - Validate from <= to → 400 nếu vi phạm
      - Trả về: total_orders, total_revenue, total_collected (sum paid_immediately + payments), total_debt
    - Implement `getDebtStats()`: danh sách customer có remaining_debt > 0, sắp xếp giảm dần
    - Implement `getMonthlyRevenue(year)`: mảng 12 phần tử, tổng revenue từng tháng
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

  - [ ] 11.2 Tạo `StatisticsController`
    - Tạo `RevenueStatsResponse` DTO
    - Tạo `DebtStatsResponse` DTO
    - Tạo `MonthlyRevenueResponse` DTO (array 12 phần tử)
    - Implement 3 endpoints: GET `/revenue`, GET `/debts`, GET `/revenue/monthly`
    - _Requirements: 7.1, 7.2, 7.3_

  - [ ]* 11.3 Viết unit test cho `StatisticsService`
    - Test revenue stats với nhiều orders trong range
    - Test monthly revenue trả về đúng 12 phần tử
    - Test invalid date range → 400
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

  - [ ]* 11.4 Viết property test cho Statistics (P15, P16, P17, P18)
    - **Property 15: Revenue statistics match order data**
    - **Property 16: Debt statistics only includes customers with outstanding debt**
    - **Property 17: Monthly revenue array is complete and accurate**
    - **Property 18: Invalid date range returns 400**
    - **Validates: Requirements 7.1, 7.2, 7.3, 7.4**

- [ ] 12. Viết Integration Tests end-to-end
  - [ ]* 12.1 Viết integration test cho luồng Order → Debt → Payment
    - Sử dụng `@SpringBootTest` + Testcontainers PostgreSQL
    - Test: tạo order → kiểm tra debt được tạo → tạo payment → kiểm tra remaining_amount giảm → kiểm tra has_debt cập nhật
    - _Requirements: 4.4, 5.3, 5.4, 6.3, 6.4_

  - [ ]* 12.2 Viết integration test cho soft delete cascade
    - Test: soft delete category → product vẫn còn trong DB → order history không bị ảnh hưởng
    - Test: soft delete product → OrderItem history vẫn nguyên vẹn
    - _Requirements: 1.6, 1.7, 2.7, 2.8_

- [ ] 13. Final Checkpoint — Đảm bảo tất cả test pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks đánh dấu `*` là optional, có thể bỏ qua để implement MVP nhanh hơn
- Mỗi task tham chiếu requirements cụ thể để đảm bảo traceability
- Property tests sử dụng thư viện **jqwik** với tối thiểu 100 iterations mỗi property
- Mỗi property test cần comment tag: `// Feature: construction-material-sales-system, Property {N}: {text}`
- Base package: `datnd.vn.salesystem`
- Tất cả response đều wrap trong `ApiResponse<T>`
