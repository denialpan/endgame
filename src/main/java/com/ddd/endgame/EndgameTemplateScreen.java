package com.ddd.endgame;

import java.util.ArrayList;
import java.util.Comparator;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class EndgameTemplateScreen extends AbstractContainerScreen<EndgameTemplateMenu> {
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getIntegerInstance(Locale.US);
    private static final int PANEL_X = 8;
    private static final int PANEL_Y = 65;
    private static final int PANEL_WIDTH = 240;
    private static final int PANEL_HEIGHT = 112;
    private static final int SEARCH_X = PANEL_X + 7;
    private static final int SEARCH_Y = PANEL_Y + 5;
    private static final int SEARCH_WIDTH = 218;
    private static final int SEARCH_HEIGHT = 12;
    private static final int SORT_NAME_X = PANEL_X + 57;
    private static final int SORT_PROGRESS_X = PANEL_X + 103;
    private static final int SORT_DIRECTION_X = PANEL_X + 168;
    private static final int SORT_BUTTON_Y = PANEL_Y + 20;
    private static final int SORT_NAME_WIDTH = 42;
    private static final int SORT_PROGRESS_WIDTH = 61;
    private static final int SORT_DIRECTION_WIDTH = 55;
    private static final int SORT_BUTTON_HEIGHT = 12;
    private static final int TOTAL_BAR_X = PANEL_X + 7;
    private static final int TOTAL_BAR_Y = PANEL_Y + 33;
    private static final int TOTAL_BAR_WIDTH = 218;
    private static final int TOTAL_BAR_HEIGHT = 10;
    private static final int ROW_START_Y = PANEL_Y + 50;
    private static final int ROW_HEIGHT = 15;
    private static final int VISIBLE_ROWS = 4;
    private static final int ITEM_BAR_X = PANEL_X + 119;
    private static final int ITEM_BAR_WIDTH = 91;
    private static final int ITEM_BAR_HEIGHT = 7;
    private static final int SCROLLBAR_X = PANEL_X + 229;
    private static final int SCROLLBAR_Y = ROW_START_Y;
    private static final int SCROLLBAR_HEIGHT = VISIBLE_ROWS * ROW_HEIGHT - 2;
    private int scrollOffset;
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

    public EndgameTemplateScreen(EndgameTemplateMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 274;
        this.titleLabelX = 8;
        this.titleLabelY = 7;
        this.inventoryLabelX = 48;
        this.inventoryLabelY = 182;
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
        this.searchBox.setResponder(value -> {
            this.scrollOffset = 0;
            this.cachedRequirementsRevision = Integer.MIN_VALUE;
        });
        this.addRenderableWidget(this.searchBox);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderProgressTooltip(guiGraphics, mouseX, mouseY);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        drawVanillaPanel(guiGraphics, x, y, this.imageWidth, this.imageHeight);

        drawSlot(guiGraphics, x + 25, y + 34);
        drawSlot(guiGraphics, x + 133, y + 34);
        guiGraphics.drawString(this.font, Component.translatable("container.dddsendgame.endgame_template.deposit"), x + 9, y + 55, 0x404040, false);
        guiGraphics.drawString(this.font, Component.translatable("container.dddsendgame.endgame_template.result"), x + 118, y + 55, 0x404040, false);

        drawRequirementPanel(guiGraphics, x + PANEL_X, y + PANEL_Y, this.sortedRows());
        drawInventorySlots(guiGraphics, x + 47, y + 193);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, this.sortedRows().size() - VISIBLE_ROWS);
        if (maxScroll <= 0 || !isMouseOver(mouseX, mouseY, PANEL_X, PANEL_Y, PANEL_WIDTH, PANEL_HEIGHT)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        this.scrollOffset = Mth.clamp(this.scrollOffset - (int)Math.signum(scrollY), 0, maxScroll);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isMouseOver(mouseX, mouseY, SORT_NAME_X, SORT_BUTTON_Y, SORT_NAME_WIDTH, SORT_BUTTON_HEIGHT)) {
                selectSortMode(SortMode.NAME);
                return true;
            }
            if (isMouseOver(mouseX, mouseY, SORT_PROGRESS_X, SORT_BUTTON_Y, SORT_PROGRESS_WIDTH, SORT_BUTTON_HEIGHT)) {
                selectSortMode(SortMode.PROGRESS);
                return true;
            }
            if (isMouseOver(mouseX, mouseY, SORT_DIRECTION_X, SORT_BUTTON_Y, SORT_DIRECTION_WIDTH, SORT_BUTTON_HEIGHT)) {
                this.sortAscending = !this.sortAscending;
                this.scrollOffset = 0;
                return true;
            }
        }

        if (button == 0 && maxScroll() > 0 && isMouseOver(mouseX, mouseY, SCROLLBAR_X, SCROLLBAR_Y, 8, SCROLLBAR_HEIGHT)) {
            this.draggingScrollbar = true;
            updateScrollFromMouse(mouseY);
            return true;
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
        this.scrollOffset = Math.min(this.scrollOffset, Math.max(0, rows.size() - VISIBLE_ROWS));
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
        this.scrollOffset = 0;
    }

    private int maxScroll() {
        return Math.max(0, this.sortedRows().size() - VISIBLE_ROWS);
    }

    private void updateScrollFromMouse(double mouseY) {
        int maxScroll = maxScroll();
        if (maxScroll <= 0) {
            this.scrollOffset = 0;
            return;
        }

        double trackTop = this.topPos + SCROLLBAR_Y + 1;
        double trackHeight = SCROLLBAR_HEIGHT - 2;
        double ratio = Mth.clamp((mouseY - trackTop) / trackHeight, 0.0D, 1.0D);
        this.scrollOffset = Mth.clamp((int)Math.round(ratio * maxScroll), 0, maxScroll);
    }

    private void drawRequirementPanel(GuiGraphics guiGraphics, int x, int y, List<RowData> rows) {
        drawInset(guiGraphics, x, y, PANEL_WIDTH, PANEL_HEIGHT);
        long totalContributed = Math.max(0L, this.cachedTotalRequired - this.cachedTotalRemaining);

        if (this.searchBox != null && this.searchBox.getValue().isEmpty() && !this.searchBox.isFocused()) {
            guiGraphics.drawString(this.font, Component.translatable("container.dddsendgame.endgame_template.search"), x + SEARCH_X - PANEL_X + 3, y + SEARCH_Y - PANEL_Y + 2, 0x707070, false);
        }
        drawSortButton(guiGraphics, x + SORT_NAME_X - PANEL_X, y + SORT_BUTTON_Y - PANEL_Y, SORT_NAME_WIDTH, Component.translatable("container.dddsendgame.endgame_template.sort.name"), this.sortMode == SortMode.NAME);
        drawSortButton(guiGraphics, x + SORT_PROGRESS_X - PANEL_X, y + SORT_BUTTON_Y - PANEL_Y, SORT_PROGRESS_WIDTH, Component.translatable("container.dddsendgame.endgame_template.sort.progress"), this.sortMode == SortMode.PROGRESS);
        drawSortButton(guiGraphics, x + SORT_DIRECTION_X - PANEL_X, y + SORT_BUTTON_Y - PANEL_Y, SORT_DIRECTION_WIDTH, Component.translatable(this.sortAscending ? "container.dddsendgame.endgame_template.sort.asc" : "container.dddsendgame.endgame_template.sort.desc"), false);
        drawProgressBar(guiGraphics, x + TOTAL_BAR_X - PANEL_X, y + TOTAL_BAR_Y - PANEL_Y, TOTAL_BAR_WIDTH, TOTAL_BAR_HEIGHT, progress(totalContributed, this.cachedTotalRequired), 0xFF55AA55);

        int maxScroll = Math.max(0, rows.size() - VISIBLE_ROWS);
        this.scrollOffset = Math.min(this.scrollOffset, maxScroll);
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = this.scrollOffset + row;
            if (index >= rows.size()) {
                break;
            }

            RowData requirement = rows.get(index);
            int rowY = y + ROW_START_Y - PANEL_Y + row * ROW_HEIGHT;
            guiGraphics.renderItem(requirement.stack(), x + 7, rowY - 1);
            guiGraphics.drawString(this.font, trimToWidth(requirement.name(), 82), x + 27, rowY + 3, 0x404040, false);
            drawProgressBar(guiGraphics, x + ITEM_BAR_X - PANEL_X, rowY + 4, ITEM_BAR_WIDTH, ITEM_BAR_HEIGHT, progress(requirement.contributed(), dddsendgame.ENDGAME_ITEM_REQUIREMENT), requirement.complete() ? 0xFF55AA55 : 0xFFAA8B2D);
        }

        drawScrollbar(guiGraphics, x + SCROLLBAR_X - PANEL_X, y + SCROLLBAR_Y - PANEL_Y, maxScroll);
    }

    private void renderProgressTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<RowData> rows = this.sortedRows();
        long totalContributed = Math.max(0L, this.cachedTotalRequired - this.cachedTotalRemaining);

        if (isMouseOver(mouseX, mouseY, TOTAL_BAR_X, TOTAL_BAR_Y, TOTAL_BAR_WIDTH, TOTAL_BAR_HEIGHT)) {
            guiGraphics.renderTooltip(this.font, List.of(
                    Component.literal("Total progress"),
                    Component.literal(NUMBER_FORMAT.format(totalContributed) + " / " + NUMBER_FORMAT.format(this.cachedTotalRequired)),
                    Component.literal(NUMBER_FORMAT.format(this.cachedTotalRemaining) + " remaining")
            ), Optional.empty(), mouseX, mouseY);
            return;
        }

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = this.scrollOffset + row;
            if (index >= rows.size()) {
                break;
            }

            int barY = ROW_START_Y + row * ROW_HEIGHT + 4;
            if (isMouseOver(mouseX, mouseY, ITEM_BAR_X, barY, ITEM_BAR_WIDTH, ITEM_BAR_HEIGHT)) {
                RowData requirement = rows.get(index);
                guiGraphics.renderTooltip(this.font, List.of(
                        requirement.stack().getHoverName(),
                        Component.literal(NUMBER_FORMAT.format(requirement.contributed()) + " / " + NUMBER_FORMAT.format(dddsendgame.ENDGAME_ITEM_REQUIREMENT)),
                        Component.literal(NUMBER_FORMAT.format(requirement.remaining()) + " remaining")
                ), Optional.empty(), mouseX, mouseY);
                return;
            }
        }
    }

    private boolean isMouseOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        int absoluteX = this.leftPos + x;
        int absoluteY = this.topPos + y;
        return mouseX >= absoluteX && mouseX < absoluteX + width && mouseY >= absoluteY && mouseY < absoluteY + height;
    }

    private static double progress(long contributed, long required) {
        return required <= 0L ? 0.0D : Mth.clamp((double)contributed / (double)required, 0.0D, 1.0D);
    }

    private static void drawVanillaPanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFFC6C6C6);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF8B8B8B);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFFE0E0E0);
    }

    private static void drawInset(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFF373737);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFFFFFFF);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFFC6C6C6);
    }

    private static void drawProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int height, double progress, int fillColor) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFF373737);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF8B8B8B);
        int filled = (int)Math.round((width - 2) * progress);
        if (filled > 0) {
            guiGraphics.fill(x + 1, y + 1, x + 1 + filled, y + height - 1, fillColor);
        }
    }

    private void drawSortButton(GuiGraphics guiGraphics, int x, int y, int width, Component label, boolean selected) {
        guiGraphics.fill(x, y, x + width, y + SORT_BUTTON_HEIGHT, 0xFF373737);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + SORT_BUTTON_HEIGHT - 1, selected ? 0xFFFFFFFF : 0xFFC6C6C6);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + SORT_BUTTON_HEIGHT - 2, selected ? 0xFFD8D8D8 : 0xFFE0E0E0);
        guiGraphics.drawString(this.font, label, x + 4, y + 2, 0x404040, false);
    }

    private void drawScrollbar(GuiGraphics guiGraphics, int x, int y, int maxScroll) {
        guiGraphics.fill(x, y, x + 8, y + SCROLLBAR_HEIGHT, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + 7, y + SCROLLBAR_HEIGHT - 1, 0xFFC6C6C6);
        int thumbHeight = maxScroll <= 0 ? SCROLLBAR_HEIGHT - 2 : Math.max(10, (SCROLLBAR_HEIGHT - 2) * VISIBLE_ROWS / (VISIBLE_ROWS + maxScroll));
        int travel = SCROLLBAR_HEIGHT - 2 - thumbHeight;
        int thumbY = y + 1 + (maxScroll <= 0 ? 0 : travel * this.scrollOffset / maxScroll);
        guiGraphics.fill(x + 2, thumbY, x + 6, thumbY + thumbHeight, 0xFF5F5F5F);
    }

    private String trimToWidth(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }

        String suffix = "...";
        while (!text.isEmpty() && this.font.width(text + suffix) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + suffix;
    }

    private static void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 18, y + 18, 0xFF373737);
        guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFFFFFFFF);
        guiGraphics.fill(x + 2, y + 2, x + 16, y + 16, 0xFF8B8B8B);
    }

    private static void drawInventorySlots(GuiGraphics guiGraphics, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(guiGraphics, x + column * 18, y + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(guiGraphics, x + column * 18, y + 59);
        }
    }

    private enum SortMode {
        NAME,
        PROGRESS
    }

    private record RowData(ItemStack stack, String name, String itemId, long remaining) {
        long contributed() {
            return dddsendgame.ENDGAME_ITEM_REQUIREMENT - this.remaining;
        }

        boolean complete() {
            return this.remaining <= 0L;
        }
    }
}
