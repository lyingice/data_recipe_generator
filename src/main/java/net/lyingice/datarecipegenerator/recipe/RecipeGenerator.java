package net.lyingice.datarecipegenerator.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import net.lyingice.datarecipegenerator.DataRecipeGeneratorMod;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 配方生成核心工具类。
 * 负责从槽位物品构建配方JSON，以及确定输出文件名和路径。
 */
public class RecipeGenerator {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	// ==================== 文件名策略 ====================

	/**
	 * 文件名生成模式
	 */
	public enum FileNameMode {
		NORMAL,        // 普通模式：使用用户自定义名称
		ACTIVE,        // 动态模式：自动获取输出物品英文ID
		HALF_ACTIVE,   // 半动态模式：动态ID + 用户后缀
		UPGRADE        // 锻造台专属：base_to_result 格式
	}

	/**
	 * 根据模式和上下文决定最终配方文件名（不含路径和扩展名）。
	 * @param mode          文件名模式
	 * @param customName    用户在"general name"输入框的值
	 * @param outputItem    输出槽位的物品（可能为空）
	 * @param baseItem      锻造台base槽位物品（仅UPGRADE模式使用）
	 * @return              最终文件名，如 "apple"、"apple_plus"、"diamond_sword_to_netherite_sword"
	 */
	public static String determineFileName(FileNameMode mode, String customName, ItemStack outputItem, ItemStack baseItem) {
		String autoName = getItemIdSafe(outputItem);

		switch (mode) {
			case ACTIVE:
				return autoName;
			case HALF_ACTIVE:
				if (customName != null && !customName.isEmpty()) {
					return autoName + "_" + customName;
				}
				return autoName;
			case UPGRADE:
				String baseName = getItemIdSafe(baseItem);
				if (!baseName.isEmpty() && !autoName.isEmpty()) {
					return baseName + "_to_" + autoName;
				}
				return autoName;
			case NORMAL:
			default:
				if (customName != null && !customName.isEmpty()) {
					return customName;
				}
				return autoName;
		}
	}

