package org.astrabank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AstrabankApplication {

    public static void main(String[] args) {
        SpringApplication.run(AstrabankApplication.class, args);
    }

}
