package net.lyingice.datarecipegenerator.common;

import java.util.Locale;

/**
 * The four file-naming strategies preserved from the original mod.
 */
public enum FileNameMode {
    /** Use the user supplied name; fall back to the output item id. */
    NORMAL,
    /** Always use the output item id. */
    AUTO,
    /** Output item id plus the user supplied suffix. */
    AUTO_SUFFIX,
    /** Base/input item id followed by "_to_" and the output item id. */
    INPUT_TO_OUTPUT;

    public String langKey() {
        return "gui.data_recipe_generator.name_mode." + name().toLowerCase(Locale.ROOT);
    }

    public FileNameMode next() {
        FileNameMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static FileNameMode fromName(String name) {
        if (name != null) {
            for (FileNameMode mode : values()) {
                if (mode.name().equalsIgnoreCase(name)) {
                    return mode;
                }
            }
        }
        return NORMAL;
    }
}
