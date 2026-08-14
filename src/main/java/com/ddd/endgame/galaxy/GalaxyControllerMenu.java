package com.ddd.endgame.galaxy;

import com.ddd.endgame.dddsendgame;

import com.ddd.endgame.block.GalaxyControllerBlockEntity;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class GalaxyControllerMenu extends AbstractContainerMenu {
    private static final int TEMPLATE_SLOT_COUNT = 0;
    private final Container container;
    private final BlockPos pos;
    @Nullable
    private final GalaxyControllerBlockEntity blockEntity;

    public GalaxyControllerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, clientContainer(playerInventory, buffer.readBlockPos()));
    }

    public GalaxyControllerMenu(int containerId, Inventory playerInventory, GalaxyControllerBlockEntity blockEntity) {
        this(containerId, playerInventory, new MenuTarget(blockEntity, blockEntity.getBlockPos(), blockEntity));
    }

    private GalaxyControllerMenu(int containerId, Inventory playerInventory, MenuTarget target) {
        super(dddsendgame.GALAXY_CONTROLLER_MENU.get(), containerId);
        checkContainerSize(target.container(), TEMPLATE_SLOT_COUNT);
        this.container = target.container();
        this.pos = target.pos();
        this.blockEntity = target.blockEntity();
    }

    public BlockPos pos() {
        return this.pos;
    }

    @Nullable
    public GalaxyControllerBlockEntity blockEntity() {
        return this.blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.blockEntity == null
                ? true
                : stillValid(ContainerLevelAccess.create(player.level(), this.pos), player, dddsendgame.GALAXY_CONTROLLER_BLOCK.get());
    }

    private static MenuTarget clientContainer(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof GalaxyControllerBlockEntity blockEntity) {
            return new MenuTarget(blockEntity, pos, blockEntity);
        }
        return new MenuTarget(new SimpleContainer(TEMPLATE_SLOT_COUNT), pos, null);
    }

    private record MenuTarget(Container container, BlockPos pos, @Nullable GalaxyControllerBlockEntity blockEntity) {
    }
}
