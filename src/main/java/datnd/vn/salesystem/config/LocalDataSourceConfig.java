package datnd.vn.salesystem.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class LocalDataSourceConfig {

    private final SyncProperties syncProperties;

    @Bean(name = "localDataSource")
    public DataSource localDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(syncProperties.getLocalDbUrl());
        config.setUsername(syncProperties.getLocalDbUsername());
        config.setPassword(syncProperties.getLocalDbPassword());
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10000);
        config.setPoolName("LocalSyncPool");
        return new HikariDataSource(config);
    }

    @Bean(name = "localJdbcTemplate")
    public JdbcTemplate localJdbcTemplate() {
        return new JdbcTemplate(localDataSource());
    }
}
