
package handler;

import app.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;

public class JsonResponse {

    public static void send(
            HttpExchange exchange, int status, String json)
            throws IOException {

        byte[] bytes = json.getBytes("UTF-8");

        exchange.getResponseHeaders().set(
            "Content-Type", "application/json; charset=UTF-8"
        );

        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    public static String error(String message) {
        return "{\"error\":" + JsonUtil.quote(message) + "}";
    }
}
