package com.ddd.endgame.galaxy;

import com.ddd.endgame.dddsendgame;
import com.ddd.endgame.block.GalaxyFreezerBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

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
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
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

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (this.menu.getCarried().isEmpty() && isHoveringCoolantSlot(this.hoveredSlot)) {
            Component coolingLine = coolingTooltipLine();
            ItemStack hoveredStack = this.hoveredSlot.getItem();
            if (!hoveredStack.isEmpty()) {
                List<Component> tooltip = new ArrayList<>(this.getTooltipFromContainerItem(hoveredStack));
                tooltip.add(coolingLine);
                guiGraphics.renderTooltip(this.font, tooltip, hoveredStack.getTooltipImage(), hoveredStack, x, y);
                return;
            }

            List<Component> tooltip = List.of(coolingLine);
            List<FormattedCharSequence> lines = tooltip.stream()
                    .map(Component::getVisualOrderText)
                    .toList();
            guiGraphics.renderTooltip(this.font, lines, x, y);
            return;
        }

        super.renderTooltip(guiGraphics, x, y);
    }

    private Component coolingTooltipLine() {
        int remainingSeconds = (remainingCoolingTicks() + 19) / 20;
        if (remainingSeconds <= 0) {
            return Component.literal("Requires cooling").withStyle(ChatFormatting.AQUA);
        }
        return Component.literal("Total remaining cooling: " + remainingSeconds + " seconds").withStyle(ChatFormatting.AQUA);
    }

    private boolean isHoveringCoolantSlot(Slot slot) {
        if (slot == null) {
            return false;
        }
        int menuSlot = this.menu.slots.indexOf(slot);
        return menuSlot >= GalaxyFreezerBlockEntity.ICE_SLOT_START && menuSlot < GalaxyFreezerBlockEntity.SLOT_COUNT;
    }

    private int remainingCoolingTicks() {
        long total = 0L;
        for (int slot = GalaxyFreezerBlockEntity.ICE_SLOT_START; slot < GalaxyFreezerBlockEntity.SLOT_COUNT; slot++) {
            ItemStack stack = this.menu.freezerStack(slot);
            if (GalaxyFreezerBlockEntity.isCoolant(stack)) {
                total += (long) stack.getCount() * GalaxyFreezerBlockEntity.coolingPeriod(stack);
            }
        }

        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, total - this.menu.coolantTicks()));
    }
}
