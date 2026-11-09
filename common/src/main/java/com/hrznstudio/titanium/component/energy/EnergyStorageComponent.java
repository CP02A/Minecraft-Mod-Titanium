/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.component.energy;

import com.google.common.collect.Lists;
import com.hrznstudio.titanium.api.IFactory;
import com.hrznstudio.titanium.api.transfer.IEnergyStorage;
import com.hrznstudio.titanium.util.IValueIOSerializable;
import com.hrznstudio.titanium.api.client.IScreenAddonProvider;
import com.hrznstudio.titanium.client.screen.addon.EnergyBarScreenAddon;
import com.hrznstudio.titanium.component.IComponentHarness;
import com.hrznstudio.titanium.container.addon.IContainerAddon;
import com.hrznstudio.titanium.container.addon.IContainerAddonProvider;
import com.hrznstudio.titanium.container.addon.IntReferenceHolderAddon;
import com.hrznstudio.titanium.container.referenceholder.FunctionReferenceHolder;
import com.hrznstudio.titanium.nbthandler.INBTSerializable;
import com.hrznstudio.titanium.util.ValueIOSerialization;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nonnull;
import java.util.List;

public class EnergyStorageComponent<T extends IComponentHarness> implements IEnergyStorage, IValueIOSerializable,
    IScreenAddonProvider, IContainerAddonProvider, INBTSerializable<CompoundTag> {

    private final int xPos;
    private final int yPos;

    protected T componentHarness;
    protected int energy;
    protected int capacity;
    protected int maxReceive;
    protected int maxExtract;

    public EnergyStorageComponent(int maxCapacity, int xPos, int yPos) {
        this(maxCapacity, maxCapacity, xPos, yPos);
    }

    public EnergyStorageComponent(int maxCapacity, int maxIO, int xPos, int yPos) {
        this(maxCapacity, maxIO, maxIO, xPos, yPos);
    }

    public EnergyStorageComponent(int maxCapacity, int maxReceive, int maxExtract, int xPos, int yPos) {
        this.capacity = maxCapacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        this.xPos = xPos;
        this.yPos = yPos;
    }

    @Override
    public int getEnergyStored() {
        return energy;
    }

    /**
     * Sets the stored energy, clamped to the capacity, and notifies the change
     */
    public void setEnergyStored(int energy) {
        int clamped = Math.min(Math.max(energy, 0), capacity);
        if (clamped != this.energy) {
            this.energy = clamped;
            onEnergyChanged();
        }
    }

    /**
     * Sets the stored energy without notifying the change, used by the loader transfer adapters
     */
    public void setEnergyRaw(int energy) {
        this.energy = energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
        if (energy > capacity) setEnergyStored(capacity);
    }

    public int getMaxReceive() {
        return maxReceive;
    }

    public int getMaxExtract() {
        return maxExtract;
    }

    public void setMaxReceive(int maxReceive) {
        this.maxReceive = maxReceive;
    }

    public void setMaxExtract(int maxExtract) {
        this.maxExtract = maxExtract;
    }

    @Override
    public boolean canReceive() {
        return maxReceive > 0;
    }

    @Override
    public boolean canExtract() {
        return maxExtract > 0;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = Math.max(0, Math.min(capacity - energy, Math.min(this.maxReceive, maxReceive)));
        if (!simulate && received > 0) {
            energy += received;
            onEnergyChanged();
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = Math.max(0, Math.min(energy, Math.min(this.maxExtract, maxExtract)));
        if (!simulate && extracted > 0) {
            energy -= extracted;
            onEnergyChanged();
        }
        return extracted;
    }

    /**
     * Adds energy ignoring the max receive rate, used by the owner of the storage
     */
    public int receiveEnergyForced(int amount, boolean simulate) {
        int received = Math.max(0, Math.min(capacity - energy, amount));
        if (!simulate && received > 0) {
            energy += received;
            onEnergyChanged();
        }
        return received;
    }

    /**
     * Removes energy ignoring the max extract rate, used by the owner of the storage
     */
    public int extractEnergyForced(int amount, boolean simulate) {
        int extracted = Math.max(0, Math.min(energy, amount));
        if (!simulate && extracted > 0) {
            energy -= extracted;
            onEnergyChanged();
        }
        return extracted;
    }

    public void onEnergyChanged() {
        update();
    }

    /**
     * Same key as NeoForge's SimpleEnergyHandler so saved data stays compatible
     */
    @Override
    public void serialize(ValueOutput output) {
        output.putInt("energy", energy);
    }

    @Override
    public void deserialize(ValueInput input) {
        energy = Math.max(0, input.getIntOr("energy", 0));
    }

    @Override
    @Nonnull
    public List<IFactory<?>> getScreenAddons() {
        return Lists.newArrayList(
            () -> new EnergyBarScreenAddon(xPos, yPos, this)
        );
    }

    @Override
    @Nonnull
    public List<IFactory<? extends IContainerAddon>> getContainerAddons() {
        return Lists.newArrayList(
            () -> new IntReferenceHolderAddon(new FunctionReferenceHolder(this::setEnergyStored, this::getEnergyStored))
        );
    }

    public void setComponentHarness(T componentHarness) {
        this.componentHarness = componentHarness;
    }

    private void update() {
        if (this.componentHarness != null) {
            this.componentHarness.markComponentForUpdate(true);
        }
    }

    public int getX() {
        return xPos;
    }

    public int getY() {
        return yPos;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return ValueIOSerialization.save(provider, this);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        ValueIOSerialization.load(provider, nbt, this);
    }
}

