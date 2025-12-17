package org.astrabank.utils;

import java.security.SecureRandom;

public class AccountNumberGenerator {

    private static final SecureRandom random = new SecureRandom();

    public static String generate() {
        StringBuilder sb = new StringBuilder();

        sb.append(random.nextInt(9) + 1);

        for (int i = 0; i < 11; i++) {
            sb.append(random.nextInt(10));
        }

        return sb.toString();
    }
}
