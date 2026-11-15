package net.lyingice.datarecipegenerator.common;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Resource-location and path helpers. These deliberately do not use
 * Minecraft's ResourceLocation/Identifier so the common module stays loader
 * independent.
 */
public final class ResourceIds {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9_./-]+");
    private static final Pattern RESOURCE_LOCATION = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern FILE_NAME_INVALID = Pattern.compile("[^a-z0-9_.-]");

    private ResourceIds() {
    }

    public static boolean isValid(String value) {
        return value != null && RESOURCE_LOCATION.matcher(value).matches();
    }

    public static boolean isValidNamespace(String value) {
        return value != null && NAMESPACE.matcher(value).matches();
    }

    /**
     * Makes a user-facing name safe to use as a file name. Minecraft recipe
     * file names are resource paths, so we mirror that rule and additionally
     * remove path separators.
     */
    public static String sanitizeFileName(String input) {
        String value = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
        value = value.replace(' ', '_');
        value = FILE_NAME_INVALID.matcher(value).replaceAll("_");
        while (value.contains("__")) {
            value = value.replace("__", "_");
        }
        while (value.startsWith("_")) {
            value = value.substring(1);
        }
        while (value.endsWith("_")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.length() > 120) {
            value = value.substring(0, 120);
        }
        return value.isEmpty() ? "recipe" : value;
    }

    /**
     * Returns the path part of a resource location ("namespace:path" -> "path").
     */
    public static String pathOf(String resourceLocation) {
        if (resourceLocation == null) {
            return "";
        }
        int colon = resourceLocation.indexOf(':');
        return colon >= 0 ? resourceLocation.substring(colon + 1) : resourceLocation;
    }

    public static boolean isValidPathSegment(String value) {
        return value != null && PATH.matcher(value).matches();
    }
}
