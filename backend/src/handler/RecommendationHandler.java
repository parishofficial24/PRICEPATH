
package handler;

import app.Database;
import dao.OrderDao;
import app.JsonUtil;
import app.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public class RecommendationHandler implements HttpHandler {

    private static final List<String> TYPES = Arrays.asList(
        "Lowest total cost",
        "Fastest delivery",
        "Best overall value"
    );

    private static class Item {
        int productId;
        int quantity;
        String name;
        List<Offer> offers = new ArrayList<>();
    }

    private static class Offer {
        int sellerProductId;
        String seller;
        int days;
        BigDecimal rating;
        BigDecimal price;
        BigDecimal discount;
        BigDecimal delivery;
        BigDecimal fee;
        BigDecimal total;
    }

    private static class Plan {
        String type;
        BigDecimal productCost = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal delivery = BigDecimal.ZERO;
        BigDecimal fee = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        int maxDays;
        double ratingSum;
        Set<String> sellers = new HashSet<>();
        List<Item> items = new ArrayList<>();
        List<Offer> selected = new ArrayList<>();
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {

        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();

        Integer userId = SessionManager.getUserId(
            ex.getRequestHeaders().getFirst("X-Session-Token"));

        if (userId == null) {
            JsonResponse.send(ex, 401,
                JsonResponse.error("Please log in first."));
            return;
        }

        try {
            if ("GET".equalsIgnoreCase(method)) {
                Map<String, String> query =
                    parse(ex.getRequestURI().getRawQuery());

                if ("/api/recommendations/saved".equals(path)) {
                    sendSaved(ex, userId);
                    return;
                }

                if ("/api/recommendations".equals(path)
                        && query.containsKey("listId")) {
                    int listId = positiveInt(query.get("listId"));
                    sendPlans(ex, userId, listId);
                    return;
                }

                if ("/api/recommendations".equals(path)) {
                    JsonResponse.send(ex, 200, "[]");
                    return;
                }

                JsonResponse.send(ex, 404,
                    JsonResponse.error("Endpoint not found."));
                return;
            }

            if ("POST".equalsIgnoreCase(method)
                    && "/api/recommendations/save".equals(path)) {

                Map<String, String> form = readForm(ex);
                int listId = positiveInt(form.get("listId"));
                String type = form.get("planType");

                if (!TYPES.contains(type)) {
                    JsonResponse.send(ex, 400,
                        JsonResponse.error("Invalid plan type."));
                    return;
                }

                saveRecommendation(ex, userId, listId, type);
                return;
            }

            JsonResponse.send(ex, 405,
                JsonResponse.error("Unsupported recommendations endpoint."));

        } catch (NumberFormatException e) {
            JsonResponse.send(ex, 400,
                JsonResponse.error("Invalid or missing numeric input."));
        } catch (SQLException e) {
            JsonResponse.send(ex, 500,
                JsonResponse.error(
                    "Database operation failed. Check the database connection and schema."));
        }
    }

    private int positiveInt(String value) {
        int n = Integer.parseInt(value);
        if (n <= 0) {
            throw new NumberFormatException();
        }
        return n;
    }

    private Map<String, String> parse(String raw) throws IOException {
        Map<String, String> m = new HashMap<>();

        if (raw != null) {
            for (String pair : raw.split("&")) {
                String[] a = pair.split("=", 2);
                m.put(
                    URLDecoder.decode(a[0], StandardCharsets.UTF_8),
                    a.length > 1
                        ? URLDecoder.decode(a[1], StandardCharsets.UTF_8)
                        : ""
                );
            }
        }
        return m;
    }

    private Map<String, String> readForm(HttpExchange ex)
            throws IOException {

        String body = new String(
            ex.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8
        );

        return parse(body);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private List<Item> loadItems(Connection c, int userId, int listId)
            throws SQLException {

        String check =
            "SELECT ListName FROM ShoppingLists " +
            "WHERE ShoppingListID=? AND UserID=?";

        try (PreparedStatement p = c.prepareStatement(check)) {
            p.setInt(1, listId);
            p.setInt(2, userId);

            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) {
                    throw new SQLException("Shopping list not found.");
                }
            }
        }

        List<Item> items = new ArrayList<>();

        String sql =
            "SELECT p.ProductID, p.Name, sli.Quantity " +
            "FROM ShoppingListItems sli " +
            "JOIN Products p ON p.ProductID=sli.ProductID " +
            "WHERE sli.ShoppingListID=? ORDER BY p.Name";

        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, listId);

            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    Item it = new Item();
                    it.productId = r.getInt("ProductID");
                    it.name = r.getString("Name");
                    it.quantity = r.getInt("Quantity");

                    if (it.quantity <= 0) {
                        throw new SQLException("Invalid item quantity.");
                    }

                    items.add(it);
                }
            }
        }

        for (Item it : items) {
            String sqlOffers =
                "SELECT sp.SellerProductID, s.SellerName, s.Rating, " +
                "sp.Price, sp.Discount, sp.DeliveryCharge, " +
                "sp.PlatformFee, sp.DeliveryDays " +
                "FROM SellerProducts sp " +
                "JOIN Sellers s ON s.SellerID=sp.SellerID " +
                "WHERE sp.ProductID=? " +
                "AND sp.StockStatus<>'out_of_stock'";

            try (PreparedStatement p = c.prepareStatement(sqlOffers)) {
                p.setInt(1, it.productId);

                try (ResultSet r = p.executeQuery()) {
                    while (r.next()) {
                        Offer o = new Offer();

                        o.sellerProductId =
                            r.getInt("SellerProductID");
                        o.seller = r.getString("SellerName");
                        o.rating = r.getBigDecimal("Rating");
                        o.price = r.getBigDecimal("Price");
                        o.discount = r.getBigDecimal("Discount");
                        o.delivery = r.getBigDecimal("DeliveryCharge");
                        o.fee = r.getBigDecimal("PlatformFee");
                        o.days = r.getInt("DeliveryDays");

                        o.total = o.price.subtract(o.discount)
                            .max(BigDecimal.ZERO)
                            .add(o.delivery)
                            .add(o.fee);

                        it.offers.add(o);
                    }
                }
            }
        }

        return items;
    }

    private Offer choose(Item item, String type) {
        Offer chosen = item.offers.get(0);

        for (Offer o : item.offers) {
            boolean better;

            if ("Lowest total cost".equals(type)) {
                better = o.total.compareTo(chosen.total) < 0;
            } else if ("Fastest delivery".equals(type)) {
                better = o.days < chosen.days
                    || (o.days == chosen.days
                        && o.total.compareTo(chosen.total) < 0);
            } else {
                double score =
                    o.rating.doubleValue() * 2.0
                    - o.days * 0.25
                    - o.total.doubleValue() / 100000.0;

                double best =
                    chosen.rating.doubleValue() * 2.0
                    - chosen.days * 0.25
                    - chosen.total.doubleValue() / 100000.0;

                better = score > best;
            }

            if (better) {
                chosen = o;
            }
        }

        return chosen;
    }

    private Plan buildPlan(List<Item> items, String type) {

        Plan plan = new Plan();
        plan.type = type;

        for (Item it : items) {
            if (it.offers.isEmpty()) {
                return null;
            }

            Offer o = choose(it, type);
            BigDecimal qty = BigDecimal.valueOf(it.quantity);

            plan.productCost = plan.productCost.add(
                o.price.multiply(qty));

            plan.discount = plan.discount.add(
                o.discount.multiply(qty));

            // Delivery and platform fees are charged once per item line.
            plan.delivery = plan.delivery.add(o.delivery);
            plan.fee = plan.fee.add(o.fee);

            plan.total = plan.total.add(
                o.price.subtract(o.discount)
                    .max(BigDecimal.ZERO)
                    .multiply(qty)
            );

            plan.maxDays = Math.max(plan.maxDays, o.days);
            plan.ratingSum += o.rating.doubleValue();
            plan.sellers.add(o.seller);
            plan.items.add(it);
            plan.selected.add(o);
        }

        plan.total = plan.total.add(plan.delivery).add(plan.fee);
        plan.total = money(plan.total);
        plan.productCost = money(plan.productCost);
        plan.discount = money(plan.discount);
        plan.delivery = money(plan.delivery);
        plan.fee = money(plan.fee);

        return plan;
    }

    private List<Plan> buildPlans(List<Item> items) {
        List<Plan> plans = new ArrayList<>();

        for (String type : TYPES) {
            Plan p = buildPlan(items, type);
            if (p != null && !items.isEmpty()) {
                plans.add(p);
            }
        }

        return plans;
    }

    private void sendPlans(HttpExchange ex, int userId, int listId)
            throws IOException, SQLException {

        try (Connection c = Database.getConnection()) {
            String listName;

            try (PreparedStatement p = c.prepareStatement(
                    "SELECT ListName FROM ShoppingLists " +
                    "WHERE ShoppingListID=? AND UserID=?")) {
                p.setInt(1, listId);
                p.setInt(2, userId);

                try (ResultSet r = p.executeQuery()) {
                    if (!r.next()) {
                        JsonResponse.send(ex, 404,
                            JsonResponse.error("Shopping list not found."));
                        return;
                    }
                    listName = r.getString(1);
                }
            }

            List<Item> items = loadItems(c, userId, listId);
            List<Plan> plans = buildPlans(items);

            StringBuilder out = new StringBuilder();
            out.append("{\"listName\":")
                .append(JsonUtil.quote(listName))
                .append(",\"warnings\":[");

            boolean first = true;

            for (Item it : items) {
                if (it.offers.isEmpty()) {
                    if (!first) out.append(',');
                    out.append(JsonUtil.quote(
                        it.name + " has no in-stock seller offers."));
                    first = false;
                }
            }

            out.append("],\"plans\":[");

            for (int i = 0; i < plans.size(); i++) {
                if (i > 0) out.append(',');

                Plan p = plans.get(i);

                out.append("{\"type\":")
                    .append(JsonUtil.quote(p.type))
                    .append(",\"total\":").append(p.total)
                    .append(",\"savings\":0.00")
                    .append(",\"sellerCount\":").append(p.sellers.size())
                    .append(",\"deliveryDays\":").append(p.maxDays)
                    .append(",\"rating\":")
                    .append(String.format(Locale.US, "%.2f",
                        p.ratingSum / p.items.size()))
                    .append(",\"productCost\":").append(p.productCost)
                    .append(",\"totalDiscount\":").append(p.discount)
                    .append(",\"totalDeliveryCharge\":").append(p.delivery)
                    .append(",\"totalPlatformFee\":").append(p.fee)
                    .append(",\"items\":[");

                for (int j = 0; j < p.items.size(); j++) {
                    if (j > 0) out.append(',');

                    Item it = p.items.get(j);
                    Offer o = p.selected.get(j);

                    BigDecimal lineTotal = money(
                        o.price.subtract(o.discount)
                            .max(BigDecimal.ZERO)
                            .multiply(BigDecimal.valueOf(it.quantity))
                    );

                    out.append("{\"productName\":")
                        .append(JsonUtil.quote(it.name))
                        .append(",\"quantity\":").append(it.quantity)
                        .append(",\"sellerName\":")
                        .append(JsonUtil.quote(o.seller))
                        .append(",\"deliveryDays\":").append(o.days)
                        .append(",\"lineTotal\":").append(lineTotal)
                        .append('}');
                }

                out.append("]}");
            }

            out.append("]}");
            JsonResponse.send(ex, 200, out.toString());
        }
    }

    private void saveRecommendation(
            HttpExchange ex, int userId, int listId, String type)
            throws IOException, SQLException {

        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);

            try {
                List<Item> items = loadItems(c, userId, listId);
                Plan plan = buildPlan(items, type);

                if (plan == null) {
                    c.rollback();
                    JsonResponse.send(ex, 400,
                        JsonResponse.error(
                            "Every product needs an in-stock seller offer."));
                    return;
                }

                int recommendationId;

                String insert =
                    "INSERT INTO Recommendations " +
                    "(UserID, RecommendationType) VALUES (?, ?)";

                try (PreparedStatement p = c.prepareStatement(
                        insert, Statement.RETURN_GENERATED_KEYS)) {
                    p.setInt(1, userId);
                    p.setString(2, type);
                    p.executeUpdate();

                    try (ResultSet keys = p.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException(
                                "Could not retrieve recommendation ID.");
                        }
                        recommendationId = keys.getInt(1);
                    }
                }

                String insertItem =
                    "INSERT INTO RecommendationItems " +
                    "(RecommendationID, ProductID, SellerProductID, " +
                    "RecommendedPrice) VALUES (?, ?, ?, ?)";

                try (PreparedStatement p = c.prepareStatement(insertItem)) {
                    for (int i = 0; i < plan.items.size(); i++) {
                        Item it = plan.items.get(i);
                        Offer o = plan.selected.get(i);

                        p.setInt(1, recommendationId);
                        p.setInt(2, it.productId);
                        p.setInt(3, o.sellerProductId);
                        p.setBigDecimal(4, money(o.total));
                        p.addBatch();
                    }
                    p.executeBatch();
                }

                c.commit();

                JsonResponse.send(ex, 200,
                    "{\"success\":true,\"recommendationId\":"
                        + recommendationId + "}");

            } catch (Exception e) {
                c.rollback();

                if (e instanceof SQLException) {
                    throw (SQLException) e;
                }

                JsonResponse.send(ex, 500,
                    JsonResponse.error("Could not save recommendation."));
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private void sendSaved(HttpExchange ex, int userId)
            throws IOException, SQLException {

        String sql =
            "SELECT r.RecommendationID, r.RecommendationType, r.CreatedAt, " +
            "p.Name, s.SellerName, ri.RecommendedPrice " +
            "FROM Recommendations r " +
            "LEFT JOIN RecommendationItems ri " +
            "ON ri.RecommendationID=r.RecommendationID " +
            "LEFT JOIN Products p ON p.ProductID=ri.ProductID " +
            "LEFT JOIN SellerProducts sp " +
            "ON sp.SellerProductID=ri.SellerProductID " +
            "LEFT JOIN Sellers s ON s.SellerID=sp.SellerID " +
            "WHERE r.UserID=? ORDER BY r.RecommendationID DESC";

        Map<Integer, Map<String, Object>> saved = new LinkedHashMap<>();

        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1, userId);

            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    int id = r.getInt("RecommendationID");

                    Map<String, Object> entry = saved.get(id);

                    if (entry == null) {
                        entry = new LinkedHashMap<>();
                        entry.put("type",
                            r.getString("RecommendationType"));
                        entry.put("createdAt",
                            String.valueOf(r.getTimestamp("CreatedAt")));
                        entry.put("items", new ArrayList<Map<String, Object>>());
                        entry.put("itemsTotal", BigDecimal.ZERO);
                        saved.put(id, entry);
                    }

                    String productName = r.getString("Name");

                    if (productName != null) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> list =
                            (List<Map<String, Object>>) entry.get("items");

                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("productName", productName);
                        item.put("sellerName", r.getString("SellerName"));

                        BigDecimal price = r.getBigDecimal("RecommendedPrice");
                        item.put("recommendedPrice", price);
                        list.add(item);

                        entry.put("itemsTotal",
                            ((BigDecimal) entry.get("itemsTotal")).add(price));
                    }
                }
            }
        }

        StringBuilder out = new StringBuilder("[");
        boolean first = true;

        for (Map<String, Object> entry : saved.values()) {
            if (!first) out.append(',');
            first = false;

            out.append("{\"type\":")
                .append(JsonUtil.quote(String.valueOf(entry.get("type"))))
                .append(",\"createdAt\":")
                .append(JsonUtil.quote(String.valueOf(entry.get("createdAt"))))
                .append(",\"itemsTotal\":")
                .append(money((BigDecimal) entry.get("itemsTotal")))
                .append(",\"items\":[");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items =
                (List<Map<String, Object>>) entry.get("items");

            for (int i = 0; i < items.size(); i++) {
                if (i > 0) out.append(',');

                Map<String, Object> item = items.get(i);

                out.append("{\"productName\":")
                    .append(JsonUtil.quote(
                        String.valueOf(item.get("productName"))))
                    .append(",\"sellerName\":")
                    .append(JsonUtil.quote(
                        String.valueOf(item.get("sellerName"))))
                    .append(",\"recommendedPrice\":")
                    .append(item.get("recommendedPrice"))
                    .append('}');
            }

            out.append("]}");
        }

        out.append(']');
        JsonResponse.send(ex, 200, out.toString());
    }

