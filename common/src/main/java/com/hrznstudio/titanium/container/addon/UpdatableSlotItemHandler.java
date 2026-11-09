/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.container.addon;

import com.hrznstudio.titanium.container.impl.ItemStorageSlot;
import com.hrznstudio.titanium.component.inventory.InventoryComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;

public class UpdatableSlotItemHandler extends ItemStorageSlot {

    private boolean enabled;
    private final InventoryComponent<?> itemHandler;

    public UpdatableSlotItemHandler(InventoryComponent<?> itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
        this.itemHandler = itemHandler;
        this.enabled = true;
    }

    public void update() {
        if (itemHandler instanceof InventoryComponent<?> inventory) {
            Pair<Integer, Integer> pos = inventory.getSlotPosition().apply(this.getContainerSlot());
            this.x = inventory.getXPos() + pos.getLeft();
            this.y = inventory.getYPos() + pos.getRight();
            this.enabled = inventory.getSlotVisiblePredicate().test(this.getContainerSlot());
        }
    }

    @Override
    public boolean isActive() {
        return enabled;
    }

    @Override
    public boolean mayPickup(Player player) {
        ItemStack resource = this.itemHandler.getStackInSlot(this.getContainerSlot());
        if (resource.isEmpty()) {
            return false;
        }
        if (itemHandler instanceof InventoryComponent<?> inventory) {
            return inventory.getExtractGUIPredicate().test(resource.copy(), this.getContainerSlot());
        }
        return true;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (!super.mayPlace(stack)) {
            return false;
        }
        if (itemHandler instanceof InventoryComponent<?> inventory) {
            return inventory.getInsertGUIPredicate().test(stack, this.getContainerSlot());
        }
        return true;
    }
}
