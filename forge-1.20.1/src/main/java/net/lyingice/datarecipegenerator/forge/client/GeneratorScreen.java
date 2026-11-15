package net.lyingice.datarecipegenerator.forge.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.lyingice.datarecipegenerator.common.ContainerType;
import net.lyingice.datarecipegenerator.common.FileNameMode;
import net.lyingice.datarecipegenerator.common.GeneratorConfig;
import net.lyingice.datarecipegenerator.common.GeneratorLayout;
import net.lyingice.datarecipegenerator.common.RecipeVariant;
import net.lyingice.datarecipegenerator.forge.DataRecipeGeneratorForge;
import net.lyingice.datarecipegenerator.forge.GenerateRecipeMessage;
import net.lyingice.datarecipegenerator.forge.GeneratorMenu;
import net.lyingice.datarecipegenerator.forge.ModNetwork;
import net.lyingice.datarecipegenerator.forge.OpenGeneratorMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Native-looking container screen with a side panel for options. The vanilla
 * texture and slot positions are used as-is; the panel only contains the
 * generator controls.
 */
public class GeneratorScreen extends AbstractContainerScreen<GeneratorMenu> {
    private final GeneratorMenu menu;
    private GeneratorConfig config;
    private ContainerType containerType;
    private int guiLeft;
    private int guiTop;
    private final List<Button> tabButtons = new ArrayList<>();
    private Button variantButton;
    private Button nameModeButton;
    private Button categoryButton;
    private Button generateButton;
    private EditBox namespaceBox;
    private EditBox fileNameBox;
    private EditBox outputPathBox;
    private EditBox groupBox;
    private EditBox cookingTimeBox;
    private EditBox experienceBox;
    private EditBox trimPatternBox;
    private Component status = Component.empty();
    private int statusColor = 0xFF808080;
    private boolean generating;

    public GeneratorScreen(GeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.menu = menu;
        this.containerType = menu.getContainerType();
        this.imageWidth = GeneratorLayout.NATIVE_WIDTH;
        this.imageHeight = GeneratorLayout.NATIVE_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - GeneratorLayout.SCREEN_WIDTH) / 2;
        this.guiTop = (this.height - GeneratorLayout.SCREEN_HEIGHT) / 2;
        this.leftPos = this.guiLeft;
        this.topPos = this.guiTop + GeneratorLayout.TAB_HEIGHT;

        if (this.config == null) {
            this.config = this.menu.getConfig().copy();
        }
        this.config.normalize();

        List<RecipeVariant> variants = DataRecipeGeneratorForge.EDITION.variants(this.containerType);
        if (!variants.contains(this.config.variant)) {
            this.config.variant = variants.isEmpty() ? this.containerType.defaultVariant() : variants.get(0);
        }
        this.config.normalize();

        this.tabButtons.clear();
        int tabX = this.guiLeft + GeneratorLayout.TAB_START_X;
        int index = 0;
        for (ContainerType type : ContainerType.ALL) {
            int x = tabX + index * (GeneratorLayout.TAB_WIDTH + GeneratorLayout.TAB_GAP);
            Button tab = Button.builder(Component.translatable(type.tabKey()), b -> switchContainer(type))
                    .bounds(x, this.guiTop, GeneratorLayout.TAB_WIDTH, 18)
                    .build();
            this.tabButtons.add(tab);
            this.addRenderableWidget(tab);
            index++;
        }

        int px = this.leftPos + GeneratorLayout.PANEL_X + GeneratorLayout.CONTROL_X;
        int py = this.topPos;

        this.variantButton = Button.builder(Component.empty(), b -> cycleVariant())
                .tooltip(Tooltip.create(Component.translatable(this.config.variant.tipKey())))
                .bounds(px, py + GeneratorLayout.ROW_VARIANT, GeneratorLayout.CONTROL_WIDTH, 18)
                .build();
        this.addRenderableWidget(this.variantButton);

