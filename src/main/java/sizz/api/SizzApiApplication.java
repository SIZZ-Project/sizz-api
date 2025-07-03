package sizz.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SizzApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SizzApiApplication.class, args);
    }

}
