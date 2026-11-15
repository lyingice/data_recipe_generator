package net.lyingice.datarecipegenerator.neoforge;

import net.lyingice.datarecipegenerator.neoforge.client.ClientPacketHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record GenerationResultMessage(boolean success, String errorCode, String detail, String path)
        implements CustomPacketPayload {
    public static final Type<GenerationResultMessage> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(DataRecipeGeneratorNeoForge.MODID, "generation_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GenerationResultMessage> STREAM_CODEC =
            StreamCodec.of(GenerationResultMessage::encode, GenerationResultMessage::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, GenerationResultMessage message) {
        buffer.writeBoolean(message.success);
        buffer.writeUtf(message.errorCode == null ? "" : message.errorCode, 128);
        buffer.writeUtf(message.detail == null ? "" : message.detail, 1024);
        buffer.writeUtf(message.path == null ? "" : message.path, 2048);
    }

    private static GenerationResultMessage decode(RegistryFriendlyByteBuf buffer) {
        return new GenerationResultMessage(
                buffer.readBoolean(),
                buffer.readUtf(128),
                buffer.readUtf(1024),
                buffer.readUtf(2048)
        );
    }

    @Override
    public Type<GenerationResultMessage> type() {
        return TYPE;
    }

    public static void handle(GenerationResultMessage message, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.getDist() == Dist.CLIENT) {
                ClientPacketHandler.handleGenerationResult(message);
            }
        });
    }
}
