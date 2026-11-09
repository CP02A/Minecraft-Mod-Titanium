/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.neoforge.transfer;

import com.hrznstudio.titanium.api.transfer.IFluidStorage;
import com.hrznstudio.titanium.api.transfer.IFluidTank;
import com.hrznstudio.titanium.fluid.TitaniumFluidStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Exposes a Titanium {@link IFluidStorage} as a transactional NeoForge fluid {@link ResourceHandler}.
 * Both use millibuckets.
 */
public class FluidStorageResourceHandler implements ResourceHandler<FluidResource> {

    /**
     * Journals are shared by every adapter of the same tank so nested transactions stay consistent
     */
    private static final Map<IFluidTank, TankJournal> JOURNALS = Collections.synchronizedMap(new WeakHashMap<>());

    private final IFluidStorage storage;

    public FluidStorageResourceHandler(IFluidStorage storage) {
        this.storage = storage;
    }

    public IFluidStorage getStorage() {
        return storage;
    }

    public static FluidResource toResource(TitaniumFluidStack stack) {
        return stack.isEmpty() ? FluidResource.EMPTY : FluidResource.of(stack.getFluidHolder(), stack.getComponentsPatch());
    }

    public static TitaniumFluidStack toTitanium(FluidResource resource, int amount) {
        return resource.isEmpty() || amount <= 0 ? TitaniumFluidStack.EMPTY : new TitaniumFluidStack(resource.typeHolder(), amount, resource.getComponentsPatch());
    }

    public static FluidStack toNeoForge(TitaniumFluidStack stack) {
        return stack.isEmpty() ? FluidStack.EMPTY : new FluidStack(stack.getFluidHolder(), stack.getAmount(), stack.getComponentsPatch());
    }

    public static TitaniumFluidStack toTitanium(FluidStack stack) {
        return stack.isEmpty() ? TitaniumFluidStack.EMPTY : new TitaniumFluidStack(stack.typeHolder(), stack.getAmount(), stack.getComponentsPatch());
    }

    private static TankJournal journal(IFluidTank tank) {
        return JOURNALS.computeIfAbsent(tank, TankJournal::new);
    }

    @Override
    public int size() {
        return storage.getTanks();
    }

    @Override
    public FluidResource getResource(int index) {
        return toResource(storage.getTank(index).getFluid());
    }

    @Override
    public long getAmountAsLong(int index) {
        return storage.getTank(index).getFluidAmount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return storage.getTank(index).getCapacity();
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return !resource.isEmpty() && storage.getTank(index).canFill(toTitanium(resource, 1));
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0) return 0;
        IFluidTank tank = storage.getTank(index);
        int filled = tank.fill(toTitanium(resource, amount), true);
        if (filled > 0) {
            TitaniumFluidStack current = tank.getFluid();
            journal(tank).updateSnapshots(transaction);
            tank.setFluid(current.isEmpty() ? toTitanium(resource, filled) : current.grow(filled));
        }
        return filled;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (amount == 0) return 0;
        IFluidTank tank = storage.getTank(index);
        TitaniumFluidStack current = tank.getFluid();
        int drained = tank.drain(toTitanium(resource, amount), true).getAmount();
        if (drained > 0) {
            journal(tank).updateSnapshots(transaction);
            tank.setFluid(current.shrink(drained));
        }
        return drained;
    }

    private static class TankJournal extends SnapshotJournal<TitaniumFluidStack> {
        // Weak so the journal map doesn't keep the tank alive
        private final WeakReference<IFluidTank> tank;

        private TankJournal(IFluidTank tank) {
            this.tank = new WeakReference<>(tank);
        }

        @Override
        protected TitaniumFluidStack createSnapshot() {
            IFluidTank tank = this.tank.get();
            return tank == null ? TitaniumFluidStack.EMPTY : tank.getFluid();
        }

        @Override
        protected void revertToSnapshot(TitaniumFluidStack snapshot) {
            IFluidTank tank = this.tank.get();
            if (tank != null) tank.setFluid(snapshot);
        }

        @Override
        protected void onRootCommit(TitaniumFluidStack originalState) {
            IFluidTank tank = this.tank.get();
            if (tank != null && !TitaniumFluidStack.matches(originalState, tank.getFluid())) {
                tank.onContentsChanged();
            }
        }
    }
}
