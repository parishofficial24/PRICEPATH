
package app;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private static final ConcurrentHashMap<String, Integer> sessions =
        new ConcurrentHashMap<>();

    public static String createSession(int userId) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, userId);
        return token;
    }

    public static Integer getUserId(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        return sessions.get(token);
    }

    public static void removeSession(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }
}
