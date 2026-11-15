package net.lyingice.datarecipegenerator.forge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.lyingice.datarecipegenerator.common.ContainerType;
import net.lyingice.datarecipegenerator.common.GeneratorConfig;
import net.lyingice.datarecipegenerator.common.RecipeVariant;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

@Mod.EventBusSubscriber(modid = DataRecipeGeneratorForge.MODID)
public final class GeneratorCommand {
    private GeneratorCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        registerRoot(event.getDispatcher(), "datarecipe");
        registerRoot(event.getDispatcher(), "drg");
    }

    private static void registerRoot(CommandDispatcher<CommandSourceStack> dispatcher, String name) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(name)
                .requires(GeneratorCommand::canUse)
                .executes(context -> open(context, ContainerType.CRAFTING, null));

        add(root, "crafting", ContainerType.CRAFTING, null);
        add(root, "crafting_shaped", ContainerType.CRAFTING, RecipeVariant.CRAFTING_SHAPED);
        add(root, "crafting_shapeless", ContainerType.CRAFTING, RecipeVariant.CRAFTING_SHAPELESS);
        add(root, "furnace", ContainerType.FURNACE, RecipeVariant.SMELTING);
        add(root, "smelting", ContainerType.FURNACE, RecipeVariant.SMELTING);
        add(root, "blast_furnace", ContainerType.BLAST_FURNACE, RecipeVariant.BLASTING);
        add(root, "blasting", ContainerType.BLAST_FURNACE, RecipeVariant.BLASTING);
        add(root, "smoker", ContainerType.SMOKER, RecipeVariant.SMOKING);
        add(root, "smoking", ContainerType.SMOKER, RecipeVariant.SMOKING);
        add(root, "campfire", ContainerType.CAMPFIRE, RecipeVariant.CAMPFIRE_COOKING);
        add(root, "campfire_cooking", ContainerType.CAMPFIRE, RecipeVariant.CAMPFIRE_COOKING);
        add(root, "stonecutter", ContainerType.STONECUTTER, RecipeVariant.STONECUTTING);
        add(root, "stonecutting", ContainerType.STONECUTTER, RecipeVariant.STONECUTTING);
        add(root, "smithing", ContainerType.SMITHING, null);
        add(root, "smithing_transform", ContainerType.SMITHING, RecipeVariant.SMITHING_TRANSFORM);
        add(root, "smithing_trim", ContainerType.SMITHING, RecipeVariant.SMITHING_TRIM);

        dispatcher.register(root);
    }

    private static void add(LiteralArgumentBuilder<CommandSourceStack> root, String name,
                            ContainerType type, RecipeVariant variant) {
        root.then(Commands.literal(name).executes(context -> open(context, type, variant)));
    }

    private static int open(CommandContext<CommandSourceStack> context, ContainerType type,
                            RecipeVariant variant) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.translatable(
                    "commands.data_recipe_generator.players_only"));
            return 0;
        }
        List<RecipeVariant> variants = DataRecipeGeneratorForge.EDITION.variants(type);
        GeneratorConfig config = new GeneratorConfig();
        if (variant != null && variants.contains(variant)) {
            config.variant = variant;
        } else if (!variants.isEmpty()) {
            config.variant = variants.get(0);
        } else {
            config.variant = type.defaultVariant();
        }
        config.category = config.variant.defaultCategory();
        openMenu(player, type, config);
        return 1;
    }

    public static void openMenu(ServerPlayer player, ContainerType type, GeneratorConfig config) {
        GeneratorConfig copy = config.copy();
        copy.normalize();
        NetworkHooks.openScreen(player,
                new SimpleMenuProvider(
                        (id, inventory, p) -> new GeneratorMenu(id, inventory, type, copy),
                        Component.translatable(type.titleKey())),
                buffer -> GeneratorMenu.writeData(buffer, type, copy));
    }

    public static boolean canUse(CommandSourceStack source) {
        if (source.hasPermission(2)) {
            return true;
        }
        ServerPlayer player = source.getPlayer();
        return player != null
                && source.getServer() != null
                && source.getServer().isSingleplayer()
                && source.getServer().isSingleplayerOwner(player.getGameProfile());
    }

    public static boolean canUse(ServerPlayer player) {
        return player.hasPermissions(2)
                || (player.getServer() != null
                && player.getServer().isSingleplayer()
                && player.getServer().isSingleplayerOwner(player.getGameProfile()));
    }
}
