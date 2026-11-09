/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.util;

import com.hrznstudio.titanium.api.transfer.IItemStorage;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;


public class ItemHandlerUtil {

    @Nonnull
    public static ItemStack getFirstItem(IItemStorage handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            if (!handler.getStackInSlot(i).isEmpty()) {
                return handler.getStackInSlot(i).copy();
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean isEmpty(IItemStorage handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            if (!handler.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

}