public void saveOrderPlan(
        HttpExchange ex,
        int userId,
        int listId,
        String type
) throws IOException, SQLException {

    if (!TYPES.contains(type)) {
        JsonResponse.send(ex, 400,
                JsonResponse.error("Invalid recommendation plan."));
        return;
    }

    try (Connection c = Database.getConnection()) {

        // Verify that the list belongs to this user.
        List<Item> items = loadItems(c, userId, listId);

        if (items == null || items.isEmpty()) {
            JsonResponse.send(ex, 400,
                    JsonResponse.error(
                            "Your shopping list is empty or unavailable."));
            return;
        }

        // Reuse the existing recommendation calculation.
        Plan plan = buildPlan(items, type);

        if (plan == null) {
            JsonResponse.send(ex, 400,
                    JsonResponse.error(
                            "A product has no eligible seller offer."));
            return;
        }

        // Save the calculated totals into the existing Orders table.
        OrderDao orderDao = new OrderDao();

        int orderId = orderDao.createOrder(
                userId,
                listId,
                money(plan.productCost),
                money(plan.discount),
                money(plan.delivery),
                money(plan.fee),
                money(plan.total)
        );

        String response = "{\"success\":true,"
                + "\"message\":\"Order saved successfully.\","
                + "\"orderId\":" + orderId
                + ",\"finalTotal\":" + money(plan.total)
                + "}";

        JsonResponse.send(ex, 200, response);
    }
}

}
