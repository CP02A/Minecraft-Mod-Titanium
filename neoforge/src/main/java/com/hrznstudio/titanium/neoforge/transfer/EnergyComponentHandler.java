/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.neoforge.transfer;

import com.hrznstudio.titanium.component.energy.EnergyStorageComponent;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Exposes an {@link EnergyStorageComponent} as a transactional NeoForge {@link EnergyHandler}.
 */
public class EnergyComponentHandler extends SnapshotJournal<Integer> implements EnergyHandler {

    private static final Map<EnergyStorageComponent<?>, EnergyComponentHandler> HANDLERS = Collections.synchronizedMap(new WeakHashMap<>());

    // Weak so the handler map doesn't keep the component alive
    private final WeakReference<EnergyStorageComponent<?>> component;

    private EnergyComponentHandler(EnergyStorageComponent<?> component) {
        this.component = new WeakReference<>(component);
    }

    /**
     * @return the handler of the component, shared so nested transactions stay consistent
     */
    public static EnergyComponentHandler of(EnergyStorageComponent<?> component) {
        return HANDLERS.computeIfAbsent(component, EnergyComponentHandler::new);
    }

    @Nullable
    public EnergyStorageComponent<?> getComponent() {
        return component.get();
    }

    @Override
    public long getAmountAsLong() {
        EnergyStorageComponent<?> component = this.component.get();
        return component == null ? 0 : component.getEnergyStored();
    }

    @Override
    public long getCapacityAsLong() {
        EnergyStorageComponent<?> component = this.component.get();
        return component == null ? 0 : component.getMaxEnergyStored();
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        EnergyStorageComponent<?> component = this.component.get();
        if (component == null) return 0;
        int received = component.receiveEnergy(amount, true);
        if (received > 0) {
            updateSnapshots(transaction);
            component.setEnergyRaw(component.getEnergyStored() + received);
        }
        return received;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        EnergyStorageComponent<?> component = this.component.get();
        if (component == null) return 0;
        int extracted = component.extractEnergy(amount, true);
        if (extracted > 0) {
            updateSnapshots(transaction);
            component.setEnergyRaw(component.getEnergyStored() - extracted);
        }
        return extracted;
    }

    @Override
    protected Integer createSnapshot() {
        return (int) getAmountAsLong();
    }

    @Override
    protected void revertToSnapshot(Integer snapshot) {
        EnergyStorageComponent<?> component = this.component.get();
        if (component != null) component.setEnergyRaw(snapshot);
    }

    @Override
    protected void onRootCommit(Integer originalState) {
        EnergyStorageComponent<?> component = this.component.get();
        if (component != null && originalState != component.getEnergyStored()) {
            component.onEnergyChanged();
        }
    }
}
