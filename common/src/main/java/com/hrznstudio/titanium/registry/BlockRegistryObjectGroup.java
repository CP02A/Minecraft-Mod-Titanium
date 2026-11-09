/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.hrznstudio.titanium.registry.RegistryEntry;
import com.hrznstudio.titanium.module.DeferredRegistryHelper;
import net.minecraft.core.registries.Registries;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;


public class BlockRegistryObjectGroup<B extends Block, I extends Item, T extends BlockEntity> implements Supplier<B> {
    private final String name;
    private final Supplier<B> blockCreator;
    private final Function<B, I> itemCreator;
    private final BlockEntityType.BlockEntitySupplier<T> tileSupplier;

    private RegistryEntry<Block, B> block;
    private RegistryEntry<Item, I> item;
    private RegistryEntry<BlockEntityType<?>, BlockEntityType<T>> tileEntity;

    public BlockRegistryObjectGroup(String name, Supplier<B> blockCreator, Function<B, I> itemCreator) {
        this(name, blockCreator, itemCreator, null);
    }

    public BlockRegistryObjectGroup(String name, Supplier<B> blockCreator, Function<B, I> itemCreator, Supplier<T> tileSupplier) {
        this.name = name;
        this.blockCreator = blockCreator;
        this.itemCreator = itemCreator;
        this.tileSupplier = (p_155268_, p_155269_) -> tileSupplier.get();
    }

    @Nonnull
    public B getBlock() {
        return Objects.requireNonNull(block).get();
    }

    @Nonnull
    public I getItem() {
        return Objects.requireNonNull(item).get();
    }

    @Nonnull
    public BlockEntityType<T> getTileEntityType() {
        return Objects.requireNonNull(tileEntity).get();
    }

    public BlockRegistryObjectGroup<B, I, ?> register(DeferredRegistryHelper helper) {
        block = helper.registerTyped(Registries.BLOCK, name, blockCreator);
        item = helper.registerTyped(Registries.ITEM, name, () -> itemCreator.apply(this.getBlock()));
        return this;
    }

    @SuppressWarnings("unchecked")
    public BlockRegistryObjectGroup<B, I, T> registerWithTile(DeferredRegistryHelper helper) {
        this.register(helper);
        if (tileSupplier != null) {
            tileEntity = helper.registerTyped(Registries.BLOCK_ENTITY_TYPE, name, () -> new BlockEntityType<>(tileSupplier, Set.of(this.getBlock())));
            helper.registerCapabilities(tileEntity);
        }
        return this;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public B get() {
        return this.getBlock();
    }
}
