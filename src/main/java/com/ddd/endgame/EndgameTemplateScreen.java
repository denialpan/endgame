package com.ddd.endgame;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class EndgameTemplateScreen extends AbstractContainerScreen<EndgameTemplateMenu> {
    private static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "textures/gui/atlas.png");
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getIntegerInstance(Locale.US);

    private static final int TEMPLATE_WIDTH = 176;
    private static final int TEMPLATE_HEIGHT = 170;
    private static final int SEARCH_X = 8;
    private static final int SEARCH_Y = 41;
    private static final int SEARCH_WIDTH = 160;
    private static final int SEARCH_HEIGHT = 9;
    private static final int TOTAL_BAR_X = 8;
    private static final int TOTAL_BAR_Y = 20;
    private static final int TOTAL_BAR_WIDTH = 160;
    private static final int TOTAL_BAR_HEIGHT = 4;
    private static final int GRADIENT_U = 8;
    private static final int GRADIENT_V = 180;
    private static final int CHECKBOX_U = 8;
    private static final int CHECKBOX_V = 188;
    private static final int CHECKBOX_SIZE = 6;
    private static final int NAME_CHECK_X = 8;
    private static final int PROGRESS_CHECK_X = 50;
    private static final int ASCENDING_CHECK_X = 92;
    private static final int CHECK_Y = 31;
    private static final int CHECK_HIT_HEIGHT = 8;
    private static final int NAME_HIT_WIDTH = 36;
    private static final int PROGRESS_HIT_WIDTH = 39;
    private static final int ASCENDING_HIT_WIDTH = 74;
    private static final int GRID_X = 7;
    private static final int GRID_Y = 55;
    private static final int GRID_COLUMNS = 9;
    private static final int GRID_ROWS = 6;
    private static final int GRID_CELL = 18;
    private static final int VISIBLE_ITEMS = GRID_COLUMNS * GRID_ROWS;

    private int scrollRow;
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

    public EndgameTemplateScreen(EndgameTemplateMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = TEMPLATE_WIDTH;
        this.imageHeight = 194;
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
                Component.translatable("container.dddsendgame.endgame_template.search")
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
        guiGraphics.fill(x + SEARCH_X + 1, y + SEARCH_Y + 1, x + SEARCH_X + SEARCH_WIDTH - 1, y + SEARCH_Y + SEARCH_HEIGHT - 1, 0xFF000000);
        if (this.searchBox != null && this.searchBox.getValue().isEmpty() && !this.searchBox.isFocused()) {
            guiGraphics.drawString(this.font, Component.translatable("container.dddsendgame.endgame_template.search.short"), x + SEARCH_X + 2, y + SEARCH_Y + 1, 0xFFB0B0B0, false);
        }

        drawTotalProgress(guiGraphics, x, y);
        drawCheckedBoxes(guiGraphics, x, y);
        drawRequirementGrid(guiGraphics, x, y, this.sortedRows());
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
            if (isMouseOver(mouseX, mouseY, NAME_CHECK_X, CHECK_Y, NAME_HIT_WIDTH, CHECK_HIT_HEIGHT)) {
                selectSortMode(SortMode.NAME);
                return true;
            }
            if (isMouseOver(mouseX, mouseY, PROGRESS_CHECK_X, CHECK_Y, PROGRESS_HIT_WIDTH, CHECK_HIT_HEIGHT)) {
                selectSortMode(SortMode.PROGRESS);
                return true;
            }
            if (isMouseOver(mouseX, mouseY, ASCENDING_CHECK_X, CHECK_Y, ASCENDING_HIT_WIDTH, CHECK_HIT_HEIGHT)) {
                this.sortAscending = !this.sortAscending;
                this.scrollRow = 0;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private EndgameTemplateBlockEntity currentTemplate() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            BlockEntity blockEntity = minecraft.level.getBlockEntity(this.menu.pos());
            if (blockEntity instanceof EndgameTemplateBlockEntity template) {
                return template;
            }
        }

        return this.menu.blockEntity();
    }

    private List<RowData> sortedRows() {
        EndgameTemplateBlockEntity template = this.currentTemplate();
        int revision = template == null ? -1 : template.requirementsRevision();
        String searchText = normalizedSearchText();
        if (revision == this.cachedRequirementsRevision
                && this.sortMode == this.cachedSortMode
                && this.sortAscending == this.cachedSortAscending
                && searchText.equals(this.cachedSearchText)) {
            return this.cachedRows;
        }

        List<EndgameRequirement> requirements = template == null ? List.of() : template.requirements();
        List<RowData> rows = new ArrayList<>(requirements.size());
        long totalRemaining = 0L;
        for (EndgameRequirement requirement : requirements) {
            ItemStack stack = requirement.displayStack();
            String name = stack.getHoverName().getString();
            long remaining = requirement.remaining();
            totalRemaining += remaining;
            if (!searchText.isEmpty() && !name.toLowerCase(Locale.ROOT).contains(searchText)) {
                continue;
            }
            rows.add(new RowData(stack, name, requirement.itemId().toString(), remaining));
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
        this.cachedTotalRequired = (long)requirements.size() * dddsendgame.ENDGAME_ITEM_REQUIREMENT;
        this.cachedTotalRemaining = totalRemaining;
        this.scrollRow = Math.min(this.scrollRow, maxScrollRows(rows.size()));
        return this.cachedRows;
    }

    private String normalizedSearchText() {
        return this.searchBox == null ? "" : this.searchBox.getValue().trim().toLowerCase(Locale.ROOT);
    }

    private void selectSortMode(SortMode mode) {
        if (this.sortMode == mode) {
            this.sortAscending = !this.sortAscending;
        } else {
            this.sortMode = mode;
            this.sortAscending = true;
        }
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

    private void drawTotalProgress(GuiGraphics guiGraphics, int x, int y) {
        long totalContributed = Math.max(0L, this.cachedTotalRequired - this.cachedTotalRemaining);
        int filled = (int)Math.round(TOTAL_BAR_WIDTH * progress(totalContributed, this.cachedTotalRequired));
        if (filled > 0) {
            guiGraphics.blit(ATLAS, x + TOTAL_BAR_X, y + TOTAL_BAR_Y, GRADIENT_U, GRADIENT_V, filled, TOTAL_BAR_HEIGHT);
        }
    }

    private void drawCheckedBoxes(GuiGraphics guiGraphics, int x, int y) {
        if (this.sortMode == SortMode.NAME) {
            drawCheck(guiGraphics, x + NAME_CHECK_X, y + CHECK_Y);
        }
        if (this.sortMode == SortMode.PROGRESS) {
            drawCheck(guiGraphics, x + PROGRESS_CHECK_X, y + CHECK_Y);
        }
        if (this.sortAscending) {
            drawCheck(guiGraphics, x + ASCENDING_CHECK_X, y + CHECK_Y);
        }
    }

    private void drawCheck(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(ATLAS, x + 1, y + 1, CHECKBOX_U, CHECKBOX_V, CHECKBOX_SIZE, CHECKBOX_SIZE);
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
            guiGraphics.renderItem(row.stack(), x + GRID_X + column * GRID_CELL + 1, y + GRID_Y + rowNumber * GRID_CELL + 1);
        }
    }

    private void renderAtlasTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        long totalContributed = Math.max(0L, this.cachedTotalRequired - this.cachedTotalRemaining);
        if (isMouseOver(mouseX, mouseY, TOTAL_BAR_X, TOTAL_BAR_Y, TOTAL_BAR_WIDTH, TOTAL_BAR_HEIGHT)) {
            guiGraphics.renderTooltip(this.font, List.of(
                    Component.literal("Total progress"),
                    Component.literal(NUMBER_FORMAT.format(totalContributed) + " / " + NUMBER_FORMAT.format(this.cachedTotalRequired)),
                    Component.literal(NUMBER_FORMAT.format(this.cachedTotalRemaining) + " remaining")
            ), Optional.empty(), mouseX, mouseY);
            return;
        }

        RowData hovered = hoveredGridRow(mouseX, mouseY);
        if (hovered != null) {
            guiGraphics.renderTooltip(this.font, List.of(
                    hovered.stack().getHoverName(),
                    Component.literal(NUMBER_FORMAT.format(hovered.contributed()) + " / " + NUMBER_FORMAT.format(dddsendgame.ENDGAME_ITEM_REQUIREMENT)),
                    Component.literal(NUMBER_FORMAT.format(hovered.remaining()) + " remaining")
            ), Optional.empty(), mouseX, mouseY);
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

    private enum SortMode {
        NAME,
        PROGRESS
    }

    private record RowData(ItemStack stack, String name, String itemId, long remaining) {
        long contributed() {
            return dddsendgame.ENDGAME_ITEM_REQUIREMENT - this.remaining;
        }
    }
}
