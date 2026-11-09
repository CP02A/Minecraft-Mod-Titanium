/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.neoforge.platform;

import com.hrznstudio.titanium.api.transfer.IEnergyStorage;
import com.hrznstudio.titanium.api.transfer.IFluidStorage;
import com.hrznstudio.titanium.api.transfer.IItemStorage;
import com.hrznstudio.titanium.block.tile.ActiveTile;
import com.hrznstudio.titanium.block.tile.PoweredTile;
import com.hrznstudio.titanium.component.energy.EnergyStorageComponent;
import com.hrznstudio.titanium.item.EnergyItem;
import com.hrznstudio.titanium.neoforge.transfer.EnergyComponentHandler;
import com.hrznstudio.titanium.neoforge.transfer.FluidStorageResourceHandler;
import com.hrznstudio.titanium.neoforge.transfer.ItemEnergyHandler;
import com.hrznstudio.titanium.neoforge.transfer.ItemStorageResourceHandler;
import com.hrznstudio.titanium.platform.services.ITransferHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class NeoForgeTransferHelper implements ITransferHelper {

    @Nullable
    public static ResourceHandler<ItemResource> wrap(@Nullable IItemStorage storage) {
        return storage == null ? null : new ItemStorageResourceHandler(storage);
    }

    @Nullable
    public static ResourceHandler<FluidResource> wrap(@Nullable IFluidStorage storage) {
        return storage == null ? null : new FluidStorageResourceHandler(storage);
    }

    @Nullable
    public static EnergyHandler wrap(@Nullable EnergyStorageComponent<?> storage) {
        return storage == null ? null : EnergyComponentHandler.of(storage);
    }

    @Override
    public void registerBlockEntityStorages(String modId, Supplier<? extends BlockEntityType<?>> type) {
        NeoForgePlatformHelper.getModBus(modId).addListener((RegisterCapabilitiesEvent event) -> {
            BlockEntityType<?> blockEntityType = type.get();
            event.registerBlockEntity(Capabilities.Energy.BLOCK, blockEntityType, (object, context) ->
                object instanceof PoweredTile<?> powered ? wrap(powered.getEnergyStorage()) : null);
            event.registerBlockEntity(Capabilities.Fluid.BLOCK, blockEntityType, (object, context) ->
                object instanceof ActiveTile<?> tile ? wrap(tile.getFluidHandler(context)) : null);
            event.registerBlockEntity(Capabilities.Item.BLOCK, blockEntityType, (object, context) ->
                object instanceof ActiveTile<?> tile ? wrap(tile.getItemHandler(context)) : null);
        });
    }

    @Override
    public void invalidateStorages(BlockEntity blockEntity) {
        blockEntity.invalidateCapabilities();
    }

    @Override
    public void registerEnergyItem(String modId, Supplier<? extends Item> item) {
        NeoForgePlatformHelper.getModBus(modId).addListener((RegisterCapabilitiesEvent event) -> {
            if (item.get() instanceof EnergyItem energyItem) {
                event.registerItem(Capabilities.Energy.ITEM, (stack, access) -> new ItemEnergyHandler(access, energyItem), energyItem);
            }
        });
    }

    @Override
    public boolean transferItems(Level level, BlockPos targetPos, Direction targetSide, IItemStorage storage, boolean push, int maxAmount) {
        ResourceHandler<ItemResource> target = level.getCapability(Capabilities.Item.BLOCK, targetPos, targetSide);
        if (target == null) return false;
        ResourceHandler<ItemResource> own = new ItemStorageResourceHandler(storage);
        return ResourceHandlerUtil.move(push ? own : target, push ? target : own, resource -> true, maxAmount, null) > 0;
    }

    @Override
    public boolean transferFluids(Level level, BlockPos targetPos, Direction targetSide, IFluidStorage storage, boolean push, int maxAmount) {
        ResourceHandler<FluidResource> target = level.getCapability(Capabilities.Fluid.BLOCK, targetPos, targetSide);
        if (target == null) return false;
        ResourceHandler<FluidResource> own = new FluidStorageResourceHandler(storage);
        return ResourceHandlerUtil.move(push ? own : target, push ? target : own, resource -> true, maxAmount, null) > 0;
    }

    @Override
    public int pushEnergy(Level level, BlockPos targetPos, Direction targetSide, IEnergyStorage storage, int maxAmount) {
        EnergyHandler target = level.getCapability(Capabilities.Energy.BLOCK, targetPos, targetSide);
        if (target == null) return 0;
        int available = storage.extractEnergy(maxAmount, true);
        if (available <= 0) return 0;
        try (Transaction transaction = Transaction.openRoot()) {
            int accepted = target.insert(available, transaction);
            if (accepted > 0 && storage.extractEnergy(accepted, true) == accepted) {
                storage.extractEnergy(accepted, false);
                transaction.commit();
                return accepted;
            }
        }
        return 0;
    }

    @Override
    public boolean interactWithFluidStorage(Player player, InteractionHand hand, Level level, BlockPos pos, IFluidStorage storage) {
        return FluidUtil.interactWithFluidHandler(player, hand, pos, new FluidStorageResourceHandler(storage), null);
    }

    @Override
    public int transferWithCarried(Player player, IFluidStorage storage, boolean intoStorage, int maxAmount, boolean simulate) {
        ItemAccess access = ItemAccess.forPlayerCursor(player, player.containerMenu).oneByOne();
        return move(access, storage, intoStorage, maxAmount, !simulate);
    }

    @Override
    public int simulateTransferWithStack(ItemStack stack, IFluidStorage storage, boolean intoStorage, int maxAmount) {
        if (stack.isEmpty()) return 0;
        return move(ItemAccess.forStack(stack.copy()).oneByOne(), storage, intoStorage, maxAmount, false);
    }

    private static int move(ItemAccess access, IFluidStorage storage, boolean intoStorage, int maxAmount, boolean commit) {
        ResourceHandler<FluidResource> itemHandler = access.getCapability(Capabilities.Fluid.ITEM);
        if (itemHandler == null) return 0;
        ResourceHandler<FluidResource> own = new FluidStorageResourceHandler(storage);
        try (Transaction transaction = Transaction.openRoot()) {
            int moved = ResourceHandlerUtil.move(intoStorage ? itemHandler : own, intoStorage ? own : itemHandler, resource -> true, maxAmount, transaction);
            if (commit) transaction.commit();
            return moved;
        }
    }

    @Override
    public boolean isFluidContainer(ItemStack stack) {
        return !stack.isEmpty() && ItemAccess.forStack(stack.copy()).oneByOne().getCapability(Capabilities.Fluid.ITEM) != null;
    }

    @Override
    public ItemStack exchangeFluidWithItem(ItemStack stack, IFluidStorage storage, boolean execute) {
        ItemAccess access = ItemAccess.forStack(stack.copyWithCount(1)).oneByOne();
        ResourceHandler<FluidResource> itemHandler = access.getCapability(Capabilities.Fluid.ITEM);
        if (itemHandler == null) return ItemStack.EMPTY;
        ResourceHandler<FluidResource> tank = new FluidStorageResourceHandler(storage);
        try (Transaction transaction = Transaction.openRoot()) {
            int moved = ResourceHandlerUtil.move(tank, itemHandler, resource -> true, Integer.MAX_VALUE, transaction);
            if (moved == 0) {
                moved = ResourceHandlerUtil.move(itemHandler, tank, resource -> true, Integer.MAX_VALUE, transaction);
            }
            if (moved == 0) return ItemStack.EMPTY;
            ItemStack result = access.getResource().toStack(access.getAmount());
            if (execute) transaction.commit();
            return result;
        }
    }
}
