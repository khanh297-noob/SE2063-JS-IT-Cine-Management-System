package cinemanagement.util;

import java.security.SecureRandom;

public final class TicketCodeGenerator {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private TicketCodeGenerator() {
    }

    public static String next() {
        StringBuilder sb = new StringBuilder("CINE-");
        for (int i = 0; i < 12; i++) {
            if (i == 4 || i == 8) {
                sb.append('-');
            }
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    public static String transactionCode(String prefix) {
        StringBuilder sb = new StringBuilder(prefix).append('-').append(System.currentTimeMillis()).append('-');
        for (int i = 0; i < 6; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
