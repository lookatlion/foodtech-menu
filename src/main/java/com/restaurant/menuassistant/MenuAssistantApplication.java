package com.restaurant.menuassistant;

import com.restaurant.menuassistant.config.OpenAiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(OpenAiProperties.class)
public class MenuAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(MenuAssistantApplication.class, args);
    }
}
