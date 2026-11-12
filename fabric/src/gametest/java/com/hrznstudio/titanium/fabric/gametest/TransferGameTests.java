/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.fabric.gametest;

import com.hrznstudio.titanium._impl.test.TestBlock;
import com.hrznstudio.titanium._impl.test.TwentyFourTestBlock;
import com.hrznstudio.titanium.api.transfer.IFluidStorage;
import com.hrznstudio.titanium.api.transfer.IItemStorage;
import com.hrznstudio.titanium.block.tile.ActiveTile;
import com.hrznstudio.titanium.fabric.transfer.FabricFluidStorage;
import com.hrznstudio.titanium.fluid.TitaniumFluidStack;
import com.hrznstudio.titanium.platform.Services;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;

/**
 * Tests the bridge between Titanium's item and fluid storages and the Fabric Transfer API, in both directions:
 * other mods inserting into and extracting from Titanium tiles (including transaction rollback), and Titanium moving
 * items and fluids into and out of storages it doesn't own (a vanilla chest and cauldron).
 */
public class TransferGameTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);
    private static final BlockPos NEXT = POS.east();

    // Items

    @GameTest
    public void itemsInsertIntoTitaniumTile(GameTestHelper helper) {
        IItemStorage titanium = placeItemTile(helper);
        Storage<ItemVariant> tile = find(helper, ItemStorage.SIDED, "item");

        long inserted;
        try (Transaction transaction = Transaction.openOuter()) {
            inserted = tile.insert(ItemVariant.of(Items.IRON_INGOT), 10, transaction);
            transaction.commit();
        }

        helper.assertValueEqual(inserted, 10L, "inserted items");
        helper.assertValueEqual(count(titanium, Items.IRON_INGOT), 10, "iron in the Titanium tile");
        helper.succeed();
    }

    @GameTest
    public void itemsExtractFromTitaniumTile(GameTestHelper helper) {
        IItemStorage titanium = placeItemTile(helper);
        titanium.getSlot(0).setStack(new ItemStack(Items.IRON_INGOT, 20));
        Storage<ItemVariant> tile = find(helper, ItemStorage.SIDED, "item");

        long extracted;
        try (Transaction transaction = Transaction.openOuter()) {
            extracted = tile.extract(ItemVariant.of(Items.IRON_INGOT), 15, transaction);
            transaction.commit();
        }

        helper.assertValueEqual(extracted, 15L, "extracted items");
        helper.assertValueEqual(count(titanium, Items.IRON_INGOT), 5, "iron in the Titanium tile");
        helper.succeed();
    }

    @GameTest
    public void abortedTransactionRestoresTitaniumItems(GameTestHelper helper) {
        IItemStorage titanium = placeItemTile(helper);
        titanium.getSlot(0).setStack(new ItemStack(Items.IRON_INGOT, 20));
        Storage<ItemVariant> tile = find(helper, ItemStorage.SIDED, "item");

        try (Transaction outer = Transaction.openOuter()) {
            helper.assertValueEqual(tile.insert(ItemVariant.of(Items.GOLD_INGOT), 8, outer), 8L, "inserted gold");
            try (Transaction nested = outer.openNested()) {
                helper.assertValueEqual(tile.extract(ItemVariant.of(Items.IRON_INGOT), 20, nested), 20L, "extracted iron");
                nested.commit();
            }
            helper.assertValueEqual(count(titanium, Items.IRON_INGOT), 0, "iron after the nested commit");
            // Closed without commit: everything is rolled back
        }

        helper.assertValueEqual(count(titanium, Items.IRON_INGOT), 20, "iron after the abort");
        helper.assertValueEqual(count(titanium, Items.GOLD_INGOT), 0, "gold after the abort");
        helper.succeed();
    }

    @GameTest
    public void titaniumMovesItemsIntoAndOutOfChest(GameTestHelper helper) {
        IItemStorage titanium = placeItemTile(helper);
        titanium.getSlot(0).setStack(new ItemStack(Items.IRON_INGOT, 20));
        helper.setBlock(NEXT, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(NEXT, ChestBlockEntity.class);

        helper.assertTrue(Services.TRANSFER.transferItems(helper.getLevel(), helper.absolutePos(NEXT), Direction.WEST, titanium, true, 8), "pushing into the chest failed");
        helper.assertValueEqual(chest.countItem(Items.IRON_INGOT), 8, "iron in the chest after the push");
        helper.assertValueEqual(count(titanium, Items.IRON_INGOT), 12, "iron in the Titanium tile after the push");

        helper.assertTrue(Services.TRANSFER.transferItems(helper.getLevel(), helper.absolutePos(NEXT), Direction.WEST, titanium, false, 5), "pulling from the chest failed");
        helper.assertValueEqual(chest.countItem(Items.IRON_INGOT), 3, "iron in the chest after the pull");
        helper.assertValueEqual(count(titanium, Items.IRON_INGOT), 17, "iron in the Titanium tile after the pull");
        helper.succeed();
    }

    // Fluids

    @GameTest
    public void fluidsInsertIntoTitaniumTile(GameTestHelper helper) {
        IFluidStorage titanium = placeFluidTile(helper);
        Storage<FluidVariant> tile = find(helper, FluidStorage.SIDED, "fluid");

        long inserted;
        try (Transaction transaction = Transaction.openOuter()) {
            inserted = tile.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET * 2, transaction);
            transaction.commit();
        }

        helper.assertValueEqual(inserted, FluidConstants.BUCKET * 2, "inserted droplets");
        assertTank(helper, titanium, 2000, "after the insert");
        helper.succeed();
    }

    @GameTest
    public void fluidsExtractFromTitaniumTile(GameTestHelper helper) {
        IFluidStorage titanium = placeFluidTile(helper);
        titanium.getTank(0).setFluid(new TitaniumFluidStack(Fluids.WATER, 3000));
        Storage<FluidVariant> tile = find(helper, FluidStorage.SIDED, "fluid");

        long extracted;
        try (Transaction transaction = Transaction.openOuter()) {
            extracted = tile.extract(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction);
            transaction.commit();
        }

        helper.assertValueEqual(extracted, FluidConstants.BUCKET, "extracted droplets");
        assertTank(helper, titanium, 2000, "after the extract");
        helper.succeed();
    }

    @GameTest
    public void abortedTransactionRestoresTitaniumFluids(GameTestHelper helper) {
        IFluidStorage titanium = placeFluidTile(helper);
        titanium.getTank(0).setFluid(new TitaniumFluidStack(Fluids.WATER, 3000));
        Storage<FluidVariant> tile = find(helper, FluidStorage.SIDED, "fluid");

        try (Transaction outer = Transaction.openOuter()) {
            helper.assertValueEqual(tile.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, outer), FluidConstants.BUCKET, "inserted droplets");
            try (Transaction nested = outer.openNested()) {
                helper.assertValueEqual(tile.extract(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET * 4, nested), FluidConstants.BUCKET * 4, "extracted droplets");
                nested.commit();
            }
            helper.assertValueEqual(titanium.getFluidInTank(0).getAmount(), 0, "water after the nested commit");
            // Closed without commit: everything is rolled back
        }

        assertTank(helper, titanium, 3000, "after the abort");
        helper.succeed();
    }

    @GameTest
    public void titaniumMovesFluidsIntoAndOutOfCauldron(GameTestHelper helper) {
        IFluidStorage titanium = placeFluidTile(helper);
        titanium.getTank(0).setFluid(new TitaniumFluidStack(Fluids.WATER, 3000));
        helper.setBlock(NEXT, Blocks.CAULDRON);

        // A cauldron only takes whole buckets
        helper.assertTrue(Services.TRANSFER.transferFluids(helper.getLevel(), helper.absolutePos(NEXT), Direction.WEST, titanium, true, 1000), "pushing into the cauldron failed");
        helper.assertBlockPresent(Blocks.WATER_CAULDRON, NEXT);
        assertTank(helper, titanium, 2000, "after the push");

        helper.assertTrue(Services.TRANSFER.transferFluids(helper.getLevel(), helper.absolutePos(NEXT), Direction.WEST, titanium, false, 1000), "pulling from the cauldron failed");
        helper.assertBlockPresent(Blocks.CAULDRON, NEXT);
        assertTank(helper, titanium, 3000, "after the pull");
        helper.succeed();
    }

    @GameTest
    public void fluidUnitsConvert(GameTestHelper helper) {
        helper.assertValueEqual(FabricFluidStorage.toDroplets(TitaniumFluidStack.BUCKET_VOLUME), FluidConstants.BUCKET, "droplets per bucket");
        helper.assertValueEqual(FabricFluidStorage.toMillibuckets(FluidConstants.BUCKET), TitaniumFluidStack.BUCKET_VOLUME, "millibuckets per bucket");
        // Amounts below one millibucket are rounded down, never up
        helper.assertValueEqual(FabricFluidStorage.toMillibuckets(FabricFluidStorage.DROPLETS_PER_MB - 1), 0, "a partial millibucket");
        helper.succeed();
    }

    private static IItemStorage placeItemTile(GameTestHelper helper) {
        helper.setBlock(POS, TwentyFourTestBlock.TEST.getBlock());
        IItemStorage storage = helper.getBlockEntity(POS, ActiveTile.class).getItemHandler(Direction.UP);
        helper.assertTrue(storage != null, "The test tile has no item storage on top");
        return storage;
    }

    private static IFluidStorage placeFluidTile(GameTestHelper helper) {
        helper.setBlock(POS, TestBlock.TEST.getBlock());
        IFluidStorage storage = helper.getBlockEntity(POS, ActiveTile.class).getFluidHandler(Direction.UP);
        helper.assertTrue(storage != null && storage.getTanks() > 0, "The test tile has no fluid tank");
        return storage;
    }

    private static <T> Storage<T> find(GameTestHelper helper, net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup<Storage<T>, Direction> lookup, String what) {
        Storage<T> storage = lookup.find(helper.getLevel(), helper.absolutePos(POS), Direction.UP);
        helper.assertTrue(storage != null, "No Fabric " + what + " storage on the Titanium tile");
        return storage;
    }

    private static int count(IItemStorage storage, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int i = 0; i < storage.getSlots(); i++) {
            ItemStack stack = storage.getStackInSlot(i);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private static void assertTank(GameTestHelper helper, IFluidStorage storage, int amount, String when) {
        TitaniumFluidStack fluid = storage.getFluidInTank(0);
        helper.assertValueEqual(fluid.getAmount(), amount, "water in the Titanium tank " + when);
        if (amount > 0) helper.assertTrue(fluid.is(Fluids.WATER), "The Titanium tank holds " + fluid.getFluid() + " " + when);
    }
}
