package datnd.vn.salesystem.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.sync")
public class SyncProperties {
    private String localDbUrl;
    private String localDbUsername;
    private String localDbPassword;
    private String cron;
}
