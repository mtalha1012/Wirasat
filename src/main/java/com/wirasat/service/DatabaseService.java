package com.wirasat.service;

import com.wirasat.util.LoadProperties;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

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

    private Connection connection;
    private static DatabaseService instance;

    // Constructor
    private DatabaseService() {
        try {
            openConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to open database connection", e);
        }
    }

    public static DatabaseService getInstance() {
        return Objects.requireNonNullElseGet(instance, () ->
                instance = new DatabaseService());
    }

    public Connection getConnection() {
        return connection;
    }

    private void openConnection() throws SQLException {
        connection = DriverManager.getConnection(
                DB_PROPERTIES.getProperty("db.url"),
                DB_PROPERTIES.getProperty("db.user"),
                DB_PROPERTIES.getProperty("db.password"));
    }

    public void closeConnection() throws SQLException {
        connection.close();
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
