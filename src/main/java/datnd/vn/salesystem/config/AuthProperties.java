package datnd.vn.salesystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {
    private String username;
    private String password;
    private String jwtSecret;
    private long jwtExpirationMs;
}
