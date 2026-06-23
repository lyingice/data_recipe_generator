package net.lyingice.datarecipegenerator.client.gui;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;

import net.lyingice.datarecipegenerator.world.inventory.SmithingRecipeGuiMenu;
import net.lyingice.datarecipegenerator.init.DataRecipeGeneratorModScreens;
import net.lyingice.datarecipegenerator.network.GenerateRecipeMessage;

import net.neoforged.neoforge.network.PacketDistributor;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.Map;

public class SmithingRecipeGuiScreen extends AbstractContainerScreen<SmithingRecipeGuiMenu> implements DataRecipeGeneratorModScreens.ScreenAccessor {
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;
	private EditBox generalpath;
	private EditBox generalname;
	private EditBox namespaceInput;
	private Checkbox mode1;
	private Checkbox mode2;
	private Checkbox mode3;
	private Checkbox mode4;
	private Button button_generate;
	private static final ResourceLocation BACKGROUND = ResourceLocation.parse("data_recipe_generator:textures/screens/smithing_recipe_gui.png");

	public SmithingRecipeGuiScreen(SmithingRecipeGuiMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 176;
		this.imageHeight = 166;
	}

	@Override
	public void updateMenuState(int elementType, String name, Object elementState) {
		menuStateUpdateActive = true;
		if (elementType == 0 && elementState instanceof String stringState) {
			if (name.equals("generalpath"))
				generalpath.setValue(stringState);
			else if (name.equals("generalname"))
				generalname.setValue(stringState);
			else if (name.equals("namespace"))
				namespaceInput.setValue(stringState);
		}
		if (elementType == 1 && elementState instanceof Boolean logicState) {
			if (name.equals("mode1")) {
				if (mode1.selected() != logicState)
					mode1.onPress();
			} else if (name.equals("mode2")) {
				if (mode2.selected() != logicState)
					mode2.onPress();
			} else if (name.equals("mode3")) {
				if (mode3.selected() != logicState)
					mode3.onPress();
			} else if (name.equals("mode4")) {
				if (mode4.selected() != logicState)
					mode4.onPress();
			}
		}
		menuStateUpdateActive = false;
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		generalpath.render(guiGraphics, mouseX, mouseY, partialTicks);
		generalname.render(guiGraphics, mouseX, mouseY, partialTicks);
		namespaceInput.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
		RenderSystem.disableBlend();
	}

	@Override
	public boolean keyPressed(int key, int b, int c) {
		if (key == 256) {
			this.minecraft.player.closeContainer();
			return true;
		}
		if (generalpath.isFocused())
			return generalpath.keyPressed(key, b, c);
		if (generalname.isFocused())
			return generalname.keyPressed(key, b, c);
		if (namespaceInput.isFocused())
			return namespaceInput.keyPressed(key, b, c);
		return super.keyPressed(key, b, c);
	}

	@Override
	public void resize(Minecraft minecraft, int width, int height) {
		String generalpathValue = generalpath.getValue();
		String generalnameValue = generalname.getValue();
		String namespaceValue = namespaceInput.getValue();
		super.resize(minecraft, width, height);
		generalpath.setValue(generalpathValue);
		generalname.setValue(generalnameValue);
		namespaceInput.setValue(namespaceValue);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
	}

	// ==================== 模式互斥与逻辑 ====================

	private void updateModeSelection(Checkbox selectedMode) {
		menuStateUpdateActive = true;
		if (mode1 != selectedMode && mode1.selected()) mode1.onPress();
		if (mode2 != selectedMode && mode2.selected()) mode2.onPress();
		if (mode3 != selectedMode && mode3.selected()) mode3.onPress();
		if (mode4 != selectedMode && mode4.selected()) mode4.onPress();
		if (!selectedMode.selected()) selectedMode.onPress();
		menuStateUpdateActive = false;

		// mode2(动态模式) / mode4(动态继承模式) 禁用配方文件名输入框
		boolean disableName = mode2.selected() || mode4.selected();
		generalname.setEditable(!disableName);
		if (disableName) {
			generalname.setValue("");
			generalname.setFocused(false);
		}
	}

	private int getFileNameMode() {
		if (mode2.selected()) return 1;  // ACTIVE
		if (mode3.selected()) return 2;  // HALF_ACTIVE
		if (mode4.selected()) return 3;  // UPGRADE
		return 0; // NORMAL
	}

	// ==================== 配方生成 ====================

