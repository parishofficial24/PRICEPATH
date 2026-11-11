
package handler;

import app.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.UserDao;
import model.User;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class AuthHandler implements HttpHandler {

    private final UserDao userDao = new UserDao();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        if ("GET".equalsIgnoreCase(method) && path.endsWith("/me")) {
            String token = exchange.getRequestHeaders().getFirst("X-Session-Token");
            Integer userId = SessionManager.getUserId(token);
            if (userId == null) {
                JsonResponse.send(exchange, 401, JsonResponse.error("Please log in first."));
                return;
            }
            try {
                User user = userDao.findById(userId);
                if (user == null) {
                    JsonResponse.send(exchange, 401, JsonResponse.error("Session user not found."));
                    return;
                }
                JsonResponse.send(exchange, 200, "{\"userId\":" + user.getUserId()
                    + ",\"name\":" + app.JsonUtil.quote(user.getName())
                    + ",\"role\":" + app.JsonUtil.quote(user.getRole()) + "}");
            } catch (SQLException e) {
                JsonResponse.send(exchange, 500, JsonResponse.error("Could not load session."));
            }
            return;
        }
        if ("POST".equalsIgnoreCase(method) && path.endsWith("/logout")) {
            String token = exchange.getRequestHeaders().getFirst("X-Session-Token");
            SessionManager.removeSession(token);
            JsonResponse.send(exchange, 200, "{\"message\":\"Logged out.\"}");
            return;
        }
        if (!"POST".equalsIgnoreCase(method)) {
            JsonResponse.send(exchange, 405, JsonResponse.error("Unsupported request method."));
            return;
        }
        Map<String, String> data = readForm(exchange);
        try {
            if (path.endsWith("/register")) register(exchange, data);
            else if (path.endsWith("/login")) login(exchange, data);
            else JsonResponse.send(exchange, 404, JsonResponse.error("Unknown authentication endpoint."));
        } catch (SQLException e) {
            // Keep the client response safe, but print the real cause in the
            // backend terminal so local setup/schema problems can be diagnosed.
            System.err.println("[PricePath] Authentication database error: " + e.getMessage());
            e.printStackTrace(System.err);
            JsonResponse.send(exchange, 500,
                JsonResponse.error(
                    "Login or registration could not be completed. Check the backend terminal for the database error."
                ));
        }
    }

    private void register(HttpExchange exchange,
                          Map<String, String> data) throws IOException,
                          SQLException {
        String name = data.get("name");
        String email = data.get("email");
        String password = data.get("password");

        if (empty(name) || empty(email) || empty(password)) {
            JsonResponse.send(exchange, 400,
                JsonResponse.error("All fields are required."));
            return;
        }

        if (password.length() < 8) {
            JsonResponse.send(exchange, 400,
                JsonResponse.error(
                    "Password must contain at least 8 characters."
                ));
            return;
        }

        boolean created = userDao.register(
            name.trim(), email.trim().toLowerCase(), password
        );

        if (created) {
            JsonResponse.send(exchange, 201,
                "{\"message\":\"Registration successful.\"}");
        } else {
            JsonResponse.send(exchange, 400,
                JsonResponse.error("Registration failed."));
        }
    }

    private void login(HttpExchange exchange,
                       Map<String, String> data) throws IOException,
                       SQLException {
        String email = data.get("email");
        String password = data.get("password");

        if (empty(email) || empty(password)) {
            JsonResponse.send(exchange, 400,
                JsonResponse.error("Email and password are required."));
            return;
        }

        User user = userDao.login(
            email.trim().toLowerCase(), password
        );

        if (user == null) {
            JsonResponse.send(exchange, 401,
                JsonResponse.error("Invalid email or password."));
            return;
        }

        String token = SessionManager.createSession(user.getUserId());

        JsonResponse.send(exchange, 200,
            "{\"message\":\"Login successful.\","
            + "\"token\":\"" + token + "\","
            + "\"userId\":" + user.getUserId() + ","
            + "\"name\":\"" + app.JsonUtil.escape(user.getName()) + "\","
            + "\"role\":\"" + app.JsonUtil.escape(user.getRole()) + "\"}");
    }

    private Map<String, String> readForm(HttpExchange exchange)
            throws IOException {
        Map<String, String> data = new HashMap<>();

        try (InputStream input = exchange.getRequestBody()) {
            String body = new String(
                input.readAllBytes(), StandardCharsets.UTF_8
            );

            for (String pair : body.split("&")) {
                String[] parts = pair.split("=", 2);
                String key = URLDecoder.decode(parts[0], "UTF-8");
                String value = parts.length > 1
                    ? URLDecoder.decode(parts[1], "UTF-8") : "";
                data.put(key, value);
            }
        }

        return data;
    }

    private boolean empty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
