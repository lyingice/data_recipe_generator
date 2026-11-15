package net.lyingice.datarecipegenerator.forge;

import net.lyingice.datarecipegenerator.forge.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> client: deliver the outcome so the screen can show it inline.
 */
public record GenerationResultMessage(boolean success, String errorCode, String detail, String path) {

    public static void encode(GenerationResultMessage message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.success);
        buffer.writeUtf(message.errorCode == null ? "" : message.errorCode, 128);
        buffer.writeUtf(message.detail == null ? "" : message.detail, 1024);
        buffer.writeUtf(message.path == null ? "" : message.path, 2048);
    }

    public static GenerationResultMessage decode(FriendlyByteBuf buffer) {
        return new GenerationResultMessage(
                buffer.readBoolean(),
                buffer.readUtf(128),
                buffer.readUtf(1024),
                buffer.readUtf(2048)
        );
    }

    public static void handle(GenerationResultMessage message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleGenerationResult(message));
        }
        context.setPacketHandled(true);
    }
}
