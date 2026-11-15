package net.lyingice.datarecipegenerator.neoforge;

import net.lyingice.datarecipegenerator.common.ContainerType;
import net.lyingice.datarecipegenerator.common.GeneratorConfig;
import net.lyingice.datarecipegenerator.common.RecipeVariant;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record OpenGeneratorMessage(ContainerType containerType, GeneratorConfig config)
        implements CustomPacketPayload {
    public static final Type<OpenGeneratorMessage> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(DataRecipeGeneratorNeoForge.MODID, "open_generator"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenGeneratorMessage> STREAM_CODEC =
            StreamCodec.of(OpenGeneratorMessage::encode, OpenGeneratorMessage::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, OpenGeneratorMessage message) {
        buffer.writeUtf(message.containerType.name());
        buffer.writeUtf(message.config.toJson());
    }

    private static OpenGeneratorMessage decode(RegistryFriendlyByteBuf buffer) {
        ContainerType type = ContainerType.fromId(buffer.readUtf(64));
        if (type == null) {
            type = ContainerType.CRAFTING;
        }
        String json = buffer.readUtf(GeneratorConfig.MAX_JSON_LENGTH);
        GeneratorConfig config;
        try {
            config = GeneratorConfig.fromJson(json);
        } catch (IllegalArgumentException e) {
            config = new GeneratorConfig();
        }
        return new OpenGeneratorMessage(type, config);
    }

    @Override
    public Type<OpenGeneratorMessage> type() {
        return TYPE;
    }

    public static void handle(OpenGeneratorMessage message, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof GeneratorMenu)) {
                return;
            }
            if (!GeneratorCommand.canUse(player)) {
                player.sendSystemMessage(Component.translatable(
                        "message.data_recipe_generator.error.no_permission"));
                return;
            }
            List<RecipeVariant> variants = DataRecipeGeneratorNeoForge.EDITION.variants(message.containerType());
            GeneratorConfig config = message.config().copy();
            if (variants.isEmpty() || !variants.contains(config.variant)) {
                config.variant = message.containerType().defaultVariant();
            }
            config.normalize();
            GeneratorCommand.openMenu(player, message.containerType(), config);
        });
    }
}
