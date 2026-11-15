package net.lyingice.datarecipegenerator.neoforge;

import net.lyingice.datarecipegenerator.common.PlatformEdition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(DataRecipeGeneratorNeoForge.MODID)
public final class DataRecipeGeneratorNeoForge {
    public static final String MODID = "data_recipe_generator";
    public static final PlatformEdition EDITION = PlatformEdition.NEOFORGE_26_1_2;
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public DataRecipeGeneratorNeoForge(IEventBus modEventBus) {
        ModMenus.MENUS.register(modEventBus);
        modEventBus.addListener(ModNetwork::register);
    }
}
