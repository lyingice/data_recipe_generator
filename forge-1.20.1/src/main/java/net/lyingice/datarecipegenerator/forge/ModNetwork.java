package net.lyingice.datarecipegenerator.forge;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(DataRecipeGeneratorForge.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private ModNetwork() {
    }

    public static void register(FMLCommonSetupEvent event) {
        int id = 0;
        CHANNEL.registerMessage(id++, OpenGeneratorMessage.class,
                OpenGeneratorMessage::encode, OpenGeneratorMessage::decode, OpenGeneratorMessage::handle);
        CHANNEL.registerMessage(id++, GenerateRecipeMessage.class,
                GenerateRecipeMessage::encode, GenerateRecipeMessage::decode, GenerateRecipeMessage::handle);
        CHANNEL.registerMessage(id++, GenerationResultMessage.class,
                GenerationResultMessage::encode, GenerationResultMessage::decode, GenerationResultMessage::handle);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
