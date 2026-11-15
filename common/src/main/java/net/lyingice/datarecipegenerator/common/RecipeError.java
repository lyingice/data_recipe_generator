package net.lyingice.datarecipegenerator.common;

import java.util.Locale;

/**
 * Errors are transmitted as stable codes so the client can localise them.
 */
public enum RecipeError {
    NO_OUTPUT,
    NO_INPUT,
    TRANSMUTE_NO_MATERIAL,
    TRANSMUTE_MATERIAL_MISMATCH,
    TRANSMUTE_TOO_MANY_MATERIALS,
    IMBUE_INCOMPLETE_GRID,
    IMBUE_MATERIAL_MISMATCH,
    SMITHING_MISSING_TEMPLATE,
    SMITHING_MISSING_ADDITION,
    TRIM_MISSING_PATTERN,
    INVALID_RESOURCE_LOCATION,
    INVALID_NAMESPACE,
    INVALID_OUTPUT_PATH,
    INVALID_NUMBER,
    UNSUPPORTED_VARIANT,
    INVALID_CONFIG,
    INVALID_COUNT,
    IO_ERROR,
    NO_PERMISSION,
    NOT_OPEN;

    public String messageKey() {
        return "message.data_recipe_generator.error." + name().toLowerCase(Locale.ROOT);
    }
}
