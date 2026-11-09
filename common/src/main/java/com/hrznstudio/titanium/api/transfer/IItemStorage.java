/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.api.transfer;

import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

/**
 * Loader independent slotted item storage. Exposed to other mods through the NeoForge item capability and the Fabric
 * item storage lookup by the platform modules.
 */
public interface IItemStorage {

    int getSlots();

    IItemSlot getSlot(int slot);

    default ItemStack getStackInSlot(int slot) {
        return getSlot(slot).getStack();
    }

    default int getSlotLimit(int slot) {
        return getSlot(slot).getCapacity(ItemStack.EMPTY);
    }

    default boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return getSlot(slot).canInsert(stack);
    }

    /**
     * Inserts a stack into the given slot.
     *
     * @return the remainder that couldn't be inserted
     */
    default ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        return insert(getSlot(slot), stack, simulate);
    }

    /**
     * Extracts up to the given amount from the slot.
     *
     * @return the extracted stack
     */
    default ItemStack extractItem(int slot, int amount, boolean simulate) {
        return extract(getSlot(slot), amount, simulate);
    }

    /**
     * Inserts the stack into any slot of the storage, first stacking with existing items.
     *
     * @return the remainder that couldn't be inserted
     */
    default ItemStack insertItem(@Nonnull ItemStack stack, boolean simulate) {
        ItemStack remaining = stack;
        for (int i = 0; i < getSlots() && !remaining.isEmpty(); i++) {
            if (!getStackInSlot(i).isEmpty()) remaining = insertItem(i, remaining, simulate);
        }
        for (int i = 0; i < getSlots() && !remaining.isEmpty(); i++) {
            if (getStackInSlot(i).isEmpty()) remaining = insertItem(i, remaining, simulate);
        }
        return remaining;
    }

    static ItemStack insert(IItemSlot slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (!slot.canInsert(stack)) return stack;
        ItemStack existing = slot.getStack();
        int limit = slot.getCapacity(stack);
        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(stack, existing)) return stack;
            limit -= existing.getCount();
        }
        if (limit <= 0) return stack;
        int toInsert = Math.min(limit, stack.getCount());
        if (!simulate) {
            slot.setStack(existing.isEmpty() ? stack.copyWithCount(toInsert) : existing.copyWithCount(existing.getCount() + toInsert));
            slot.onContentsChanged();
        }
        return toInsert == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - toInsert);
    }

    static ItemStack extract(IItemSlot slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        ItemStack existing = slot.getStack();
        if (existing.isEmpty()) return ItemStack.EMPTY;
        int toExtract = Math.min(amount, existing.getCount());
        ItemStack extracted = existing.copyWithCount(toExtract);
        if (!slot.canExtract(extracted)) return ItemStack.EMPTY;
        if (!simulate) {
            slot.setStack(toExtract == existing.getCount() ? ItemStack.EMPTY : existing.copyWithCount(existing.getCount() - toExtract));
            slot.onContentsChanged();
        }
        return extracted;
    }
}