        this.namespaceBox = new EditBox(this.font, px, py + GeneratorLayout.ROW_NAMESPACE,
                GeneratorLayout.HALF_WIDTH, 16,
                Component.translatable("gui.data_recipe_generator.namespace"));
        this.namespaceBox.setMaxLength(64);
        this.namespaceBox.setHint(Component.translatable("gui.data_recipe_generator.hint.namespace"));
        this.namespaceBox.setValue(this.config.namespace);
        this.namespaceBox.setResponder(value -> this.config.namespace = value);
        this.addRenderableWidget(this.namespaceBox);

        this.nameModeButton = Button.builder(Component.empty(), b -> cycleNameMode())
                .bounds(px + GeneratorLayout.HALF_X, py + GeneratorLayout.ROW_NAME_MODE,
                        GeneratorLayout.HALF_WIDTH, 18)
                .build();
        this.addRenderableWidget(this.nameModeButton);

        this.fileNameBox = new EditBox(this.font, px, py + GeneratorLayout.ROW_FILE_NAME,
                GeneratorLayout.CONTROL_WIDTH, 16,
                Component.translatable("gui.data_recipe_generator.file_name"));
        this.fileNameBox.setMaxLength(120);
        this.fileNameBox.setHint(Component.translatable("gui.data_recipe_generator.hint.file_name"));
        this.fileNameBox.setValue(this.config.customFileName);
        this.fileNameBox.setResponder(value -> this.config.customFileName = value);
        this.addRenderableWidget(this.fileNameBox);

        this.outputPathBox = new EditBox(this.font, px, py + GeneratorLayout.ROW_OUTPUT_PATH,
                GeneratorLayout.CONTROL_WIDTH, 16,
                Component.translatable("gui.data_recipe_generator.output_path"));
        this.outputPathBox.setMaxLength(200);
        this.outputPathBox.setHint(Component.translatable("gui.data_recipe_generator.hint.output_path"));
        this.outputPathBox.setValue(this.config.outputPath);
        this.outputPathBox.setResponder(value -> this.config.outputPath = value);
        this.addRenderableWidget(this.outputPathBox);

        this.categoryButton = Button.builder(Component.empty(), b -> cycleCategory())
                .bounds(px, py + GeneratorLayout.ROW_CATEGORY, GeneratorLayout.HALF_WIDTH, 18)
                .build();
        this.addRenderableWidget(this.categoryButton);

        this.groupBox = new EditBox(this.font, px + GeneratorLayout.HALF_X, py + GeneratorLayout.ROW_CATEGORY + 1,
                GeneratorLayout.HALF_WIDTH, 16,
                Component.translatable("gui.data_recipe_generator.group"));
        this.groupBox.setMaxLength(128);
        this.groupBox.setHint(Component.translatable("gui.data_recipe_generator.hint.group"));
        this.groupBox.setValue(this.config.group);
        this.groupBox.setResponder(value -> this.config.group = value);
        this.addRenderableWidget(this.groupBox);

        this.cookingTimeBox = new EditBox(this.font, px, py + GeneratorLayout.ROW_EXTRAS,
                GeneratorLayout.HALF_WIDTH, 16,
                Component.translatable("gui.data_recipe_generator.cooking_time"));
        this.cookingTimeBox.setMaxLength(12);
        this.cookingTimeBox.setHint(Component.translatable("gui.data_recipe_generator.hint.cooking_time"));
        this.cookingTimeBox.setValue(this.config.cookingTime);
        this.cookingTimeBox.setResponder(value -> this.config.cookingTime = value);
        this.addRenderableWidget(this.cookingTimeBox);

        this.experienceBox = new EditBox(this.font, px + GeneratorLayout.HALF_X, py + GeneratorLayout.ROW_EXTRAS,
                GeneratorLayout.HALF_WIDTH, 16,
                Component.translatable("gui.data_recipe_generator.experience"));
        this.experienceBox.setMaxLength(16);
        this.experienceBox.setHint(Component.translatable("gui.data_recipe_generator.hint.experience"));
        this.experienceBox.setValue(this.config.experience);
        this.experienceBox.setResponder(value -> this.config.experience = value);
        this.addRenderableWidget(this.experienceBox);

