package com.ddd.endgame.galaxy;

import com.ddd.endgame.dddsendgame;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GalaxyFreezerScreen extends AbstractContainerScreen<GalaxyFreezerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(dddsendgame.MODID, "textures/gui/galaxy_freezer.png");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 166;
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
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, WIDTH, HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
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
