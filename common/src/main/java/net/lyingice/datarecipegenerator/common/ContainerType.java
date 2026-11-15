package net.lyingice.datarecipegenerator.common;

import java.util.List;

/**
 * A vanilla container that owns recipes which can be expressed as data-pack JSON.
 * <p>
 * Brewing stands, anvils, grindstones, enchanting tables, looms and cartography
 * tables are intentionally absent: vanilla implements those crafts in code and
 * there is no plain recipe JSON to generate for them.
 */
public enum ContainerType {
    CRAFTING("crafting"),
    FURNACE("furnace"),
    BLAST_FURNACE("blast_furnace"),
    SMOKER("smoker"),
    CAMPFIRE("campfire"),
    STONECUTTER("stonecutter"),
    SMITHING("smithing");

    public static final List<ContainerType> ALL = List.of(values());

    private final String id;

    ContainerType(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

    public String titleKey() {
        return "gui.data_recipe_generator.container." + this.id;
    }

    public String tabKey() {
        return "gui.data_recipe_generator.tab." + this.id;
    }

    public RecipeVariant defaultVariant() {
        return switch (this) {
            case CRAFTING -> RecipeVariant.CRAFTING_SHAPED;
            case FURNACE -> RecipeVariant.SMELTING;
            case BLAST_FURNACE -> RecipeVariant.BLASTING;
            case SMOKER -> RecipeVariant.SMOKING;
            case CAMPFIRE -> RecipeVariant.CAMPFIRE_COOKING;
            case STONECUTTER -> RecipeVariant.STONECUTTING;
            case SMITHING -> RecipeVariant.SMITHING_TRANSFORM;
        };
    }

    public static ContainerType fromId(String id) {
        if (id != null) {
            for (ContainerType type : values()) {
                if (type.id.equalsIgnoreCase(id) || type.name().equalsIgnoreCase(id)) {
                    return type;
                }
            }
        }
        return null;
    }
}
