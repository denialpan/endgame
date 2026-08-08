package com.ddd.endgame;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GalaxyFreezerScreen extends AbstractContainerScreen<GalaxyFreezerMenu> {
    private static final int WIDTH = 176;
    private static final int HEIGHT = 166;
    private static final int SLOT_START_X = 61;
    private static final int SLOT_START_Y = 26;
    private static final int PLAYER_INVENTORY_X = 7;
    private static final int PLAYER_INVENTORY_Y = 83;
    private static final int HOTBAR_Y = 141;
    private static final int SLOT_SPACING = 18;
    private static final int SLOT_SIZE = 18;
    private static final int INVALID_MULTIBLOCK_TEXT_Y = 64;

    public GalaxyFreezerScreen(GalaxyFreezerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
        this.titleLabelX = 8;
        this.titleLabelY = 8;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFFC6C6C6);
        guiGraphics.fill(this.leftPos + 1, this.topPos + 1, this.leftPos + this.imageWidth - 1, this.topPos + this.imageHeight - 1, 0xFF8B8B8B);
        guiGraphics.fill(this.leftPos + 2, this.topPos + 2, this.leftPos + this.imageWidth - 2, this.topPos + this.imageHeight - 2, 0xFFC6C6C6);
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                drawSlot(guiGraphics, SLOT_START_X + col * SLOT_SPACING, SLOT_START_Y + row * SLOT_SPACING);
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(guiGraphics, PLAYER_INVENTORY_X + col * SLOT_SPACING, PLAYER_INVENTORY_Y + row * SLOT_SPACING);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(guiGraphics, PLAYER_INVENTORY_X + col * SLOT_SPACING, HOTBAR_Y);
        }
    }

    private void drawSlot(GuiGraphics guiGraphics, int relativeX, int relativeY) {
        int x = this.leftPos + relativeX;
        int y = this.topPos + relativeY;
        guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF373737);
        guiGraphics.fill(x + 1, y + 1, x + SLOT_SIZE, y + SLOT_SIZE, 0xFFFFFFFF);
        guiGraphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF8B8B8B);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        if (this.menu.blockEntity() != null && !this.menu.blockEntity().isMultiblockValid()) {
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable("container.dddsendgame.galaxy_freezer.invalid"),
                    this.imageWidth / 2,
                    INVALID_MULTIBLOCK_TEXT_Y,
                    0xFF4040
            );
        }
    }
}
