/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.fabric.gametest;

import com.hrznstudio.titanium.item.EnergyItem;
import com.hrznstudio.titanium.module.DeferredRegistryHelper;
import com.hrznstudio.titanium.registry.RegistryEntry;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.Set;

/**
 * Test-only content: a block that only speaks Team Reborn Energy (like another mod's machine would) and a Titanium
 * {@link EnergyItem} registered through Titanium's own registry helper.
 */
public class TitaniumGameTestMod implements ModInitializer {
    public static final String MOD_ID = "titanium_gametest";

    public static final int BUFFER_CAPACITY = 1000;
    public static final int BUFFER_MAX_INSERT = 100;
    public static final int BUFFER_MAX_EXTRACT = 100;

    public static final int BATTERY_CAPACITY = 1000;
    public static final int BATTERY_INPUT = 100;
    public static final int BATTERY_OUTPUT = 40;

    public static Block TR_BUFFER;
    public static BlockEntityType<TrBufferBlockEntity> TR_BUFFER_TYPE;
    public static RegistryEntry<Item, EnergyItem> BATTERY;

    @Override
    public void onInitialize() {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "tr_buffer"));
        TR_BUFFER = Registry.register(BuiltInRegistries.BLOCK, key, new TrBufferBlock(BlockBehaviour.Properties.of().setId(key)));
        TR_BUFFER_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, key.identifier(), new BlockEntityType<>(TrBufferBlockEntity::new, Set.of(TR_BUFFER)));
        EnergyStorage.SIDED.registerForBlockEntity((blockEntity, direction) -> blockEntity.energy, TR_BUFFER_TYPE);

        BATTERY = new DeferredRegistryHelper(MOD_ID).registerTyped(Registries.ITEM, "battery",
            () -> new EnergyItem("battery", BATTERY_CAPACITY, BATTERY_INPUT, BATTERY_OUTPUT, new Item.Properties()));
    }

    public static class TrBufferBlock extends Block implements EntityBlock {
        public TrBufferBlock(Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new TrBufferBlockEntity(pos, state);
        }
    }

    public static class TrBufferBlockEntity extends BlockEntity {
        public final SimpleEnergyStorage energy = new SimpleEnergyStorage(BUFFER_CAPACITY, BUFFER_MAX_INSERT, BUFFER_MAX_EXTRACT) {
            @Override
            protected void onFinalCommit() {
                setChanged();
            }
        };

        public TrBufferBlockEntity(BlockPos pos, BlockState state) {
            super(TR_BUFFER_TYPE, pos, state);
        }
    }
}
