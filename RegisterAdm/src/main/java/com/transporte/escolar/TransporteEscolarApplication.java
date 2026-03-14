package com.transporte.escolar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.transporte.escolar")
@EnableJpaRepositories(basePackages = "com.transporte.escolar.repository")
public class TransporteEscolarApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransporteEscolarApplication.class, args);
    }
}
