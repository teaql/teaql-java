package io.teaql.spring.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TeaQLWebAutoConfiguration {

    @Bean
    public TeaQLController teaQLController() {
        return new TeaQLController();
    }
}
