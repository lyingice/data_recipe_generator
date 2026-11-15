package net.lyingice.datarecipegenerator.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeBuilderTest {

    private static List<SlotStack> craftingSlots() {
        List<SlotStack> slots = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            slots.add(SlotStack.EMPTY);
        }
        return slots;
    }

    private static List<SlotStack> slotList(int count) {
        List<SlotStack> slots = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            slots.add(SlotStack.EMPTY);
        }
        return slots;
    }

    private static GeneratorConfig config() {
        GeneratorConfig config = new GeneratorConfig();
        config.namespace = "test";
        config.category = "misc";
        return config;
    }

    @Test
    void buildsShapedRecipeForForge1201() throws Exception {
        List<SlotStack> slots = craftingSlots();
        slots.set(0, SlotStack.of("minecraft:oak_planks", 1));
        slots.set(4, SlotStack.of("minecraft:stick", 2));
        slots.set(7, SlotStack.of("minecraft:stick", 2));
        slots.set(9, SlotStack.of("minecraft:wooden_pickaxe", 1));

        RecipeBuildResult result = RecipeBuilder.build(RecipeVariant.CRAFTING_SHAPED, config(), slots,
                PlatformEdition.FORGE_1_20_1);

        JsonObject json = JsonParser.parseString(result.json()).getAsJsonObject();
        assertEquals("minecraft:crafting_shaped", json.get("type").getAsString());
        assertEquals("misc", json.get("category").getAsString());
        JsonArray pattern = json.getAsJsonArray("pattern");
        assertEquals(3, pattern.size());
        assertEquals("A ", pattern.get(0).getAsString());
        assertEquals(" B", pattern.get(1).getAsString());
        assertEquals(" B", pattern.get(2).getAsString());
        assertEquals("minecraft:oak_planks", json.getAsJsonObject("key").getAsJsonObject("A").get("item").getAsString());
        assertEquals("minecraft:wooden_pickaxe", json.getAsJsonObject("result").get("item").getAsString());
        assertEquals("wooden_pickaxe", result.fileName());
    }

    @Test
    void buildsShapedRecipeForNeoForge2612() throws Exception {
        List<SlotStack> slots = craftingSlots();
        slots.set(0, SlotStack.of("minecraft:oak_planks", 1));
        slots.set(9, SlotStack.of("minecraft:stick", 4));

        RecipeBuildResult result = RecipeBuilder.build(RecipeVariant.CRAFTING_SHAPED, config(), slots,
                PlatformEdition.NEOFORGE_26_1_2);

        JsonObject json = JsonParser.parseString(result.json()).getAsJsonObject();
        assertEquals("minecraft:crafting_shaped", json.get("type").getAsString());
        assertEquals("minecraft:oak_planks", json.getAsJsonObject("key").get("A").getAsString());
        assertEquals("minecraft:stick", json.getAsJsonObject("result").get("id").getAsString());
        assertEquals(4, json.getAsJsonObject("result").get("count").getAsInt());
    }

    @Test
    void buildsShapelessRecipeWithTags() throws Exception {
        List<SlotStack> slots = craftingSlots();
        slots.set(0, SlotStack.of("minecraft:oak_planks", 1));
        slots.set(1, SlotStack.of("minecraft:oak_planks", 1));
        slots.set(9, SlotStack.of("minecraft:stick", 4));

        GeneratorConfig config = config();
        config.ingredientOverrides = new ArrayList<>(List.of("#minecraft:planks", "", "", "", "", "", "", "", ""));

        RecipeBuildResult result = RecipeBuilder.build(RecipeVariant.CRAFTING_SHAPELESS, config, slots,
                PlatformEdition.NEOFORGE_26_1_2);
        JsonObject json = JsonParser.parseString(result.json()).getAsJsonObject();
        JsonArray ingredients = json.getAsJsonArray("ingredients");
        assertEquals(2, ingredients.size());
        assertEquals("#minecraft:planks", ingredients.get(0).getAsString());
        assertEquals("minecraft:oak_planks", ingredients.get(1).getAsString());
    }

    @Test
    void stonecutterUsesVersionSpecificResultShape() throws Exception {
        List<SlotStack> slots = slotList(2);
        slots.set(0, SlotStack.of("minecraft:stone", 1));
        slots.set(1, SlotStack.of("minecraft:stone_slab", 2));

        JsonObject oldJson = JsonParser.parseString(RecipeBuilder.build(
                RecipeVariant.STONECUTTING, config(), slots, PlatformEdition.FORGE_1_20_1).json()).getAsJsonObject();
        assertEquals("minecraft:stone_slab", oldJson.get("result").getAsString());
        assertEquals(2, oldJson.get("count").getAsInt());

        JsonObject newJson = JsonParser.parseString(RecipeBuilder.build(
                RecipeVariant.STONECUTTING, config(), slots, PlatformEdition.NEOFORGE_26_1_2).json()).getAsJsonObject();
        assertEquals("minecraft:stone_slab", newJson.getAsJsonObject("result").get("id").getAsString());
        assertEquals(2, newJson.getAsJsonObject("result").get("count").getAsInt());
    }

    @Test
    void cookingUsesDefaultsAndOverrides() throws Exception {
        List<SlotStack> slots = slotList(3);
        slots.set(0, SlotStack.of("minecraft:raw_iron", 1));
        slots.set(2, SlotStack.of("minecraft:iron_ingot", 1));

        JsonObject json = JsonParser.parseString(RecipeBuilder.build(
                RecipeVariant.SMELTING, config(), slots, PlatformEdition.FORGE_1_20_1).json()).getAsJsonObject();
        assertEquals(200, json.get("cookingtime").getAsInt());
        assertEquals(0.1F, json.get("experience").getAsFloat(), 0.0001F);
        assertEquals("minecraft:iron_ingot", json.get("result").getAsString());

        GeneratorConfig overridden = config();
        overridden.cookingTime = "350";
        overridden.experience = "1.5";
        JsonObject json2 = JsonParser.parseString(RecipeBuilder.build(
                RecipeVariant.SMELTING, overridden, slots, PlatformEdition.FORGE_1_20_1).json()).getAsJsonObject();
        assertEquals(350, json2.get("cookingtime").getAsInt());
        assertEquals(1.5F, json2.get("experience").getAsFloat(), 0.0001F);
    }

    @Test
    void forgeSmithingTransformRequiresTemplateAndAddition() {
        List<SlotStack> slots = slotList(4);
        slots.set(1, SlotStack.of("minecraft:diamond_sword", 1));
        slots.set(3, SlotStack.of("minecraft:netherite_sword", 1));

        RecipeValidationException error = assertThrows(RecipeValidationException.class,
                () -> RecipeBuilder.build(RecipeVariant.SMITHING_TRANSFORM, config(), slots,
                        PlatformEdition.FORGE_1_20_1));
        assertEquals(RecipeError.SMITHING_MISSING_TEMPLATE, error.error());
    }

    @Test
    void neoForgeSmithingTransformAllowsMissingTemplate() throws Exception {
        List<SlotStack> slots = slotList(4);
        slots.set(1, SlotStack.of("minecraft:diamond_sword", 1));
        slots.set(3, SlotStack.of("minecraft:netherite_sword", 1));

        JsonObject json = JsonParser.parseString(RecipeBuilder.build(
                RecipeVariant.SMITHING_TRANSFORM, config(), slots, PlatformEdition.NEOFORGE_26_1_2).json()).getAsJsonObject();
        assertFalse(json.has("template"));
        assertFalse(json.has("addition"));
        assertTrue(json.has("base"));
    }

    @Test
    void trimHandlesPatternPerVersion() throws Exception {
        List<SlotStack> slots = slotList(4);
        slots.set(0, SlotStack.of("minecraft:wayfinder_armor_trim_smithing_template", 1));
        slots.set(1, SlotStack.of("minecraft:iron_chestplate", 1));
        slots.set(2, SlotStack.of("minecraft:redstone", 1));

        JsonObject oldJson = JsonParser.parseString(RecipeBuilder.build(
                RecipeVariant.SMITHING_TRIM, config(), slots, PlatformEdition.FORGE_1_20_1).json()).getAsJsonObject();
        assertFalse(oldJson.has("pattern"));

        GeneratorConfig patternConfig = config();
        patternConfig.trimPattern = "minecraft:sentry";
        JsonObject newJson = JsonParser.parseString(RecipeBuilder.build(
                RecipeVariant.SMITHING_TRIM, patternConfig, slots, PlatformEdition.NEOFORGE_26_1_2).json()).getAsJsonObject();
        assertEquals("minecraft:sentry", newJson.get("pattern").getAsString());

        patternConfig.trimPattern = "not a pattern";
        RecipeValidationException error = assertThrows(RecipeValidationException.class,
                () -> RecipeBuilder.build(RecipeVariant.SMITHING_TRIM, patternConfig, slots,
                        PlatformEdition.NEOFORGE_26_1_2));
        assertEquals(RecipeError.TRIM_MISSING_PATTERN, error.error());
    }

    @Test
    void transmuteValidatesMaterials() throws Exception {
        List<SlotStack> slots = craftingSlots();
        slots.set(4, SlotStack.of("minecraft:leather_helmet", 1));
        slots.set(0, SlotStack.of("minecraft:red_dye", 1));
        slots.set(1, SlotStack.of("minecraft:red_dye", 1));
        slots.set(9, SlotStack.of("minecraft:red_leather_helmet", 1));

        JsonObject json = JsonParser.parseString(RecipeBuilder.build(
                RecipeVariant.CRAFTING_TRANSMUTE, config(), slots, PlatformEdition.NEOFORGE_26_1_2).json()).getAsJsonObject();
        assertEquals("minecraft:leather_helmet", json.get("input").getAsString());
        assertEquals("minecraft:red_dye", json.get("material").getAsString());
        assertEquals(2, json.getAsJsonObject("material_count").get("max").getAsInt());

        slots.set(1, SlotStack.of("minecraft:blue_dye", 1));
        RecipeValidationException error = assertThrows(RecipeValidationException.class,
                () -> RecipeBuilder.build(RecipeVariant.CRAFTING_TRANSMUTE, config(), slots,
                        PlatformEdition.NEOFORGE_26_1_2));
        assertEquals(RecipeError.TRANSMUTE_MATERIAL_MISMATCH, error.error());
    }

    @Test
    void imbueRequiresFullGrid() throws Exception {
        List<SlotStack> slots = craftingSlots();
        slots.set(4, SlotStack.of("minecraft:potion", 1));
        for (int i = 0; i < 9; i++) {
            if (i != 4) {
                slots.set(i, SlotStack.of("minecraft:glowstone_dust", 1));
            }
        }
        slots.set(9, SlotStack.of("minecraft:potion", 1));

        JsonObject json = JsonParser.parseString(RecipeBuilder.build(
                RecipeVariant.CRAFTING_IMBUE, config(), slots, PlatformEdition.NEOFORGE_26_1_2).json()).getAsJsonObject();
        assertEquals("minecraft:potion", json.get("source").getAsString());
        assertEquals("minecraft:glowstone_dust", json.get("material").getAsString());

        slots.set(0, SlotStack.EMPTY);
        RecipeValidationException error = assertThrows(RecipeValidationException.class,
                () -> RecipeBuilder.build(RecipeVariant.CRAFTING_IMBUE, config(), slots,
                        PlatformEdition.NEOFORGE_26_1_2));
        assertEquals(RecipeError.IMBUE_INCOMPLETE_GRID, error.error());
    }

    @Test
    void namingModesArePreserved() throws Exception {
        List<SlotStack> slots = craftingSlots();
        slots.set(0, SlotStack.of("minecraft:oak_planks", 1));
        slots.set(9, SlotStack.of("minecraft:stick", 4));

        GeneratorConfig config = config();
        config.customFileName = "My Sticks";
        RecipeBuildResult normal = RecipeBuilder.build(RecipeVariant.CRAFTING_SHAPED, config, slots,
                PlatformEdition.FORGE_1_20_1);
        assertEquals("my_sticks", normal.fileName());

        config.fileNameMode = FileNameMode.AUTO;
        assertEquals("stick", RecipeBuilder.build(RecipeVariant.CRAFTING_SHAPED, config, slots,
                PlatformEdition.FORGE_1_20_1).fileName());

        config.fileNameMode = FileNameMode.AUTO_SUFFIX;
        assertEquals("stick_my_sticks", RecipeBuilder.build(RecipeVariant.CRAFTING_SHAPED, config, slots,
                PlatformEdition.FORGE_1_20_1).fileName());

        config.fileNameMode = FileNameMode.INPUT_TO_OUTPUT;
        assertEquals("oak_planks_to_stick", RecipeBuilder.build(RecipeVariant.CRAFTING_SHAPED, config, slots,
                PlatformEdition.FORGE_1_20_1).fileName());
    }

    @Test
    void builds1211CookingWithSingleItemResult() throws Exception {
        List<SlotStack> slots = slotList(3);
        slots.set(0, SlotStack.of("minecraft:raw_iron", 1));
        slots.set(2, SlotStack.of("minecraft:iron_ingot", 1));

        GeneratorConfig config = config();
        config.cookingTime = "100";
        JsonObject json = JsonParser.parseString(RecipeBuilder.build(
                RecipeVariant.SMELTING, config, slots, PlatformEdition.NEOFORGE_1_21_1).json()).getAsJsonObject();
        assertEquals("minecraft:iron_ingot", json.getAsJsonObject("result").get("id").getAsString());
        assertFalse(json.getAsJsonObject("result").has("count"));
        assertEquals("minecraft:raw_iron", json.getAsJsonObject("ingredient").get("item").getAsString());
        assertEquals(100, json.get("cookingtime").getAsInt());
    }

    @Test
    void builds1211StonecuttingWithGroup() throws Exception {
        List<SlotStack> slots = slotList(2);
        slots.set(0, SlotStack.of("minecraft:stone", 1));
        slots.set(1, SlotStack.of("minecraft:stone_slab", 2));

        GeneratorConfig config = config();
        config.group = "slabs";
        JsonObject json = JsonParser.parseString(RecipeBuilder.build(
                RecipeVariant.STONECUTTING, config, slots, PlatformEdition.NEOFORGE_1_21_1).json()).getAsJsonObject();
        assertEquals("slabs", json.get("group").getAsString());
        assertEquals("minecraft:stone", json.getAsJsonObject("ingredient").get("item").getAsString());
        assertEquals("minecraft:stone_slab", json.getAsJsonObject("result").get("id").getAsString());
        assertEquals(2, json.getAsJsonObject("result").get("count").getAsInt());
    }

    @Test
    void builds1211TrimWithoutPattern() throws Exception {
        List<SlotStack> slots = slotList(4);
        slots.set(0, SlotStack.of("minecraft:sentry_armor_trim_smithing_template", 1));
        slots.set(1, SlotStack.of("minecraft:iron_chestplate", 1));
        slots.set(2, SlotStack.of("minecraft:redstone", 1));

        JsonObject json = JsonParser.parseString(RecipeBuilder.build(
                RecipeVariant.SMITHING_TRIM, config(), slots, PlatformEdition.NEOFORGE_1_21_1).json()).getAsJsonObject();
        assertFalse(json.has("pattern"));
    }

    @Test
    void builds1211RequiresSmithingParts() {
        List<SlotStack> slots = slotList(4);
        slots.set(1, SlotStack.of("minecraft:diamond_sword", 1));
        slots.set(3, SlotStack.of("minecraft:netherite_sword", 1));

        RecipeValidationException error = assertThrows(RecipeValidationException.class,
                () -> RecipeBuilder.build(RecipeVariant.SMITHING_TRANSFORM, config(), slots,
                        PlatformEdition.NEOFORGE_1_21_1));
        assertEquals(RecipeError.SMITHING_MISSING_TEMPLATE, error.error());
    }

    @Test
    void rejects2612OnlyVariantsOn1211() {
        List<SlotStack> slots = craftingSlots();
        slots.set(0, SlotStack.of("minecraft:oak_planks", 1));
        slots.set(9, SlotStack.of("minecraft:stick", 1));
        RecipeValidationException error = assertThrows(RecipeValidationException.class,
                () -> RecipeBuilder.build(RecipeVariant.CRAFTING_TRANSMUTE, config(), slots,
                        PlatformEdition.NEOFORGE_1_21_1));
        assertEquals(RecipeError.UNSUPPORTED_VARIANT, error.error());
    }

    @Test
    void unsupportedVariantIsRejected() {
        List<SlotStack> slots = craftingSlots();
        slots.set(0, SlotStack.of("minecraft:oak_planks", 1));
        slots.set(9, SlotStack.of("minecraft:stick", 1));
        RecipeValidationException error = assertThrows(RecipeValidationException.class,
                () -> RecipeBuilder.build(RecipeVariant.CRAFTING_TRANSMUTE, config(), slots,
                        PlatformEdition.FORGE_1_20_1));
        assertEquals(RecipeError.UNSUPPORTED_VARIANT, error.error());
    }
}
