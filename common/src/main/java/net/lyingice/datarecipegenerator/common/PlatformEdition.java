package net.lyingice.datarecipegenerator.common;

import java.util.List;

/**
 * Version-specific capabilities and file layout. Keeping this in the common
 * layer lets the shared recipe writer produce a loadable datapack for either
 * Minecraft generation without touching loader classes.
 */
public enum PlatformEdition {
    FORGE_1_20_1(false, false, false, 15, "recipes"),
    NEOFORGE_1_21_1(false, false, false, 48, "recipe"),
    NEOFORGE_26_1_2(true, true, true, 101, "recipe");

    private final boolean supportsTransmute;
    private final boolean supportsImbue;
    private final boolean trimNeedsPattern;
    private final int dataPackFormat;
    private final String recipeFolder;

    PlatformEdition(boolean supportsTransmute, boolean supportsImbue, boolean trimNeedsPattern,
                    int dataPackFormat, String recipeFolder) {
        this.supportsTransmute = supportsTransmute;
        this.supportsImbue = supportsImbue;
        this.trimNeedsPattern = trimNeedsPattern;
        this.dataPackFormat = dataPackFormat;
        this.recipeFolder = recipeFolder;
    }

    public boolean supports(RecipeVariant variant) {
        return switch (variant) {
            case CRAFTING_TRANSMUTE -> this.supportsTransmute;
            case CRAFTING_IMBUE -> this.supportsImbue;
            default -> true;
        };
    }

    public List<RecipeVariant> variants(ContainerType container) {
        return List.of(RecipeVariant.values()).stream()
                .filter(variant -> variant.container() == container)
                .filter(this::supports)
                .toList();
    }

    public boolean trimNeedsPattern() {
        return this.trimNeedsPattern;
    }

    /**
     * 1.20.1 and 1.21.1 serialize smithing_transform with mandatory
     * template/base/addition fields; 26.1 made template and addition optional.
     */
    public boolean requiresSmithingParts() {
        return this == FORGE_1_20_1 || this == NEOFORGE_1_21_1;
    }

    /**
     * 1.21.1 still has the stonecutter "group" field; 26.1 removed it from the
     * recipe codec.
     */
    public boolean supportsStonecutterGroup() {
        return this != NEOFORGE_26_1_2;
    }

    /**
     * 1.21.1's cooking serializer uses ItemStack.STRICT_SINGLE_ITEM_CODEC:
     * an object with id/components but no count. 26.1 allows a count.
     */
    public boolean cookingSingleItem() {
        return this == NEOFORGE_1_21_1;
    }

    /**
     * 1.20.1 and 1.21.1 use {"item": ...} / {"tag": ...} ingredient objects.
     * The plain holder-set string form only became valid in 26.1.
     */
    public boolean usesLegacyIngredientObjects() {
        return this != NEOFORGE_26_1_2;
    }

    public int dataPackFormat() {
        return this.dataPackFormat;
    }

    public String recipeFolder() {
        return this.recipeFolder;
    }
}
