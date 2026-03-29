package com.jasonlat.ai.config;

import com.zaxxer.hikari.HikariDataSource;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Objects;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Primary
    @Bean("mysqlDataSource")
    public DataSource mysqlDataSource(@Value("${spring.datasource.mysql.driver-class-name:com.mysql.cj.jdbc.Driver}") String driverClassName,
                                      @Value("${spring.datasource.mysql.url}") String url,
                                      @Value("${spring.datasource.mysql.username}") String username,
                                      @Value("${spring.datasource.mysql.password}") String password,
                                      @Value("${spring.datasource.mysql.hikari.pool-name:Mysql_Retail_HikariCP}") String poolName,
                                      @Value("${spring.datasource.mysql.hikari.maximum-pool-size:20}") int maximumPoolSize,
                                      @Value("${spring.datasource.mysql.hikari.minimum-idle:10}") int minimumIdle,
                                      @Value("${spring.datasource.mysql.hikari.idle-timeout:30000}") long idleTimeout,
                                      @Value("${spring.datasource.mysql.hikari.connection-timeout:30000}") long connectionTimeout,
                                      @Value("${spring.datasource.mysql.hikari.max-lifetime:1800000}") long maxLifetime) {
        // 连接池配置
        HikariDataSource dataSource = buildHikariDataSource(driverClassName, url, username, password,poolName, maximumPoolSize, minimumIdle, idleTimeout, connectionTimeout);

        dataSource.setMaxLifetime(maxLifetime);
        dataSource.setInitializationFailTimeout(1);  // 设置为1ms，如果连接失败则快速失败
        dataSource.setConnectionTestQuery("SELECT 1"); // 简单的连接测试查询
        dataSource.setAutoCommit(true);

        log.info("MysqlDataSource init completed");
        return dataSource;
    }

    @Bean("sqlSessionFactory")
    public SqlSessionFactoryBean sqlSessionFactory(@Qualifier("mysqlDataSource") DataSource mysqlDataSource) throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(mysqlDataSource);

        // 设置MyBatis配置文件位置（允许不存在）
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource configResource = resolver.getResource("classpath:/mybatis/config/mybatis-config.xml");
            if (configResource.exists()) {
                sqlSessionFactoryBean.setConfigLocation(configResource);
                log.info("Loaded MyBatis configuration from: classpath:/mybatis/config/mybatis-config.xml");
            } else {
                log.info("MyBatis configuration file not found, using default configuration");
            }
        } catch (Exception e) {
            log.warn("MyBatis configuration file not found, proceeding without configuration");
        }

        // 处理Mapper XML文件位置，允许为空
        try {
            Resource[] mapperResources = resolver.getResources("classpath:/mybatis/mapper/*.xml");
            if (mapperResources.length > 0) {
                sqlSessionFactoryBean.setMapperLocations(mapperResources);
                log.info("Loaded {} MyBatis mapper files", mapperResources.length);
            } else {
                log.info("No MyBatis mapper files found in classpath:/mybatis/mapper/");
            }
        } catch (Exception e) {
            log.warn("MyBatis mapper directory not found, proceeding without mapper files");
        }

        log.info("SqlSessionFactory init completed");
        return sqlSessionFactoryBean;
    }

    @Bean("sqlSessionTemplate")
    public SqlSessionTemplate sqlSessionTemplate(@Qualifier("sqlSessionFactory") SqlSessionFactoryBean sqlSessionFactory) throws Exception {
        log.info("SqlSessionTemplate init completed");
        return new SqlSessionTemplate(Objects.requireNonNull(sqlSessionFactory.getObject()));
    }





    // ==========================================  pgVector   ==========================================
    @Bean("pgVectorDataSource")
    public DataSource pgVectorDataSource(@Value("${spring.datasource.pgvector.driver-class-name:org.postgresql.Driver}") String driverClassName,
                                         @Value("${spring.datasource.pgvector.url}") String url,
                                         @Value("${spring.datasource.pgvector.username}") String username,
                                         @Value("${spring.datasource.pgvector.password}") String password,
                                         @Value("${spring.datasource.pgvector.hikari.pool-name:PgVector_Retail_HikariCP}") String poolName,
                                         @Value("${spring.datasource.pgvector.hikari.maximum-pool-size:5}") int maximumPoolSize,
                                         @Value("${spring.datasource.pgvector.hikari.minimum-idle:2}") int minimumIdle,
                                         @Value("${spring.datasource.pgvector.hikari.idle-timeout:30000}") long idleTimeout,
                                         @Value("${spring.datasource.pgvector.hikari.connection-timeout:30000}") long connectionTimeout) {
        // 连接池配置
        HikariDataSource dataSource = buildHikariDataSource(driverClassName, url, username, password, poolName, maximumPoolSize, minimumIdle, idleTimeout, connectionTimeout);

        // 确保在启动时连接数据库
        dataSource.setInitializationFailTimeout(1);  // 设置为1ms，如果连接失败则快速失败
        dataSource.setConnectionTestQuery("SELECT 1"); // 简单的连接测试查询
        dataSource.setAutoCommit(true);

        log.info("PgVectorDataSource init completed");
        return dataSource;
    }

    @Bean("pgVectorJdbcTemplate")
    public JdbcTemplate pgVectorJdbcTemplate(@Qualifier("pgVectorDataSource") DataSource dataSource) {
        log.info("PgVectorJdbcTemplate init completed");
        return new JdbcTemplate(dataSource);
    }


    private HikariDataSource buildHikariDataSource(String driverClassName, String url, String username, String password, String poolName, int maximumPoolSize, int minimumIdle, long idleTimeout, long connectionTimeout) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setPoolName(poolName);

        dataSource.setMaximumPoolSize(maximumPoolSize);
        dataSource.setMinimumIdle(minimumIdle);
        dataSource.setIdleTimeout(idleTimeout);
        dataSource.setConnectionTimeout(connectionTimeout);

        return dataSource;
    }

}
