package com.laptopshop.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Chuẩn hóa chuỗi tiếng Việt có dấu thành không dấu phục vụ tìm kiếm linh hoạt.
 */
public final class VietnameseTextNormalizer {

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    private VietnameseTextNormalizer() {}

    public static String normalize(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String replaced = input
                .replace('\u0111', 'd')
                .replace('\u0110', 'D');
        String decomposed = Normalizer.normalize(replaced, Normalizer.Form.NFD);
        String stripped = COMBINING_MARKS.matcher(decomposed).replaceAll("");
        return MULTI_SPACE.matcher(stripped.trim()).replaceAll(" ").toLowerCase(Locale.ROOT);
    }

    public static String normalizeTruncated(String input, int maxLength) {
        String normalized = normalize(input);
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }
}
