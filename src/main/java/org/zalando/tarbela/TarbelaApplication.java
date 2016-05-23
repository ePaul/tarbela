package org.zalando.tarbela;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAutoConfiguration
@EnableScheduling
@SpringBootApplication
public class TarbelaApplication {
    public static void main(final String[] args) {
        SpringApplication.run(TarbelaApplication.class, args);
    }
}
