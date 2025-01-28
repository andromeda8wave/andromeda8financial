package com.financialapp.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseConnection sets up a file-based H2 database
 * so data persists across application restarts.
 */
public class DatabaseConnection {

    // File-based H2 URL (relative path: ./db/financialdb)
    private static final String JDBC_URL = "jdbc:h2:./db/financialdb;";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    static {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialize the database schema if needed.
     * No demo data inserted.
     */
    public static void initDatabase() {
        // Optional: If you only create tables once, you can do it here.
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Create Categories table
            stmt.execute("CREATE TABLE IF NOT EXISTS categories (" +
                         " id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                         " name VARCHAR(255) NOT NULL," +
                         " type VARCHAR(50) NOT NULL" + // "INCOME" or "EXPENSE"
                         ")");

            // Create Subcategories table
            stmt.execute("CREATE TABLE IF NOT EXISTS subcategories (" +
                         " id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                         " category_id BIGINT NOT NULL," +
                         " name VARCHAR(255) NOT NULL," +
                         " FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE" +
                         ")");

            // Create Currencies table
            stmt.execute("CREATE TABLE IF NOT EXISTS currencies (" +
                         " id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                         " code VARCHAR(10) NOT NULL," +
                         " name VARCHAR(100) NOT NULL" +
                         ")");

            // Create Wallets table
            stmt.execute("CREATE TABLE IF NOT EXISTS wallets (" +
                         " id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                         " name VARCHAR(255) NOT NULL" +
                         ")");

            // Create Transactions table (subcategory_id is nullable!)
            stmt.execute("CREATE TABLE IF NOT EXISTS transactions (" +
                         " id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                         " date DATE NOT NULL," +
                         " category_id BIGINT NOT NULL," +
                         " subcategory_id BIGINT NULL," +  // ALLOWS NULL
                         " amount DECIMAL(15,2) NOT NULL," +
                         " currency_id BIGINT NOT NULL," +
                         " wallet_id BIGINT NOT NULL," +
                         " comment VARCHAR(255)," +
                         " FOREIGN KEY (category_id) REFERENCES categories(id)," +
                         " FOREIGN KEY (subcategory_id) REFERENCES subcategories(id)," +
                         " FOREIGN KEY (currency_id) REFERENCES currencies(id)," +
                         " FOREIGN KEY (wallet_id) REFERENCES wallets(id)" +
                         ")");

            // Create Plans table
            stmt.execute("CREATE TABLE IF NOT EXISTS plans (" +
                         " id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                         " category_id BIGINT NOT NULL," +
                         " plan_amount DECIMAL(15,2) NOT NULL," +
                         " plan_month INT NOT NULL," +  // 1..12
                         " plan_year INT NOT NULL," +
                         " FOREIGN KEY (category_id) REFERENCES categories(id)" +
                         ")");

            System.out.println("Database initialized or already existing (file-based).");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns a connection to our file-based H2 database.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
    }
}