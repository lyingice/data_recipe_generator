package net.lyingice.datarecipegenerator.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;

import net.lyingice.datarecipegenerator.DataRecipeGeneratorMod;
import net.lyingice.datarecipegenerator.recipe.RecipeGenerator;

import java.nio.file.Path;
import java.util.Arrays;

/**
 * 客户端→服务端：请求生成配方文件。
 * 携带GUI的所有状态信息。
 */
public record GenerateRecipeMessage(
		String guiType,           // "crafting" 或 "smithing"
		String generalPath,       // 配方生成路径
		String generalName,       // 配方文件名
		String namespace,         // 命名空间
		boolean isShapeless,      // 工作台：是否为无形配方
		String recipeGroup,       // 配方组 (misc, equipment, building, redstone)
		int fileNameMode,         // 0=normal, 1=active, 2=half_active, 3=upgrade(锻造台)
		ItemStack[] slots         // 槽位物品快照
) implements CustomPacketPayload {

	public static final Type<GenerateRecipeMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DataRecipeGeneratorMod.MODID, "generate_recipe"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GenerateRecipeMessage> STREAM_CODEC =
			StreamCodec.of(GenerateRecipeMessage::write, GenerateRecipeMessage::read);

	public static void write(RegistryFriendlyByteBuf buffer, GenerateRecipeMessage msg) {
		buffer.writeUtf(msg.guiType);
		buffer.writeUtf(msg.generalPath != null ? msg.generalPath : "");
		buffer.writeUtf(msg.generalName != null ? msg.generalName : "");
		buffer.writeUtf(msg.namespace);
		buffer.writeBoolean(msg.isShapeless);
		buffer.writeUtf(msg.recipeGroup != null ? msg.recipeGroup : "");
		buffer.writeInt(msg.fileNameMode);
		buffer.writeVarInt(msg.slots.length);
		for (ItemStack stack : msg.slots) {
			ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, stack);
		}
	}

	public static GenerateRecipeMessage read(RegistryFriendlyByteBuf buffer) {
		String guiType = buffer.readUtf();
		String generalPath = buffer.readUtf();
		String generalName = buffer.readUtf();
		String namespace = buffer.readUtf();
		boolean isShapeless = buffer.readBoolean();
		String recipeGroup = buffer.readUtf();
		int fileNameMode = buffer.readInt();
		int slotCount = buffer.readVarInt();
		ItemStack[] slots = new ItemStack[slotCount];
		for (int i = 0; i < slotCount; i++) {
			slots[i] = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
		}
		return new GenerateRecipeMessage(guiType, generalPath, generalName, namespace, isShapeless, recipeGroup, fileNameMode, slots);
	}

	@Override
	public Type<GenerateRecipeMessage> type() {
		return TYPE;
	}

	/**
	 * 服务端处理器：生成配方JSON并写入文件。
	 */
	public static void handleGenerate(final GenerateRecipeMessage message, final IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.flow() == PacketFlow.SERVERBOUND && context.player() instanceof ServerPlayer serverPlayer) {
				try {
					// 确定文件名模式
					RecipeGenerator.FileNameMode mode = switch (message.fileNameMode) {
						case 1 -> RecipeGenerator.FileNameMode.ACTIVE;
						case 2 -> RecipeGenerator.FileNameMode.HALF_ACTIVE;
						case 3 -> RecipeGenerator.FileNameMode.UPGRADE;
						default -> RecipeGenerator.FileNameMode.NORMAL;
					};

					// 获取输出物品（不同GUI的输出槽位位置不同）
					ItemStack outputItem = ItemStack.EMPTY;
					ItemStack baseItem = ItemStack.EMPTY;
					String jsonContent;

					if ("crafting".equals(message.guiType)) {
						// 工作台：slots[0-8]=9个输入槽, slots[9]=输出槽
						outputItem = message.slots.length > 9 ? message.slots[9] : ItemStack.EMPTY;
						ItemStack[] inputSlots = Arrays.copyOf(message.slots, Math.min(9, message.slots.length));

						if (message.isShapeless) {
							jsonContent = RecipeGenerator.buildShapelessRecipe(inputSlots, outputItem, message.namespace, message.recipeGroup);
						} else {
							jsonContent = RecipeGenerator.buildShapedRecipe(inputSlots, outputItem, message.namespace, message.recipeGroup);
						}
					} else {
						// 锻造台：slots[0]=模板, slots[1]=基础, slots[2]=附加, slots[3]=输出
						ItemStack templateSlot = message.slots.length > 0 ? message.slots[0] : ItemStack.EMPTY;
						ItemStack baseSlot = message.slots.length > 1 ? message.slots[1] : ItemStack.EMPTY;
						ItemStack additionSlot = message.slots.length > 2 ? message.slots[2] : ItemStack.EMPTY;
						outputItem = message.slots.length > 3 ? message.slots[3] : ItemStack.EMPTY;
						baseItem = baseSlot;

						jsonContent = RecipeGenerator.buildSmithingRecipe(templateSlot, baseSlot, additionSlot, outputItem, message.namespace);
					}

					// 确定文件名
					String fileName = RecipeGenerator.determineFileName(mode, message.generalName, outputItem, baseItem);

					// 确保namespace有效
					String ns = (message.namespace != null && !message.namespace.isEmpty()) ? message.namespace : "custom";

					// 服务端运行目录
					Path serverDir = serverPlayer.level().getServer().getServerDirectory();

					// 写入文件
					Path writtenPath = RecipeGenerator.writeRecipeFile(serverDir, message.generalPath, ns, fileName, jsonContent);

					// 发送成功消息给玩家
					serverPlayer.sendSystemMessage(Component.translatable(
							"message.data_recipe_generator.recipe_generated",
							writtenPath.toString()
					));
					DataRecipeGeneratorMod.LOGGER.info("Recipe generated: {}", writtenPath);

				} catch (Exception e) {
					DataRecipeGeneratorMod.LOGGER.error("Failed to generate recipe", e);
					if (context.player() instanceof ServerPlayer sp) {
						sp.sendSystemMessage(Component.translatable("message.data_recipe_generator.recipe_generate_failed", e.getMessage()));
					}
				}
			}
		}).exceptionally(e -> {
			context.connection().disconnect(Component.literal(e.getMessage()));
			return null;
		});
	}
	public static void registerMessage(FMLCommonSetupEvent event) {
		DataRecipeGeneratorMod.addServerBoundNetworkMessage(GenerateRecipeMessage.TYPE, GenerateRecipeMessage.STREAM_CODEC, GenerateRecipeMessage::handleGenerate);
	}
}
