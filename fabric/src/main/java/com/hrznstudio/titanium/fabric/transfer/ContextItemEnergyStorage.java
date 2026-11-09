/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.fabric.transfer;

import com.hrznstudio.titanium.api.transfer.IEnergyStorage;
import com.hrznstudio.titanium.attachment.StoredEnergyAttachment;
import com.hrznstudio.titanium.energy.EnergyStorageItemStack;
import com.hrznstudio.titanium.item.EnergyItem;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.item.ItemStack;

/**
 * Energy storage of an {@link EnergyItem} inside a Fabric {@link ContainerItemContext}, updating the item through the
 * context.
 */
public class ContextItemEnergyStorage implements IEnergyStorage {
    private final ContainerItemContext context;
    private final EnergyItem item;

    public ContextItemEnergyStorage(ContainerItemContext context, EnergyItem item) {
        this.context = context;
        this.item = item;
    }

    private StoredEnergyAttachment get() {
        StoredEnergyAttachment attachment = context.getItemVariant().get(StoredEnergyAttachment.TYPE.get());
        return attachment == null ? new StoredEnergyAttachment(item) : attachment;
    }

    @Override
    public int getEnergyStored() {
        return get().stored();
    }

    @Override
    public int getMaxEnergyStored() {
        return item.getCapacity();
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int inserted = Math.max(0, Math.min(getMaxEnergyStored() - getEnergyStored(), Math.min(item.getInput(), maxReceive)));
        return inserted > 0 && (simulate || update(getEnergyStored() + inserted)) ? inserted : 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = Math.max(0, Math.min(getEnergyStored(), Math.min(item.getOutput(), maxExtract)));
        return extracted > 0 && (simulate || update(getEnergyStored() - extracted)) ? extracted : 0;
    }

    @Override
    public boolean canReceive() {
        return item.getInput() > 0;
    }

    @Override
    public boolean canExtract() {
        return item.getOutput() > 0;
    }

    private boolean update(int stored) {
        ItemVariant current = context.getItemVariant();
        if (!current.isOf(item)) return false;
        ItemStack stack = current.toStack();
        stack.set(StoredEnergyAttachment.TYPE.get(), EnergyStorageItemStack.with(item, stored));
        ItemVariant updated = ItemVariant.of(stack);
        long amount = context.getAmount();
        try (Transaction transaction = Transaction.isOpen() ? Transaction.openNested(Transaction.getCurrentUnsafe()) : Transaction.openOuter()) {
            if (context.exchange(updated, amount, transaction) == amount) {
                transaction.commit();
                return true;
            }
        }
        return false;
    }
}
