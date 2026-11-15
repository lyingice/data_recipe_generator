package net.lyingice.datarecipegenerator.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts a GUI snapshot into the JSON format used by a specific Minecraft
 * generation. This is the only place that knows about recipe JSON shapes.
 */
public final class RecipeBuilder {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final int CRAFTING_SLOTS = 9;
    private static final int MAX_FORGE_RESULT = 64;
    private static final int MAX_NEOFORGE_RESULT = 99;

    private RecipeBuilder() {
    }

    public static RecipeBuildResult build(RecipeVariant variant, GeneratorConfig config,
                                          List<SlotStack> slots, PlatformEdition edition)
            throws RecipeValidationException {
        if (variant == null) {
            throw new RecipeValidationException(RecipeError.INVALID_CONFIG, "missing variant");
        }
        if (!edition.supports(variant)) {
            throw new RecipeValidationException(RecipeError.UNSUPPORTED_VARIANT, variant.name());
        }
        GeneratorConfig cfg = config == null ? new GeneratorConfig() : config.copy();
        cfg.variant = variant;
        cfg.normalize();

        List<IngredientSpec> resolved = resolveSlots(cfg, slots);
        JsonObject json = new JsonObject();
        json.addProperty("type", variant.recipeTypeId());
        addBookInfo(json, variant, cfg, edition);

        int outputCount = 0;
        switch (variant) {
            case CRAFTING_SHAPED -> outputCount = shaped(json, resolved, slots, edition);
            case CRAFTING_SHAPELESS -> outputCount = shapeless(json, resolved, slots, edition);
            case CRAFTING_TRANSMUTE -> outputCount = transmute(json, resolved, slots, edition);
            case CRAFTING_IMBUE -> outputCount = imbue(json, resolved, slots, edition);
            case SMELTING, BLASTING, SMOKING, CAMPFIRE_COOKING
                    -> outputCount = cooking(json, variant, cfg, resolved, slots, edition);
            case STONECUTTING -> outputCount = stonecutting(json, resolved, slots, edition);
            case SMITHING_TRANSFORM -> outputCount = smithingTransform(json, resolved, slots, edition);
            case SMITHING_TRIM -> outputCount = smithingTrim(json, cfg, resolved, edition);
        }

        String outputItemId = outputItemId(variant, slots);
        String baseItemId = baseItemId(variant, resolved, slots);
        String autoName = autoName(variant, cfg, edition, outputItemId, baseItemId);
        String fileName = chooseFileName(cfg, autoName, baseItemId, variant);

        return new RecipeBuildResult(GSON.toJson(json), fileName, autoName, outputItemId, baseItemId, outputCount);
    }

    // ------------------------------------------------------------------
    // Slot resolution
    // ------------------------------------------------------------------

    private static List<IngredientSpec> resolveSlots(GeneratorConfig config, List<SlotStack> slots)
            throws RecipeValidationException {
        List<IngredientSpec> resolved = new ArrayList<>(slots.size());
        for (int i = 0; i < slots.size(); i++) {
            String override = config.overrideFor(i);
            if (!override.isEmpty()) {
                resolved.add(IngredientSpec.parse(override));
            } else {
                SlotStack stack = slots.get(i);
                if (stack == null || stack.isEmpty()) {
                    resolved.add(null);
                } else {
                    resolved.add(IngredientSpec.item(stack.itemId()));
                }
            }
        }
        return resolved;
    }

    private static SlotStack slot(List<SlotStack> slots, int index) {
        return index >= 0 && index < slots.size() && slots.get(index) != null ? slots.get(index) : SlotStack.EMPTY;
    }

    private static IngredientSpec ingredient(List<IngredientSpec> resolved, int index) {
        return index >= 0 && index < resolved.size() ? resolved.get(index) : null;
    }

    private static SlotStack requireOutput(List<SlotStack> slots, int index) throws RecipeValidationException {
        SlotStack output = slot(slots, index);
        if (output.isEmpty()) {
            throw new RecipeValidationException(RecipeError.NO_OUTPUT);
        }
        return output;
    }

