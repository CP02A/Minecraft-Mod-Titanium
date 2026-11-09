/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.neoforge.transfer;

import com.hrznstudio.titanium.api.transfer.IItemSlot;
import com.hrznstudio.titanium.api.transfer.IItemStorage;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Exposes a Titanium {@link IItemStorage} as a transactional NeoForge item {@link ResourceHandler}.
 */
public class ItemStorageResourceHandler implements ResourceHandler<ItemResource> {

    /**
     * Journals are shared by every adapter of the same slot so nested transactions stay consistent
     */
    private static final Map<IItemSlot, SlotJournal> JOURNALS = Collections.synchronizedMap(new WeakHashMap<>());

    private final IItemStorage storage;

    public ItemStorageResourceHandler(IItemStorage storage) {
        this.storage = storage;
    }

    public IItemStorage getStorage() {
        return storage;
    }

    private static SlotJournal journal(IItemSlot slot) {
        return JOURNALS.computeIfAbsent(slot, SlotJournal::new);
    }

    @Override
    public int size() {
        return storage.getSlots();
    }

    @Override
    public ItemResource getResource(int index) {
        return ItemResource.of(storage.getSlot(index).getStack());
    }

    @Override
    public long getAmountAsLong(int index) {
        return storage.getSlot(index).getStack().getCount();
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return storage.getSlot(index).getCapacity(resource.isEmpty() ? ItemStack.EMPTY : resource.toStack(1));
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return !resource.isEmpty() && storage.getSlot(index).canInsert(resource.toStack(1));
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0) return 0;
        IItemSlot slot = storage.getSlot(index);
        ItemStack remainder = IItemStorage.insert(slot, resource.toStack(amount), true);
        int inserted = amount - remainder.getCount();
        if (inserted > 0) {
            ItemStack current = slot.getStack();
            journal(slot).updateSnapshots(transaction);
            slot.setStack(current.isEmpty() ? resource.toStack(inserted) : current.copyWithCount(current.getCount() + inserted));
        }
        return inserted;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0) return 0;
        IItemSlot slot = storage.getSlot(index);
        ItemStack current = slot.getStack();
        if (current.isEmpty() || !resource.matches(current)) return 0;
        int extracted = IItemStorage.extract(slot, amount, true).getCount();
        if (extracted > 0) {
            journal(slot).updateSnapshots(transaction);
            slot.setStack(extracted == current.getCount() ? ItemStack.EMPTY : current.copyWithCount(current.getCount() - extracted));
        }
        return extracted;
    }

    private static class SlotJournal extends SnapshotJournal<ItemStack> {
        // Weak so the journal map doesn't keep the slot alive
        private final WeakReference<IItemSlot> slot;

        private SlotJournal(IItemSlot slot) {
            this.slot = new WeakReference<>(slot);
        }

        @Override
        protected ItemStack createSnapshot() {
            IItemSlot slot = this.slot.get();
            return slot == null ? ItemStack.EMPTY : slot.getStack().copy();
        }

        @Override
        protected void revertToSnapshot(ItemStack snapshot) {
            IItemSlot slot = this.slot.get();
            if (slot != null) slot.setStack(snapshot);
        }

        @Override
        protected void onRootCommit(ItemStack originalState) {
            IItemSlot slot = this.slot.get();
            if (slot != null && !ItemStack.matches(originalState, slot.getStack())) {
                slot.onContentsChanged();
            }
        }
    }
}
