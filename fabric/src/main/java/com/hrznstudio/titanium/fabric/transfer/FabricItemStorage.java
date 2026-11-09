/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.fabric.transfer;

import com.hrznstudio.titanium.api.transfer.IItemSlot;
import com.hrznstudio.titanium.api.transfer.IItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedSlottedStorage;
import net.minecraft.world.item.ItemStack;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Exposes Titanium {@link IItemStorage}s as transactional Fabric item storages.
 */
public final class FabricItemStorage {

    /**
     * Slot storages are shared by every view of the same slot so nested transactions stay consistent
     */
    private static final Map<IItemSlot, SlotStorage> SLOTS = Collections.synchronizedMap(new WeakHashMap<>());

    private FabricItemStorage() {
    }

    public static SlotStorage of(IItemSlot slot) {
        return SLOTS.computeIfAbsent(slot, SlotStorage::new);
    }

    public static SlottedStorage<ItemVariant> of(IItemStorage storage) {
        List<SlotStorage> slots = new ArrayList<>(storage.getSlots());
        for (int i = 0; i < storage.getSlots(); i++) {
            slots.add(of(storage.getSlot(i)));
        }
        return new CombinedSlottedStorage<>(slots);
    }

    public static class SlotStorage extends SingleStackStorage {
        // Weak so the slot map doesn't keep the slot alive
        private final WeakReference<IItemSlot> slot;

        private SlotStorage(IItemSlot slot) {
            this.slot = new WeakReference<>(slot);
        }

        @Override
        protected ItemStack getStack() {
            IItemSlot slot = this.slot.get();
            return slot == null ? ItemStack.EMPTY : slot.getStack();
        }

        @Override
        protected void setStack(ItemStack stack) {
            IItemSlot slot = this.slot.get();
            if (slot != null) slot.setStack(stack);
        }

        @Override
        protected boolean canInsert(ItemVariant variant) {
            IItemSlot slot = this.slot.get();
            return slot != null && slot.canInsert(variant.toStack());
        }

        @Override
        protected boolean canExtract(ItemVariant variant) {
            IItemSlot slot = this.slot.get();
            return slot != null && slot.canExtract(variant.toStack(Math.max(1, getStack().getCount())));
        }

        @Override
        protected int getCapacity(ItemVariant variant) {
            IItemSlot slot = this.slot.get();
            return slot == null ? 0 : slot.getCapacity(variant.isBlank() ? ItemStack.EMPTY : variant.toStack());
        }

        @Override
        protected void onFinalCommit() {
            IItemSlot slot = this.slot.get();
            if (slot != null) slot.onContentsChanged();
        }
    }
}
