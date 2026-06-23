package net.lyingice.datarecipegenerator.world.inventory;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.util.TriState;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.core.BlockPos;

import net.lyingice.datarecipegenerator.init.DataRecipeGeneratorModMenus;

import io.netty.buffer.Unpooled;

@EventBusSubscriber
public class RecipeGuiOpenHandler {

	private static final ResourceLocation CRAFTING_TABLE = ResourceLocation.withDefaultNamespace("crafting_table");
	private static final ResourceLocation SMITHING_TABLE = ResourceLocation.withDefaultNamespace("smithing_table");

	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		Player player = event.getEntity();
		Level level = event.getLevel();

		if (!player.isCreative()) return;
		if (!player.isShiftKeyDown()) return;
		if (event.getHand() != InteractionHand.MAIN_HAND) return;

		BlockPos pos = event.getPos();
		ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
		if (blockId == null) return;

		boolean isCrafting = CRAFTING_TABLE.equals(blockId);
		boolean isSmithing = SMITHING_TABLE.equals(blockId);

		if (!isCrafting && !isSmithing) return;

		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            event.setUseBlock(TriState.FALSE);
            event.setUseItem(TriState.FALSE);
			event.setCancellationResult(InteractionResult.SUCCESS);

			FriendlyByteBuf extraData = new FriendlyByteBuf(Unpooled.buffer());
			extraData.writeBlockPos(pos);

			if (isCrafting) {
				serverPlayer.openMenu(new SimpleMenuProvider(
						(id, inv, p) -> new CraftingRecipeGuiMenu(id, inv, extraData),
						Component.translatable("gui.data_recipe_generator.crafting_recipe_gui.title")
				), buf -> buf.writeBlockPos(pos));
			} else {
				serverPlayer.openMenu(new SimpleMenuProvider(
						(id, inv, p) -> new SmithingRecipeGuiMenu(id, inv, extraData),
						Component.translatable("gui.data_recipe_generator.smithing_recipe_gui.title")
				), buf -> buf.writeBlockPos(pos));
			}
		} else {
            event.setUseBlock(TriState.FALSE);
            event.setUseItem(TriState.FALSE);
			event.setCancellationResult(InteractionResult.SUCCESS);
		}
	}
}