    private static int outputCount(SlotStack output, PlatformEdition edition) throws RecipeValidationException {
        int count = Math.max(1, output.count());
        int max = edition == PlatformEdition.FORGE_1_20_1 ? MAX_FORGE_RESULT : MAX_NEOFORGE_RESULT;
        if (count > max) {
            throw new RecipeValidationException(RecipeError.INVALID_COUNT, String.valueOf(count));
        }
        return count;
    }

    // ------------------------------------------------------------------
    // JSON fragments
    // ------------------------------------------------------------------

    private static JsonElement ingredientJson(IngredientSpec ingredient, PlatformEdition edition) {
        if (edition.usesLegacyIngredientObjects()) {
            JsonObject object = new JsonObject();
            object.addProperty(ingredient.tag() ? "tag" : "item", ingredient.id());
            return object;
        }
        return new JsonPrimitive(ingredient.tag() ? "#" + ingredient.id() : ingredient.id());
    }

    private static JsonObject resultJson(String itemId, int count, PlatformEdition edition) {
        JsonObject result = new JsonObject();
        result.addProperty(edition == PlatformEdition.FORGE_1_20_1 ? "item" : "id", itemId);
        result.addProperty("count", count);
        return result;
    }

    private static void addBookInfo(JsonObject json, RecipeVariant variant, GeneratorConfig config,
                                    PlatformEdition edition) {
        if (variant.categoryKind() != RecipeVariant.CategoryKind.NONE) {
            if (!config.category.isBlank()) {
                json.addProperty("category", config.category);
            }
            if (!config.group.isBlank()) {
                json.addProperty("group", config.group);
            }
        } else if (variant == RecipeVariant.STONECUTTING
                && edition.supportsStonecutterGroup()
                && !config.group.isBlank()) {
            // 1.20.1/1.21.1's stonecutter serializer still has a group field;
            // 26.1 removed it from the codec.
            json.addProperty("group", config.group);
        }
    }

    // ------------------------------------------------------------------
    // Crafting
    // ------------------------------------------------------------------

