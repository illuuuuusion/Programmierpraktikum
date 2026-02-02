package de.uni_jena.fpp.chatroom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;


 // PBKDF2 password hashing helper.
// Hashes passwords with per-user random salt
// Stores iterations per user record

public final class PasswordUtil {

    public static final int DEFAULT_ITERATIONS = 120_000;
    public static final int SALT_BYTES = 16;
    public static final int KEY_BITS = 256;
    private static final SecureRandom RNG = new SecureRandom();

    private PasswordUtil() {}

    public static byte[] newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RNG.nextBytes(salt);
        return salt;
    }

    public static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            try {
                SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                return skf.generateSecret(spec).getEncoded();
            } catch (GeneralSecurityException ex) {
                throw new RuntimeException("PBKDF2 not available", ex);
            }
        } finally {
            spec.clearPassword();
        }
    }

    public static boolean matches(char[] password, User user) {
        byte[] computed = pbkdf2(password, user.getSalt(), user.getIterations());
        boolean ok = MessageDigest.isEqual(computed, user.getPasswordHash());
        Arrays.fill(computed, (byte) 0);
        return ok;
    }
}
