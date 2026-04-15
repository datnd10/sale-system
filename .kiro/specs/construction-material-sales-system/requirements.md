# Requirements Document

## Introduction

Hệ thống backend quản lý bán hàng vật liệu xây dựng (tôn, sắt, thép) dành cho nội bộ gia đình sử dụng. Hệ thống cung cấp các API REST đơn giản để quản lý danh mục hàng hóa, sản phẩm, đơn hàng, công nợ khách hàng, thanh toán và thống kê cơ bản. Không yêu cầu quản lý tồn kho hay xác thực phức tạp.

## Glossary

- **System**: Hệ thống backend bán vật liệu xây dựng (Spring Boot REST API)
- **Category**: Danh mục mặt hàng (ví dụ: Tôn, Sắt, Thép, Ống nước,...)
- **Product**: Sản phẩm cụ thể thuộc một danh mục, có tên, đơn vị tính và giá bán
- **Customer**: Khách hàng mua hàng, có thể phát sinh công nợ
- **Order**: Đơn hàng ghi nhận giao dịch mua bán giữa khách hàng và cửa hàng
- **OrderItem**: Dòng chi tiết trong đơn hàng, gồm sản phẩm, số lượng và đơn giá tại thời điểm bán
- **Debt**: Công nợ của khách hàng — phần tiền còn lại chưa thanh toán sau khi tạo đơn hàng
- **Payment**: Giao dịch thanh toán của khách hàng để trả nợ
- **API**: REST API endpoint được hệ thống cung cấp
- **Display Code**: Mã hiển thị tự sinh dạng `PREFIX + số thứ tự 7 chữ số` (ví dụ: `KH0000001`), dùng để nhận diện bản ghi ra ngoài, không phải khóa chính nội bộ

---

## Requirements

### Requirement 1: Quản lý danh mục mặt hàng (Category)

**User Story:** As a cửa hàng, I want to quản lý danh mục mặt hàng, so that tôi có thể phân loại sản phẩm theo nhóm (tôn, sắt, thép,...).

#### Acceptance Criteria

1. THE System SHALL cung cấp API để tạo mới một Category với tên danh mục (bắt buộc) và mô tả (tùy chọn).
2. WHEN tên Category bị trùng lặp, THEN THE System SHALL trả về lỗi HTTP 409 với thông báo rõ ràng.
3. THE System SHALL cung cấp API để lấy danh sách tất cả Category đang hoạt động.
4. THE System SHALL cung cấp API để cập nhật tên và mô tả của một Category theo ID.
5. WHEN một Category ID không tồn tại, THEN THE System SHALL trả về lỗi HTTP 404.
6. THE System SHALL cung cấp API để xóa mềm (soft delete) một Category theo ID.
7. IF một Category đang có Product liên kết, THEN THE System SHALL từ chối xóa và trả về lỗi HTTP 409.

---

### Requirement 2: Quản lý sản phẩm (Product)

**User Story:** As a cửa hàng, I want to quản lý thông tin chi tiết từng sản phẩm, so that tôi có thể theo dõi giá bán và thông tin sản phẩm.

#### Acceptance Criteria

1. THE System SHALL cung cấp API để tạo mới một Product với các trường: tên sản phẩm (bắt buộc), Category ID (bắt buộc), đơn vị tính (bắt buộc, ví dụ: kg, tấm, cây), giá bán (bắt buộc, số dương), mô tả (tùy chọn).
2. WHEN giá bán của Product nhỏ hơn hoặc bằng 0, THEN THE System SHALL trả về lỗi HTTP 400.
3. WHEN Category ID không tồn tại khi tạo Product, THEN THE System SHALL trả về lỗi HTTP 404.
4. THE System SHALL cung cấp API để lấy danh sách Product, hỗ trợ lọc theo Category ID.
5. THE System SHALL cung cấp API để lấy chi tiết một Product theo ID.
6. THE System SHALL cung cấp API để cập nhật thông tin và giá bán của một Product theo ID.
7. THE System SHALL cung cấp API để xóa mềm một Product theo ID.
8. IF một Product đã xuất hiện trong OrderItem, THEN THE System SHALL vẫn cho phép xóa mềm nhưng không xóa cứng dữ liệu lịch sử.

