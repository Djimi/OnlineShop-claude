package com.onlineshop.gateway.config;

import com.onlineshop.gateway.handler.ProductInfoHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.GET;

@Configuration
public class ProductInfoRouteConfig {

    @Bean
    public RouterFunction<ServerResponse> productInfoRoute(ProductInfoHandler handler) {
        return RouterFunctions.route(GET("/api/product-info"), handler::getProductInfo);
    }
}
