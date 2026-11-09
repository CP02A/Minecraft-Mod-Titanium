/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */


package com.hrznstudio.titanium.datagenerator.loot;

import com.hrznstudio.titanium.datagenerator.loot.block.BasicBlockLootTables;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Loot tables are bootstrapped as datapack registry entries since 26.2.
 * Register this through {@code GatherDataRegistryEntriesEvent#add(Registries.LOOT_TABLE, new TitaniumLootTableProvider(blocks))}
 * or use {@link #blockEntry(Supplier)} with {@code GatherDataRegistryEntriesEvent#lootTable(...)}.
 */
public class TitaniumLootTableProvider extends LootTableProvider {

    public TitaniumLootTableProvider(Supplier<List<Block>> blocks) {
        super(Set.of(), List.of(blockEntry(blocks)));
    }

    public static SubProviderEntry blockEntry(Supplier<List<Block>> blocks) {
        return new SubProviderEntry(context -> new BasicBlockLootTables(blocks, context), LootContextParamSets.BLOCK);
    }
}
