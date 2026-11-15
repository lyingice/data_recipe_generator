package net.lyingice.datarecipegenerator.common;

/**
 * Stable screen geometry shared by both loaders. Slot coordinates are the
 * vanilla ones, so the background always lines up with the real container
 * texture on 1.20.1 and 26.1.2.
 */
public final class GeneratorLayout {
    public static final int NATIVE_WIDTH = 176;
    public static final int NATIVE_HEIGHT = 166;

    public static final int TAB_HEIGHT = 20;
    public static final int TAB_WIDTH = 52;
    public static final int TAB_GAP = 2;
    public static final int TAB_START_X = 9;

    public static final int PANEL_WIDTH = 210;
    public static final int PANEL_HEIGHT = 180;
    public static final int PANEL_GAP = 8;
    public static final int PANEL_X = NATIVE_WIDTH + PANEL_GAP;

    public static final int SCREEN_WIDTH = NATIVE_WIDTH + PANEL_GAP + PANEL_WIDTH;
    public static final int SCREEN_HEIGHT = TAB_HEIGHT + NATIVE_HEIGHT + 14;

    /** Panel-relative controls. */
    public static final int CONTROL_X = 4;
    public static final int CONTROL_WIDTH = 202;
    public static final int HALF_WIDTH = 96;
    public static final int HALF_X = 106;

    public static final int ROW_VARIANT = 4;
    public static final int ROW_NAMESPACE = 26;
    public static final int ROW_NAME_MODE = 24;
    public static final int ROW_FILE_NAME = 48;
    public static final int ROW_OUTPUT_PATH = 70;
    public static final int ROW_CATEGORY = 92;
    public static final int ROW_EXTRAS = 114;
    public static final int ROW_GENERATE = 136;
    public static final int ROW_STATUS_1 = 160;
    public static final int ROW_STATUS_2 = 170;

    private static final int[] CRAFTING_X = {30, 48, 66, 30, 48, 66, 30, 48, 66, 124};
    private static final int[] CRAFTING_Y = {17, 17, 17, 35, 35, 35, 53, 53, 53, 35};
    private static final int[] COOKING_X = {56, 56, 116};
    private static final int[] COOKING_Y = {17, 53, 35};
    private static final int[] STONECUTTER_X = {20, 143};
    private static final int[] STONECUTTER_Y = {33, 33};
    private static final int[] SMITHING_X = {8, 26, 44, 98};
    private static final int[] SMITHING_Y = {48, 48, 48, 48};

    private GeneratorLayout() {
    }

    public static int slotCount(ContainerType type) {
        return slotX(type).length;
    }

    public static int[] slotX(ContainerType type) {
        return switch (type) {
            case CRAFTING -> CRAFTING_X;
            case FURNACE, BLAST_FURNACE, SMOKER, CAMPFIRE -> COOKING_X;
            case STONECUTTER -> STONECUTTER_X;
            case SMITHING -> SMITHING_X;
        };
    }

    public static int[] slotY(ContainerType type) {
        return switch (type) {
            case CRAFTING -> CRAFTING_Y;
            case FURNACE, BLAST_FURNACE, SMOKER, CAMPFIRE -> COOKING_Y;
            case STONECUTTER -> STONECUTTER_Y;
            case SMITHING -> SMITHING_Y;
        };
    }

    public static String texture(ContainerType type) {
        return switch (type) {
            case CRAFTING -> "minecraft:textures/gui/container/crafting_table.png";
            case FURNACE, CAMPFIRE -> "minecraft:textures/gui/container/furnace.png";
            case BLAST_FURNACE -> "minecraft:textures/gui/container/blast_furnace.png";
            case SMOKER -> "minecraft:textures/gui/container/smoker.png";
            case STONECUTTER -> "minecraft:textures/gui/container/stonecutter.png";
            case SMITHING -> "minecraft:textures/gui/container/smithing.png";
        };
    }

    /** Offsets of the three input roles relative to the container's slot list. */
    public static int outputSlot(ContainerType type) {
        return switch (type) {
            case CRAFTING -> 9;
            case FURNACE, BLAST_FURNACE, SMOKER, CAMPFIRE -> 2;
            case STONECUTTER -> 1;
            case SMITHING -> 3;
        };
    }
}
