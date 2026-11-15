package net.lyingice.datarecipegenerator.forge;

import net.lyingice.datarecipegenerator.common.GeneratorConfig;
import net.lyingice.datarecipegenerator.common.RecipeError;
import net.lyingice.datarecipegenerator.common.RecipeValidationException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Client -> server: generate a recipe from the slot contents currently held by
 * the server-side menu. The client never sends item data, so the result cannot
 * be forged.
 */
public record GenerateRecipeMessage(GeneratorConfig config) {

    public static void encode(GenerateRecipeMessage message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.config.toJson());
    }

    public static GenerateRecipeMessage decode(FriendlyByteBuf buffer) {
        String json = buffer.readUtf(GeneratorConfig.MAX_JSON_LENGTH);
        try {
            return new GenerateRecipeMessage(GeneratorConfig.fromJson(json));
        } catch (IllegalArgumentException e) {
            return new GenerateRecipeMessage(new GeneratorConfig());
        }
    }

    public static void handle(GenerateRecipeMessage message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.containerMenu instanceof GeneratorMenu menu)) {
                return;
            }
            if (!GeneratorCommand.canUse(player)) {
                player.sendSystemMessage(Component.translatable(
                        "message.data_recipe_generator.error.no_permission"));
                return;
            }
            GeneratorConfig config = message.config().copy();
            config.normalize();
            if (config.variant.container() != menu.getContainerType()) {
                String detail = config.variant.name();
                player.sendSystemMessage(Component.translatable(
                        RecipeError.UNSUPPORTED_VARIANT.messageKey(), detail));
                ModNetwork.sendToPlayer(player, new GenerationResultMessage(false,
                        RecipeError.UNSUPPORTED_VARIANT.name().toLowerCase(Locale.ROOT), detail, ""));
                return;
            }
            try {
                GeneratorMenu.generate(player, menu, config);
            } catch (RuntimeException e) {
                // generate() handles its own exceptions; this is a final guard
                // so a bad payload cannot disconnect the player.
                DataRecipeGeneratorForge.LOGGER.error("Recipe request failed", e);
            }
        });
        context.setPacketHandled(true);
    }
}
