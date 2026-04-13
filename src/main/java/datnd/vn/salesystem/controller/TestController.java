package datnd.vn.salesystem.controller;

import datnd.vn.salesystem.config.DbHealthCheck;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {
    @GetMapping
    public String test() {
        return "test 123";
    }

    private final DbHealthCheck dbHealthService;

    public TestController(DbHealthCheck dbHealthService) {
        this.dbHealthService = dbHealthService;
    }

    @GetMapping("/health/db")
    public String checkDb() {
        return dbHealthService.isDatabaseUp() ? "DB OK" : "DB DOWN";
    }
}
