package net.lyingice.datarecipegenerator.neoforge;

import net.lyingice.datarecipegenerator.common.GeneratorConfig;
import net.lyingice.datarecipegenerator.common.RecipeError;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Locale;

public record GenerateRecipeMessage(GeneratorConfig config) implements CustomPacketPayload {
    public static final Type<GenerateRecipeMessage> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(DataRecipeGeneratorNeoForge.MODID, "generate_recipe"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GenerateRecipeMessage> STREAM_CODEC =
            StreamCodec.of(GenerateRecipeMessage::encode, GenerateRecipeMessage::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, GenerateRecipeMessage message) {
        buffer.writeUtf(message.config.toJson());
    }

    private static GenerateRecipeMessage decode(RegistryFriendlyByteBuf buffer) {
        String json = buffer.readUtf(GeneratorConfig.MAX_JSON_LENGTH);
        try {
            return new GenerateRecipeMessage(GeneratorConfig.fromJson(json));
        } catch (IllegalArgumentException e) {
            return new GenerateRecipeMessage(new GeneratorConfig());
        }
    }

    @Override
    public Type<GenerateRecipeMessage> type() {
        return TYPE;
    }

    public static void handle(GenerateRecipeMessage message, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof GeneratorMenu menu)) {
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
                DataRecipeGeneratorNeoForge.LOGGER.error("Recipe request failed", e);
            }
        });
    }
}
