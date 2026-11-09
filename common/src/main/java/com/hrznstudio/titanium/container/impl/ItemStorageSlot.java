/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.container.impl;

import com.hrznstudio.titanium.component.inventory.InventoryComponent;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Menu slot backed by an {@link InventoryComponent}. Players interact with the slot directly, ignoring the automation
 * extraction filter of the inventory.
 * <p>
 * Vanilla menu code mutates the stacks returned by {@link #getItem()}, so copies are returned and written back on
 * {@link #setChanged()}.
 */
public class ItemStorageSlot extends Slot {

    private static final Container EMPTY_CONTAINER = new SimpleContainer(0);

    private final InventoryComponent<?> inventory;
    @Nullable
    private ItemStack cachedReturnedStack = null;

    public ItemStorageSlot(InventoryComponent<?> inventory, int index, int xPosition, int yPosition) {
        super(EMPTY_CONTAINER, index, xPosition, yPosition);
        this.inventory = inventory;
    }

    public InventoryComponent<?> getInventory() {
        return inventory;
    }

    protected ItemStack getStackCopy() {
        return inventory.getStackInSlot(this.getContainerSlot()).copy();
    }

    protected void setStackCopy(ItemStack stack) {
        inventory.setStackInSlot(this.getContainerSlot(), stack);
    }

    @Override
    public ItemStack getItem() {
        return cachedReturnedStack = getStackCopy();
    }

    @Override
    public void set(ItemStack stack) {
        setStackCopy(stack);
        cachedReturnedStack = stack;
    }

    @Override
    public void setChanged() {
        if (cachedReturnedStack != null && !ItemStack.matches(cachedReturnedStack, getStackCopy())) {
            set(cachedReturnedStack);
        }
    }

    @Override
    public ItemStack remove(int amount) {
        ItemStack stack = getStackCopy();
        ItemStack ret = stack.split(amount);
        set(stack);
        cachedReturnedStack = null;
        return ret;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return !stack.isEmpty() && inventory.isItemValid(this.getContainerSlot(), stack);
    }

    @Override
    public void onQuickCraft(ItemStack oldStackIn, ItemStack newStackIn) {
    }

    @Override
    public int getMaxStackSize() {
        return inventory.getCapacity(this.getContainerSlot(), ItemStack.EMPTY);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return inventory.getCapacity(this.getContainerSlot(), stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        return !inventory.getStackInSlot(this.getContainerSlot()).isEmpty();
    }

    public boolean isSameInventory(Slot other) {
        return other instanceof ItemStorageSlot slot && slot.inventory == this.inventory;
    }
}
