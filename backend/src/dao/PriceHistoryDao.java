
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

public class PriceHistoryDao {

    public List<Map<String, Object>> getHistory(int sellerProductId)
            throws SQLException {

        List<Map<String, Object>> history = new ArrayList<>();

        String sql =
            "SELECT Price, Discount, DeliveryCharge, PlatformFee, "
          + "RecordedAt FROM PriceHistory "
          + "WHERE SellerProductID = ? "
          + "ORDER BY RecordedAt ASC";

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, sellerProductId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> entry = new LinkedHashMap<>();

                    entry.put("price", rs.getDouble("Price"));
                    entry.put("discount", rs.getDouble("Discount"));
                    entry.put("deliveryCharge",
                              rs.getDouble("DeliveryCharge"));
                    entry.put("platformFee", rs.getDouble("PlatformFee"));
                    entry.put("recordedAt",
                              rs.getTimestamp("RecordedAt").toString());

                    history.add(entry);
                }
            }
        }

        return history;
    }
}