        this.trimPatternBox = new EditBox(this.font, px, py + GeneratorLayout.ROW_EXTRAS,
                GeneratorLayout.CONTROL_WIDTH, 16,
                Component.translatable("gui.data_recipe_generator.trim_pattern"));
        this.trimPatternBox.setMaxLength(128);
        this.trimPatternBox.setHint(Component.translatable("gui.data_recipe_generator.hint.trim_pattern"));
        this.trimPatternBox.setValue(this.config.trimPattern);
        this.trimPatternBox.setResponder(value -> this.config.trimPattern = value);
        this.addRenderableWidget(this.trimPatternBox);

        this.generateButton = Button.builder(
                        Component.translatable("gui.data_recipe_generator.generate"), b -> generate())
                .bounds(px, py + GeneratorLayout.ROW_GENERATE, GeneratorLayout.CONTROL_WIDTH, 20)
                .build();
        this.addRenderableWidget(this.generateButton);

        refreshWidgets();
    }

    private void refreshWidgets() {
        if (this.variantButton != null) {
            this.variantButton.setMessage(Component.translatable(this.config.variant.langKey()));
            this.variantButton.setTooltip(Tooltip.create(
                    Component.translatable(this.config.variant.tipKey())));
        }
        if (this.nameModeButton != null) {
            this.nameModeButton.setMessage(Component.translatable(this.config.fileNameMode.langKey()));
        }
        if (this.categoryButton != null) {
            boolean show = !this.config.variant.categories().isEmpty();
            this.categoryButton.visible = show;
            this.categoryButton.active = show;
            if (show) {
                this.categoryButton.setMessage(Component.translatable(
                        "gui.data_recipe_generator.category",
                        Component.translatable("gui.data_recipe_generator.category." + this.config.category)));
            }
        }
        if (this.fileNameBox != null) {
            this.fileNameBox.setEditable(this.config.fileNameMode != FileNameMode.AUTO);
        }
        boolean cooking = this.config.variant.isCooking();
        if (this.cookingTimeBox != null) {
            this.cookingTimeBox.visible = cooking;
            this.cookingTimeBox.active = cooking;
        }
        if (this.experienceBox != null) {
            this.experienceBox.visible = cooking;
            this.experienceBox.active = cooking;
        }
        boolean trim = this.config.variant.isTrim()
                && DataRecipeGeneratorForge.EDITION.trimNeedsPattern();
        if (this.trimPatternBox != null) {
            this.trimPatternBox.visible = trim;
            this.trimPatternBox.active = trim;
        }
        if (this.generateButton != null) {
            this.generateButton.active = !this.generating;
        }
    }

    private void switchContainer(ContainerType type) {
        if (type == this.containerType) {
            return;
        }
        GeneratorConfig copy = this.config.copy();
        List<RecipeVariant> variants = DataRecipeGeneratorForge.EDITION.variants(type);
        if (!variants.contains(copy.variant)) {
            copy.variant = variants.isEmpty() ? type.defaultVariant() : variants.get(0);
        }
        if (!copy.variant.categories().isEmpty()
                && !copy.variant.categories().contains(copy.category)) {
            copy.category = copy.variant.defaultCategory();
        }
        copy.normalize();
        ModNetwork.sendToServer(new OpenGeneratorMessage(type, copy));
    }

    private void cycleVariant() {
        List<RecipeVariant> variants = DataRecipeGeneratorForge.EDITION.variants(this.containerType);
        if (variants.size() <= 1) {
            return;
        }
        int index = variants.indexOf(this.config.variant);
        this.config.variant = variants.get((index + 1) % variants.size());
        if (!this.config.variant.categories().isEmpty()
                && !this.config.variant.categories().contains(this.config.category)) {
            this.config.category = this.config.variant.defaultCategory();
        }
        refreshWidgets();
    }

    private void cycleNameMode() {
        this.config.fileNameMode = this.config.fileNameMode.next();
        refreshWidgets();
    }

    private void cycleCategory() {
        List<String> categories = this.config.variant.categories();
        if (categories.isEmpty()) {
            return;
        }
        int index = categories.indexOf(this.config.category);
        this.config.category = categories.get((index + 1) % categories.size());
        refreshWidgets();
    }

    private void generate() {
        if (this.generating) {
            return;
        }
        this.generating = true;
        this.status = Component.translatable("gui.data_recipe_generator.status.generating");
        this.statusColor = 0xFFFFAA00;
        refreshWidgets();
        ModNetwork.sendToServer(new GenerateRecipeMessage(this.config.copy()));
    }

    public void setGenerationResult(boolean success, String errorCode, String detail, String path) {
        this.generating = false;
        if (success) {
            this.status = Component.translatable("gui.data_recipe_generator.status.generated",
                    path == null ? "" : path);
            this.statusColor = 0xFF55FF55;
        } else {
            String key = errorCode == null || errorCode.isBlank()
                    ? "gui.data_recipe_generator.status.failed"
                    : "message.data_recipe_generator.error." + errorCode;
            this.status = Component.translatable(key, detail == null ? "" : detail);
            this.statusColor = 0xFFFF5555;
        }
        refreshWidgets();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ResourceLocation texture = ResourceLocation.tryParse(GeneratorLayout.texture(this.containerType));
        if (texture != null) {
            graphics.blit(texture, this.leftPos, this.topPos, 0, 0,
                    GeneratorLayout.NATIVE_WIDTH, GeneratorLayout.NATIVE_HEIGHT, 256, 256);
        }
        drawChrome(graphics);
        RenderSystem.disableBlend();
    }

    private void drawChrome(GuiGraphics graphics) {
        graphics.fill(this.guiLeft, this.guiTop,
                this.guiLeft + GeneratorLayout.SCREEN_WIDTH, this.guiTop + GeneratorLayout.TAB_HEIGHT,
                0xC0101010);

        int px = this.leftPos + GeneratorLayout.PANEL_X;
        int py = this.topPos;
        int pw = GeneratorLayout.PANEL_WIDTH;
        int ph = GeneratorLayout.PANEL_HEIGHT;
        graphics.fill(px, py, px + pw, py + ph, 0xF0101010);
        graphics.fill(px, py, px + pw, py + 1, 0xFF4A4A4A);
        graphics.fill(px, py, px + 1, py + ph, 0xFF4A4A4A);
        graphics.fill(px + pw - 1, py, px + pw, py + ph, 0xFF4A4A4A);
        graphics.fill(px, py + ph - 1, px + pw, py + ph, 0xFF4A4A4A);

        int active = ContainerType.ALL.indexOf(this.containerType);
        if (active >= 0) {
            int x = this.guiLeft + GeneratorLayout.TAB_START_X
                    + active * (GeneratorLayout.TAB_WIDTH + GeneratorLayout.TAB_GAP);
            graphics.fill(x, this.guiTop + GeneratorLayout.TAB_HEIGHT - 2,
                    x + GeneratorLayout.TAB_WIDTH, this.guiTop + GeneratorLayout.TAB_HEIGHT,
                    0xFFFFD700);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.containerType != ContainerType.CRAFTING) {
            graphics.drawString(this.font,
                    Component.translatable(this.containerType.titleKey()),
                    this.titleLabelX, this.titleLabelY, 4210752, false);
        }
        graphics.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        drawStatus(graphics);
    }

    private void drawStatus(GuiGraphics graphics) {
        int x = this.leftPos + GeneratorLayout.PANEL_X + GeneratorLayout.CONTROL_X;
        int y = this.topPos + GeneratorLayout.ROW_STATUS_1;
        int width = GeneratorLayout.CONTROL_WIDTH;
        Component text = this.status.getString().isEmpty()
                ? Component.translatable("gui.data_recipe_generator.status.hint")
                : this.status;
        int color = this.status.getString().isEmpty() ? 0xFF808080 : this.statusColor;
        List<FormattedCharSequence> lines = this.font.split(text, width);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            graphics.drawString(this.font, lines.get(i), x, y + i * 10, color, false);
        }
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        return mouseX < this.guiLeft || mouseY < this.guiTop
                || mouseX >= this.guiLeft + GeneratorLayout.SCREEN_WIDTH
                || mouseY >= this.guiTop + GeneratorLayout.SCREEN_HEIGHT;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 && !(this.getFocused() instanceof EditBox)) {
            generate();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
