package net.lyingice.datarecipegenerator.forge;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, DataRecipeGeneratorForge.MODID);

    public static final RegistryObject<MenuType<GeneratorMenu>> GENERATOR =
            MENUS.register("generator", () -> IForgeMenuType.create(GeneratorMenu::new));

    private ModMenus() {
    }
}
