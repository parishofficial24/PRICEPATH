
package dao;

import app.Database;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class OrderDao {

    public int createOrder(
            int userId,
            int listId,
            BigDecimal productCost,
            BigDecimal discount,
            BigDecimal delivery,
            BigDecimal platformFee,
            BigDecimal finalTotal
    ) throws SQLException {

        String sql = "INSERT INTO Orders " +
                "(UserID, ShoppingListID, TotalProductCost, TotalDiscount, " +
                "TotalDeliveryCharge, TotalPlatformFee, FinalTotal) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            p.setInt(1, userId);
            p.setInt(2, listId);
            p.setBigDecimal(3, productCost);
            p.setBigDecimal(4, discount);
            p.setBigDecimal(5, delivery);
            p.setBigDecimal(6, platformFee);
            p.setBigDecimal(7, finalTotal);

            int rows = p.executeUpdate();

            if (rows == 0) {
                throw new SQLException("Order could not be saved.");
            }

            try (ResultSet keys = p.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }

            throw new SQLException("Order saved, but its ID could not be retrieved.");
        }
    }

    public List<Map<String, Object>> getOrders(int userId)
            throws SQLException {

        List<Map<String, Object>> out = new ArrayList<>();

        String sql = "SELECT o.OrderID, o.ShoppingListID, sl.ListName, " +
                "o.TotalProductCost, o.TotalDiscount, " +
                "o.TotalDeliveryCharge, o.TotalPlatformFee, " +
                "o.FinalTotal, o.CreatedAt " +
                "FROM Orders o " +
                "LEFT JOIN ShoppingLists sl " +
                "ON sl.ShoppingListID = o.ShoppingListID " +
                "WHERE o.UserID = ? " +
                "ORDER BY o.OrderID DESC";

        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1, userId);

            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();

                    m.put("orderId", r.getInt("OrderID"));
                    m.put("shoppingListId", r.getObject("ShoppingListID"));
                    m.put("listName", r.getString("ListName"));
                    m.put("totalProductCost", r.getBigDecimal("TotalProductCost"));
                    m.put("totalDiscount", r.getBigDecimal("TotalDiscount"));
                    m.put("totalDeliveryCharge", r.getBigDecimal("TotalDeliveryCharge"));
                    m.put("totalPlatformFee", r.getBigDecimal("TotalPlatformFee"));
                    m.put("finalTotal", r.getBigDecimal("FinalTotal"));

                    Timestamp createdAt = r.getTimestamp("CreatedAt");
                    m.put("createdAt",
                            createdAt == null ? null : createdAt.toString());

                    out.add(m);
                }
            }
        }

        return out;
    }
}
