package net.lyingice.datarecipegenerator.forge;

import net.lyingice.datarecipegenerator.common.PlatformEdition;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(DataRecipeGeneratorForge.MODID)
public final class DataRecipeGeneratorForge {
    public static final String MODID = "data_recipe_generator";
    public static final PlatformEdition EDITION = PlatformEdition.FORGE_1_20_1;
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public DataRecipeGeneratorForge() {
        // Forge 1.20.1 instantiates the mod with a no-argument constructor.
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModMenus.MENUS.register(modEventBus);
        modEventBus.addListener(ModNetwork::register);
    }
}
