package net.lyingice.datarecipegenerator.neoforge121;

import net.lyingice.datarecipegenerator.common.PlatformEdition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(DataRecipeGeneratorNeoForge121.MODID)
public final class DataRecipeGeneratorNeoForge121 {
    public static final String MODID = "data_recipe_generator";
    public static final PlatformEdition EDITION = PlatformEdition.NEOFORGE_1_21_1;
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public DataRecipeGeneratorNeoForge121(IEventBus modEventBus) {
        ModMenus.MENUS.register(modEventBus);
        modEventBus.addListener(ModNetwork::register);
    }
}
