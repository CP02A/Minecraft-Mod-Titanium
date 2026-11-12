/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.fabric.transfer;

import com.hrznstudio.titanium.attachment.StoredEnergyAttachment;
import com.hrznstudio.titanium.energy.EnergyStorageItemStack;
import com.hrznstudio.titanium.item.EnergyItem;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;
import team.reborn.energy.api.EnergyStorage;

/**
 * Transactional Team Reborn {@link EnergyStorage} of an {@link EnergyItem} inside a {@link ContainerItemContext}. The
 * energy stays in Titanium's {@link StoredEnergyAttachment}, so it is the same energy Titanium itself sees.
 */
public class TREnergyItemStorage implements EnergyStorage {
    private final ContainerItemContext context;
    private final EnergyItem item;

    public TREnergyItemStorage(ContainerItemContext context, EnergyItem item) {
        this.context = context;
        this.item = item;
    }

    @Override
    public boolean supportsInsertion() {
        return item.getInput() > 0;
    }

    @Override
    public boolean supportsExtraction() {
        return item.getOutput() > 0;
    }

    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notNegative(maxAmount);
        int stored = stored();
        int inserted = (int) Math.min(item.getCapacity() - stored, Math.min(item.getInput(), maxAmount));
        return inserted > 0 && update(stored + inserted, transaction) ? inserted : 0;
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notNegative(maxAmount);
        int stored = stored();
        int extracted = (int) Math.min(stored, Math.min(item.getOutput(), maxAmount));
        return extracted > 0 && update(stored - extracted, transaction) ? extracted : 0;
    }

    @Override
    public long getAmount() {
        return stored();
    }

    @Override
    public long getCapacity() {
        return item.getCapacity();
    }

    private int stored() {
        if (!context.getItemVariant().isOf(item)) return 0;
        StoredEnergyAttachment attachment = context.getItemVariant().get(StoredEnergyAttachment.TYPE.get());
        return attachment == null ? 0 : attachment.stored();
    }

    private boolean update(int stored, TransactionContext transaction) {
        ItemVariant current = context.getItemVariant();
        if (!current.isOf(item)) return false;
        ItemStack stack = current.toStack();
        stack.set(StoredEnergyAttachment.TYPE.get(), EnergyStorageItemStack.with(item, stored));
        long amount = context.getAmount();
        return context.exchange(ItemVariant.of(stack), amount, transaction) == amount;
    }
}
