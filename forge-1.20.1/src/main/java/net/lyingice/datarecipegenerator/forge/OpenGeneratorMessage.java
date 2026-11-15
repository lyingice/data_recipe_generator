package net.lyingice.datarecipegenerator.forge;

import net.lyingice.datarecipegenerator.common.ContainerType;
import net.lyingice.datarecipegenerator.common.GeneratorConfig;
import net.lyingice.datarecipegenerator.common.RecipeVariant;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * Client -> server: switch the open generator to another container while
 * keeping the current options.
 */
public record OpenGeneratorMessage(ContainerType containerType, GeneratorConfig config) {

    public static void encode(OpenGeneratorMessage message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.containerType.name());
        buffer.writeUtf(message.config.toJson());
    }

    public static OpenGeneratorMessage decode(FriendlyByteBuf buffer) {
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

    public static void handle(OpenGeneratorMessage message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.containerMenu instanceof GeneratorMenu)) {
                return;
            }
            if (!GeneratorCommand.canUse(player)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                        "message.data_recipe_generator.error.no_permission"));
                return;
            }
            List<RecipeVariant> variants = DataRecipeGeneratorForge.EDITION.variants(message.containerType());
            GeneratorConfig config = message.config().copy();
            if (variants.isEmpty() || !variants.contains(config.variant)) {
                config.variant = message.containerType().defaultVariant();
            }
            config.normalize();
            GeneratorCommand.openMenu(player, message.containerType(), config);
        });
        context.setPacketHandled(true);
    }
}
