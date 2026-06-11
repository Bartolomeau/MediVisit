package pl.wspa.medivisit.util;

import org.mindrot.jbcrypt.BCrypt;

/** Haszowanie hasel algorytmem bcrypt. */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(10));
    }

    public static boolean verify(String plainPassword, String hash) {
        if (plainPassword == null || hash == null || hash.isEmpty()) {
            return false;
        }
        return BCrypt.checkpw(plainPassword, hash);
    }
}
