/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.fluid;

import com.hrznstudio.titanium.module.DeferredRegistryHelper;
import com.hrznstudio.titanium.platform.Services;
import com.hrznstudio.titanium.registry.RegistryEntry;
import com.hrznstudio.titanium.tab.TitaniumTab;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registers a source and flowing fluid, a bucket and a liquid block, plus the platform specific fluid attributes.
 */
public class TitaniumFluidInstance {

    private static final List<TitaniumFluidInstance> INSTANCES = new CopyOnWriteArrayList<>();

    private final RegistryEntry<Fluid, Fluid> flowingFluid;
    private final RegistryEntry<Fluid, Fluid> sourceFluid;
    private final RegistryEntry<Item, Item> bucketFluid;
    private final RegistryEntry<Block, Block> blockFluid;
    private final String fluid;
    private final String modId;
    private final TitaniumFluidProperties properties;
    private final TitaniumFluidRenderProperties renderProperties;

    public TitaniumFluidInstance(DeferredRegistryHelper helper, String fluid, TitaniumFluidProperties properties, TitaniumFluidRenderProperties renderProperties, @Nullable TitaniumTab group) {
        this.fluid = fluid;
        this.modId = helper.getModId();
        this.properties = properties;
        this.renderProperties = renderProperties;
        INSTANCES.add(this);
        this.sourceFluid = helper.registerGeneric(Registries.FLUID, fluid, () -> Services.FLUID.createSourceFluid(this));
        this.flowingFluid = helper.registerGeneric(Registries.FLUID, fluid + "_flowing", () -> Services.FLUID.createFlowingFluid(this));
        this.bucketFluid = helper.registerGeneric(Registries.ITEM, fluid + "_bucket", () -> {
            var item = new BucketItem(this.sourceFluid.get(), DeferredRegistryHelper.applyItemRegistrationId(new Item.Properties()).craftRemainder(Items.BUCKET).stacksTo(1));
            if (group != null) group.getTabList().add(item);
            return item;
        });
        this.blockFluid = helper.registerGeneric(Registries.BLOCK, fluid, () -> new LiquidBlock((FlowingFluid) sourceFluid.get(), DeferredRegistryHelper.applyBlockRegistrationId(Block.Properties.of()).mapColor(MapColor.WATER).replaceable().noCollision().strength(100.0F).pushReaction(PushReaction.POPPED).noLootTable().liquid().sound(SoundType.EMPTY)) {
        });
        Services.FLUID.registerFluidAttributes(helper, this);
    }

    public RegistryEntry<Fluid, Fluid> getFlowingFluid() {
        return flowingFluid;
    }

    public RegistryEntry<Fluid, Fluid> getSourceFluid() {
        return sourceFluid;
    }

    public RegistryEntry<Item, Item> getBucketFluid() {
        return bucketFluid;
    }

    public RegistryEntry<Block, Block> getBlockFluid() {
        return blockFluid;
    }

    public String getFluid() {
        return fluid;
    }

    public String getModId() {
        return modId;
    }

    public TitaniumFluidProperties getProperties() {
        return properties;
    }

    public TitaniumFluidRenderProperties getRenderProperties() {
        return renderProperties;
    }

    public static List<TitaniumFluidInstance> getInstances() {
        return List.copyOf(INSTANCES);
    }
}
