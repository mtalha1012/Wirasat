package com.wirasat.service;

import com.wirasat.util.LoadProperties;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseService {
    // Attributes
    private static final ClassLoader classLoader = DatabaseService.class.getClassLoader();
    private static final Properties DB_PROPERTIES = LoadProperties.loadProperties(
            "db.properties", classLoader);
//    private static final Properties FAMILY_QUERIES = LoadProperties.loadProperties(
//            "family-queries.properties", classLoader);
//    private static final Properties ASSET_QUERIES = LoadProperties.loadProperties(
//            "asset-queries.properties", classLoader);
//    private static final Properties LIABILITY_QUERIES = LoadProperties.loadProperties(
//            "liability-queries.properties", classLoader);
//    private static final Properties WASIYAT_QUERIES = LoadProperties.loadProperties(
//            "wasiyat-queries.properties", classLoader);

    private HikariDataSource dataSource;
    private static DatabaseService instance;

    // Constructor
    private DatabaseService() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DB_PROPERTIES.getProperty("db.url"));
        config.setUsername(DB_PROPERTIES.getProperty("db.user"));
        config.setPassword(DB_PROPERTIES.getProperty("db.password"));
        
        // Optimizations for MySQL
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        // Pool configurations
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setMaxLifetime(1800000); // 30 minutes
        config.setConnectionTimeout(30000);

        dataSource = new HikariDataSource(config);
    }

    public static DatabaseService getInstance() {
        return Objects.requireNonNullElseGet(instance, () ->
                instance = new DatabaseService());
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void closeConnection() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private String getQuery(Properties queries, String key) {
        String query = queries.getProperty(key);
        if (query == null) throw new RuntimeException("Query Not found: " + key);
        return query;
    }

//    public String getFamilyQuery(String key) {
//        return getQuery(FAMILY_QUERIES, key);
//    }
//
//    public String getAssetQuery(String key) {
//        return getQuery(ASSET_QUERIES, key);
//    }
//
//    public String getLiabilityQuery(String key) {
//        return getQuery(LIABILITY_QUERIES, key);
//    }
//
//    public String getWasiyatQuery(String key) {
//        return getQuery(WASIYAT_QUERIES, key);
//    }
}
