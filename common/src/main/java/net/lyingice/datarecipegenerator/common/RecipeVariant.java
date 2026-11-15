package net.lyingice.datarecipegenerator.common;

import java.util.List;
import java.util.Locale;

/**
 * Every recipe format the generator knows about. Each variant maps to exactly
 * one vanilla recipe type id and one GUI container.
 */
public enum RecipeVariant {
    CRAFTING_SHAPED(ContainerType.CRAFTING, "minecraft:crafting_shaped", CategoryKind.CRAFTING),
    CRAFTING_SHAPELESS(ContainerType.CRAFTING, "minecraft:crafting_shapeless", CategoryKind.CRAFTING),
    CRAFTING_TRANSMUTE(ContainerType.CRAFTING, "minecraft:crafting_transmute", CategoryKind.CRAFTING),
    CRAFTING_IMBUE(ContainerType.CRAFTING, "minecraft:crafting_imbue", CategoryKind.CRAFTING),
    SMELTING(ContainerType.FURNACE, "minecraft:smelting", CategoryKind.COOKING, 200, 0.1F),
    BLASTING(ContainerType.BLAST_FURNACE, "minecraft:blasting", CategoryKind.COOKING, 100, 0.1F),
    SMOKING(ContainerType.SMOKER, "minecraft:smoking", CategoryKind.COOKING, 100, 0.05F),
    CAMPFIRE_COOKING(ContainerType.CAMPFIRE, "minecraft:campfire_cooking", CategoryKind.COOKING, 600, 0.1F),
    STONECUTTING(ContainerType.STONECUTTER, "minecraft:stonecutting", CategoryKind.NONE),
    SMITHING_TRANSFORM(ContainerType.SMITHING, "minecraft:smithing_transform", CategoryKind.NONE),
    SMITHING_TRIM(ContainerType.SMITHING, "minecraft:smithing_trim", CategoryKind.NONE);

    private static final List<String> CRAFTING_CATEGORIES = List.of("building", "redstone", "equipment", "misc");
    private static final List<String> COOKING_CATEGORIES = List.of("food", "blocks", "misc");

    private final ContainerType container;
    private final String recipeTypeId;
    private final CategoryKind categoryKind;
    private final int defaultCookingTime;
    private final float defaultExperience;

    RecipeVariant(ContainerType container, String recipeTypeId, CategoryKind categoryKind) {
        this(container, recipeTypeId, categoryKind, -1, 0.0F);
    }

    RecipeVariant(ContainerType container, String recipeTypeId, CategoryKind categoryKind,
                  int defaultCookingTime, float defaultExperience) {
        this.container = container;
        this.recipeTypeId = recipeTypeId;
        this.categoryKind = categoryKind;
        this.defaultCookingTime = defaultCookingTime;
        this.defaultExperience = defaultExperience;
    }

    public ContainerType container() {
        return this.container;
    }

    public String recipeTypeId() {
        return this.recipeTypeId;
    }

    public CategoryKind categoryKind() {
        return this.categoryKind;
    }

    public int defaultCookingTime() {
        return this.defaultCookingTime;
    }

    public float defaultExperience() {
        return this.defaultExperience;
    }

    public String langKey() {
        return "gui.data_recipe_generator.variant." + name().toLowerCase(Locale.ROOT);
    }

    public String tipKey() {
        return "gui.data_recipe_generator.variant_tip." + name().toLowerCase(Locale.ROOT);
    }

    public List<String> categories() {
        return switch (this.categoryKind) {
            case CRAFTING -> CRAFTING_CATEGORIES;
            case COOKING -> COOKING_CATEGORIES;
            case NONE -> List.of();
        };
    }

    public String defaultCategory() {
        return this.categoryKind == CategoryKind.NONE ? "" : "misc";
    }

    public boolean isCooking() {
        return this.categoryKind == CategoryKind.COOKING;
    }

    public boolean isTrim() {
        return this == SMITHING_TRIM;
    }

    public static RecipeVariant fromName(String name) {
        if (name != null) {
            for (RecipeVariant variant : values()) {
                if (variant.name().equalsIgnoreCase(name)) {
                    return variant;
                }
            }
        }
        return null;
    }

    public enum CategoryKind {
        NONE,
        CRAFTING,
        COOKING
    }
}
