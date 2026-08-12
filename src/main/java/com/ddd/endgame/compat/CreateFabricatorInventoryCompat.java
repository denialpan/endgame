package com.ddd.endgame.compat;

import com.ddd.endgame.item.RandomBlockPlacerItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class CreateFabricatorInventoryCompat {
    private CreateFabricatorInventoryCompat() {
    }

    public static ItemStack visibleStack(ItemStack stack) {
        return isFabricator(stack) ? RandomBlockPlacerItem.selectedItemStack(stack, Integer.MAX_VALUE) : stack;
    }

    public static ItemStack extractFromFabricator(Object handler, int slot, int amount) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = rawStackInSlot(handler, slot);
        return isFabricator(stack) ? RandomBlockPlacerItem.selectedItemStack(stack, amount) : ItemStack.EMPTY;
    }

    private static boolean isFabricator(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof RandomBlockPlacerItem;
    }

    private static ItemStack rawStackInSlot(Object handler, int slot) {
        String className = handler.getClass().getName();
        try {
            return switch (className) {
                case "com.simibubi.create.foundation.item.SmartInventory" -> stackFromHandlerField(handler, "inv", slot);
                case "com.simibubi.create.foundation.item.ItemHandlerWrapper" -> stackFromHandlerField(handler, "wrapped", slot);
                case "com.simibubi.create.content.logistics.depot.DepotItemHandler" -> stackFromDepot(handler, slot);
                case "com.simibubi.create.content.logistics.chute.ChuteItemHandler" -> stackFromBlockEntityField(handler, "item");
                case "com.simibubi.create.content.logistics.packager.PackagerItemHandler" -> stackFromBlockEntityField(handler, "heldBox");
                case "com.simibubi.create.content.logistics.tunnel.BrassTunnelItemHandler" -> stackFromBrassTunnel(handler, slot);
                case "com.simibubi.create.content.kinetics.belt.transport.ItemHandlerBeltSegment" -> stackFromBeltSegment(handler);
                default -> ItemStack.EMPTY;
            };
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return ItemStack.EMPTY;
        }
    }

    private static ItemStack stackFromHandlerField(Object owner, String fieldName, int slot) throws ReflectiveOperationException {
        Object handler = field(owner, fieldName);
        if (handler instanceof IItemHandler itemHandler) {
            return itemHandler.getStackInSlot(slot);
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack stackFromDepot(Object handler, int slot) throws ReflectiveOperationException {
        Object behaviour = field(handler, "behaviour");
        if (slot == 0) {
            return stackFromTransportedItem(field(behaviour, "heldItem"));
        }

        Object outputBuffer = field(behaviour, "processingOutputBuffer");
        if (outputBuffer instanceof IItemHandler itemHandler) {
            return itemHandler.getStackInSlot(slot - 1);
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack stackFromBlockEntityField(Object handler, String stackFieldName) throws ReflectiveOperationException {
        Object blockEntity = field(handler, "blockEntity");
        Object stack = field(blockEntity, stackFieldName);
        return stack instanceof ItemStack itemStack ? itemStack : ItemStack.EMPTY;
    }

    private static ItemStack stackFromBrassTunnel(Object handler, int slot) throws ReflectiveOperationException {
        Object blockEntity = field(handler, "blockEntity");
        Object beltCapability = invoke(blockEntity, "getBeltCapability");
        if (beltCapability instanceof IItemHandler itemHandler) {
            return itemHandler.getStackInSlot(slot);
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack stackFromBeltSegment(Object handler) throws ReflectiveOperationException {
        Object beltInventory = field(handler, "beltInventory");
        int offset = (int) field(handler, "offset");
        return stackFromTransportedItem(invoke(beltInventory, "getStackAtOffset", int.class, offset));
    }

    private static ItemStack stackFromTransportedItem(Object transportedItem) throws ReflectiveOperationException {
        if (transportedItem == null) {
            return ItemStack.EMPTY;
        }

        Object stack = field(transportedItem, "stack");
        return stack instanceof ItemStack itemStack ? itemStack : ItemStack.EMPTY;
    }

    private static Object field(Object owner, String name) throws ReflectiveOperationException {
        Field field = field(owner.getClass(), name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static Object invoke(Object owner, String name) throws ReflectiveOperationException {
        Method method = owner.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        return method.invoke(owner);
    }

    private static Object invoke(Object owner, String name, Class<?> parameterType, Object argument) throws ReflectiveOperationException {
        Method method = owner.getClass().getDeclaredMethod(name, parameterType);
        method.setAccessible(true);
        return method.invoke(owner, argument);
    }
}
