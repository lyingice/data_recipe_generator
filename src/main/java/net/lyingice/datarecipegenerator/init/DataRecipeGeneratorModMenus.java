/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.lyingice.datarecipegenerator.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.client.Minecraft;

import net.lyingice.datarecipegenerator.world.inventory.CraftingRecipeGuiMenu;
import net.lyingice.datarecipegenerator.world.inventory.SmithingRecipeGuiMenu;
import net.lyingice.datarecipegenerator.network.MenuStateUpdateMessage;
import net.lyingice.datarecipegenerator.DataRecipeGeneratorMod;

import java.util.Map;

public class DataRecipeGeneratorModMenus {
	public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(Registries.MENU, DataRecipeGeneratorMod.MODID);
	public static final DeferredHolder<MenuType<?>, MenuType<CraftingRecipeGuiMenu>> CRAFTING_RECIPE_GUI = REGISTRY.register("crafting_recipe_gui", () -> IMenuTypeExtension.create(CraftingRecipeGuiMenu::new));
	public static final DeferredHolder<MenuType<?>, MenuType<SmithingRecipeGuiMenu>> SMITHING_RECIPE_GUI = REGISTRY.register("smithing_recipe_gui", () -> IMenuTypeExtension.create(SmithingRecipeGuiMenu::new));

	public interface MenuAccessor {
		Map<String, Object> getMenuState();

		Map<Integer, Slot> getSlots();

		default void sendMenuStateUpdate(Player player, int elementType, String name, Object elementState, boolean needClientUpdate) {
			getMenuState().put(elementType + ":" + name, elementState);
			if (player instanceof ServerPlayer serverPlayer) {
				PacketDistributor.sendToPlayer(serverPlayer, new MenuStateUpdateMessage(elementType, name, elementState));
			} else if (player.level().isClientSide) {
				if (Minecraft.getInstance().screen instanceof DataRecipeGeneratorModScreens.ScreenAccessor accessor && needClientUpdate)
					accessor.updateMenuState(elementType, name, elementState);
				PacketDistributor.sendToServer(new MenuStateUpdateMessage(elementType, name, elementState));
			}
		}

		default <T> T getMenuState(int elementType, String name, T defaultValue) {
			try {
				return (T) getMenuState().getOrDefault(elementType + ":" + name, defaultValue);
			} catch (ClassCastException e) {
				return defaultValue;
			}
		}
	}
}