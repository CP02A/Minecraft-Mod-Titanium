/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.datagenerator.loot.block;

import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

public class BasicBlockLootTables extends BlockLootSubProvider {
    private final Supplier<List<Block>> blocksToProcess;

    public BasicBlockLootTables(Supplier<List<Block>> blocksToProcess, LootTableSubProvider.Context context) {
        super(new HashSet<>(), FeatureFlags.REGISTRY.allFlags(), context);
        this.blocksToProcess = blocksToProcess;
    }


    public LootTable.Builder droppingNothing() {
        return LootTable.lootTable();
    }

    public LootTable.Builder droppingSelf(ItemLike itemProvider) {
        return LootTable.lootTable()
            .withPool(applyExplosionCondition(itemProvider, LootPool.lootPool()
                .setRolls(ContextIntProviders.exactly(1))
                .add(LootItem.lootTableItem(itemProvider))));
    }

    @Override
    public void generate() {
        blocksToProcess.get()
            .forEach(block -> {
                if (block instanceof IBlockLootTableProvider) {
                    this.add(block, ((IBlockLootTableProvider) block).getLootTable(this));
                }
            });
    }

    /**
     * Only generates the tables of the given blocks instead of validating every registered block like vanilla does
     */
    @Override
    public void run() {
        for (Block block : blocksToProcess.get()) {
            if (block instanceof IBlockLootTableProvider provider) {
                block.getLootTable().ifPresent(key -> this.output.accept(key, provider.getLootTable(this)));
            }
        }
    }
}
