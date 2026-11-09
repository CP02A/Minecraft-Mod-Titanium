/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */


package com.hrznstudio.titanium.energy;

import com.hrznstudio.titanium.api.transfer.IEnergyStorage;
import com.hrznstudio.titanium.attachment.StoredEnergyAttachment;
import com.hrznstudio.titanium.item.EnergyItem;
import net.minecraft.world.item.ItemStack;

/**
 * Energy storage of an {@link EnergyItem} stack, stored in the {@link StoredEnergyAttachment} data component.
 * Modifies the given stack directly; the platform modules expose transactional versions to other mods.
 */
public class EnergyStorageItemStack implements IEnergyStorage {
    private final ItemStack stack;
    private final EnergyItem item;

    public EnergyStorageItemStack(ItemStack stack, EnergyItem item) {
        this.stack = stack;
        this.item = item;
    }

    public static StoredEnergyAttachment get(ItemStack stack, EnergyItem item) {
        return stack.getOrDefault(StoredEnergyAttachment.TYPE.get(), new StoredEnergyAttachment(item));
    }

    public static StoredEnergyAttachment with(EnergyItem item, int stored) {
        return new StoredEnergyAttachment(stored, item.getCapacity(), item.getInput(), item.getOutput());
    }

    public void putInternal(int energy) {
        set(Math.min(getEnergyStored() + energy, getMaxEnergyStored()));
    }

    @Override
    public int getEnergyStored() {
        return get(stack, item).stored();
    }

    @Override
    public int getMaxEnergyStored() {
        return item.getCapacity();
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int inserted = Math.max(0, Math.min(getMaxEnergyStored() - getEnergyStored(), Math.min(item.getInput(), maxReceive)));
        if (!simulate && inserted > 0) set(getEnergyStored() + inserted);
        return inserted;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = Math.max(0, Math.min(getEnergyStored(), Math.min(item.getOutput(), maxExtract)));
        if (!simulate && extracted > 0) set(getEnergyStored() - extracted);
        return extracted;
    }

    @Override
    public boolean canReceive() {
        return item.getInput() > 0;
    }

    @Override
    public boolean canExtract() {
        return item.getOutput() > 0;
    }

    private void set(int stored) {
        stack.set(StoredEnergyAttachment.TYPE.get(), with(item, stored));
    }
}
