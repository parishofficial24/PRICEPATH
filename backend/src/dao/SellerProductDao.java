package dao;

import app.Database;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SellerProductDao {
    public List<Map<String, Object>> getOffers(int productId) throws SQLException {
        List<Map<String, Object>> offers = new ArrayList<>();
        String sql = "SELECT sp.SellerProductID, sp.ProductID, sp.SellerID, s.SellerName, s.Rating, " +
            "sp.Price, sp.Discount, sp.DeliveryCharge, sp.PlatformFee, sp.DeliveryDays, sp.StockStatus " +
            "FROM SellerProducts sp JOIN Sellers s ON sp.SellerID=s.SellerID WHERE sp.ProductID=? " +
            "ORDER BY (sp.Price-sp.Discount+sp.DeliveryCharge+sp.PlatformFee) ASC";
        try (Connection con = Database.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal price = rs.getBigDecimal("Price");
                    BigDecimal discount = rs.getBigDecimal("Discount");
                    BigDecimal delivery = rs.getBigDecimal("DeliveryCharge");
                    BigDecimal fee = rs.getBigDecimal("PlatformFee");
                    BigDecimal discounted = price.subtract(discount).max(BigDecimal.ZERO);
                    BigDecimal total = discounted.add(delivery).add(fee).setScale(2, RoundingMode.HALF_UP);
                    Map<String,Object> offer = new LinkedHashMap<>();
                    offer.put("sellerProductId", rs.getInt("SellerProductID"));
                    offer.put("productId", rs.getInt("ProductID"));
                    offer.put("sellerId", rs.getInt("SellerID"));
                    offer.put("sellerName", rs.getString("SellerName"));
                    offer.put("sellerRating", rs.getBigDecimal("Rating"));
                    offer.put("price", price); offer.put("discount", discount);
                    offer.put("discountedPrice", discounted.setScale(2, RoundingMode.HALF_UP));
                    offer.put("deliveryCharge", delivery); offer.put("platformFee", fee);
                    offer.put("deliveryDays", rs.getInt("DeliveryDays"));
                    String stock = rs.getString("StockStatus"); offer.put("stockStatus", stock);
                    offer.put("eligible", !"out_of_stock".equalsIgnoreCase(stock));
                    offer.put("finalPrice", total); offers.add(offer);
                }
            }
        }
        return offers;
    }
}
