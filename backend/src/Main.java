
package app;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;

import handler.StaticFileHandler;
import handler.AuthHandler;
import handler.ProductHandler;
import handler.AdminHandler;
import handler.ShoppingListHandler;
import handler.RecommendationHandler;
import handler.PriceHistoryHandler;
import handler.OrderHandler;

public class Main {
    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(
                new InetSocketAddress(Database.PORT), 0
            );

            // API routes
            server.createContext("/api/health", exchange -> {
                byte[] body = "{\"status\":\"ok\",\"service\":\"PricePath Java API\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, body.length);
                try (java.io.OutputStream out = exchange.getResponseBody()) { out.write(body); }
            });
            server.createContext("/api/auth/", new AuthHandler());
            server.createContext("/api/products", new ProductHandler());
            server.createContext("/api/categories", new ProductHandler());
            server.createContext("/api/admin", new AdminHandler());
            server.createContext("/api/lists", new ShoppingListHandler());
            server.createContext("/api/recommendations", new RecommendationHandler());
            server.createContext("/api/price-history", new PriceHistoryHandler());
            server.createContext("/api/orders", new OrderHandler());

            // Frontend pages and static files
            server.createContext("/", new StaticFileHandler());

            server.setExecutor(null);
            server.start();

            System.out.println("================================");
            System.out.println("      PricePath is running");
            System.out.println("================================");
            System.out.println(
                "Open: http://localhost:" + Database.PORT + "/login.html"
            );
            System.out.println("Keep this terminal open while using PricePath.");

        } catch (IOException e) {
            System.err.println("Could not start PricePath server.");
            e.printStackTrace();
        }
    }
}
