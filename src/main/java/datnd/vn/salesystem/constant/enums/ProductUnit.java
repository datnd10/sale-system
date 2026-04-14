package datnd.vn.salesystem.constant.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductUnit {

    KG("kg", "Kilogram"),

    TAN("tấn", "Tấn"),

    CAY("cây", "Cây / thanh"),

    MET("m", "Mét"),

    CAI("cái", "Cái"),

    M2("m²", "Mét vuông");


    /** Nhãn hiển thị ngắn gọn (dùng trong response). */
    private final String label;

    /** Mô tả đầy đủ. */
    private final String description;
}
