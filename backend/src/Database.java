
package app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// Manages database connection settings and creates JDBC connections.
public class Database {

    public static final String DB_URL = "jdbc:mysql://localhost:3306/pricepath_new?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata";
    public static final String DB_USER = "root";
    public static final String DB_PASSWORD = System.getenv("PRICEPATH_DB_PASSWORD") == null ? "" : System.getenv("PRICEPATH_DB_PASSWORD");
    public static final String ADMIN_EMAIL = "admin@pricepath.com";
    public static final String FRONTEND_FOLDER = "../frontend";
    public static final int PORT = 8080;

    // Opens and returns a new JDBC database connection.
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}
