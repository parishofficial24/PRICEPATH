
package dao;

import app.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ShoppingListDao {

    public int createList(int userId, String listName)
            throws SQLException {

        String sql =
            "INSERT INTO ShoppingLists (UserID, ListName) VALUES (?, ?)";

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, userId);
            ps.setString(2, listName);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        throw new SQLException("Could not create shopping list.");
    }

    public List<Map<String, Object>> getLists(int userId)
            throws SQLException {

        List<Map<String, Object>> lists = new ArrayList<>();

        String sql =
            "SELECT ShoppingListID, ListName FROM ShoppingLists "
          + "WHERE UserID = ? ORDER BY ShoppingListID DESC";

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("listId", rs.getInt("ShoppingListID"));
                    item.put("listName", rs.getString("ListName"));
                    lists.add(item);
                }
            }
        }

        return lists;
    }

    public boolean addItem(int listId, int productId, int quantity)
            throws SQLException {

        if (quantity < 1) {
            throw new IllegalArgumentException(
                "Quantity must be at least 1."
            );
        }

        String sql =
            "INSERT INTO ShoppingListItems "
          + "(ShoppingListID, ProductID, Quantity) VALUES (?, ?, ?) "
          + "ON DUPLICATE KEY UPDATE Quantity = Quantity + VALUES(Quantity)";

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, listId);
            ps.setInt(2, productId);
            ps.setInt(3, quantity);

            return ps.executeUpdate() > 0;
        }
    }

    public boolean removeItem(int listId, int productId)
            throws SQLException {

        String sql =
            "DELETE FROM ShoppingListItems "
          + "WHERE ShoppingListID = ? AND ProductID = ?";

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, listId);
            ps.setInt(2, productId);

            return ps.executeUpdate() > 0;
        }
    }
}
