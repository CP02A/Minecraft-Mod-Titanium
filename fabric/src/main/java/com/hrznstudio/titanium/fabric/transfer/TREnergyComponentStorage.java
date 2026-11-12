/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.fabric.transfer;

import com.hrznstudio.titanium.component.energy.EnergyStorageComponent;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import team.reborn.energy.api.EnergyStorage;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Exposes an {@link EnergyStorageComponent} as a transactional Team Reborn {@link EnergyStorage}, so other Fabric mods
 * can move energy in and out of Titanium tiles. Changes are rolled back when the transaction is aborted.
 */
public class TREnergyComponentStorage extends SnapshotParticipant<Integer> implements EnergyStorage {

    private static final Map<EnergyStorageComponent<?>, TREnergyComponentStorage> STORAGES = Collections.synchronizedMap(new WeakHashMap<>());

    // Weak so the storage map doesn't keep the component alive
    private final WeakReference<EnergyStorageComponent<?>> component;

    private TREnergyComponentStorage(EnergyStorageComponent<?> component) {
        this.component = new WeakReference<>(component);
    }

    /**
     * @return the storage of the component, shared so nested transactions stay consistent
     */
    public static TREnergyComponentStorage of(EnergyStorageComponent<?> component) {
        return STORAGES.computeIfAbsent(component, TREnergyComponentStorage::new);
    }

    @Nullable
    public EnergyStorageComponent<?> getComponent() {
        return component.get();
    }

    @Override
    public boolean supportsInsertion() {
        EnergyStorageComponent<?> component = this.component.get();
        return component != null && component.canReceive();
    }

    @Override
    public boolean supportsExtraction() {
        EnergyStorageComponent<?> component = this.component.get();
        return component != null && component.canExtract();
    }

    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notNegative(maxAmount);
        EnergyStorageComponent<?> component = this.component.get();
        if (component == null) return 0;
        int received = component.receiveEnergy(clamp(maxAmount), true);
        if (received > 0) {
            updateSnapshots(transaction);
            component.setEnergyRaw(component.getEnergyStored() + received);
        }
        return received;
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notNegative(maxAmount);
        EnergyStorageComponent<?> component = this.component.get();
        if (component == null) return 0;
        int extracted = component.extractEnergy(clamp(maxAmount), true);
        if (extracted > 0) {
            updateSnapshots(transaction);
            component.setEnergyRaw(component.getEnergyStored() - extracted);
        }
        return extracted;
    }

    @Override
    public long getAmount() {
        EnergyStorageComponent<?> component = this.component.get();
        return component == null ? 0 : component.getEnergyStored();
    }

    @Override
    public long getCapacity() {
        EnergyStorageComponent<?> component = this.component.get();
        return component == null ? 0 : component.getMaxEnergyStored();
    }

    @Override
    protected Integer createSnapshot() {
        return (int) getAmount();
    }

    @Override
    protected void readSnapshot(Integer snapshot) {
        EnergyStorageComponent<?> component = this.component.get();
        if (component != null) component.setEnergyRaw(snapshot);
    }

    @Override
    protected void onFinalCommit() {
        EnergyStorageComponent<?> component = this.component.get();
        if (component != null) component.onEnergyChanged();
    }

    /**
     * Titanium storages are int based, Team Reborn Energy amounts are longs
     */
    static int clamp(long amount) {
        return (int) Math.min(amount, Integer.MAX_VALUE);
    }
}
