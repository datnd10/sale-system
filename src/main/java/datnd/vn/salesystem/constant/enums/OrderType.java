package datnd.vn.salesystem.constant.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderType {

    SALE("Bán hàng", "Đơn bán vật liệu xây dựng"),
    PAYMENT("Trả nợ", "Đơn ghi nhận thanh toán công nợ");

    private final String label;
    private final String description;
}