---

### Requirement 3: Quản lý khách hàng (Customer)

**User Story:** As a cửa hàng, I want to quản lý thông tin khách hàng, so that tôi có thể theo dõi lịch sử mua hàng và công nợ.

#### Acceptance Criteria

1. THE System SHALL cung cấp API để tạo mới một Customer với tên (bắt buộc), số điện thoại (tùy chọn), địa chỉ (tùy chọn).
2. WHEN số điện thoại Customer bị trùng lặp, THEN THE System SHALL trả về lỗi HTTP 409.
3. THE System SHALL cung cấp API để lấy danh sách tất cả Customer.
4. THE System SHALL cung cấp API để lấy chi tiết một Customer theo ID, bao gồm tổng công nợ hiện tại.
5. THE System SHALL cung cấp API để cập nhật thông tin Customer theo ID.

---

### Requirement 4: Quản lý đơn hàng (Order)

**User Story:** As a cửa hàng, I want to tạo và quản lý đơn hàng, so that tôi có thể ghi nhận giao dịch bán hàng.

#### Acceptance Criteria

1. THE System SHALL cung cấp API để tạo mới một Order với Customer ID (bắt buộc), ngày đặt hàng (mặc định là ngày hiện tại), danh sách OrderItem (ít nhất 1 item), và số tiền đã thanh toán ngay (tùy chọn, mặc định 0).
2. WHEN tạo Order, THE System SHALL lưu đơn giá (unit_price) của từng OrderItem tại thời điểm tạo đơn, không phụ thuộc vào giá Product sau này.
3. WHEN tạo Order, THE System SHALL tính tổng tiền đơn hàng (total_amount) bằng tổng (số lượng × đơn giá) của tất cả OrderItem.
4. WHEN tạo Order, THE System SHALL tạo bản ghi Debt cho Customer với giá trị bằng total_amount trừ số tiền đã thanh toán ngay.
5. IF số tiền thanh toán ngay lớn hơn total_amount, THEN THE System SHALL trả về lỗi HTTP 400.
6. THE System SHALL cung cấp API để lấy danh sách Order, hỗ trợ lọc theo Customer ID và khoảng thời gian.
7. THE System SHALL cung cấp API để lấy chi tiết một Order theo ID, bao gồm danh sách OrderItem.
8. WHEN một Order ID không tồn tại, THEN THE System SHALL trả về lỗi HTTP 404.
9. THE System SHALL cung cấp API để cập nhật ghi chú (note) của Order theo ID.
10. THE System SHALL cung cấp API để xóa mềm (soft delete) một Order theo ID.

---

### Requirement 5: Quản lý công nợ (Debt)

**User Story:** As a cửa hàng, I want to theo dõi công nợ của từng khách hàng, so that tôi biết khách hàng nào còn nợ bao nhiêu.

#### Acceptance Criteria

1. THE System SHALL cung cấp API để lấy danh sách công nợ của tất cả Customer, hiển thị tổng nợ còn lại (remaining_debt) của từng người.
2. THE System SHALL cung cấp API để lấy chi tiết công nợ của một Customer theo Customer ID, bao gồm danh sách các Order chưa thanh toán đủ.
3. WHILE remaining_debt của một Customer lớn hơn 0, THE System SHALL đánh dấu Customer đó là đang có nợ (has_debt = true).
4. WHEN tất cả công nợ của một Customer được thanh toán đủ, THE System SHALL cập nhật has_debt = false cho Customer đó.

---

### Requirement 6: Thanh toán (Payment)

**User Story:** As a cửa hàng, I want to ghi nhận thanh toán của khách hàng, so that tôi có thể cập nhật công nợ chính xác.

#### Acceptance Criteria