    private static int shaped(JsonObject json, List<IngredientSpec> resolved, List<SlotStack> slots,
                              PlatformEdition edition) throws RecipeValidationException {
        IngredientSpec[] grid = new IngredientSpec[CRAFTING_SLOTS];
        boolean any = false;
        for (int i = 0; i < CRAFTING_SLOTS; i++) {
            grid[i] = ingredient(resolved, i);
            if (grid[i] != null) {
                any = true;
            }
        }
        if (!any) {
            throw new RecipeValidationException(RecipeError.NO_INPUT);
        }

        int minRow = 3;
        int maxRow = -1;
        int minCol = 3;
        int maxCol = -1;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (grid[row * 3 + col] != null) {
                    minRow = Math.min(minRow, row);
                    maxRow = Math.max(maxRow, row);
                    minCol = Math.min(minCol, col);
                    maxCol = Math.max(maxCol, col);
                }
            }
        }

        JsonArray pattern = new JsonArray();
        JsonObject key = new JsonObject();
        Map<String, Character> assigned = new LinkedHashMap<>();
        char nextKey = 'A';

        for (int row = minRow; row <= maxRow; row++) {
            StringBuilder line = new StringBuilder();
            for (int col = minCol; col <= maxCol; col++) {
                IngredientSpec ingredient = grid[row * 3 + col];
                if (ingredient == null) {
                    line.append(' ');
                    continue;
                }
                JsonElement element = ingredientJson(ingredient, edition);
                String canonical = element.toString();
                Character character = assigned.get(canonical);
                if (character == null) {
                    character = nextKey++;
                    assigned.put(canonical, character);
                    key.add(String.valueOf(character), element);
                }
                line.append(character.charValue());
            }
            pattern.add(line.toString());
        }

        json.add("pattern", pattern);
        json.add("key", key);

        SlotStack output = requireOutput(slots, GeneratorLayout.outputSlot(ContainerType.CRAFTING));
        int count = outputCount(output, edition);
        json.add("result", resultJson(output.itemId(), count, edition));
        return count;
    }

    private static int shapeless(JsonObject json, List<IngredientSpec> resolved, List<SlotStack> slots,
                                 PlatformEdition edition) throws RecipeValidationException {
        JsonArray ingredients = new JsonArray();
        for (int i = 0; i < CRAFTING_SLOTS; i++) {
            IngredientSpec ingredient = ingredient(resolved, i);
            if (ingredient != null) {
                ingredients.add(ingredientJson(ingredient, edition));
            }
        }
        if (ingredients.isEmpty()) {
            throw new RecipeValidationException(RecipeError.NO_INPUT);
        }
        json.add("ingredients", ingredients);

        SlotStack output = requireOutput(slots, GeneratorLayout.outputSlot(ContainerType.CRAFTING));
        int count = outputCount(output, edition);
        json.add("result", resultJson(output.itemId(), count, edition));
        return count;
    }

    private static int transmute(JsonObject json, List<IngredientSpec> resolved, List<SlotStack> slots,
                                 PlatformEdition edition) throws RecipeValidationException {
        IngredientSpec input = ingredient(resolved, 4);
        if (input == null) {
            throw new RecipeValidationException(RecipeError.NO_INPUT);
        }

        IngredientSpec material = null;
        int materialItems = 0;
        for (int i = 0; i < CRAFTING_SLOTS; i++) {
            if (i == 4) {
                continue;
            }
            IngredientSpec candidate = ingredient(resolved, i);
            if (candidate == null) {
                continue;
            }
            if (material == null) {
                material = candidate;
            } else if (!material.equals(candidate)) {
                throw new RecipeValidationException(RecipeError.TRANSMUTE_MATERIAL_MISMATCH);
            }
            // The GUI slots are templates, not consumed stacks: one filled
            // material slot means one material item in the generated recipe.
            materialItems++;
        }
        if (material == null) {
            throw new RecipeValidationException(RecipeError.TRANSMUTE_NO_MATERIAL);
        }
        if (materialItems > 8) {
            throw new RecipeValidationException(RecipeError.TRANSMUTE_TOO_MANY_MATERIALS,
                    String.valueOf(materialItems));
        }

        json.add("input", ingredientJson(input, edition));
        json.add("material", ingredientJson(material, edition));
        if (materialItems > 1) {
            JsonObject bounds = new JsonObject();
            bounds.addProperty("min", 1);
            bounds.addProperty("max", materialItems);
            json.add("material_count", bounds);
        }

        SlotStack output = requireOutput(slots, GeneratorLayout.outputSlot(ContainerType.CRAFTING));
        int count = outputCount(output, edition);
        json.add("result", resultJson(output.itemId(), count, edition));
        return count;
    }

    private static int imbue(JsonObject json, List<IngredientSpec> resolved, List<SlotStack> slots,
                             PlatformEdition edition) throws RecipeValidationException {
        IngredientSpec source = ingredient(resolved, 4);
        if (source == null) {
            throw new RecipeValidationException(RecipeError.IMBUE_INCOMPLETE_GRID);
        }
        IngredientSpec material = null;
        for (int i = 0; i < CRAFTING_SLOTS; i++) {
            if (i == 4) {
                continue;
            }
            IngredientSpec candidate = ingredient(resolved, i);
            if (candidate == null) {
                throw new RecipeValidationException(RecipeError.IMBUE_INCOMPLETE_GRID);
            }
            if (material == null) {
                material = candidate;
            } else if (!material.equals(candidate)) {
                throw new RecipeValidationException(RecipeError.IMBUE_MATERIAL_MISMATCH);
            }
        }

        json.add("source", ingredientJson(source, edition));
        json.add("material", ingredientJson(material, edition));

        SlotStack output = requireOutput(slots, GeneratorLayout.outputSlot(ContainerType.CRAFTING));
        int count = outputCount(output, edition);
        json.add("result", resultJson(output.itemId(), count, edition));
        return count;
    }

    // ------------------------------------------------------------------
    // Cooking and stonecutting
    // ------------------------------------------------------------------

    private static int cooking(JsonObject json, RecipeVariant variant, GeneratorConfig config,
                               List<IngredientSpec> resolved, List<SlotStack> slots,
                               PlatformEdition edition) throws RecipeValidationException {
        IngredientSpec input = ingredient(resolved, 0);
        if (input == null) {
            throw new RecipeValidationException(RecipeError.NO_INPUT);
        }
        SlotStack output = requireOutput(slots, GeneratorLayout.outputSlot(ContainerType.FURNACE));
        int count = outputCount(output, edition);

        json.add("ingredient", ingredientJson(input, edition));
        if (edition == PlatformEdition.FORGE_1_20_1) {
            // 1.20.1's vanilla cooking serializer requires a result item id
            // string and always produces one item. Forge additionally accepts
            // an ItemStack object, but the standard form is more portable.
            json.addProperty("result", output.itemId());
            count = 1;
        } else if (edition.cookingSingleItem()) {
            // 1.21.1 uses STRICT_SINGLE_ITEM_CODEC: id/components, no count.
            JsonObject single = new JsonObject();
            single.addProperty("id", output.itemId());
            json.add("result", single);
            count = 1;
        } else {
            json.add("result", resultJson(output.itemId(), count, edition));
        }
        json.addProperty("experience", parseExperience(config, variant));
        json.addProperty("cookingtime", parseCookingTime(config, variant));
        return count;
    }

    private static int stonecutting(JsonObject json, List<IngredientSpec> resolved, List<SlotStack> slots,
                                    PlatformEdition edition) throws RecipeValidationException {
        IngredientSpec input = ingredient(resolved, 0);
        if (input == null) {
            throw new RecipeValidationException(RecipeError.NO_INPUT);
        }
        SlotStack output = requireOutput(slots, GeneratorLayout.outputSlot(ContainerType.STONECUTTER));
        int count = outputCount(output, edition);

        json.add("ingredient", ingredientJson(input, edition));
        if (edition == PlatformEdition.FORGE_1_20_1) {
            // 1.20.1's stonecutter serializer uses a plain item id string plus a
            // sibling count field, not an ItemStack object.
            json.addProperty("result", output.itemId());
            json.addProperty("count", count);
        } else {
            json.add("result", resultJson(output.itemId(), count, edition));
        }
        return count;
    }

    // ------------------------------------------------------------------
    // Smithing
    // ------------------------------------------------------------------

    private static int smithingTransform(JsonObject json, List<IngredientSpec> resolved,
                                         List<SlotStack> slots, PlatformEdition edition)
            throws RecipeValidationException {
        IngredientSpec template = ingredient(resolved, 0);
        IngredientSpec base = ingredient(resolved, 1);
        IngredientSpec addition = ingredient(resolved, 2);
        if (base == null) {
            throw new RecipeValidationException(RecipeError.NO_INPUT);
        }
        if (edition.requiresSmithingParts()) {
            if (template == null) {
                throw new RecipeValidationException(RecipeError.SMITHING_MISSING_TEMPLATE);
            }
            if (addition == null) {
                throw new RecipeValidationException(RecipeError.SMITHING_MISSING_ADDITION);
            }
        }

        if (template != null) {
            json.add("template", ingredientJson(template, edition));
        }
        json.add("base", ingredientJson(base, edition));
        if (addition != null) {
            json.add("addition", ingredientJson(addition, edition));
        }

        SlotStack output = requireOutput(slots, GeneratorLayout.outputSlot(ContainerType.SMITHING));
        int count = outputCount(output, edition);
        json.add("result", resultJson(output.itemId(), count, edition));
        return count;
    }

    private static int smithingTrim(JsonObject json, GeneratorConfig config,
                                    List<IngredientSpec> resolved, PlatformEdition edition)
            throws RecipeValidationException {
        IngredientSpec template = ingredient(resolved, 0);
        IngredientSpec base = ingredient(resolved, 1);
        IngredientSpec addition = ingredient(resolved, 2);
        if (template == null) {
            throw new RecipeValidationException(RecipeError.SMITHING_MISSING_TEMPLATE);
        }
        if (base == null) {
            throw new RecipeValidationException(RecipeError.NO_INPUT);
        }
        if (addition == null) {
            throw new RecipeValidationException(RecipeError.SMITHING_MISSING_ADDITION);
        }

        json.add("template", ingredientJson(template, edition));
        json.add("base", ingredientJson(base, edition));
        json.add("addition", ingredientJson(addition, edition));

        if (edition.trimNeedsPattern()) {
            String pattern = config.trimPattern == null ? "" : config.trimPattern.trim();
            if (!ResourceIds.isValid(pattern)) {
                throw new RecipeValidationException(RecipeError.TRIM_MISSING_PATTERN, pattern);
            }
            json.addProperty("pattern", pattern);
        }
        // 1.20.1 derives the trim pattern from the template item and has no
        // pattern field in its serializer.
        return 0;
    }

    // ------------------------------------------------------------------
    // Naming
    // ------------------------------------------------------------------

    private static String outputItemId(RecipeVariant variant, List<SlotStack> slots) {
        if (variant == RecipeVariant.SMITHING_TRIM) {
            SlotStack base = slot(slots, 1);
            return base.isEmpty() ? "" : base.itemId();
        }
        return slot(slots, GeneratorLayout.outputSlot(variant.container())).itemId();
    }

    private static String baseItemId(RecipeVariant variant, List<IngredientSpec> resolved,
                                     List<SlotStack> slots) {
        return switch (variant) {
            case SMITHING_TRANSFORM, SMITHING_TRIM -> {
                IngredientSpec base = ingredient(resolved, 1);
                yield base == null ? "" : base.id();
            }
            case CRAFTING_TRANSMUTE, CRAFTING_IMBUE -> {
                IngredientSpec input = ingredient(resolved, 4);
                yield input == null ? "" : input.id();
            }
            case CRAFTING_SHAPED, CRAFTING_SHAPELESS -> {
                for (int i = 0; i < CRAFTING_SLOTS; i++) {
                    IngredientSpec input = ingredient(resolved, i);
                    if (input != null) {
                        yield input.id();
                    }
                }
                yield "";
            }
            default -> {
                IngredientSpec input = ingredient(resolved, 0);
                yield input == null ? "" : input.id();
            }
        };
    }

    private static String autoName(RecipeVariant variant, GeneratorConfig config, PlatformEdition edition,
                                   String outputItemId, String baseItemId) {
        if (variant == RecipeVariant.SMITHING_TRIM) {
            String basePath = ResourceIds.pathOf(baseItemId);
            String name = (basePath.isEmpty() ? "trim" : basePath) + "_trim";
            if (edition.trimNeedsPattern()) {
                String patternPath = ResourceIds.pathOf(config.trimPattern);
                if (!patternPath.isEmpty()) {
                    name += "_" + patternPath;
                }
            }
            return name;
        }
        String outputPath = ResourceIds.pathOf(outputItemId);
        return outputPath.isEmpty() ? "recipe" : outputPath;
    }

    private static String chooseFileName(GeneratorConfig config, String autoName, String baseItemId,
                                         RecipeVariant variant) {
        String custom = config.customFileName == null ? "" : config.customFileName.trim();
        String selected;
        switch (config.fileNameMode) {
            case AUTO -> selected = autoName;
            case AUTO_SUFFIX -> selected = custom.isEmpty() ? autoName : autoName + "_" + custom;
            case INPUT_TO_OUTPUT -> {
                if (variant == RecipeVariant.SMITHING_TRIM) {
                    selected = autoName;
                } else {
                    String basePath = ResourceIds.pathOf(baseItemId);
                    selected = basePath.isEmpty() ? autoName : basePath + "_to_" + autoName;
                }
            }
            case NORMAL -> selected = custom.isEmpty() ? autoName : custom;
            default -> selected = autoName;
        }
        return ResourceIds.sanitizeFileName(selected);
    }

    // ------------------------------------------------------------------
    // Numbers
    // ------------------------------------------------------------------

    private static int parseCookingTime(GeneratorConfig config, RecipeVariant variant)
            throws RecipeValidationException {
        if (config.cookingTime == null || config.cookingTime.isBlank()) {
            return variant.defaultCookingTime();
        }
        try {
            int value = Integer.parseInt(config.cookingTime.trim());
            if (value <= 0 || value > 1_000_000) {
                throw new RecipeValidationException(RecipeError.INVALID_NUMBER, config.cookingTime);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new RecipeValidationException(RecipeError.INVALID_NUMBER, config.cookingTime);
        }
    }

    private static float parseExperience(GeneratorConfig config, RecipeVariant variant)
            throws RecipeValidationException {
        if (config.experience == null || config.experience.isBlank()) {
            return variant.defaultExperience();
        }
        try {
            float value = Float.parseFloat(config.experience.trim());
            if (!Float.isFinite(value) || value < 0.0F) {
                throw new RecipeValidationException(RecipeError.INVALID_NUMBER, config.experience);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new RecipeValidationException(RecipeError.INVALID_NUMBER, config.experience);
        }
    }
}
