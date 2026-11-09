/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.api.transfer;

import net.minecraft.world.item.ItemStack;

/**
 * A single item slot. These are the primitives the loader specific transfer adapters (NeoForge ResourceHandler,
 * Fabric Storage) journal so transactions can be rolled back.
 */
public interface IItemSlot {

    /**
     * @return the current stack, must not be modified
     */
    ItemStack getStack();

    /**
     * Replaces the stack without firing change notifications.
     */
    void setStack(ItemStack stack);

    /**
     * @param stack the stack to check the capacity for, or {@link ItemStack#EMPTY} for the generic slot limit
     * @return the maximum amount of the given stack this slot can hold
     */
    int getCapacity(ItemStack stack);

    /**
     * Checks if the stack can be inserted by automation
     */
    boolean canInsert(ItemStack stack);

    /**
     * Checks if the stack can be extracted by automation
     */
    boolean canExtract(ItemStack stack);

    /**
     * Called after the contents of the slot changed (after the outermost transaction committed for loader adapters)
     */
    void onContentsChanged();
}
