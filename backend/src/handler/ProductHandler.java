
package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.ProductDao;
import dao.SellerProductDao;
import model.Product;
import app.JsonUtil;

import java.io.IOException;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ProductHandler implements HttpHandler {

    private final ProductDao productDao = new ProductDao();
    private final SellerProductDao offerDao = new SellerProductDao();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonResponse.send(exchange, 405,
                JsonResponse.error("GET method required."));
            return;
        }

        String path = exchange.getRequestURI().getPath();

        try {
            if (path.equals("/api/categories")) {
                sendCategories(exchange);
            } else if (path.matches("/api/products/\\d+/(offers|compare)")) {
                int id = Integer.parseInt(
                    path.split("/")[3]
                );
                sendOffers(exchange, id);
            } else if (path.matches("/api/products/\\d+")) {
                int id = Integer.parseInt(
                    path.substring(path.lastIndexOf('/') + 1)
                );
                sendProduct(exchange, id);
            } else {
                Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
                sendProducts(exchange, params.getOrDefault("q", ""), params.getOrDefault("category", ""));
            }
        } catch (SQLException | NumberFormatException e) {
            JsonResponse.send(exchange, 500,
                JsonResponse.error("Could not retrieve products."));
        }
    }

    private void sendCategories(HttpExchange exchange) throws SQLException, IOException {
        java.util.List<String> categories = new java.util.ArrayList<>();
        String sql = "SELECT DISTINCT Category FROM Products WHERE Category IS NOT NULL ORDER BY Category";
        try (java.sql.Connection con = app.Database.getConnection();
             java.sql.PreparedStatement ps = con.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) categories.add(rs.getString(1));
        }
        StringBuilder json = new StringBuilder("[");
        for (String category : categories) {
            if (json.length() > 1) json.append(",");
            json.append(JsonUtil.quote(category));
        }
        json.append("]");
        JsonResponse.send(exchange, 200, json.toString());
    }

    private Map<String, String> parseQuery(String rawQuery) throws IOException {
        Map<String, String> params = new java.util.HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) return params;
        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], "UTF-8");
            String value = parts.length > 1 ? URLDecoder.decode(parts[1], "UTF-8") : "";
            params.put(key, value);
        }
        return params;
    }

    private void sendProducts(HttpExchange exchange, String keyword, String category)
            throws SQLException, IOException {
        List<Product> products = productDao.search(keyword, category);
        StringBuilder json = new StringBuilder("[");

        for (Product p : products) {
            if (json.length() > 1) json.append(",");

            json.append("{")
                .append("\"productId\":").append(p.getProductId()).append(",")
                .append("\"name\":").append(JsonUtil.quote(p.getName())).append(",")
                .append("\"category\":").append(JsonUtil.quote(p.getCategory())).append(",")
                .append("\"brand\":").append(JsonUtil.quote(p.getBrand())).append(",")
                .append("\"description\":").append(JsonUtil.quote(p.getDescription())).append(",")
                .append("\"offerCount\":").append(offerDao.getOffers(p.getProductId()).size()).append(",")
                .append("\"lowestFinalCost\":").append(lowestEligibleCost(p.getProductId()))
                .append("}");
        }

        json.append("]");
        JsonResponse.send(exchange, 200, json.toString());
    }

    private String lowestEligibleCost(int productId) throws SQLException {
        java.util.List<java.util.Map<String, Object>> offers = offerDao.getOffers(productId);
        java.math.BigDecimal lowest = null;
        for (java.util.Map<String, Object> offer : offers) {
            if (Boolean.TRUE.equals(offer.get("eligible"))) {
                java.math.BigDecimal cost = (java.math.BigDecimal) offer.get("finalPrice");
                if (lowest == null || cost.compareTo(lowest) < 0) lowest = cost;
            }
        }
        return lowest == null ? "null" : lowest.toPlainString();
    }

    private void sendProduct(HttpExchange exchange, int id)
            throws SQLException, IOException {
        Product p = productDao.findById(id);

        if (p == null) {
            JsonResponse.send(exchange, 404,
                JsonResponse.error("Product not found."));
            return;
        }

        String json = "{"
            + "\"productId\":" + p.getProductId() + ","
            + "\"name\":" + JsonUtil.quote(p.getName()) + ","
            + "\"category\":" + JsonUtil.quote(p.getCategory()) + ","
            + "\"brand\":" + JsonUtil.quote(p.getBrand()) + ","
            + "\"description\":" + JsonUtil.quote(p.getDescription())
            + "}";

        JsonResponse.send(exchange, 200, json);
    }

    private void sendOffers(HttpExchange exchange, int id)
            throws SQLException, IOException {
        Product product = productDao.findById(id);
        if (product == null) {
            JsonResponse.send(exchange, 404, JsonResponse.error("Product not found."));
            return;
        }
        List<Map<String, Object>> offers = offerDao.getOffers(id);
        Integer lowest = null, fastest = null, bestValue = null;
        java.math.BigDecimal lowestCost = null;
        Integer fastestDays = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Map<String,Object> o : offers) {
            if (!Boolean.TRUE.equals(o.get("eligible"))) continue;
            int offerId = ((Number)o.get("sellerProductId")).intValue();
            java.math.BigDecimal cost = (java.math.BigDecimal)o.get("finalPrice");
            int days = ((Number)o.get("deliveryDays")).intValue();
            double rating = ((java.math.BigDecimal)o.get("sellerRating")).doubleValue();
            if (lowestCost == null || cost.compareTo(lowestCost) < 0) { lowestCost = cost; lowest = offerId; }
            if (fastestDays == null || days < fastestDays) { fastestDays = days; fastest = offerId; }
            double score = rating * 2.0 - days * 0.5 - cost.doubleValue() / 10000.0;
            if (score > bestScore) { bestScore = score; bestValue = offerId; }
        }
        StringBuilder json = new StringBuilder("{\"product\":{");
        json.append("\"productId\":").append(product.getProductId())
            .append(",\"name\":").append(JsonUtil.quote(product.getName()))
            .append(",\"brand\":").append(JsonUtil.quote(product.getBrand()))
            .append(",\"category\":").append(JsonUtil.quote(product.getCategory()))
            .append(",\"description\":").append(JsonUtil.quote(product.getDescription()))
            .append("},\"offers\":[");
        for (int i=0;i<offers.size();i++) {
            if (i>0) json.append(',');
            Map<String,Object> o=offers.get(i);
            java.math.BigDecimal price=(java.math.BigDecimal)o.get("price");
            java.math.BigDecimal discount=(java.math.BigDecimal)o.get("discount");
            java.math.BigDecimal discounted=(java.math.BigDecimal)o.get("discountedPrice");
            java.math.BigDecimal finalPrice=(java.math.BigDecimal)o.get("finalPrice");
            double pct=price.signum()==0?0:discount.multiply(new java.math.BigDecimal("100")).divide(price,2,java.math.RoundingMode.HALF_UP).doubleValue();
            json.append("{\"sellerProductId\":").append(o.get("sellerProductId"))
                .append(",\"productId\":").append(o.get("productId"))
                .append(",\"sellerId\":").append(o.get("sellerId"))
                .append(",\"sellerName\":").append(JsonUtil.quote(o.get("sellerName").toString()))
                .append(",\"sellerRating\":").append(o.get("sellerRating"))
                .append(",\"price\":").append(price.toPlainString())
                .append(",\"discountAmount\":").append(discount.toPlainString())
                .append(",\"discountPercent\":").append(pct)
                .append(",\"discountedPrice\":").append(discounted.toPlainString())
                .append(",\"deliveryCharge\":").append(o.get("deliveryCharge"))
                .append(",\"platformFee\":").append(o.get("platformFee"))
                .append(",\"deliveryDays\":").append(o.get("deliveryDays"))
                .append(",\"stockStatus\":").append(JsonUtil.quote(o.get("stockStatus").toString()))
                .append(",\"eligible\":").append(o.get("eligible"))
                .append(",\"finalCost\":").append(finalPrice.toPlainString()).append('}');
        }
        json.append("],\"picks\":{\"lowestCost\":").append(lowest==null?"null":lowest)
            .append(",\"fastest\":").append(fastest==null?"null":fastest)
            .append(",\"bestValue\":").append(bestValue==null?"null":bestValue).append("}}");
        JsonResponse.send(exchange, 200, json.toString());
    }
}