	private void onGenerateRecipe() {
		String guiType = "smithing";
		String generalPath = generalpath.getValue().trim();
		String generalName = generalname.getValue().trim();
		String namespace = namespaceInput.getValue().trim();
		if (namespace.isEmpty()) namespace = "custom";
		int fileNameMode = getFileNameMode();

		Map<Integer, Slot> slots = this.menu.getSlots();
		ItemStack[] slotStacks = new ItemStack[4];
		for (int i = 0; i < 4; i++) {
			Slot slot = slots.get(i);
			slotStacks[i] = (slot != null && slot.hasItem()) ? slot.getItem().copy() : ItemStack.EMPTY;
		}

		GenerateRecipeMessage msg = new GenerateRecipeMessage(
				guiType, generalPath, generalName, namespace,
				false, "", fileNameMode, slotStacks
		);
		PacketDistributor.sendToServer(msg);
	}

	@Override
	public void init() {
		super.init();
		generalpath = new EditBox(this.font, this.leftPos + -101, this.topPos + -28, 118, 18, Component.translatable("gui.data_recipe_generator.smithing_recipe_gui.generalpath"));
		generalpath.setMaxLength(8192);
		generalpath.setResponder(content -> {
			if (!menuStateUpdateActive)
				menu.sendMenuStateUpdate(entity, 0, "generalpath", content, false);
		});
		generalpath.setHint(Component.translatable("gui.data_recipe_generator.smithing_recipe_gui.generalpath"));
		this.addWidget(this.generalpath);
		generalname = new EditBox(this.font, this.leftPos + 34, this.topPos + -28, 118, 18, Component.translatable("gui.data_recipe_generator.smithing_recipe_gui.generalname"));
		generalname.setMaxLength(8192);
		generalname.setResponder(content -> {
			if (!menuStateUpdateActive)
				menu.sendMenuStateUpdate(entity, 0, "generalname", content, false);
		});
		generalname.setHint(Component.translatable("gui.data_recipe_generator.smithing_recipe_gui.generalname"));
		this.addWidget(this.generalname);

		// 命名空间输入框 (6号功能)
		namespaceInput = new EditBox(this.font, this.leftPos + 6, this.topPos + 6, 85, 18,
				Component.translatable("gui.data_recipe_generator.smithing_recipe_gui.namespace"));
		namespaceInput.setMaxLength(256);
		namespaceInput.setHint(Component.translatable("gui.data_recipe_generator.smithing_recipe_gui.namespace"));
		namespaceInput.setResponder(content -> {
			if (!menuStateUpdateActive)
				menu.sendMenuStateUpdate(entity, 0, "namespace", content, false);
		});
		this.addWidget(this.namespaceInput);

		button_generate = Button.builder(Component.translatable("gui.data_recipe_generator.smithing_recipe_gui.button_generate"), e -> {
			onGenerateRecipe();
		}).bounds(this.leftPos + 96, this.topPos + 16, 65, 20).build();
		this.addRenderableWidget(button_generate);
		mode1 = Checkbox.builder(Component.translatable("gui.data_recipe_generator.smithing_recipe_gui.mode1"), this.font).pos(this.leftPos + -102, this.topPos + 16).onValueChange((checkbox, value) -> {
			if (value) updateModeSelection(mode1);
			if (!menuStateUpdateActive)
				menu.sendMenuStateUpdate(entity, 1, "mode1", mode1.selected(), false);
		}).build();
		this.addRenderableWidget(mode1);
		mode2 = Checkbox.builder(Component.translatable("gui.data_recipe_generator.smithing_recipe_gui.mode2"), this.font).pos(this.leftPos + -102, this.topPos + 43).onValueChange((checkbox, value) -> {
			if (value) updateModeSelection(mode2);
			if (!menuStateUpdateActive)
				menu.sendMenuStateUpdate(entity, 1, "mode2", mode2.selected(), false);
		}).build();
		this.addRenderableWidget(mode2);
		mode3 = Checkbox.builder(Component.translatable("gui.data_recipe_generator.smithing_recipe_gui.mode3"), this.font).pos(this.leftPos + -102, this.topPos + 70).onValueChange((checkbox, value) -> {
			if (value) updateModeSelection(mode3);
			if (!menuStateUpdateActive)
				menu.sendMenuStateUpdate(entity, 1, "mode3", mode3.selected(), false);
		}).build();
		this.addRenderableWidget(mode3);
		mode4 = Checkbox.builder(Component.translatable("gui.data_recipe_generator.smithing_recipe_gui.mode4"), this.font).pos(this.leftPos + -102, this.topPos + 97).onValueChange((checkbox, value) -> {
			if (value) updateModeSelection(mode4);
			if (!menuStateUpdateActive)
				menu.sendMenuStateUpdate(entity, 1, "mode4", mode4.selected(), false);
		}).build();
		this.addRenderableWidget(mode4);
	}
}