1. THE System SHALL cung cấp API để tạo một Payment với Customer ID (bắt buộc), số tiền thanh toán (bắt buộc, số dương), ngày thanh toán (mặc định ngày hiện tại), và ghi chú (tùy chọn).
2. WHEN số tiền Payment nhỏ hơn hoặc bằng 0, THEN THE System SHALL trả về lỗi HTTP 400.
3. WHEN tạo Payment, THE System SHALL trừ số tiền thanh toán vào tổng công nợ của Customer.
4. IF số tiền Payment lớn hơn tổng công nợ hiện tại của Customer, THEN THE System SHALL vẫn chấp nhận và ghi nhận số dư âm (trả thừa) để đối soát sau.
5. THE System SHALL cung cấp API để lấy lịch sử Payment của một Customer theo Customer ID.
6. THE System SHALL cung cấp API để lấy danh sách tất cả Payment, hỗ trợ lọc theo khoảng thời gian.

---

### Requirement 7: Thống kê cơ bản (Statistics)

**User Story:** As a cửa hàng, I want to xem thống kê doanh thu và công nợ, so that tôi có cái nhìn tổng quan về tình hình kinh doanh.

#### Acceptance Criteria

1. THE System SHALL cung cấp API thống kê doanh thu theo khoảng thời gian, trả về: tổng số đơn hàng, tổng doanh thu (total_revenue), tổng đã thu (total_collected), tổng còn nợ (total_debt).
2. THE System SHALL cung cấp API thống kê công nợ, trả về danh sách Customer có remaining_debt lớn hơn 0, sắp xếp theo remaining_debt giảm dần.
3. THE System SHALL cung cấp API thống kê doanh thu theo tháng trong một năm, trả về mảng 12 phần tử với tổng doanh thu từng tháng.
4. WHEN khoảng thời gian truy vấn không hợp lệ (ngày bắt đầu sau ngày kết thúc), THEN THE System SHALL trả về lỗi HTTP 400.

---

### Requirement 9: Mã hiển thị tự sinh (Display Code)

**User Story:** As a cửa hàng, I want to mỗi bản ghi có một mã dễ đọc, so that tôi có thể nhận diện và tra cứu nhanh mà không cần nhớ ID số.

#### Acceptance Criteria

1. THE System SHALL tự động sinh mã hiển thị (`code`) cho mỗi bản ghi khi tạo mới, theo format `PREFIX + số thứ tự 7 chữ số` (ví dụ: `KH0000001`).
2. THE System SHALL sử dụng các prefix sau cho từng loại bản ghi:
   - Category: `DM` (ví dụ: `DM0000001`)
   - Product: `SP` (ví dụ: `SP0000001`)
   - Customer: `KH` (ví dụ: `KH0000001`)
   - Order: `HD` (ví dụ: `HD0000001`)
   - Debt: `CN` (ví dụ: `CN0000001`)
   - Payment: `TT` (ví dụ: `TT0000001`)
3. THE System SHALL đảm bảo `code` là duy nhất trong phạm vi từng bảng.
4. THE System SHALL trả về `code` trong tất cả response DTO của bản ghi tương ứng.
5. `code` là trường chỉ để hiển thị — khóa chính nội bộ vẫn là `id` (BIGSERIAL) dùng cho các quan hệ và URL path.

---

### Requirement 8: Xử lý lỗi và định dạng phản hồi chung

**User Story:** As a developer tích hợp, I want to nhận phản hồi lỗi nhất quán từ API, so that tôi có thể xử lý lỗi dễ dàng.

#### Acceptance Criteria

1. THE System SHALL trả về tất cả phản hồi API theo định dạng JSON thống nhất với các trường: `success` (boolean), `data` (object/array, null nếu lỗi), `message` (string), `timestamp` (ISO 8601).
2. WHEN xảy ra lỗi validation đầu vào, THE System SHALL trả về HTTP 400 kèm danh sách các trường lỗi cụ thể.
3. WHEN xảy ra lỗi không mong muốn (500), THE System SHALL ghi log lỗi và trả về thông báo lỗi chung, không lộ stack trace.
