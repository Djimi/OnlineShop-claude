package com.onlineshop.auth.config;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;

@Aspect
@Component
@Profile("db-troubleshooting")
public class DbTroubleshootingDataSourceAspect {

    private static final Logger logger = LoggerFactory.getLogger("com.onlineshop.auth.config.datasource");

    @Value("${auth.troubleshooting.datasource.acquire-slow-threshold-ms:2}")
    private long slowAcquireThresholdMs;

    @PostConstruct
    void init() {
        logger.info(
                "Datasource troubleshooting probe active (acquire-slow-threshold-ms={})",
                slowAcquireThresholdMs
        );
    }

    @Around("execution(* javax.sql.DataSource.getConnection(..)) && target(dataSource)")
    public Object logConnectionAcquire(ProceedingJoinPoint joinPoint, DataSource dataSource) throws Throwable {
        long acquireStart = System.nanoTime();
        Connection connection = (Connection) joinPoint.proceed();
        long acquireNanos = System.nanoTime() - acquireStart;
        logAcquire(acquireNanos, dataSource);
        return wrapConnectionForLifetimeLogging(connection, acquireNanos);
    }

    private Connection wrapConnectionForLifetimeLogging(Connection connection, long acquireNanos) {
        long borrowedAtNanos = System.nanoTime();
        InvocationHandler connectionHandler = (proxy, method, args) -> invokeConnectionMethod(
                method,
                args,
                connection,
                borrowedAtNanos,
                acquireNanos
        );

        return (Connection) Proxy.newProxyInstance(
                connection.getClass().getClassLoader(),
                new Class<?>[]{Connection.class},
                connectionHandler
        );
    }

    private Object invokeConnectionMethod(
            Method method,
            Object[] args,
            Connection connection,
            long borrowedAtNanos,
            long acquireNanos
    ) throws Throwable {
        if ("close".equals(method.getName())) {
            double heldMs = nanosToMillis(System.nanoTime() - borrowedAtNanos);
            logger.debug(
                    "DB connection closed after {} ms (acquire={} ms)",
                    formatMs(heldMs),
                    formatMs(nanosToMillis(acquireNanos))
            );
        }
        return method.invoke(connection, args);
    }

    private void logAcquire(long acquireNanos, DataSource dataSource) {
        HikariPoolMXBean poolBean = null;
        String poolName = "unknown";
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            poolBean = hikariDataSource.getHikariPoolMXBean();
            poolName = hikariDataSource.getPoolName();
        }

        Integer active = poolBean != null ? poolBean.getActiveConnections() : null;
        Integer idle = poolBean != null ? poolBean.getIdleConnections() : null;
        Integer waiting = poolBean != null ? poolBean.getThreadsAwaitingConnection() : null;
        Integer total = poolBean != null ? poolBean.getTotalConnections() : null;

        double acquireMs = nanosToMillis(acquireNanos);
        if (acquireNanos >= slowAcquireThresholdMs * 1_000_000L) {
            logger.warn(
                    "DB connection acquisition took {} ms [threshold={} ms, pool={}, active={}, idle={}, waiting={}, total={}]",
                    formatMs(acquireMs),
                    slowAcquireThresholdMs,
                    poolName,
                    active,
                    idle,
                    waiting,
                    total
            );
            return;
        }

        logger.debug(
                "DB connection acquisition took {} ms [pool={}, active={}, idle={}, waiting={}, total={}]",
                formatMs(acquireMs),
                poolName,
                active,
                idle,
                waiting,
                total
        );
    }

    private static double nanosToMillis(long nanos) {
        return nanos / 1_000_000.0;
    }

    private static String formatMs(double millis) {
        return String.format("%.3f", millis);
    }
}
