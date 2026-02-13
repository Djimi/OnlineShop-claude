package com.onlineshop.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("db-troubleshooting")
@EnableAspectJAutoProxy(proxyTargetClass = false)
public class DbTroubleshootingAopConfig {
}
