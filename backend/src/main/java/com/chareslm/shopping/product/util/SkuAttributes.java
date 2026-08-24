package com.chareslm.shopping.product.util;

import com.chareslm.shopping.common.api.ErrorCode;
import com.chareslm.shopping.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SKU 规格写入 MySQL JSON 列前规范化：合法 JSON、或「颜色:黑」键值对。
 */
public final class SkuAttributes {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SkuAttributes() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.trim();
        JsonNode json = tryParseJson(text);
        if (json != null) {
            try {
                return MAPPER.writeValueAsString(json);
            } catch (Exception exception) {
                throw invalid();
            }
        }
        Map<String, String> pairs = parsePairs(stripObjectBraces(text));
        if (!pairs.isEmpty()) {
            try {
                return MAPPER.writeValueAsString(pairs);
            } catch (Exception exception) {
                throw invalid();
            }
        }
        if (text.startsWith("{") || text.startsWith("[")) {
            throw invalid();
        }
        try {
            return MAPPER.writeValueAsString(Map.of("规格", text));
        } catch (Exception exception) {
            throw invalid();
        }
    }

    private static JsonNode tryParseJson(String text) {
        try {
            JsonNode node = MAPPER.readTree(text);
            if (node.isObject() || node.isArray()) {
                return node;
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String stripObjectBraces(String text) {
        if (text.length() >= 2 && text.startsWith("{") && text.endsWith("}")) {
            return text.substring(1, text.length() - 1).trim();
        }
        return text;
    }

    private static Map<String, String> parsePairs(String text) {
        Map<String, String> pairs = new LinkedHashMap<>();
        for (String part : text.split("[,，;；]")) {
            String item = part.trim();
            if (item.isEmpty()) {
                continue;
            }
            int index = indexOfSeparator(item);
            if (index <= 0 || index >= item.length() - 1) {
                return Map.of();
            }
            String key = item.substring(0, index).trim();
            String value = item.substring(index + 1).trim();
            if (key.isEmpty() || value.isEmpty()) {
                return Map.of();
            }
            pairs.put(unquote(key), unquote(value));
        }
        return pairs;
    }

    private static int indexOfSeparator(String item) {
        int colon = indexOfAny(item, ':', '：', '=');
        return colon;
    }

    private static int indexOfAny(String item, char... chars) {
        int best = -1;
        for (char candidate : chars) {
            int found = item.indexOf(candidate);
            if (found >= 0 && (best < 0 || found < best)) {
                best = found;
            }
        }
        return best;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1).trim();
            }
        }
        return value;
    }

    private static BusinessException invalid() {
        return new BusinessException(ErrorCode.VALIDATION_ERROR.code(),
                "SKU 规格须为 JSON，例如 {\"颜色\":\"黑\"}，或使用 颜色:黑");
    }
}
