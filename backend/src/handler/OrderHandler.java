
package handler;

import app.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.OrderDao;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OrderHandler implements HttpHandler {

    private final OrderDao dao = new OrderDao();

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        String token = exchange.getRequestHeaders()
                .getFirst("X-Session-Token");

        Integer userId = SessionManager.getUserId(token);

        if (userId == null) {
            JsonResponse.send(exchange, 401,
                    JsonResponse.error("Please log in first."));
            return;
        }

        try {
            // GET /api/orders
            if ("GET".equalsIgnoreCase(method)
                    && "/api/orders".equals(path)) {

                List<Map<String, Object>> orders =
                        dao.getOrders(userId);

                StringBuilder json = new StringBuilder("[");

                for (Map<String, Object> o : orders) {
                    if (json.length() > 1) {
                        json.append(',');
                    }

                    json.append("{\"orderId\":")
                            .append(o.get("orderId"))
                            .append(",\"shoppingListId\":")
                            .append(o.get("shoppingListId") == null
                                    ? "null" : o.get("shoppingListId"))
                            .append(",\"listName\":")
                            .append(app.JsonUtil.quote(
                                    String.valueOf(o.get("listName"))))
                            .append(",\"totalProductCost\":")
                            .append(o.get("totalProductCost"))
                            .append(",\"totalDiscount\":")
                            .append(o.get("totalDiscount"))
                            .append(",\"totalDeliveryCharge\":")
                            .append(o.get("totalDeliveryCharge"))
                            .append(",\"totalPlatformFee\":")
                            .append(o.get("totalPlatformFee"))
                            .append(",\"finalTotal\":")
                            .append(o.get("finalTotal"))
                            .append(",\"createdAt\":")
                            .append(app.JsonUtil.quote(
                                    String.valueOf(o.get("createdAt"))))
                            .append('}');
                }

                json.append(']');
                JsonResponse.send(exchange, 200, json.toString());
                return;
            }

            // POST /api/orders/save
            if ("POST".equalsIgnoreCase(method)
                    && "/api/orders/save".equals(path)) {

                Map<String, String> form = readForm(exchange);

                int listId;
                try {
                    listId = Integer.parseInt(
                            form.getOrDefault("listId", ""));
                } catch (NumberFormatException e) {
                    JsonResponse.send(exchange, 400,
                            JsonResponse.error("Invalid shopping list ID."));
                    return;
                }

                if (listId <= 0) {
                    JsonResponse.send(exchange, 400,
                            JsonResponse.error("Invalid shopping list ID."));
                    return;
                }

                String planType = form.get("planType");

                if (planType == null || planType.trim().isEmpty()) {
                    JsonResponse.send(exchange, 400,
                            JsonResponse.error("Please select a plan."));
                    return;
                }

                new RecommendationHandler().saveOrderPlan(
                        exchange, userId, listId, planType);
                return;
            }

            JsonResponse.send(exchange, 404,
                    JsonResponse.error("Order endpoint not found."));

        } catch (SQLException e) {
            e.printStackTrace();
            JsonResponse.send(exchange, 500,
                    JsonResponse.error("Could not save or load orders."));
        } catch (Exception e) {
            e.printStackTrace();
            JsonResponse.send(exchange, 500,
                    JsonResponse.error("An unexpected error occurred."));
        } finally {
            exchange.close();
        }
    }

    private Map<String, String> readForm(HttpExchange exchange)
            throws IOException {

        String body = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);

        Map<String, String> form = new LinkedHashMap<>();

        if (body.isEmpty()) {
            return form;
        }

        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);

            String key = URLDecoder.decode(
                    parts[0], StandardCharsets.UTF_8);

            String value = parts.length > 1
                    ? URLDecoder.decode(
                            parts[1], StandardCharsets.UTF_8)
                    : "";

            form.put(key, value);
        }

        return form;
    }
}
