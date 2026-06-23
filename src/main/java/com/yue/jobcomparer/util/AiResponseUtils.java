package com.yue.jobcomparer.util;

public final class AiResponseUtils {

    // Private constructor， prevent creating
    private AiResponseUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String stripMarkdownFence(String response) {
        if (response == null) {
            return null;
        }
        String trimmed = response.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline != -1) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }
}
