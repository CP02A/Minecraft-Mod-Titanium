/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.fabric.transfer;

import com.hrznstudio.titanium.api.transfer.IFluidStorage;
import com.hrznstudio.titanium.api.transfer.IFluidTank;
import com.hrznstudio.titanium.fluid.TitaniumFluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedSlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Exposes Titanium {@link IFluidStorage}s as transactional Fabric fluid storages. Titanium uses millibuckets, Fabric uses
 * droplets ({@value #DROPLETS_PER_MB} per millibucket).
 */
public final class FabricFluidStorage {

    public static final long DROPLETS_PER_MB = FluidConstants.BUCKET / TitaniumFluidStack.BUCKET_VOLUME;

    /**
     * Tank storages are shared by every view of the same tank so nested transactions stay consistent
     */
    private static final Map<IFluidTank, TankStorage> TANKS = Collections.synchronizedMap(new WeakHashMap<>());

    private FabricFluidStorage() {
    }

    public static FluidVariant toVariant(TitaniumFluidStack stack) {
        return stack.isEmpty() ? FluidVariant.blank() : FluidVariant.of(stack.getFluid(), stack.getComponentsPatch());
    }

    public static TitaniumFluidStack toTitanium(FluidVariant variant, int millibuckets) {
        return variant.isBlank() || millibuckets <= 0 ? TitaniumFluidStack.EMPTY : new TitaniumFluidStack(variant.typeHolder(), millibuckets, variant.getComponentsPatch());
    }

    public static int toMillibuckets(long droplets) {
        return (int) Math.min(droplets / DROPLETS_PER_MB, Integer.MAX_VALUE);
    }

    public static long toDroplets(int millibuckets) {
        return millibuckets * DROPLETS_PER_MB;
    }

    public static TankStorage of(IFluidTank tank) {
        return TANKS.computeIfAbsent(tank, TankStorage::new);
    }

    public static SlottedStorage<FluidVariant> of(IFluidStorage storage) {
        List<TankStorage> tanks = new ArrayList<>(storage.getTanks());
        for (int i = 0; i < storage.getTanks(); i++) {
            tanks.add(of(storage.getTank(i)));
        }
        return new CombinedSlottedStorage<>(tanks);
    }

    public static class TankStorage extends SnapshotParticipant<TitaniumFluidStack> implements SingleSlotStorage<FluidVariant> {
        // Weak so the tank map doesn't keep the tank alive
        private final WeakReference<IFluidTank> tank;

        private TankStorage(IFluidTank tank) {
            this.tank = new WeakReference<>(tank);
        }

        private TitaniumFluidStack getFluid() {
            IFluidTank tank = this.tank.get();
            return tank == null ? TitaniumFluidStack.EMPTY : tank.getFluid();
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            IFluidTank tank = this.tank.get();
            if (tank == null) return 0;
            int filled = tank.fill(toTitanium(resource, toMillibuckets(maxAmount)), true);
            if (filled > 0) {
                TitaniumFluidStack current = tank.getFluid();
                updateSnapshots(transaction);
                tank.setFluid(current.isEmpty() ? toTitanium(resource, filled) : current.grow(filled));
            }
            return toDroplets(filled);
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            IFluidTank tank = this.tank.get();
            if (tank == null) return 0;
            TitaniumFluidStack current = tank.getFluid();
            int drained = tank.drain(toTitanium(resource, toMillibuckets(maxAmount)), true).getAmount();
            if (drained > 0) {
                updateSnapshots(transaction);
                tank.setFluid(current.shrink(drained));
            }
            return toDroplets(drained);
        }

        @Override
        public boolean isResourceBlank() {
            return getFluid().isEmpty();
        }

        @Override
        public FluidVariant getResource() {
            return toVariant(getFluid());
        }

        @Override
        public long getAmount() {
            return toDroplets(getFluid().getAmount());
        }

        @Override
        public long getCapacity() {
            IFluidTank tank = this.tank.get();
            return tank == null ? 0 : toDroplets(tank.getCapacity());
        }

        @Override
        protected TitaniumFluidStack createSnapshot() {
            return getFluid();
        }

        @Override
        protected void readSnapshot(TitaniumFluidStack snapshot) {
            IFluidTank tank = this.tank.get();
            if (tank != null) tank.setFluid(snapshot);
        }

        @Override
        protected void onFinalCommit() {
            IFluidTank tank = this.tank.get();
            if (tank != null) tank.onContentsChanged();
        }
    }
}
