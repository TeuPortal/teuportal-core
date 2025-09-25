package com.teuportal.core;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "TeuPortal Core API", version = "v1"))
public class CoreApiSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreApiSpringApplication.class, args);
    }
}
