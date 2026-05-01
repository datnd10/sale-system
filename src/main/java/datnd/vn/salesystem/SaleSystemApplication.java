package datnd.vn.salesystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SaleSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaleSystemApplication.class, args);
    }

}
