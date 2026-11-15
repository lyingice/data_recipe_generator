package net.lyingice.datarecipegenerator.neoforge121;

import net.lyingice.datarecipegenerator.common.ContainerType;
import net.lyingice.datarecipegenerator.common.GeneratorConfig;
import net.lyingice.datarecipegenerator.common.GeneratorLayout;
import net.lyingice.datarecipegenerator.common.RecipeBuildResult;
import net.lyingice.datarecipegenerator.common.RecipeBuilder;
import net.lyingice.datarecipegenerator.common.RecipeError;
import net.lyingice.datarecipegenerator.common.RecipeValidationException;
import net.lyingice.datarecipegenerator.common.RecipeWriter;
import net.lyingice.datarecipegenerator.common.SlotStack;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GeneratorMenu extends AbstractContainerMenu {
    private final ContainerType containerType;
    private final GeneratorConfig config;
    private final SimpleContainer container;
    private final int customSlotCount;

    /** Client constructor: reads the kind and config written by the server. */
    public GeneratorMenu(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(id, inventory, readType(data), readConfig(data));
    }

    /** Server constructor. */
    public GeneratorMenu(int id, Inventory inventory, ContainerType containerType, GeneratorConfig config) {
        super(ModMenus.GENERATOR.get(), id);
        this.containerType = containerType == null ? ContainerType.CRAFTING : containerType;
        this.config = config == null ? new GeneratorConfig() : config.copy();
        this.config.normalize();
        this.customSlotCount = GeneratorLayout.slotCount(this.containerType);
        this.container = new SimpleContainer(this.customSlotCount);

        int[] xs = GeneratorLayout.slotX(this.containerType);
        int[] ys = GeneratorLayout.slotY(this.containerType);
        for (int i = 0; i < this.customSlotCount; i++) {
            this.addSlot(new Slot(this.container, i, xs[i], ys[i]));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    public ContainerType getContainerType() {
        return this.containerType;
    }

    public GeneratorConfig getConfig() {
        return this.config;
    }

    public SimpleContainer getContainer() {
        return this.container;
    }

    public int getCustomSlotCount() {
        return this.customSlotCount;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < this.customSlotCount) {
                if (!this.moveItemStackTo(stack, this.customSlotCount, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, this.customSlotCount, false)) {
                if (index < this.customSlotCount + 27) {
                    if (!this.moveItemStackTo(stack, this.customSlotCount + 27, this.slots.size(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(stack, this.customSlotCount, this.customSlotCount + 27, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        boolean drop = !serverPlayer.isAlive() || serverPlayer.hasDisconnected();
        for (int i = 0; i < this.container.getContainerSize(); i++) {
            ItemStack stack = this.container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (drop) {
                serverPlayer.drop(stack, false);
            } else {
                serverPlayer.getInventory().placeItemBackInInventory(stack);
            }
            this.container.setItem(i, ItemStack.EMPTY);
        }
    }

    public static void writeData(FriendlyByteBuf buffer, ContainerType type, GeneratorConfig config) {
        buffer.writeUtf(type.name());
        buffer.writeUtf(config.toJson());
    }

    private static ContainerType readType(RegistryFriendlyByteBuf buffer) {
        ContainerType type = ContainerType.fromId(buffer.readUtf(64));
        return type == null ? ContainerType.CRAFTING : type;
    }

    private static GeneratorConfig readConfig(RegistryFriendlyByteBuf buffer) {
        String json = buffer.readUtf(GeneratorConfig.MAX_JSON_LENGTH);
        try {
            return GeneratorConfig.fromJson(json);
        } catch (IllegalArgumentException e) {
            return new GeneratorConfig();
        }
    }

    // ------------------------------------------------------------------
    // Server-side generation
    // ------------------------------------------------------------------

    public static void generate(ServerPlayer player, GeneratorMenu menu, GeneratorConfig config) {
        config.normalize();
        try {
            RecipeBuildResult built = RecipeBuilder.build(config.variant, config, snapshot(menu),
                    DataRecipeGeneratorNeoForge121.EDITION);
            Path serverDirectory = player.level().getServer().getServerDirectory();
            Path written = RecipeWriter.write(serverDirectory, config, DataRecipeGeneratorNeoForge121.EDITION,
                    built.fileName(), built.json());
            String display = displayPath(serverDirectory, written);
            player.sendSystemMessage(Component.translatable(
                    "message.data_recipe_generator.recipe_generated", display));
            ModNetwork.sendToPlayer(player, new GenerationResultMessage(true, "", "", display));
        } catch (RecipeValidationException e) {
            sendError(player, e.error(), e.detail());
        } catch (IOException e) {
            DataRecipeGeneratorNeoForge121.LOGGER.error("Failed to write generated recipe", e);
            sendError(player, RecipeError.IO_ERROR, String.valueOf(e.getMessage()));
        } catch (Exception e) {
            DataRecipeGeneratorNeoForge121.LOGGER.error("Unexpected recipe generation error", e);
            sendError(player, RecipeError.INVALID_CONFIG, String.valueOf(e.getMessage()));
        }
    }

    public static List<SlotStack> snapshot(GeneratorMenu menu) {
        List<SlotStack> result = new ArrayList<>();
        for (int i = 0; i < menu.customSlotCount; i++) {
            ItemStack stack = menu.container.getItem(i);
            if (stack.isEmpty()) {
                result.add(SlotStack.EMPTY);
            } else {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                result.add(new SlotStack(id.toString(), stack.getCount()));
            }
        }
        return result;
    }

    private static void sendError(ServerPlayer player, RecipeError error, String detail) {
        String code = error.name().toLowerCase(Locale.ROOT);
        player.sendSystemMessage(Component.translatable(error.messageKey(), detail == null ? "" : detail)
                .withStyle(ChatFormatting.RED));
        ModNetwork.sendToPlayer(player, new GenerationResultMessage(false, code, detail == null ? "" : detail, ""));
    }

    private static String displayPath(Path serverDirectory, Path written) {
        try {
            return serverDirectory.toAbsolutePath().normalize().relativize(written).toString();
        } catch (Exception ignored) {
            return written.toString();
        }
    }
}
