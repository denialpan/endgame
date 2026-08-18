package com.ddd.endgame.galaxy;

import com.ddd.endgame.Xevitia;

import com.ddd.endgame.EndgameRequirement;

import com.ddd.endgame.block.GalaxyCompressorBlockEntity;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.fml.ModList;

public class GalaxyCompressorScreen extends AbstractContainerScreen<GalaxyCompressorMenu> {
    private static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath(Xevitia.MODID, "textures/gui/atlas.png");
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getIntegerInstance(Locale.US);
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.0000000");

    private static final int TEMPLATE_WIDTH = 176;
    private static final int TEMPLATE_HEIGHT = 176;
    private static final int SEARCH_X = 9;
    private static final int SEARCH_Y = 30;
    private static final int SEARCH_WIDTH = 160;
    private static final int SEARCH_HEIGHT = 9;
    private static final int TOTAL_BAR_X = 8;
    private static final int TOTAL_BAR_Y = 21;
    private static final int TOTAL_BAR_WIDTH = 160;
    private static final int TOTAL_BAR_HEIGHT = 4;
    private static final int GRADIENT_U = 0;
    private static final int GRADIENT_V = 193;
    private static final int SCROLL_THUMB_U = 0;
    private static final int SCROLL_THUMB_V = 177;
    private static final int SCROLL_THUMB_WIDTH = 12;
    private static final int SCROLL_THUMB_HEIGHT = 15;
    private static final int SORT_BUTTON_ATLAS_U = 176;
    private static final int SORT_BUTTON_WIDTH = 18;
    private static final int SORT_BUTTON_HEIGHT = 20;
    private static final int SORT_BY_NUMBER_ATLAS_V = 0;
    private static final int SORT_ASCENDING_ATLAS_V = 22;
    private static final int SORT_BY_NAME_ATLAS_V = 44;
    private static final int SORT_DESCENDING_ATLAS_V = 66;
    private static final int SORT_BUTTON_X = -21;
    private static final int SORT_BY_BUTTON_Y = 5;
    private static final int SORT_DIRECTION_BUTTON_Y = 27;
    private static final int GRID_X = 7;
    private static final int GRID_Y = 43;
    private static final int GRID_COLUMNS = 8;
    private static final int GRID_ROWS = 7;
    private static final int GRID_CELL = 18;
    private static final int VISIBLE_ITEMS = GRID_COLUMNS * GRID_ROWS;
    private static final int SCROLLBAR_X = 152;
    private static final int SCROLLBAR_Y = 43;
    private static final int SCROLLBAR_WIDTH = 16;
    private static final int SCROLLBAR_HEIGHT = 126;
    private static final int SCROLLBAR_BOTTOM_PADDING = 2;

    private int scrollRow;
    private boolean draggingScrollbar;
    private SortMode sortMode = SortMode.NAME;
    private boolean sortAscending = true;
    private EditBox searchBox;
    private List<RowData> cachedRows = List.of();
    private SortMode cachedSortMode = null;
    private boolean cachedSortAscending;
    private String cachedSearchText = "";
    private int cachedRequirementsRevision = Integer.MIN_VALUE;
    private long cachedTotalRequired;
    private long cachedTotalRemaining;
    private int cachedCompletedStacks;
    private int cachedTotalStacks;
    private int cachedCompletedMods;
    private int cachedTotalMods;
    private final Map<String, String> modDisplayNameCache = new HashMap<>();

