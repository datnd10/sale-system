package datnd.vn.salesystem.controller;

import datnd.vn.salesystem.common.ApiResponse;
import datnd.vn.salesystem.config.AuthProperties;
import datnd.vn.salesystem.config.JwtUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "Đăng nhập hệ thống")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthProperties authProperties;
    private final JwtUtil jwtUtil;

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class LoginResponse {
        private final String token;
        private final String username;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        if (!authProperties.getUsername().equals(request.getUsername())
                || !authProperties.getPassword().equals(request.getPassword())) {
            return ApiResponse.error("Tên đăng nhập hoặc mật khẩu không đúng");
        }

        String token = jwtUtil.generateToken(request.getUsername());
        return ApiResponse.success(new LoginResponse(token, request.getUsername()), "Đăng nhập thành công");
    }
}
