package dao;

import app.Database;
import model.Product;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {
    public List<Product> search(String keyword, String category) throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT ProductID, Name, Category, Brand, Description FROM Products " +
            "WHERE (? = '' OR Name LIKE ? OR Brand LIKE ? OR Category LIKE ?) " +
            "AND (? = '' OR Category = ?) ORDER BY Name";
        try (Connection con = Database.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            String term = "%" + (keyword == null ? "" : keyword.trim()) + "%";
            String cat = category == null ? "" : category.trim();
            ps.setString(1, keyword == null ? "" : keyword.trim());
            ps.setString(2, term); ps.setString(3, term); ps.setString(4, term);
            ps.setString(5, cat); ps.setString(6, cat);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) products.add(new Product(rs.getInt("ProductID"), rs.getString("Name"), rs.getString("Category"), rs.getString("Brand"), rs.getString("Description")));
            }
        }
        return products;
    }
    public List<Product> search(String keyword) throws SQLException { return search(keyword, ""); }
    public Product findById(int productId) throws SQLException {
        String sql = "SELECT ProductID, Name, Category, Brand, Description FROM Products WHERE ProductID = ?";
        try (Connection con = Database.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new Product(rs.getInt("ProductID"), rs.getString("Name"), rs.getString("Category"), rs.getString("Brand"), rs.getString("Description"));
            }
        }
        return null;
    }
}
