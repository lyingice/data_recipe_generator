/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.lyingice.datarecipegenerator.init;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.lyingice.datarecipegenerator.client.gui.CraftingRecipeGuiScreen;
import net.lyingice.datarecipegenerator.client.gui.SmithingRecipeGuiScreen;

@EventBusSubscriber(Dist.CLIENT)
public class DataRecipeGeneratorModScreens {
	@SubscribeEvent
	public static void clientLoad(RegisterMenuScreensEvent event) {
		event.register(DataRecipeGeneratorModMenus.CRAFTING_RECIPE_GUI.get(), CraftingRecipeGuiScreen::new);
		event.register(DataRecipeGeneratorModMenus.SMITHING_RECIPE_GUI.get(), SmithingRecipeGuiScreen::new);
	}

	public interface ScreenAccessor {
		void updateMenuState(int elementType, String name, Object elementState);
	}
}