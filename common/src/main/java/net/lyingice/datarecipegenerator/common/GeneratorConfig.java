package net.lyingice.datarecipegenerator.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;

/**
 * All user-editable options for one generation request. Instances are sent over
 * the network as a single JSON string; only plain fields are used so Gson can
 * round-trip the object on both loaders without any loader-specific code.
 */
public final class GeneratorConfig {
    public static final int MAX_JSON_LENGTH = 16_384;

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public RecipeVariant variant = RecipeVariant.CRAFTING_SHAPED;
    public String namespace = "custom";
    /** Relative to data_recipe_generator_output; blank means the pack root. */
    public String outputPath = "";
    public FileNameMode fileNameMode = FileNameMode.NORMAL;
    public String customFileName = "";
    public String category = "misc";
    public String group = "";
    /** Blank means "use the variant default". */
    public String cookingTime = "";
    /** Blank means "use the variant default". */
    public String experience = "";
    public String trimPattern = "minecraft:sentry";
    /** Optional per-slot overrides; a leading '#' denotes a tag. */
    public List<String> ingredientOverrides = new ArrayList<>();

    public GeneratorConfig copy() {
        GeneratorConfig copy = new GeneratorConfig();
        copy.variant = this.variant;
        copy.namespace = this.namespace;
        copy.outputPath = this.outputPath;
        copy.fileNameMode = this.fileNameMode;
        copy.customFileName = this.customFileName;
        copy.category = this.category;
        copy.group = this.group;
        copy.cookingTime = this.cookingTime;
        copy.experience = this.experience;
        copy.trimPattern = this.trimPattern;
        copy.ingredientOverrides = new ArrayList<>(this.ingredientOverrides);
        return copy;
    }

    public void normalize() {
        if (this.variant == null) {
            this.variant = RecipeVariant.CRAFTING_SHAPED;
        }
        if (this.namespace == null || this.namespace.isBlank()) {
            this.namespace = "custom";
        }
        this.namespace = this.namespace.trim();
        if (this.outputPath == null) {
            this.outputPath = "";
        }
        this.outputPath = this.outputPath.trim().replace('\\', '/');
        if (this.fileNameMode == null) {
            this.fileNameMode = FileNameMode.NORMAL;
        }
        if (this.customFileName == null) {
            this.customFileName = "";
        }
        this.customFileName = this.customFileName.trim();
        if (this.category == null) {
            this.category = this.variant.defaultCategory();
        }
        this.category = this.category.trim().toLowerCase();
        if (!this.variant.categories().isEmpty() && !this.variant.categories().contains(this.category)) {
            this.category = this.variant.defaultCategory();
        }
        if (this.group == null) {
            this.group = "";
        }
        this.group = this.group.trim();
        if (this.group.length() > 128) {
            this.group = this.group.substring(0, 128);
        }
        if (this.cookingTime == null) {
            this.cookingTime = "";
        }
        this.cookingTime = this.cookingTime.trim();
        if (this.experience == null) {
            this.experience = "";
        }
        this.experience = this.experience.trim();
        if (this.trimPattern == null || this.trimPattern.isBlank()) {
            this.trimPattern = "minecraft:sentry";
        }
        this.trimPattern = this.trimPattern.trim();
        if (this.ingredientOverrides == null) {
            this.ingredientOverrides = new ArrayList<>();
        }
        List<String> cleaned = new ArrayList<>();
        for (int i = 0; i < this.ingredientOverrides.size() && cleaned.size() < 16; i++) {
            String value = this.ingredientOverrides.get(i);
            if (value == null) {
                value = "";
            }
            value = value.trim();
            if (value.length() > 256) {
                value = value.substring(0, 256);
            }
            cleaned.add(value);
        }
        this.ingredientOverrides = cleaned;
    }

    public String overrideFor(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= this.ingredientOverrides.size()) {
            return "";
        }
        String value = this.ingredientOverrides.get(slotIndex);
        return value == null ? "" : value.trim();
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static GeneratorConfig fromJson(String json) {
        if (json == null || json.isBlank()) {
            return new GeneratorConfig();
        }
        if (json.length() > MAX_JSON_LENGTH) {
            throw new IllegalArgumentException("Config JSON is too long");
        }
        try {
            GeneratorConfig config = GSON.fromJson(json, GeneratorConfig.class);
            if (config == null) {
                return new GeneratorConfig();
            }
            config.normalize();
            return config;
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Invalid config JSON", e);
        }
    }
}
