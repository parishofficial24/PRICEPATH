
package handler;

import app.Database;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StaticFileHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();

        if (requestPath.equals("/")) {
            requestPath = "/login.html";
        }

        Path frontendPath = Paths.get(
            Database.FRONTEND_FOLDER
        ).toAbsolutePath().normalize();

        Path requestedFile = frontendPath
            .resolve(requestPath.substring(1))
            .normalize();

        if (!requestedFile.startsWith(frontendPath)
                || !Files.isRegularFile(requestedFile)) {
            sendResponse(exchange, 404, "Page not found.");
            return;
        }

        String fileName = requestedFile.getFileName().toString();
        String contentType = "text/plain; charset=UTF-8";

        if (fileName.endsWith(".html")) {
            contentType = "text/html; charset=UTF-8";
        } else if (fileName.endsWith(".css")) {
            contentType = "text/css; charset=UTF-8";
        } else if (fileName.endsWith(".js")) {
            contentType = "application/javascript; charset=UTF-8";
        }

        byte[] content = Files.readAllBytes(requestedFile);
        exchange.getResponseHeaders().set(
            "Content-Type", contentType
        );
        exchange.sendResponseHeaders(200, content.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(content);
        }
    }

    private void sendResponse(
            HttpExchange exchange, int status, String message)
            throws IOException {

        byte[] content = message.getBytes("UTF-8");
        exchange.getResponseHeaders().set(
            "Content-Type", "text/plain; charset=UTF-8"
        );
        exchange.sendResponseHeaders(status, content.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(content);
        }
    }
}
