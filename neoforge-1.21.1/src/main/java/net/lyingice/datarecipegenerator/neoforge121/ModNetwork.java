package net.lyingice.datarecipegenerator.neoforge121;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(DataRecipeGeneratorNeoForge121.MODID);
        registrar.playToServer(OpenGeneratorMessage.TYPE, OpenGeneratorMessage.STREAM_CODEC,
                OpenGeneratorMessage::handle);
        registrar.playToServer(GenerateRecipeMessage.TYPE, GenerateRecipeMessage.STREAM_CODEC,
                GenerateRecipeMessage::handle);
        registrar.playToClient(GenerationResultMessage.TYPE, GenerationResultMessage.STREAM_CODEC,
                GenerationResultMessage::handle);
    }

    public static void sendToServer(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public static void sendToPlayer(ServerPlayer player,
                                    net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