    public GalaxyCompressorScreen(GalaxyCompressorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = TEMPLATE_WIDTH;
        this.imageHeight = TEMPLATE_HEIGHT;
        this.titleLabelX = 7;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        this.searchBox = new EditBox(
                this.font,
                this.leftPos + SEARCH_X,
                this.topPos + SEARCH_Y,
                SEARCH_WIDTH,
                SEARCH_HEIGHT,
                Component.translatable("container.xevitia.galaxy_compressor.search")
        );
        this.searchBox.setMaxLength(64);
        this.searchBox.setBordered(false);
        this.searchBox.setTextColor(0xFFFFFFFF);
        this.searchBox.setResponder(value -> {
            this.scrollRow = 0;
            this.cachedRequirementsRevision = Integer.MIN_VALUE;
        });
        this.addRenderableWidget(this.searchBox);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderAtlasTooltips(guiGraphics, mouseX, mouseY);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(ATLAS, x, y, 0, 0, TEMPLATE_WIDTH, TEMPLATE_HEIGHT);
        if (this.searchBox != null && this.searchBox.getValue().isEmpty() && !this.searchBox.isFocused()) {
            guiGraphics.drawString(this.font, Component.translatable("container.xevitia.galaxy_compressor.search.short"), x + SEARCH_X + 2, y + SEARCH_Y + 1, 0xFFB0B0B0, false);
        }

        if (this.hasNetworkConflict()) {
            drawNetworkConflict(guiGraphics, x, y);
            return;
        }

        drawTotalProgress(guiGraphics, x, y);
        drawSortButtons(guiGraphics, x, y);
        drawRequirementGrid(guiGraphics, x, y, this.sortedRows());
        drawScrollbar(guiGraphics, x, y);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox != null && this.searchBox.isFocused()) {
            if (keyCode == 256) {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            return this.searchBox.keyPressed(keyCode, scanCode, modifiers) || true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBox != null && this.searchBox.isFocused()) {
            return this.searchBox.charTyped(codePoint, modifiers) || true;
        }

        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isMouseOver(mouseX, mouseY, GRID_X, GRID_Y, GRID_COLUMNS * GRID_CELL, GRID_ROWS * GRID_CELL)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int maxScroll = maxScrollRows();
        if (maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        this.scrollRow = Mth.clamp(this.scrollRow - (int)Math.signum(scrollY), 0, maxScroll);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isMouseOver(mouseX, mouseY, SORT_BUTTON_X, SORT_BY_BUTTON_Y, SORT_BUTTON_WIDTH, SORT_BUTTON_HEIGHT)) {
                selectSortMode(this.sortMode == SortMode.NAME ? SortMode.PROGRESS : SortMode.NAME);
                return true;
            }
            if (isMouseOver(mouseX, mouseY, SORT_BUTTON_X, SORT_DIRECTION_BUTTON_Y, SORT_BUTTON_WIDTH, SORT_BUTTON_HEIGHT)) {
                this.sortAscending = !this.sortAscending;
                this.scrollRow = 0;
                return true;
            }
            if (!this.hasNetworkConflict()) {
                if (maxScrollRows() > 0 && isMouseOver(mouseX, mouseY, SCROLLBAR_X, SCROLLBAR_Y, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT)) {
                    this.draggingScrollbar = true;
                    updateScrollFromMouse(mouseY);
                    return true;
                }
                RowData clicked = hoveredGridRow((int)mouseX, (int)mouseY);
                if (clicked != null) {
                    if (!clicked.fluid()) {
                        openJeiRecipes(clicked.stack());
                    }
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingScrollbar) {
            updateScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.draggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private GalaxyCompressorBlockEntity currentController() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            BlockEntity blockEntity = minecraft.level.getBlockEntity(this.menu.pos());
            if (blockEntity instanceof GalaxyCompressorBlockEntity compressor) {
                return compressor;
            }
        }

        return this.menu.blockEntity();
    }

    private boolean hasNetworkConflict() {
        GalaxyCompressorBlockEntity compressor = this.currentController();
        return compressor != null && compressor.hasMultipleConnectedControllers();
    }

    private int connectedInputCount() {
        GalaxyCompressorBlockEntity compressor = this.currentController();
        return compressor == null ? 0 : compressor.connectedInputCount();
    }

    private List<RowData> sortedRows() {
        GalaxyCompressorBlockEntity compressor = this.currentController();
        int revision = compressor == null ? -1 : compressor.requirementsRevision();
        String searchText = normalizedSearchText();
        if (revision == this.cachedRequirementsRevision
                && this.sortMode == this.cachedSortMode
                && this.sortAscending == this.cachedSortAscending
                && searchText.equals(this.cachedSearchText)) {
            return this.cachedRows;
        }

        List<EndgameRequirement> requirements = compressor == null ? List.of() : compressor.requirements();
        List<RowData> rows = new ArrayList<>(requirements.size());
        long totalRequired = 0L;
        long totalRemaining = 0L;
        int completedStacks = 0;
        Map<String, Boolean> completedMods = new HashMap<>();
        for (EndgameRequirement requirement : requirements) {
            ItemStack stack = requirement.displayStack();
            FluidStack fluidStack = requirement.displayFluid();
            String name = requirement.displayName().getString();
            String namespace = requirement.id().getNamespace();
            String modName = modDisplayName(namespace);
            long remaining = requirement.remaining();
            long required = requirement.required();
            totalRequired += required;
            totalRemaining += remaining;
            if (requirement.complete()) {
                completedStacks++;
            }
            completedMods.merge(namespace, requirement.complete(), Boolean::logicalAnd);
            if (!matchesSearch(searchText, name, namespace, modName)) {
                continue;
            }
            rows.add(new RowData(stack, fluidStack, name, requirement.id().toString(), requirement.fluid(), remaining, required));
        }

        Comparator<RowData> comparator = switch (this.sortMode) {
            case NAME -> Comparator.comparing(RowData::name, String.CASE_INSENSITIVE_ORDER).thenComparing(RowData::itemId);
            case PROGRESS -> Comparator.comparingLong(RowData::contributed).thenComparing(RowData::name, String.CASE_INSENSITIVE_ORDER);
        };

        if (!this.sortAscending) {
            comparator = comparator.reversed();
        }

        rows.sort(comparator);
        this.cachedRows = List.copyOf(rows);
        this.cachedSortMode = this.sortMode;
        this.cachedSortAscending = this.sortAscending;
        this.cachedSearchText = searchText;
        this.cachedRequirementsRevision = revision;
        this.cachedTotalRequired = totalRequired;
        this.cachedTotalRemaining = totalRemaining;
        this.cachedCompletedStacks = completedStacks;
        this.cachedTotalStacks = requirements.size();
        this.cachedCompletedMods = (int)completedMods.values().stream().filter(Boolean::booleanValue).count();
        this.cachedTotalMods = completedMods.size();
        this.scrollRow = Math.min(this.scrollRow, maxScrollRows(rows.size()));
        return this.cachedRows;
    }

    private String normalizedSearchText() {
        return this.searchBox == null ? "" : this.searchBox.getValue().trim().toLowerCase(Locale.ROOT);
    }

    private boolean matchesSearch(String searchText, String name, String namespace, String modName) {
        if (searchText.isEmpty()) {
            return true;
        }

        String query = searchText.startsWith("@") ? searchText.substring(1) : searchText;
        if (query.isEmpty()) {
            return true;
        }

        return name.toLowerCase(Locale.ROOT).contains(query)
                || namespace.toLowerCase(Locale.ROOT).contains(query)
                || modName.toLowerCase(Locale.ROOT).contains(query);
    }

    private String modDisplayName(String namespace) {
        return this.modDisplayNameCache.computeIfAbsent(namespace, modId -> ModList.get()
                .getModContainerById(modId)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(modId));
    }

    private void selectSortMode(SortMode mode) {
        this.sortMode = mode;
        this.scrollRow = 0;
        this.cachedRequirementsRevision = Integer.MIN_VALUE;
    }

    private int maxScrollRows() {
        return maxScrollRows(this.sortedRows().size());
    }

    private static int maxScrollRows(int itemCount) {
        int totalRows = Math.ceilDiv(itemCount, GRID_COLUMNS);
        return Math.max(0, totalRows - GRID_ROWS);
    }

    private void updateScrollFromMouse(double mouseY) {
        int maxScroll = maxScrollRows();
        if (maxScroll <= 0) {
            this.scrollRow = 0;
            return;
        }

        double trackTop = this.topPos + SCROLLBAR_Y;
        double travel = SCROLLBAR_HEIGHT - SCROLL_THUMB_HEIGHT - SCROLLBAR_BOTTOM_PADDING;
        double ratio = Mth.clamp((mouseY - trackTop - SCROLL_THUMB_HEIGHT / 2.0D) / travel, 0.0D, 1.0D);
        this.scrollRow = Mth.clamp((int)Math.round(ratio * maxScroll), 0, maxScroll);
    }

    private void drawTotalProgress(GuiGraphics guiGraphics, int x, int y) {
        long totalContributed = Math.max(0L, this.cachedTotalRequired - this.cachedTotalRemaining);
        if (this.cachedTotalRequired <= 0L || totalContributed <= 0L) {
            return;
        }

        int filled = totalContributed >= this.cachedTotalRequired
                ? TOTAL_BAR_WIDTH
                : Math.min(TOTAL_BAR_WIDTH - 1, Math.max(1, (int)Math.floor(TOTAL_BAR_WIDTH * progress(totalContributed, this.cachedTotalRequired))));
        guiGraphics.blit(ATLAS, x + TOTAL_BAR_X, y + TOTAL_BAR_Y, GRADIENT_U, GRADIENT_V, filled, TOTAL_BAR_HEIGHT);
    }

    private void drawSortButtons(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(ATLAS, x + SORT_BUTTON_X, y + SORT_BY_BUTTON_Y, SORT_BUTTON_ATLAS_U, this.sortMode == SortMode.NAME ? SORT_BY_NAME_ATLAS_V : SORT_BY_NUMBER_ATLAS_V, SORT_BUTTON_WIDTH, SORT_BUTTON_HEIGHT);
        guiGraphics.blit(ATLAS, x + SORT_BUTTON_X, y + SORT_DIRECTION_BUTTON_Y, SORT_BUTTON_ATLAS_U, this.sortAscending ? SORT_ASCENDING_ATLAS_V : SORT_DESCENDING_ATLAS_V, SORT_BUTTON_WIDTH, SORT_BUTTON_HEIGHT);
    }

    private void drawNetworkConflict(GuiGraphics guiGraphics, int x, int y) {
        Component message = Component.literal("multiple compressors detected");
        guiGraphics.drawString(this.font, message, x + TEMPLATE_WIDTH / 2 - this.font.width(message) / 2, y + TEMPLATE_HEIGHT / 2 - this.font.lineHeight / 2, 0xFFFF3333, false);
    }

    private void drawRequirementGrid(GuiGraphics guiGraphics, int x, int y, List<RowData> rows) {
        int start = this.scrollRow * GRID_COLUMNS;
        for (int index = 0; index < VISIBLE_ITEMS; index++) {
            int rowIndex = start + index;
            if (rowIndex >= rows.size()) {
                break;
            }

            RowData row = rows.get(rowIndex);
            int column = index % GRID_COLUMNS;
            int rowNumber = index / GRID_COLUMNS;
            int itemX = x + GRID_X + column * GRID_CELL + 1;
            int itemY = y + GRID_Y + rowNumber * GRID_CELL + 1;
            if (row.fluid()) {
                drawFluidIcon(guiGraphics, row.fluidStack(), itemX, itemY);
            } else {
                guiGraphics.renderItem(row.stack(), itemX, itemY);
            }
        }
    }

    private static void drawFluidIcon(GuiGraphics guiGraphics, FluidStack fluidStack, int x, int y) {
        if (fluidStack.isEmpty()) {
            return;
        }

        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluidStack.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(extensions.getStillTexture(fluidStack));
        int color = extensions.getTintColor(fluidStack);
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float alpha = ((color >> 24) & 0xFF) / 255.0F;
        if (alpha <= 0.0F) {
            alpha = 1.0F;
        }

        guiGraphics.setColor(red, green, blue, alpha);
        guiGraphics.blit(x, y, 0, 16, 16, sprite);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawScrollbar(GuiGraphics guiGraphics, int x, int y) {
        int maxScroll = maxScrollRows();
        int thumbY = y + SCROLLBAR_Y;
        if (maxScroll > 0) {
            int travel = SCROLLBAR_HEIGHT - SCROLL_THUMB_HEIGHT - SCROLLBAR_BOTTOM_PADDING;
            thumbY += travel * this.scrollRow / maxScroll;
        }
        guiGraphics.blit(ATLAS, x + SCROLLBAR_X + 4, thumbY + 1, SCROLL_THUMB_U, SCROLL_THUMB_V, SCROLL_THUMB_WIDTH, SCROLL_THUMB_HEIGHT);
    }

    private void renderAtlasTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.hasNetworkConflict()) {
            return;
        }

        if (isMouseOver(mouseX, mouseY, SORT_BUTTON_X, SORT_BY_BUTTON_Y, SORT_BUTTON_WIDTH, SORT_BUTTON_HEIGHT)) {
            Component value = Component.translatable(this.sortMode == SortMode.NAME
                    ? "container.xevitia.galaxy_compressor.sort.name"
                    : "container.xevitia.galaxy_compressor.sort.progress");
            guiGraphics.renderTooltip(this.font, Component.translatable("container.xevitia.galaxy_compressor.sort_by.tooltip", value), mouseX, mouseY);
            return;
        }

        if (isMouseOver(mouseX, mouseY, SORT_BUTTON_X, SORT_DIRECTION_BUTTON_Y, SORT_BUTTON_WIDTH, SORT_BUTTON_HEIGHT)) {
            Component value = Component.translatable(this.sortAscending
                    ? "container.xevitia.galaxy_compressor.sort.asc"
                    : "container.xevitia.galaxy_compressor.sort.desc");
            guiGraphics.renderTooltip(this.font, Component.translatable("container.xevitia.galaxy_compressor.sort_direction.tooltip", value), mouseX, mouseY);
            return;
        }

        long totalContributed = Math.max(0L, this.cachedTotalRequired - this.cachedTotalRemaining);
        if (isMouseOver(mouseX, mouseY, TOTAL_BAR_X, TOTAL_BAR_Y, TOTAL_BAR_WIDTH, TOTAL_BAR_HEIGHT)) {
            guiGraphics.renderTooltip(this.font, List.of(
                    Component.literal("Total progress"),
                    Component.literal(NUMBER_FORMAT.format(totalContributed) + " / " + NUMBER_FORMAT.format(this.cachedTotalRequired)),
                    Component.literal(formatPercent(totalContributed, this.cachedTotalRequired) + "% complete"),
                    Component.literal("Unique Items: " + NUMBER_FORMAT.format(this.cachedCompletedStacks) + " / " + NUMBER_FORMAT.format(this.cachedTotalStacks) + " complete"),
                    Component.literal("Mods: " + NUMBER_FORMAT.format(this.cachedCompletedMods) + " / " + NUMBER_FORMAT.format(this.cachedTotalMods) + " complete"),
                    Component.literal(NUMBER_FORMAT.format(this.cachedTotalRemaining) + " remaining")
            ), Optional.empty(), mouseX, mouseY);
            return;
        }

        RowData hovered = hoveredGridRow(mouseX, mouseY);
        if (hovered != null) {
            List<Component> tooltip = hovered.fluid()
                    ? new ArrayList<>(List.of(Component.literal(hovered.name())))
                    : new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), hovered.stack()));
            tooltip.add(Component.empty());
            tooltip.add(Component.literal(hovered.fluid() ? "Endgame fluid progress" : "Endgame progress").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal(NUMBER_FORMAT.format(hovered.contributed()) + " / " + NUMBER_FORMAT.format(hovered.required()) + (hovered.fluid() ? " mB" : "")).withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.literal(formatPercent(hovered.contributed(), hovered.required()) + "% complete").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.literal(NUMBER_FORMAT.format(hovered.remaining()) + (hovered.fluid() ? " mB" : "") + " remaining").withStyle(ChatFormatting.DARK_GRAY));
            guiGraphics.renderTooltip(this.font, tooltip, hovered.fluid() ? Optional.empty() : hovered.stack().getTooltipImage(), mouseX, mouseY);
        }
    }

    private static void openJeiRecipes(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        try {
            Class<?> internalClass = Class.forName("mezz.jei.common.Internal");
            Object runtime = internalClass.getMethod("getJeiRuntime").invoke(null);
            if (runtime == null) {
                return;
            }

            Object helpers = runtime.getClass().getMethod("getJeiHelpers").invoke(runtime);
            Object focusFactory = helpers.getClass().getMethod("getFocusFactory").invoke(helpers);
            Object recipesGui = runtime.getClass().getMethod("getRecipesGui").invoke(runtime);
            Class<?> roleClass = Class.forName("mezz.jei.api.recipe.RecipeIngredientRole");
            Class<?> typeClass = Class.forName("mezz.jei.api.ingredients.IIngredientType");
            Class<?> vanillaTypesClass = Class.forName("mezz.jei.api.constants.VanillaTypes");
            Object outputRole = Enum.valueOf((Class<? extends Enum>)roleClass.asSubclass(Enum.class), "OUTPUT");
            Object itemStackType = vanillaTypesClass.getField("ITEM_STACK").get(null);
            Object focus = focusFactory.getClass()
                    .getMethod("createFocus", roleClass, typeClass, Object.class)
                    .invoke(focusFactory, outputRole, itemStackType, stack.copy());
            recipesGui.getClass().getMethod("show", List.class).invoke(recipesGui, List.of(focus));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // JEI is optional; clicks are a no-op if it is not installed or its runtime is unavailable.
        }
    }

    private RowData hoveredGridRow(int mouseX, int mouseY) {
        if (!isMouseOver(mouseX, mouseY, GRID_X, GRID_Y, GRID_COLUMNS * GRID_CELL, GRID_ROWS * GRID_CELL)) {
            return null;
        }

        int relativeX = mouseX - this.leftPos - GRID_X;
        int relativeY = mouseY - this.topPos - GRID_Y;
        int column = relativeX / GRID_CELL;
        int row = relativeY / GRID_CELL;
        int index = (this.scrollRow + row) * GRID_COLUMNS + column;
        List<RowData> rows = this.sortedRows();
        return index >= 0 && index < rows.size() ? rows.get(index) : null;
    }

    private boolean isMouseOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        int absoluteX = this.leftPos + x;
        int absoluteY = this.topPos + y;
        return mouseX >= absoluteX && mouseX < absoluteX + width && mouseY >= absoluteY && mouseY < absoluteY + height;
    }

    private static double progress(long contributed, long required) {
        return required <= 0L ? 0.0D : Mth.clamp((double)contributed / (double)required, 0.0D, 1.0D);
    }

    private static String formatPercent(long contributed, long required) {
        return PERCENT_FORMAT.format(progress(contributed, required) * 100.0D);
    }

    private enum SortMode {
        NAME,
        PROGRESS
    }

    private record RowData(ItemStack stack, FluidStack fluidStack, String name, String itemId, boolean fluid, long remaining, long required) {
        long contributed() {
            return this.required - this.remaining;
        }
    }
}
