/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.container.impl;

import com.hrznstudio.titanium.component.inventory.InventoryComponent;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.function.BooleanSupplier;

public class DisableableItemHandlerSlot extends ItemStorageSlot {
    private final BooleanSupplier isDisabled;

    public DisableableItemHandlerSlot(InventoryComponent<?> itemHandler, int index, int xPosition, int yPosition, BasicInventoryContainer basicInventoryContainer) {
        this(itemHandler, index, xPosition, yPosition, basicInventoryContainer::isDisabled);
    }

    public DisableableItemHandlerSlot(InventoryComponent<?> itemHandler, int index, int xPosition, int yPosition, BooleanSupplier isDisabled) {
        super(itemHandler, index, xPosition, yPosition);
        this.isDisabled = isDisabled;
    }

    @Override
    public boolean isActive() {
        return !isDisabled.getAsBoolean();
    }

    @Override
    public boolean mayPlace(@Nonnull ItemStack stack) {
        return !isDisabled.getAsBoolean() && super.mayPlace(stack);
    }
}
