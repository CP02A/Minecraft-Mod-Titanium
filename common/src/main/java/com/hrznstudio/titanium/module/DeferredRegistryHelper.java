/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.module;

import com.hrznstudio.titanium.block.BasicBlock;
import com.hrznstudio.titanium.block.BasicTileBlock;
import com.hrznstudio.titanium.item.EnergyItem;
import com.hrznstudio.titanium.platform.Services;
import com.hrznstudio.titanium.registry.RegistryEntry;
import com.hrznstudio.titanium.tab.TitaniumTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Registers objects for a mod. On NeoForge registration is deferred to the registry events, on Fabric objects are
 * registered right away.
 */
public class DeferredRegistryHelper {

    private static final ThreadLocal<ResourceKey<Block>> CURRENT_BLOCK_KEY = new ThreadLocal<>();
    private static final ThreadLocal<ResourceKey<Item>> CURRENT_ITEM_KEY = new ThreadLocal<>();

    private final String modId;

    public DeferredRegistryHelper(String modId) {
        this.modId = modId;
    }

    public String getModId() {
        return modId;
    }

    /**
     * Sets the registry id of the block that is currently being created by this helper in the properties. Required for
     * blocks since 1.21.2.
     */
    public static Block.Properties applyBlockRegistrationId(Block.Properties properties) {
        ResourceKey<Block> key = CURRENT_BLOCK_KEY.get();
        return key == null ? properties : properties.setId(key);
    }

    /**
     * Sets the registry id of the item that is currently being created by this helper in the properties. Required for
     * items since 1.21.2.
     */
    public static Item.Properties applyItemRegistrationId(Item.Properties properties) {
        ResourceKey<Item> key = CURRENT_ITEM_KEY.get();
        return key == null ? properties : properties.setId(key);
    }

    private <T, R extends T> RegistryEntry<T, R> register(ResourceKey<? extends Registry<T>> key, String name, Supplier<? extends R> object) {
        return Services.REGISTRY.register(modId, key, name, id -> {
            if (Registries.BLOCK.equals(key)) {
                CURRENT_BLOCK_KEY.set(ResourceKey.create(Registries.BLOCK, id));
            } else if (Registries.ITEM.equals(key)) {
                CURRENT_ITEM_KEY.set(ResourceKey.create(Registries.ITEM, id));
            }
            try {
                return object.get();
            } finally {
                CURRENT_BLOCK_KEY.remove();
                CURRENT_ITEM_KEY.remove();
            }
        });
    }

    public <T> RegistryEntry<T, T> registerGeneric(ResourceKey<? extends Registry<T>> key, String name, Supplier<T> object) {
        RegistryEntry<T, T> entry = this.register(key, name, object);
        if (Registries.ITEM.equals(key)) {
            Services.TRANSFER.registerEnergyItem(modId, () -> {
                Item item = (Item) entry.get();
                return item instanceof EnergyItem ? item : null;
            });
        }
        return entry;
    }

    public <T, R extends T> RegistryEntry<T, R> registerTyped(ResourceKey<? extends Registry<T>> key, String name, Supplier<R> object) {
        RegistryEntry<T, R> entry = this.register(key, name, object);
        if (Registries.ITEM.equals(key)) {
            Services.TRANSFER.registerEnergyItem(modId, () -> {
                Item item = (Item) entry.get();
                return item instanceof EnergyItem ? item : null;
            });
        }
        return entry;
    }

    public RegistryEntry<BlockEntityType<?>, BlockEntityType<?>> registerBlockEntityType(String name, Supplier<BlockEntityType<?>> object) {
        return this.register(Registries.BLOCK_ENTITY_TYPE, name, object);
    }

    public RegistryEntry<EntityType<?>, EntityType<?>> registerEntityType(String name, Supplier<EntityType<?>> object) {
        return this.register(Registries.ENTITY_TYPE, name, object);
    }

    public RegistryEntry<Block, Block> registerBlockWithItem(String name, Supplier<? extends BasicBlock> blockSupplier, @Nullable TitaniumTab tab) {
        RegistryEntry<Block, Block> blockRegistryObject = registerGeneric(Registries.BLOCK, name, blockSupplier::get);
        registerGeneric(Registries.ITEM, name, () -> {
            Item item = new BlockItem(blockRegistryObject.get(), applyItemRegistrationId(new Item.Properties().useBlockDescriptionPrefix()));
            if (tab != null) tab.getTabList().add(item);
            return item;
        });
        return blockRegistryObject;
    }

    public RegistryEntry<Block, Block> registerBlockWithItem(String name, Supplier<? extends Block> blockSupplier, Function<RegistryEntry<Block, Block>, Supplier<Item>> itemSupplier, @Nullable TitaniumTab tab) {
        RegistryEntry<Block, Block> block = registerGeneric(Registries.BLOCK, name, blockSupplier::get);
        registerGeneric(Registries.ITEM, name, () -> {
            Item item = itemSupplier.apply(block).get();
            if (tab != null) tab.getTabList().add(item);
            return item;
        });
        return block;
    }

    /**
     * Exposes the item, fluid and energy storages of the Titanium tiles of the given type to other mods
     */
    public void registerCapabilities(Supplier<? extends BlockEntityType<?>> type) {
        Services.TRANSFER.registerBlockEntityStorages(modId, type);
    }

    public BlockWithTile registerBlockWithTile(String name, Supplier<BasicTileBlock<?>> blockSupplier, @Nullable TitaniumTab tab) {
        RegistryEntry<Block, Block> blockRegistryObject = registerBlockWithItem(name, blockSupplier, tab);
        RegistryEntry<BlockEntityType<?>, BlockEntityType<?>> type = registerBlockEntityType(name, () -> new BlockEntityType<>(((BasicTileBlock<?>) blockRegistryObject.get()).getTileEntityFactory(), Set.of(blockRegistryObject.get())));
        registerCapabilities(type);
        return new BlockWithTile(blockRegistryObject, type);
    }

    public BlockWithTile registerBlockWithTileItem(String name, Supplier<BasicTileBlock<?>> blockSupplier, Function<RegistryEntry<Block, Block>, Supplier<Item>> itemSupplier, @Nullable TitaniumTab tab) {
        RegistryEntry<Block, Block> blockRegistryObject = registerBlockWithItem(name, blockSupplier, itemSupplier, tab);
        RegistryEntry<BlockEntityType<?>, BlockEntityType<?>> type = registerBlockEntityType(name, () -> new BlockEntityType<>(((BasicTileBlock<?>) blockRegistryObject.get()).getTileEntityFactory(), Set.of(blockRegistryObject.get())));
        registerCapabilities(type);
        return new BlockWithTile(blockRegistryObject, type);
    }
}
