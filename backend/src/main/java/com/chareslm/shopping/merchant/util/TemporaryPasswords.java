package com.chareslm.shopping.merchant.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TemporaryPasswords {
    private static final char[] LOWER = "abcdefghijkmnopqrstuvwxyz".toCharArray();
    private static final char[] UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final char[] DIGITS = "23456789".toCharArray();
    private static final char[] SPECIAL = "!@#$%^&*".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private TemporaryPasswords() {
    }

    /** SMTP 关闭时使用的固定初始密码（满足强密码规则，便于课程演示）。 */
    public static final String OFFLINE_DEFAULT = "123456QWERqwer!@";

    public static String issue(boolean mailEnabled) {
        return mailEnabled ? generate() : OFFLINE_DEFAULT;
    }

    public static String generate() {
        List<Character> chars = new ArrayList<>();
        chars.add(pick(LOWER));
        chars.add(pick(UPPER));
        chars.add(pick(DIGITS));
        chars.add(pick(SPECIAL));
        char[] all = (new String(LOWER) + new String(UPPER) + new String(DIGITS) + new String(SPECIAL)).toCharArray();
        while (chars.size() < 20) {
            chars.add(pick(all));
        }
        Collections.shuffle(chars, RANDOM);
        StringBuilder result = new StringBuilder(chars.size());
        chars.forEach(result::append);
        return result.toString();
    }

    private static char pick(char[] source) {
        return source[RANDOM.nextInt(source.length)];
    }
}
