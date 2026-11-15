package net.lyingice.datarecipegenerator.neoforge.client;

import net.lyingice.datarecipegenerator.neoforge.DataRecipeGeneratorNeoForge;
import net.lyingice.datarecipegenerator.neoforge.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = DataRecipeGeneratorNeoForge.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.GENERATOR.get(), GeneratorScreen::new);
    }
}