	/**
	 * 安全地获取物品的注册名路径部分（不含命名空间）。
	 * 例如 minecraft:apple → apple
	 */
	public static String getItemIdSafe(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return "unknown";
		}
		ResourceLocation rl = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (rl == null) {
			return "unknown";
		}
		return rl.getPath();
	}

	/**
	 * 获取物品的完整注册名（含命名空间）。
	 * 例如 minecraft:apple
	 */
	public static String getItemFullId(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return "minecraft:air";
		}
		ResourceLocation rl = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (rl == null) {
			return "minecraft:air";
		}
		return rl.toString();
	}

	// ==================== JSON构建 ====================

	/**
	 * 将物品转为配方中的 ingredient JSON 对象。
	 * 支持普通物品和tag（以#开头的物品视为tag）。
	 */
	public static JsonObject itemToIngredient(ItemStack stack) {
		JsonObject ingredient = new JsonObject();
		if (stack == null || stack.isEmpty()) {
			ingredient.addProperty("item", "minecraft:air");
			return ingredient;
		}
		String fullId = getItemFullId(stack);
		// 如果物品ID以"tag:"开头则视为标签
		// 实际使用中通过物品注册名无法表达tag，这里保留扩展点
		ingredient.addProperty("item", fullId);
		return ingredient;
	}

	/**
	 * 构建工作台有序配方 (minecraft:crafting_shaped)
	 * @param slots         9个槽位 (ItemStack[9])，顺序为左上到右下
	 * @param resultSlot    输出槽位
	 * @param namespace     命名空间
	 * @param group         配方组 (如 "misc", "building" 等)，可为null
	 * @return              格式化的JSON字符串
	 */
	public static String buildShapedRecipe(ItemStack[] slots, ItemStack resultSlot, String namespace, String group) {
		JsonObject recipe = new JsonObject();
		recipe.addProperty("type", "minecraft:crafting_shaped");

		if (group != null && !group.isEmpty()) {
			recipe.addProperty("group", group);
		}

		// 检测哪些行和列被使用
		boolean[] usedRows = new boolean[3];
		boolean[] usedCols = new boolean[3];
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				if (!slots[row * 3 + col].isEmpty()) {
					usedRows[row] = true;
					usedCols[col] = true;
				}
			}
		}

		// 找到最小编码范围
		int minRow = 2, maxRow = 0, minCol = 2, maxCol = 0;
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				if (!slots[row * 3 + col].isEmpty()) {
					if (row < minRow) minRow = row;
					if (row > maxRow) maxRow = row;
					if (col < minCol) minCol = col;
					if (col > maxCol) maxCol = col;
				}
			}
		}

		// 构建pattern和key
		Map<String, JsonObject> keyMap = new LinkedHashMap<>();
		JsonArray pattern = new JsonArray();
		char nextKey = 'A';

		for (int row = minRow; row <= maxRow; row++) {
			StringBuilder rowPattern = new StringBuilder();
			for (int col = minCol; col <= maxCol; col++) {
				ItemStack slot = slots[row * 3 + col];
				if (slot.isEmpty()) {
					rowPattern.append(' ');
				} else {
					// 查找是否已有相同物品的key
					String existingKey = findExistingKey(keyMap, slot);
					if (existingKey != null) {
						rowPattern.append(existingKey);
					} else {
						char keyChar = nextKey++;
						String key = String.valueOf(keyChar);
						keyMap.put(key, itemToIngredient(slot));
						rowPattern.append(key);
					}
				}
			}
			pattern.add(rowPattern.toString());
		}

		recipe.add("pattern", pattern);

		JsonObject key = new JsonObject();
		for (Map.Entry<String, JsonObject> entry : keyMap.entrySet()) {
			key.add(entry.getKey(), entry.getValue());
		}
		recipe.add("key", key);

		// result
		JsonObject result = new JsonObject();
		result.addProperty("id", getItemFullId(resultSlot));
		result.addProperty("count", resultSlot.isEmpty() ? 1 : resultSlot.getCount());
		recipe.add("result", result);

		return GSON.toJson(recipe);
	}

	/**
	 * 构建工作台无序配方 (minecraft:crafting_shapeless)
	 */
	public static String buildShapelessRecipe(ItemStack[] slots, ItemStack resultSlot, String namespace, String group) {
		JsonObject recipe = new JsonObject();
		recipe.addProperty("type", "minecraft:crafting_shapeless");

		if (group != null && !group.isEmpty()) {
			recipe.addProperty("group", group);
		}

		JsonArray ingredients = new JsonArray();
		for (ItemStack slot : slots) {
			if (!slot.isEmpty()) {
				ingredients.add(itemToIngredient(slot));
			}
		}
		recipe.add("ingredients", ingredients);

		JsonObject result = new JsonObject();
		result.addProperty("id", getItemFullId(resultSlot));
		result.addProperty("count", resultSlot.isEmpty() ? 1 : resultSlot.getCount());
		recipe.add("result", result);

		return GSON.toJson(recipe);
	}

	/**
	 * 构建锻造台配方 (minecraft:smithing_transform)
	 * @param templateSlot  模板槽
	 * @param baseSlot      基础物品槽
	 * @param additionSlot  附加材料槽
	 * @param resultSlot    输出槽
	 * @param namespace     命名空间
	 */
	public static String buildSmithingRecipe(ItemStack templateSlot, ItemStack baseSlot,
	                                           ItemStack additionSlot, ItemStack resultSlot,
	                                           String namespace) {
		JsonObject recipe = new JsonObject();
		recipe.addProperty("type", "minecraft:smithing_transform");

		recipe.add("template", itemToIngredient(templateSlot));
		recipe.add("base", itemToIngredient(baseSlot));
		recipe.add("addition", itemToIngredient(additionSlot));

		JsonObject result = new JsonObject();
		result.addProperty("id", getItemFullId(resultSlot));
		result.addProperty("count", resultSlot.isEmpty() ? 1 : resultSlot.getCount());
		recipe.add("result", result);

		return GSON.toJson(recipe);
	}

	// ==================== 辅助方法 ====================

	/**
	 * 在keyMap中查找与给定物品相同的已注册key。
	 */
	private static String findExistingKey(Map<String, JsonObject> keyMap, ItemStack stack) {
		JsonObject ingredient = itemToIngredient(stack);
		for (Map.Entry<String, JsonObject> entry : keyMap.entrySet()) {
			if (entry.getValue().equals(ingredient)) {
				return entry.getKey();
			}
		}
		return null;
	}

	// ==================== 文件写入 ====================

	/**
	 * 将配方JSON写入文件。
	 * @param baseDir      基础目录（服务端运行目录或用户自定义路径）
	 * @param userPath     用户自定义路径（复选框勾选时使用），可为空
	 * @param namespace    命名空间
	 * @param fileName     文件名（不含扩展名）
	 * @param jsonContent  配方JSON内容
	 * @return             实际写入的文件路径
	 */
	public static Path writeRecipeFile(Path baseDir, String userPath, String namespace, String fileName, String jsonContent) throws IOException {
		Path recipesDir;
		if (userPath != null && !userPath.isEmpty()) {
			// 用户自定义路径
			recipesDir = Paths.get(userPath);
		} else {
			// 默认路径：baseDir/data_recipe_generator_output/<namespace>/recipes/
			recipesDir = baseDir.resolve("data_recipe_generator_output")
			                   .resolve(namespace)
			                   .resolve("recipes");
		}
		Files.createDirectories(recipesDir);
		Path recipeFile = recipesDir.resolve(fileName + ".json");
		Files.writeString(recipeFile, jsonContent);
		return recipeFile;
	}
}
