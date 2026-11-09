/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.component.bundle;

import com.hrznstudio.titanium.api.IFactory;
import com.hrznstudio.titanium.api.transfer.IFluidStorage;
import com.hrznstudio.titanium.platform.Services;
import com.hrznstudio.titanium.api.client.AssetTypes;
import com.hrznstudio.titanium.block.tile.BasicTile;
import com.hrznstudio.titanium.client.screen.addon.AssetScreenAddon;
import com.hrznstudio.titanium.component.IComponentBundle;
import com.hrznstudio.titanium.component.IComponentHandler;
import com.hrznstudio.titanium.component.IComponentHarness;
import com.hrznstudio.titanium.component.inventory.InventoryComponent;
import com.hrznstudio.titanium.component.progress.ProgressBarComponent;
import com.hrznstudio.titanium.container.addon.IContainerAddon;
import com.hrznstudio.titanium.nbthandler.INBTSerializable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class TankInteractionBundle<T extends BasicTile & IComponentHarness> implements IComponentBundle, INBTSerializable<CompoundTag> {

    private final Supplier<Optional<IFluidStorage>> fluidHandler;
    private int posX;
    private int posY;
    private InventoryComponent<T> input;
    private InventoryComponent<T> output;
    private ProgressBarComponent<T> bar;

    public TankInteractionBundle(Supplier<Optional<IFluidStorage>> fluidHandler, int posX, int posY, T componentHarness, int maxProgress) {
        this.fluidHandler = fluidHandler;
        this.posX = posX;
        this.posY = posY;
        this.input = new InventoryComponent<T>("tank_input", posX + 5, posY + 7, 1)
            .setSlotToItemStackRender(0, new ItemStack(Items.BUCKET))
            .setOutputFilter((stack, integer) -> false)
            .setSlotToColorRender(0, DyeColor.BLUE)
            .setInputFilter((stack, integer) -> hasFluidHandler(stack))
            .setComponentHarness(componentHarness);
        this.output = new InventoryComponent<T>("tank_output", posX + 5, posY + 60, 1)
            .setSlotToItemStackRender(0, new ItemStack(Items.BUCKET))
            .setInputFilter((stack, integer) -> false)
            .setSlotToColorRender(0, DyeColor.ORANGE)
            .setComponentHarness(componentHarness);
        this.bar = new ProgressBarComponent<T>(posX + 5, posY + 30, maxProgress)
            .setBarDirection(ProgressBarComponent.BarDirection.ARROW_DOWN)
            .setCanReset(t -> true)
            .setCanIncrease(t -> !this.input.getStackInSlot(0).isEmpty() && hasFluidHandler(this.input.getStackInSlot(0)) && !getOutputStack(false).isEmpty() && (this.output.getStackInSlot(0).isEmpty() || ItemStack.isSameItemSameComponents(getOutputStack(false), this.output.getStackInSlot(0))))
            .setOnFinishWork(() -> {
                ItemStack result = getOutputStack(false);
                if (this.output.insertItem(0, result, true).isEmpty()) {
                    result = getOutputStack(true);
                    this.output.insertItem(0, result, false);
                    ItemStack inputStack = this.input.getStackInSlot(0);
                    this.input.setStackInSlot(0, inputStack.copyWithCount(inputStack.getCount() - 1));
                    componentHarness.setChanged();
                }
            })
            .setComponentHarness(componentHarness);

    }

    @Override
    public void accept(IComponentHandler... handler) {
        for (IComponentHandler iComponentHandler : handler) {
            iComponentHandler.add(this.input, this.output, this.bar);
        }
    }

    private static boolean hasFluidHandler(ItemStack stack) {
        return !stack.isEmpty() && Services.TRANSFER.isFluidContainer(stack);
    }

    public ItemStack getOutputStack(boolean execute) {
        if (fluidHandler.get().isEmpty()) return ItemStack.EMPTY;
        var tank = fluidHandler.get().get();
        ItemStack stack = this.input.getStackInSlot(0).copyWithCount(1);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        return Services.TRANSFER.exchangeFluidWithItem(stack, tank, execute);
    }

    @Override
    public List<IFactory<?>> getScreenAddons() {
        return Collections.singletonList(() -> new AssetScreenAddon(AssetTypes.AUGMENT_BACKGROUND, posX, posY, true));
    }

    @Override
    public List<IFactory<? extends IContainerAddon>> getContainerAddons() {
        return Collections.emptyList();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag compoundNBT = new CompoundTag();
        compoundNBT.put("Input", this.input.serializeNBT(provider));
        compoundNBT.put("Output", this.output.serializeNBT(provider));
        compoundNBT.put("Bar", this.bar.serializeNBT(provider));
        return compoundNBT;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        this.input.deserializeNBT(provider, nbt.getCompoundOrEmpty("Input"));
        this.output.deserializeNBT(provider, nbt.getCompoundOrEmpty("Output"));
        this.bar.deserializeNBT(provider, nbt.getCompoundOrEmpty("Bar"));
    }
}
