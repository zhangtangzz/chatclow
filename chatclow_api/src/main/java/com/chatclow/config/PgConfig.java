package com.chatclow.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "pgvector.enabled", havingValue = "true")
public class PgConfig {

    @Value("${pgvector.url:jdbc:postgresql://localhost:5432/chatclow}")
    private String url;

    @Value("${pgvector.username:postgres}")
    private String username;

    @Value("${pgvector.password:postgres}")
    private String password;

    /**
     * PG 专属数据源，仅用于 pgJdbcTemplate。
     * 返回 HikariDataSource（子类）而非 DataSource，避免被 Spring Boot 自动配置当作主数据源。
     */
    @Bean
    public HikariDataSource pgDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setMaximumPoolSize(5);
        ds.setPoolName("pgvector-pool");
        return ds;
    }

    /**
     * PG 专属 JdbcTemplate，PgVectorStore 通过 @Qualifier("pgJdbcTemplate") 注入
     */
    @Bean
    public JdbcTemplate pgJdbcTemplate() {
        return new JdbcTemplate(pgDataSource());
    }
}
