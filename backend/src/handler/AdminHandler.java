
package handler;

import app.Database;
import app.JsonUtil;
import app.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdminHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String token = exchange.getRequestHeaders()
            .getFirst("X-Session-Token");

        Integer userId = SessionManager.getUserId(token);

        if (userId == null) {
            JsonResponse.send(exchange, 401,
                JsonResponse.error("Please log in first."));
            return;
        }

        try (Connection con = Database.getConnection()) {
            String roleSql = "SELECT Email FROM Users WHERE UserID = ?";

            try (PreparedStatement ps = con.prepareStatement(roleSql)) {
                ps.setInt(1, userId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()
                            || !app.Database.ADMIN_EMAIL.equalsIgnoreCase(rs.getString("Email"))) {
                        JsonResponse.send(exchange, 403,
                            JsonResponse.error("Admin access required."));
                        return;
                    }
                }
            }

            int products = count(con, "Products");
            int sellers = count(con, "Sellers");

            String json = "{\"productCount\":" + products
                + ",\"sellerCount\":" + sellers + "}";

            JsonResponse.send(exchange, 200, json);

        } catch (SQLException e) {
            JsonResponse.send(exchange, 500,
                JsonResponse.error("Could not load admin dashboard."));
        }
    }

    private int count(Connection con, String table)
            throws SQLException {
        // Table names are fixed constants supplied by this class.
        String sql = "SELECT COUNT(*) FROM " + table;

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
