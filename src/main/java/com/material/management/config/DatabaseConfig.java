package com.material.management.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import javax.sql.DataSource;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据库配置类
 * 
 * @author Material Management System
 * @version 1.0.0
 */
@Configuration
public class DatabaseConfig {

    @Value("${material.management.database.host}")
    private String host;

    @Value("${material.management.database.username}")
    private String username;

    @Value("${material.management.database.password}")
    private String password;

    // 缓存数据源，避免重复创建
    private final ConcurrentHashMap<String, DataSource> dataSourceCache = new ConcurrentHashMap<>();

    /**
     * 创建动态数据源
     * 用于连接不同的数据库
     */
    @Bean
    public DataSource dynamicDataSource() {
        return createOptimizedDataSource("information_schema");
    }

    /**
     * 创建JdbcTemplate
     */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * 创建用于特定数据库的数据源（使用缓存和连接池）
     */
    public DataSource createDataSourceForDatabase(String databaseName) {
        return dataSourceCache.computeIfAbsent(databaseName, this::createOptimizedDataSource);
    }

    /**
     * 创建优化的数据源（使用HikariCP连接池）- 高性能版本
     */
    private DataSource createOptimizedDataSource(String databaseName) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // 构建优化的JDBC URL
        String jdbcUrl = String.format(
            "jdbc:mysql://%s:3306/%s?" +
            "useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&" +
            "allowPublicKeyRetrieval=true&cachePrepStmts=true&useServerPrepStmts=true&" +
            "prepStmtCacheSize=500&prepStmtCacheSqlLimit=4096&useLocalSessionState=true&" +
            "rewriteBatchedStatements=true&cacheResultSetMetadata=true&cacheServerConfiguration=true&" +
            "elideSetAutoCommits=true&maintainTimeStats=false&useLocalTransactionState=true&" +
            "tcpKeepAlive=true&tcpNoDelay=true&useCursorFetch=true&defaultFetchSize=1000",
            host, databaseName
        );
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);

        // 连接池优化配置 - 针对大数据查询优化
        config.setMinimumIdle(3);
        config.setMaximumPoolSize(15);
        config.setConnectionTimeout(30000);  // 30秒
        config.setIdleTimeout(600000);       // 10分钟
        config.setMaxLifetime(1800000);      // 30分钟
        config.setLeakDetectionThreshold(60000);
        config.setPoolName("HikariCP-" + databaseName);

        // 连接验证优化
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(3000);

        // 初始化SQL - 优化会话设置
        config.setConnectionInitSql(
            "SET SESSION sql_mode='STRICT_TRANS_TABLES,NO_ZERO_DATE,NO_ZERO_IN_DATE,ERROR_FOR_DIVISION_BY_ZERO';" +
            "SET SESSION innodb_lock_wait_timeout=10;" +
            "SET SESSION read_buffer_size=2097152;" +  // 2MB
            "SET SESSION sort_buffer_size=2097152;"    // 2MB
        );

        return new HikariDataSource(config);
    }

    /**
     * 定时清理过期的数据源缓存
     * 每小时执行一次
     */
    @Scheduled(fixedRate = 3600000) // 1小时
    public void cleanupDataSourceCache() {
        // 这里可以添加清理逻辑，比如关闭长时间未使用的连接池
        // 目前HikariCP会自动管理连接，所以暂时不需要手动清理
    }
}
