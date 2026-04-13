package datnd.vn.salesystem.config;


import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DbHealthCheck {

    private final JdbcTemplate jdbcTemplate;

    public DbHealthCheck(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isDatabaseUp() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return result != null && result == 1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
