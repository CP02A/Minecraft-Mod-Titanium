/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.neoforge.transfer;

import com.hrznstudio.titanium.attachment.StoredEnergyAttachment;
import com.hrznstudio.titanium.energy.EnergyStorageItemStack;
import com.hrznstudio.titanium.item.EnergyItem;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Transactional NeoForge {@link EnergyHandler} of an {@link EnergyItem}, writing through the {@link ItemAccess}.
 */
public class ItemEnergyHandler implements EnergyHandler {
    private final ItemAccess access;
    private final EnergyItem item;

    public ItemEnergyHandler(ItemAccess access, EnergyItem item) {
        this.access = access;
        this.item = item;
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        int inserted = Math.min(getCapacityAsInt() - getAmountAsInt(), Math.min(item.getInput(), amount));
        return inserted > 0 && update(getAmountAsInt() + inserted, transaction) ? inserted : 0;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        int extracted = Math.min(getAmountAsInt(), Math.min(item.getOutput(), amount));
        return extracted > 0 && update(getAmountAsInt() - extracted, transaction) ? extracted : 0;
    }

    @Override
    public long getAmountAsLong() {
        return get().stored();
    }

    @Override
    public long getCapacityAsLong() {
        return item.getCapacity();
    }

    private boolean update(int stored, TransactionContext transaction) {
        ItemResource current = access.getResource();
        if (!current.is(item)) return false;
        ItemResource updated = current.with(StoredEnergyAttachment.TYPE.get(), EnergyStorageItemStack.with(item, stored));
        return !updated.isEmpty() && access.exchange(updated, access.getAmount(), transaction) == access.getAmount();
    }

    public StoredEnergyAttachment get() {
        return access.getResource().getOrDefault(StoredEnergyAttachment.TYPE.get(), new StoredEnergyAttachment(item));
    }
}
