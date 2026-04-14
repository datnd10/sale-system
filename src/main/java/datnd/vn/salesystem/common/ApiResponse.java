package datnd.vn.salesystem.common;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        String timestamp
) {
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, java.time.Instant.now().toString());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, java.time.Instant.now().toString());
    }
}
