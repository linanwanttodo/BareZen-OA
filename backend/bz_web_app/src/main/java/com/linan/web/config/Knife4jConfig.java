package com.linan.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BareZen_Base API 文档")
                        .description("BareZen_Base 前后端分离后台管理系统接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("BareZen Team")
                                .email("admin@barezen.com")));
    }
}
