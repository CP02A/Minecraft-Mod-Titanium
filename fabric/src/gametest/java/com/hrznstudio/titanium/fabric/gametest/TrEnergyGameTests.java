/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.fabric.gametest;

import com.hrznstudio.titanium._impl.creative.CreativeFEGeneratorBlock;
import com.hrznstudio.titanium._impl.test.MachineTestBlock;
import com.hrznstudio.titanium._impl.test.tile.MachineTestTile;
import com.hrznstudio.titanium.api.transfer.IEnergyStorage;
import com.hrznstudio.titanium.attachment.StoredEnergyAttachment;
import com.hrznstudio.titanium.component.energy.EnergyStorageComponent;
import com.hrznstudio.titanium.energy.EnergyStorageItemStack;
import com.hrznstudio.titanium.fabric.gametest.TitaniumGameTestMod.TrBufferBlockEntity;
import com.hrznstudio.titanium.platform.Services;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/**
 * Tests the bridge between Titanium's energy and the Team Reborn Energy API, in both directions, for block entities
 * and items, including transaction rollback.
 */
public class TrEnergyGameTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);
    private static final BlockPos NEXT = POS.east();

    @GameTest
    public void trEnergyMovesIntoTitaniumTile(GameTestHelper helper) {
        EnergyStorageComponent<?> machine = placeMachine(helper);
        EnergyStorage tile = findBlockStorage(helper, POS);
        SimpleEnergyStorage source = new SimpleEnergyStorage(1000, 1000, 1000);
        source.amount = 500;

        long moved;
        try (Transaction transaction = Transaction.openOuter()) {
            moved = EnergyStorageUtil.move(source, tile, 300, transaction);
            transaction.commit();
        }

        helper.assertValueEqual(moved, 300L, "moved energy");
        helper.assertValueEqual(machine.getEnergyStored(), 300, "Titanium tile energy");
        helper.assertValueEqual(source.amount, 200L, "source energy");
        helper.succeed();
    }

    @GameTest
    public void titaniumTileExtractsIntoTrEnergy(GameTestHelper helper) {
        EnergyStorageComponent<?> machine = placeMachine(helper);
        machine.setEnergyStored(800);
        EnergyStorage tile = findBlockStorage(helper, POS);
        SimpleEnergyStorage sink = new SimpleEnergyStorage(1000, 1000, 1000);

        long moved;
        try (Transaction transaction = Transaction.openOuter()) {
            moved = EnergyStorageUtil.move(tile, sink, 500, transaction);
            transaction.commit();
        }

        helper.assertValueEqual(moved, 500L, "moved energy");
        helper.assertValueEqual(machine.getEnergyStored(), 300, "Titanium tile energy");
        helper.assertValueEqual(sink.amount, 500L, "sink energy");
        helper.succeed();
    }

    @GameTest
    public void abortedTransactionRestoresTitaniumTile(GameTestHelper helper) {
        EnergyStorageComponent<?> machine = placeMachine(helper);
        machine.setEnergyStored(1000);
        EnergyStorage tile = findBlockStorage(helper, POS);

        try (Transaction outer = Transaction.openOuter()) {
            helper.assertValueEqual(tile.insert(200, outer), 200L, "inserted energy");
            helper.assertValueEqual(machine.getEnergyStored(), 1200, "energy inside the transaction");
            try (Transaction nested = outer.openNested()) {
                helper.assertValueEqual(tile.extract(700, nested), 700L, "extracted energy");
                nested.commit();
            }
            helper.assertValueEqual(machine.getEnergyStored(), 500, "energy after the nested commit");
            // Closed without commit: everything is rolled back
        }

        helper.assertValueEqual(machine.getEnergyStored(), 1000, "energy after the abort");
        helper.assertValueEqual(tile.getAmount(), 1000L, "TR view of the energy after the abort");
        helper.succeed();
    }

    @GameTest
    public void titaniumPushesIntoTrBlock(GameTestHelper helper) {
        helper.setBlock(NEXT, TitaniumGameTestMod.TR_BUFFER);
        TrBufferBlockEntity buffer = helper.getBlockEntity(NEXT, TrBufferBlockEntity.class);
        EnergyStorageComponent<?> source = new EnergyStorageComponent<>(1000, 0, 0);
        source.setEnergyStored(1000);

        int pushed = Services.TRANSFER.pushEnergy(helper.getLevel(), helper.absolutePos(NEXT), Direction.WEST, source, 250);

        // The buffer only accepts BUFFER_MAX_INSERT per operation
        helper.assertValueEqual(pushed, TitaniumGameTestMod.BUFFER_MAX_INSERT, "pushed energy");
        helper.assertValueEqual(source.getEnergyStored(), 1000 - TitaniumGameTestMod.BUFFER_MAX_INSERT, "Titanium source energy");
        helper.assertValueEqual(buffer.energy.amount, (long) TitaniumGameTestMod.BUFFER_MAX_INSERT, "TR buffer energy");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void creativeGeneratorChargesTrBlock(GameTestHelper helper) {
        helper.setBlock(POS, CreativeFEGeneratorBlock.INSTANCE.getBlock());
        helper.setBlock(NEXT, TitaniumGameTestMod.TR_BUFFER);

        helper.succeedWhen(() -> helper.assertValueEqual(
            helper.getBlockEntity(NEXT, TrBufferBlockEntity.class).energy.amount,
            (long) TitaniumGameTestMod.BUFFER_CAPACITY, "TR buffer energy"));
    }

    @GameTest
    public void energyItemThroughTrEnergy(GameTestHelper helper) {
        Slot slot = new Slot(new ItemStack(TitaniumGameTestMod.BATTERY.get()));
        EnergyStorage item = ContainerItemContext.ofSingleSlot(slot).find(EnergyStorage.ITEM);
        helper.assertTrue(item != null, "Titanium energy item isn't exposed through Team Reborn Energy");

        try (Transaction transaction = Transaction.openOuter()) {
            helper.assertValueEqual(item.insert(150, transaction), (long) TitaniumGameTestMod.BATTERY_INPUT, "inserted energy");
            transaction.commit();
        }
        helper.assertValueEqual(titaniumEnergy(slot), TitaniumGameTestMod.BATTERY_INPUT, "Titanium view after the insert");

        try (Transaction transaction = Transaction.openOuter()) {
            item.insert(50, transaction);
        }
        helper.assertValueEqual(titaniumEnergy(slot), TitaniumGameTestMod.BATTERY_INPUT, "Titanium view after the abort");

        try (Transaction transaction = Transaction.openOuter()) {
            helper.assertValueEqual(item.extract(1000, transaction), (long) TitaniumGameTestMod.BATTERY_OUTPUT, "extracted energy");
            transaction.commit();
        }
        helper.assertValueEqual(titaniumEnergy(slot), TitaniumGameTestMod.BATTERY_INPUT - TitaniumGameTestMod.BATTERY_OUTPUT, "Titanium view after the extract");
        helper.succeed();
    }

    @GameTest
    public void drainedEnergyItemSavesAndSyncs(GameTestHelper helper) {
        ItemStack stack = new ItemStack(TitaniumGameTestMod.BATTERY.get());
        stack.set(StoredEnergyAttachment.TYPE.get(), EnergyStorageItemStack.with(TitaniumGameTestMod.BATTERY.get(), 0));
        RegistryAccess registries = helper.getLevel().registryAccess();

        // Saving
        Tag saved = ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack)
            .getOrThrow(error -> new AssertionError("Saving a drained energy item failed: " + error));
        ItemStack loaded = ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), saved)
            .getOrThrow(error -> new AssertionError("Loading a drained energy item failed: " + error));
        helper.assertTrue(ItemStack.matches(stack, loaded), "Drained energy item changed when saved and loaded");

        // Syncing to clients
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            ItemStack.STREAM_CODEC.encode(buffer, stack);
            helper.assertTrue(ItemStack.matches(stack, ItemStack.STREAM_CODEC.decode(buffer)), "Drained energy item changed when synced");
        } finally {
            buffer.release();
        }

        // Items that only receive or only send energy have a zero rate
        StoredEnergyAttachment inputOnly = new StoredEnergyAttachment(0, 1000, 100, 0);
        StoredEnergyAttachment.CODEC.encodeStart(NbtOps.INSTANCE, inputOnly)
            .getOrThrow(error -> new AssertionError("Saving an input only energy item failed: " + error));
        helper.succeed();
    }

    private static EnergyStorageComponent<?> placeMachine(GameTestHelper helper) {
        helper.setBlock(POS, MachineTestBlock.TEST.getBlock());
        return helper.getBlockEntity(POS, MachineTestTile.class).getEnergyStorage();
    }

    private static EnergyStorage findBlockStorage(GameTestHelper helper, BlockPos pos) {
        EnergyStorage storage = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(pos), Direction.UP);
        helper.assertTrue(storage != null, "No Team Reborn Energy storage at " + pos);
        return storage;
    }

    private static int titaniumEnergy(Slot slot) {
        return TitaniumGameTestMod.BATTERY.get().getEnergyStorage(slot.stack).map(IEnergyStorage::getEnergyStored).orElse(-1);
    }

    private static class Slot extends SingleStackStorage {
        private ItemStack stack;

        private Slot(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        protected ItemStack getStack() {
            return stack;
        }

        @Override
        protected void setStack(ItemStack stack) {
            this.stack = stack;
        }
    }
}
