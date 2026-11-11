
package dao;

import app.Database;
import model.Seller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SellerDao {

    public List<Seller> getAllSellers() throws SQLException {
        List<Seller> sellers = new ArrayList<>();

        String sql = "SELECT SellerID, SellerName, Rating, "
                   + "DeliveryDaysAvg FROM Sellers "
                   + "ORDER BY SellerName";

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                sellers.add(new Seller(
                    rs.getInt("SellerID"),
                    rs.getString("SellerName"),
                    rs.getDouble("Rating"),
                    rs.getInt("DeliveryDaysAvg")
                ));
            }
        }

        return sellers;
    }
}
