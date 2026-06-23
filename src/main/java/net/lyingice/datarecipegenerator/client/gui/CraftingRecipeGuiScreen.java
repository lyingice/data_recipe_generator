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

import net.lyingice.datarecipegenerator.world.inventory.CraftingRecipeGuiMenu;
import net.lyingice.datarecipegenerator.init.DataRecipeGeneratorModScreens;
import net.lyingice.datarecipegenerator.network.GenerateRecipeMessage;

import net.neoforged.neoforge.network.PacketDistributor;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.Map;

public class CraftingRecipeGuiScreen extends AbstractContainerScreen<CraftingRecipeGuiMenu> implements DataRecipeGeneratorModScreens.ScreenAccessor {
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;
	private EditBox ctafting1;
	private EditBox crafting2;
	private EditBox namespaceInput;
	private Checkbox crafting3;
	private Checkbox groupmisc;
	private Checkbox groupequipment;
	private Checkbox groupbuilding;
	private Checkbox groupredstone;
	private Checkbox mode1;
	private Checkbox mode2;
	private Checkbox mode3;
	private Button button_general;
	private static final ResourceLocation BACKGROUND = ResourceLocation.parse("data_recipe_generator:textures/screens/crafting_recipe_gui.png");

	public CraftingRecipeGuiScreen(CraftingRecipeGuiMenu container, Inventory inventory, Component text) {
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
			if (name.equals("ctafting1"))
				ctafting1.setValue(stringState);
			else if (name.equals("crafting2"))
				crafting2.setValue(stringState);
			else if (name.equals("namespace"))
				namespaceInput.setValue(stringState);
		}
		if (elementType == 1 && elementState instanceof Boolean logicState) {
			if (name.equals("crafting3")) {
				if (crafting3.selected() != logicState)
					crafting3.onPress();
			} else if (name.equals("groupmisc")) {
				if (groupmisc.selected() != logicState)
					groupmisc.onPress();
			} else if (name.equals("groupequipment")) {
				if (groupequipment.selected() != logicState)
					groupequipment.onPress();
			} else if (name.equals("groupbuilding")) {
				if (groupbuilding.selected() != logicState)
					groupbuilding.onPress();
			} else if (name.equals("groupredstone")) {
				if (groupredstone.selected() != logicState)
					groupredstone.onPress();
			} else if (name.equals("mode1")) {
				if (mode1.selected() != logicState)
					mode1.onPress();
			} else if (name.equals("mode2")) {
				if (mode2.selected() != logicState)
					mode2.onPress();
			} else if (name.equals("mode3")) {
				if (mode3.selected() != logicState)
					mode3.onPress();
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
		ctafting1.render(guiGraphics, mouseX, mouseY, partialTicks);
		crafting2.render(guiGraphics, mouseX, mouseY, partialTicks);
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
		if (ctafting1.isFocused())
			return ctafting1.keyPressed(key, b, c);
		if (crafting2.isFocused())
			return crafting2.keyPressed(key, b, c);
		if (namespaceInput.isFocused())
			return namespaceInput.keyPressed(key, b, c);
		return super.keyPressed(key, b, c);
	}

	@Override
	public void resize(Minecraft minecraft, int width, int height) {
		String ctafting1Value = ctafting1.getValue();
		String crafting2Value = crafting2.getValue();
		String namespaceValue = namespaceInput.getValue();
		super.resize(minecraft, width, height);
		ctafting1.setValue(ctafting1Value);
		crafting2.setValue(crafting2Value);
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
		if (!selectedMode.selected()) selectedMode.onPress();
		menuStateUpdateActive = false;

		// mode2(动态模式) 禁用配方文件名输入框
		boolean isActiveMode = mode2.selected();
		crafting2.setEditable(!isActiveMode);
		if (isActiveMode) {
			crafting2.setValue("");
			crafting2.setFocused(false);
		}
	}

	private void updateGroupSelection(Checkbox selectedGroup) {
		menuStateUpdateActive = true;
		if (groupmisc != selectedGroup && groupmisc.selected()) groupmisc.onPress();
		if (groupequipment != selectedGroup && groupequipment.selected()) groupequipment.onPress();
		if (groupbuilding != selectedGroup && groupbuilding.selected()) groupbuilding.onPress();
		if (groupredstone != selectedGroup && groupredstone.selected()) groupredstone.onPress();
		if (!selectedGroup.selected()) selectedGroup.onPress();
		menuStateUpdateActive = false;
	}

	private String getSelectedRecipeGroup() {
		if (groupmisc.selected()) return "misc";
		if (groupequipment.selected()) return "equipment";
		if (groupbuilding.selected()) return "building";
		if (groupredstone.selected()) return "redstone";
		return "";
	}

	private int getFileNameMode() {
		if (mode2.selected()) return 1;
		if (mode3.selected()) return 2;
		return 0;
	}

	// ==================== 配方生成 ====================

	private void onGenerateRecipe() {
		String guiType = "crafting";
		String generalPath = ctafting1.getValue().trim();
		String generalName = crafting2.getValue().trim();
		String namespace = namespaceInput.getValue().trim();
		if (namespace.isEmpty()) namespace = "custom";
		boolean isShapeless = crafting3.selected();
		String recipeGroup = getSelectedRecipeGroup();
		int fileNameMode = getFileNameMode();

		Map<Integer, Slot> slots = this.menu.getSlots();
		ItemStack[] slotStacks = new ItemStack[10];
		for (int i = 0; i < 10; i++) {
			Slot slot = slots.get(i);
			slotStacks[i] = (slot != null && slot.hasItem()) ? slot.getItem().copy() : ItemStack.EMPTY;
		}

		GenerateRecipeMessage msg = new GenerateRecipeMessage(
				guiType, generalPath, generalName, namespace,
				isShapeless, recipeGroup, fileNameMode, slotStacks
		);
		PacketDistributor.sendToServer(msg);
	}

	@Override
	public void init() {
		super.init();
		ctafting1 = new EditBox(this.font, this.leftPos + -110, this.topPos + -28, 118, 18, Component.translatable("gui.data_recipe_generator.crafting_recipe_gui.ctafting1"));
		ctafting1.setMaxLength(8192);
		ctafting1.setResponder(content -> {
			if (!menuStateUpdateActive)
				menu.sendMenuStateUpdate(entity, 0, "ctafting1", content, false);
		});
		ctafting1.setHint(Component.translatable("gui.data_recipe_generator.crafting_recipe_gui.ctafting1"));
		this.addWidget(this.ctafting1);
		crafting2 = new EditBox(this.font, this.leftPos + 34, this.topPos + -28, 118, 18, Component.translatable("gui.data_recipe_generator.crafting_recipe_gui.crafting2"));
		crafting2.setMaxLength(8192);
		crafting2.setResponder(content -> {
			if (!menuStateUpdateActive)
				menu.sendMenuStateUpdate(entity, 0, "crafting2", content, false);
		});
		crafting2.setHint(Component.translatable("gui.data_recipe_generator.crafting_recipe_gui.crafting2"));
		this.addWidget(this.crafting2);

		// 命名空间输入框 (6号功能)
		namespaceInput = new EditBox(this.font, this.leftPos + 6, this.topPos + 6, 85, 18,
				Component.translatable("gui.data_recipe_generator.crafting_recipe_gui.namespace"));
		namespaceInput.setMaxLength(256);
		namespaceInput.setHint(Component.translatable("gui.data_recipe_generator.crafting_recipe_gui.namespace"));
		namespaceInput.setResponder(content -> {
			if (!menuStateUpdateActive)
				menu.sendMenuStateUpdate(entity, 0, "namespace", content, false);
		});
		this.addWidget(this.namespaceInput);

		button_general = Button.builder(Component.translatable("gui.data_recipe_generator.crafting_recipe_gui.button_general"), e -> {
			onGenerateRecipe();
		}).bounds(this.leftPos + 96, this.topPos + 7, 60, 20).build();
		this.addRenderableWidget(button_general);
		crafting3 = Checkbox.builder(Component.translatable("gui.data_recipe_generator.crafting_recipe_gui.crafting3"), this.font).pos(this.leftPos + 87, this.topPos + 61).onValueChange((checkbox, value) -> {
			if (!menuStateUpdateActive)
				menu.sendMenuStateUpdate(entity, 1, "crafting3", value, false);
		}).build();
		this.addRenderableWidget(crafting3);
		groupmisc = Checkbox.builder(Component.translatable("gui.data_recipe_generator.crafting_recipe_gui.groupmisc"), this.font).pos(this.leftPos + 177, this.topPos + 7).onValueChange((checkbox, value) -> {
			if (value) updateGroupSelection(groupmisc);
			if (!menuStateUpdateActive)
				menu.sendMenuStateUpdate(entity, 1, "groupmisc", groupmisc.selected(), false);
		}).build();
		this.addRenderableWidget(groupmisc);
		groupequipment = Checkbox.builder(Component.translatable("gui.data_recipe_generator.crafting_recipe_gui.groupequipment"), this.font).pos(this.leftPos + 177, this.topPos + 25).onValueChange((checkbox, value) -> {
			if (value) updateGroupSelection(groupequipment);
			if (!menuStateUpdateActive)
				menu.sendMenuStateUpdate(entity, 1, "groupequipment", groupequipment.selected(), false);
		}).build();
		this.addRenderableWidget(groupequipment);
		groupbuilding = Checkbox.builder(Component.translatable("gui.data_recipe_generator.crafting_recipe_gui.groupbuilding"), this.font).pos(this.leftPos + 177, this.topPos + 43).onValueChange((checkbox, value) -> {
			if (value) updateGroupSelection(groupbuilding);
			if (!menuStateUpdateActive)
				menu.sendMenuStateUpdate(entity, 1, "groupbuilding", groupbuilding.selected(), false);
		}).build();
		this.addRenderableWidget(groupbuilding);
		groupredstone = Checkbox.builder(Component.translatable("gui.data_recipe_generator.crafting_recipe_gui.groupredstone"), this.font).pos(this.leftPos + 177, this.topPos + 61).onValueChange((checkbox, value) -> {
			if (value) updateGroupSelection(groupredstone);
			if (!menuStateUpdateActive)
				menu.sendMenuStateUpdate(entity, 1, "groupredstone", groupredstone.selected(), false);
		}).build();
		this.addRenderableWidget(groupredstone);
		mode1 = Checkbox.builder(Component.translatable("gui.data_recipe_generator.crafting_recipe_gui.mode1"), this.font).pos(this.leftPos + -111, this.topPos + 16).onValueChange((checkbox, value) -> {
			if (value) updateModeSelection(mode1);
			if (!menuStateUpdateActive)
				menu.sendMenuStateUpdate(entity, 1, "mode1", mode1.selected(), false);
		}).build();
		this.addRenderableWidget(mode1);
		mode2 = Checkbox.builder(Component.translatable("gui.data_recipe_generator.crafting_recipe_gui.mode2"), this.font).pos(this.leftPos + -111, this.topPos + 43).onValueChange((checkbox, value) -> {
			if (value) updateModeSelection(mode2);
			if (!menuStateUpdateActive)
				menu.sendMenuStateUpdate(entity, 1, "mode2", mode2.selected(), false);
		}).build();
		this.addRenderableWidget(mode2);
		mode3 = Checkbox.builder(Component.translatable("gui.data_recipe_generator.crafting_recipe_gui.mode3"), this.font).pos(this.leftPos + -111, this.topPos + 70).onValueChange((checkbox, value) -> {
			if (value) updateModeSelection(mode3);
			if (!menuStateUpdateActive)
				menu.sendMenuStateUpdate(entity, 1, "mode3", mode3.selected(), false);
		}).build();
		this.addRenderableWidget(mode3);
	}
}
