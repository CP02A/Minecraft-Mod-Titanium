/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.platform.services;

import com.hrznstudio.titanium.api.transfer.IEnergyStorage;
import com.hrznstudio.titanium.api.transfer.IFluidStorage;
import com.hrznstudio.titanium.api.transfer.IItemStorage;
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

/**
 * Bridges Titanium's storages with the transfer systems of the platform (NeoForge capabilities and transfer API,
 * Fabric API lookups and transfer API). Fluid amounts are in millibuckets.
 */
public interface ITransferHelper {

    /**
     * Exposes the item, fluid and energy storages of Titanium tiles of the given type to other mods.
     */
    void registerBlockEntityStorages(String modId, Supplier<? extends BlockEntityType<?>> type);

    /**
     * Notifies the platform that the storages exposed by the block entity changed (for example when the face modes of
     * a sided component changed), so cached lookups are refreshed.
     */
    void invalidateStorages(BlockEntity blockEntity);

    /**
     * Exposes the energy of a {@link com.hrznstudio.titanium.item.EnergyItem} to other mods.
     */
    void registerEnergyItem(String modId, Supplier<? extends Item> item);

    /**
     * Moves items between the storage and the block at the target position, accessed from the given side.
     *
     * @param push true to move from the storage to the target, false to move from the target to the storage
     * @return true if anything moved
     */
    boolean transferItems(Level level, BlockPos targetPos, Direction targetSide, IItemStorage storage, boolean push, int maxAmount);

    /**
     * Moves fluids between the storage and the block at the target position, accessed from the given side.
     *
     * @param push true to move from the storage to the target, false to move from the target to the storage
     * @return true if anything moved
     */
    boolean transferFluids(Level level, BlockPos targetPos, Direction targetSide, IFluidStorage storage, boolean push, int maxAmount);

    /**
     * Pushes energy from the storage to the block at the target position, accessed from the given side.
     *
     * @return the amount of energy moved
     */
    int pushEnergy(Level level, BlockPos targetPos, Direction targetSide, IEnergyStorage storage, int maxAmount);

    /**
     * Handles a player right-clicking a block that holds the storage with a fluid container.
     *
     * @return true if fluid was moved
     */
    boolean interactWithFluidStorage(Player player, InteractionHand hand, Level level, BlockPos pos, IFluidStorage storage);

    /**
     * Moves fluid between the stack the player holds in the cursor of the open menu and the storage.
     *
     * @param intoStorage true to empty the carried item into the storage, false to fill the carried item
     * @param simulate    when true nothing is modified
     * @return the amount of fluid moved
     */
    int transferWithCarried(Player player, IFluidStorage storage, boolean intoStorage, int maxAmount, boolean simulate);

    /**
     * Same as {@link #transferWithCarried(Player, IFluidStorage, boolean, int, boolean)} but for an arbitrary stack,
     * used on the client to preview the result of a transfer.
     */
    int simulateTransferWithStack(ItemStack stack, IFluidStorage storage, boolean intoStorage, int maxAmount);

    /**
     * @return true if the stack exposes a fluid storage
     */
    boolean isFluidContainer(ItemStack stack);

    /**
     * Fills a single item from the storage or, if that is not possible, empties it into the storage.
     *
     * @param stack   a single item, not modified
     * @param execute when false nothing is modified
     * @return the resulting item, or {@link ItemStack#EMPTY} if nothing could be moved
     */
    ItemStack exchangeFluidWithItem(ItemStack stack, IFluidStorage storage, boolean execute);
}
