package dao;

import app.Database;
import app.PasswordUtil;
import model.User;
import java.sql.*;

public class UserDao {
    public boolean register(String name, String email, String password) throws SQLException {
        String sql = "INSERT INTO Users (Name, Email, Password) VALUES (?, ?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, name);
            p.setString(2, email);
            p.setString(3, PasswordUtil.hashPassword(password));
            return p.executeUpdate() == 1;
        }
    }

    public User login(String email, String password) throws SQLException {
        String sql = "SELECT UserID, Name, Email, Password FROM Users WHERE Email = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, email);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next() || !PasswordUtil.verifyPassword(password, r.getString("Password"))) return null;
                String role = email.equalsIgnoreCase(Database.ADMIN_EMAIL) ? "admin" : "user";
                return new User(r.getInt("UserID"), r.getString("Name"), r.getString("Email"), null, role);
            }
        }
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT UserID, Name, Email, Password FROM Users WHERE Email = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, email);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return null;
                String role = email.equalsIgnoreCase(Database.ADMIN_EMAIL) ? "admin" : "user";
                return new User(r.getInt("UserID"), r.getString("Name"), r.getString("Email"), r.getString("Password"), role);
            }
        }
    }

    public User findById(int userId) throws SQLException {
        String sql = "SELECT UserID, Name, Email FROM Users WHERE UserID = ?";
        try (Connection con = Database.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new User(rs.getInt("UserID"), rs.getString("Name"), rs.getString("Email"), null, rs.getString("Email").equalsIgnoreCase(Database.ADMIN_EMAIL) ? "admin" : "user");
            }
        }
        return null;
    }
}
