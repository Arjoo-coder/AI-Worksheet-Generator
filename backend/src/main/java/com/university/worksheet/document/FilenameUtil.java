package com.university.worksheet.document;

import java.util.Locale;

public final class FilenameUtil {

    private FilenameUtil() {}

    /** Builds a meaningful, filesystem-safe filename, e.g. "Backend_Technologies_Experiment_02". */
    public static String buildBaseName(String subject, String experimentNumber) {
        String safeSubject = sanitize(subject);
        if (safeSubject.isBlank()) {
            safeSubject = "Worksheet";
        }
        String number = (experimentNumber == null || experimentNumber.isBlank())
                ? ""
                : "_Experiment_" + sanitize(experimentNumber);
        return safeSubject + number;
    }

    public static String sanitize(String input) {
        if (input == null) return "";
        String trimmed = input.trim().replaceAll("\\s+", "_");
        String cleaned = trimmed.replaceAll("[^a-zA-Z0-9_\\-]", "");
        return capitalizeWords(cleaned);
    }

    private static String capitalizeWords(String input) {
        String[] parts = input.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append('_');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.length() == 0 ? input : sb.toString();
    }
}
