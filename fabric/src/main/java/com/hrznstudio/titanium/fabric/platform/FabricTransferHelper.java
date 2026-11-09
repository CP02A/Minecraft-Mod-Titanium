/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.fabric.platform;

import com.hrznstudio.titanium.api.transfer.IEnergyStorage;
import com.hrznstudio.titanium.api.transfer.IFluidStorage;
import com.hrznstudio.titanium.api.transfer.IItemStorage;
import com.hrznstudio.titanium.block.tile.ActiveTile;
import com.hrznstudio.titanium.block.tile.PoweredTile;
import com.hrznstudio.titanium.fabric.TitaniumApiLookups;
import com.hrznstudio.titanium.fabric.transfer.ContextItemEnergyStorage;
import com.hrznstudio.titanium.fabric.transfer.FabricFluidStorage;
import com.hrznstudio.titanium.fabric.transfer.FabricItemStorage;
import com.hrznstudio.titanium.item.EnergyItem;
import com.hrznstudio.titanium.platform.services.ITransferHelper;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public class FabricTransferHelper implements ITransferHelper {

    @Override
    public void registerBlockEntityStorages(String modId, Supplier<? extends BlockEntityType<?>> type) {
        BlockEntityType<?> blockEntityType = type.get();
        ItemStorage.SIDED.registerForBlockEntities((blockEntity, direction) -> {
            if (blockEntity instanceof ActiveTile<?> tile) {
                IItemStorage storage = tile.getItemHandler(direction);
                return storage == null ? null : FabricItemStorage.of(storage);
            }
            return null;
        }, blockEntityType);
        FluidStorage.SIDED.registerForBlockEntities((blockEntity, direction) -> {
            if (blockEntity instanceof ActiveTile<?> tile) {
                IFluidStorage storage = tile.getFluidHandler(direction);
                return storage == null ? null : FabricFluidStorage.of(storage);
            }
            return null;
        }, blockEntityType);
        TitaniumApiLookups.ENERGY.registerForBlockEntities((blockEntity, direction) ->
            blockEntity instanceof PoweredTile<?> powered ? powered.getEnergyStorage() : null, blockEntityType);
    }

    @Override
    public void invalidateStorages(BlockEntity blockEntity) {
        // Fabric lookups aren't cached by the provider
    }

    @Override
    public void registerEnergyItem(String modId, Supplier<? extends Item> item) {
        if (item.get() instanceof EnergyItem energyItem) {
            TitaniumApiLookups.ENERGY_ITEM.registerForItems((stack, context) -> new ContextItemEnergyStorage(context, energyItem), energyItem);
        }
    }

    @Override
    public boolean transferItems(Level level, BlockPos targetPos, Direction targetSide, IItemStorage storage, boolean push, int maxAmount) {
        Storage<ItemVariant> target = ItemStorage.SIDED.find(level, targetPos, targetSide);
        if (target == null) return false;
        Storage<ItemVariant> own = FabricItemStorage.of(storage);
        return StorageUtil.move(push ? own : target, push ? target : own, variant -> true, maxAmount, null) > 0;
    }

    @Override
    public boolean transferFluids(Level level, BlockPos targetPos, Direction targetSide, IFluidStorage storage, boolean push, int maxAmount) {
        Storage<FluidVariant> target = FluidStorage.SIDED.find(level, targetPos, targetSide);
        if (target == null) return false;
        Storage<FluidVariant> own = FabricFluidStorage.of(storage);
        return StorageUtil.move(push ? own : target, push ? target : own, variant -> true, FabricFluidStorage.toDroplets(maxAmount), null) > 0;
    }

    @Override
    public int pushEnergy(Level level, BlockPos targetPos, Direction targetSide, IEnergyStorage storage, int maxAmount) {
        IEnergyStorage target = TitaniumApiLookups.ENERGY.find(level, targetPos, targetSide);
        if (target == null || !target.canReceive()) return 0;
        int available = storage.extractEnergy(maxAmount, true);
        if (available <= 0) return 0;
        int accepted = target.receiveEnergy(available, true);
        if (accepted <= 0) return 0;
        int extracted = storage.extractEnergy(accepted, false);
        return target.receiveEnergy(extracted, false);
    }

    @Override
    public boolean interactWithFluidStorage(Player player, InteractionHand hand, Level level, BlockPos pos, IFluidStorage storage) {
        return FluidStorageUtil.interactWithFluidStorage(FabricFluidStorage.of(storage), player, hand);
    }

    @Override
    public int transferWithCarried(Player player, IFluidStorage storage, boolean intoStorage, int maxAmount, boolean simulate) {
        return move(ContainerItemContext.ofPlayerCursor(player, player.containerMenu), storage, intoStorage, maxAmount, !simulate);
    }

    @Override
    public int simulateTransferWithStack(ItemStack stack, IFluidStorage storage, boolean intoStorage, int maxAmount) {
        if (stack.isEmpty()) return 0;
        return move(ContainerItemContext.withConstant(stack.copyWithCount(1)), storage, intoStorage, maxAmount, false);
    }

    private static int move(ContainerItemContext context, IFluidStorage storage, boolean intoStorage, int maxAmount, boolean commit) {
        Storage<FluidVariant> itemStorage = context.find(FluidStorage.ITEM);
        if (itemStorage == null) return 0;
        Storage<FluidVariant> own = FabricFluidStorage.of(storage);
        try (Transaction transaction = Transaction.openOuter()) {
            long moved = StorageUtil.move(intoStorage ? itemStorage : own, intoStorage ? own : itemStorage, variant -> true, FabricFluidStorage.toDroplets(maxAmount), transaction);
            if (commit) transaction.commit();
            return FabricFluidStorage.toMillibuckets(moved);
        }
    }

    @Override
    public boolean isFluidContainer(ItemStack stack) {
        return !stack.isEmpty() && ContainerItemContext.withConstant(stack.copyWithCount(1)).find(FluidStorage.ITEM) != null;
    }

    @Override
    public ItemStack exchangeFluidWithItem(ItemStack stack, IFluidStorage storage, boolean execute) {
        SingleStackStorage slot = new SingleStackStorage() {
            private ItemStack contents = stack.copyWithCount(1);

            @Override
            protected ItemStack getStack() {
                return contents;
            }

            @Override
            protected void setStack(ItemStack stack) {
                this.contents = stack;
            }
        };
        ContainerItemContext context = ContainerItemContext.ofSingleSlot(slot);
        Storage<FluidVariant> itemStorage = context.find(FluidStorage.ITEM);
        if (itemStorage == null) return ItemStack.EMPTY;
        Storage<FluidVariant> tank = FabricFluidStorage.of(storage);
        try (Transaction transaction = Transaction.openOuter()) {
            long moved = StorageUtil.move(tank, itemStorage, variant -> true, Long.MAX_VALUE, transaction);
            if (moved == 0) {
                moved = StorageUtil.move(itemStorage, tank, variant -> true, Long.MAX_VALUE, transaction);
            }
            if (moved == 0) return ItemStack.EMPTY;
            ItemStack result = slot.getResource().toStack((int) slot.getAmount());
            if (execute) transaction.commit();
            return result;
        }
    }
}
