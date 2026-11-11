
package app;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String hashPassword(String password) {
        try {
            byte[] salt = new byte[16];
            RANDOM.nextBytes(salt);

            MessageDigest digest =
                MessageDigest.getInstance("SHA-256");

            digest.update(salt);
            byte[] hash = digest.digest(
                password.getBytes("UTF-8")
            );

            return Base64.getEncoder().encodeToString(salt)
                + ":"
                + Base64.getEncoder().encodeToString(hash);

        } catch (Exception e) {
            throw new RuntimeException(
                "Could not hash password.", e
            );
        }
    }

    public static boolean verifyPassword(
            String password, String storedValue) {
        try {
            String[] parts = storedValue.split(":");

            if (parts.length != 2) {
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash =
                Base64.getDecoder().decode(parts[1]);

            MessageDigest digest =
                MessageDigest.getInstance("SHA-256");

            digest.update(salt);
            byte[] actualHash = digest.digest(
                password.getBytes("UTF-8")
            );

            return MessageDigest.isEqual(
                expectedHash, actualHash
            );

        } catch (Exception e) {
            return false;
        }
    }
}
