package net.lyingice.datarecipegenerator.neoforge121.client;

import net.lyingice.datarecipegenerator.neoforge121.GenerationResultMessage;
import net.minecraft.client.Minecraft;

public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void handleGenerationResult(GenerationResultMessage message) {
        if (Minecraft.getInstance().screen instanceof GeneratorScreen screen) {
            screen.setGenerationResult(message.success(), message.errorCode(), message.detail(), message.path());
        }
    }
}
