/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.component.inventory;

import com.google.common.collect.Lists;
import com.hrznstudio.titanium.api.IFactory;
import com.hrznstudio.titanium.api.transfer.IItemSlot;
import com.hrznstudio.titanium.api.transfer.IItemStorage;
import com.hrznstudio.titanium.api.client.IScreenAddonProvider;
import com.hrznstudio.titanium.client.screen.addon.SlotsScreenAddon;
import com.hrznstudio.titanium.component.IComponentHarness;
import com.hrznstudio.titanium.container.addon.IContainerAddon;
import com.hrznstudio.titanium.container.addon.IContainerAddonProvider;
import com.hrznstudio.titanium.container.addon.SlotContainerAddon;
import com.hrznstudio.titanium.util.IValueIOSerializable;
import com.hrznstudio.titanium.util.ValueIOSerialization;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public class InventoryComponent<T extends IComponentHarness> implements IItemStorage, IValueIOSerializable, IScreenAddonProvider,
        IContainerAddonProvider {

    /**
     * Same key as NeoForge's StacksResourceHandler so saved data stays compatible
     */
    public static final String VALUE_IO_KEY = "stacks";

    private final String name;
    private NonNullList<ItemStack> stacks;
    private IItemSlot[] slots;
    private int xPos;
    private int yPos;
    private int xSize;
    private int ySize;
    private T componentHarness;
    private BiPredicate<ItemStack, Integer> insertPredicate;
    private BiPredicate<ItemStack, Integer> extractPredicate;
    private BiPredicate<ItemStack, Integer> insertGUIPredicate;
    private BiPredicate<ItemStack, Integer> extractGUIPredicate;
    private BiConsumer<ItemStack, Integer> onSlotChanged;
    private Map<Integer, Integer> slotAmountFilter;
    private Map<Integer, ItemStack> slotToStackRenderMap;
    private boolean colorGuiEnabled;
    private Map<Integer, Color> slotToColorRenderMap;
    private int slotLimit;
    private Function<Integer, Pair<Integer, Integer>> slotPosition;
    private Predicate<Integer> slotVisiblePredicate;

    public InventoryComponent(String name, int xPos, int yPos, int size) {
        this.name = name;
        setSize(size);
        this.xPos = xPos;
        this.yPos = yPos;
        this.setRange(size, 1);
        this.insertPredicate = (stack, integer) -> true;
        this.extractPredicate = (stack, integer) -> true;
        this.insertGUIPredicate = (stack, integer) -> true;
        this.extractGUIPredicate = (stack, integer) -> true;
        this.onSlotChanged = (stack, integer) -> {
        };
        this.slotAmountFilter = new HashMap<>();
        this.slotToStackRenderMap = new HashMap<>();
        this.colorGuiEnabled = false;
        this.slotToColorRenderMap = new HashMap<>();
        this.slotLimit = 64;
        this.slotPosition = integer -> Pair.of(18 * (integer % xSize), 18 * (integer / xSize));
        this.slotVisiblePredicate = integer -> true;
    }

    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return ValueIOSerialization.save(provider, this);
    }

    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        ValueIOSerialization.load(provider, tag, this);
    }

    /**
     * Defines how many slots/row and slots/column
     *
     * @param x How many slots there are horizontally
     * @param y How many slots there are vertically
     * @return itself
     */
    public InventoryComponent<T> setRange(int x, int y) {
        this.xSize = x;
        this.ySize = y;
        return this;
    }

    /**
     * Sets the tile where the inventory is to allow markForUpdate automatically
     *
     * @param componentHarness the object which owns this component
     * @return itself
     */
    public InventoryComponent<T> setComponentHarness(T componentHarness) {
        this.componentHarness = componentHarness;
        return this;
    }

    /**
     * Sets the predicate input filter to filter what items go into which slot.
     *
     * @param predicate A bi predicate where the itemstack is the item trying to be inserted and the slot where is trying to be inserted to
     * @return itself
     */
    public InventoryComponent<T> setInputFilter(BiPredicate<ItemStack, Integer> predicate) {
        this.insertPredicate = predicate;
        return this;
    }

    /**
     * Sets the predicate output filter to filter what can be extracted from which slot.
     *
     * @param predicate A bi predicate where the itemstack is the item trying to be extracted and the slot where is trying to be extracted
     * @return itself
     */
    public InventoryComponent<T> setOutputFilter(BiPredicate<ItemStack, Integer> predicate) {
        this.extractPredicate = predicate;
        return this;
    }

    /**
     * Changes the amount of slots, keeping the contents of the slots that remain
     */
    public void setSize(int size) {
        NonNullList<ItemStack> newStacks = NonNullList.withSize(size, ItemStack.EMPTY);
        if (this.stacks != null) {
            for (int i = 0; i < Math.min(size, this.stacks.size()); i++) {
                newStacks.set(i, this.stacks.get(i));
            }
        }
        this.stacks = newStacks;
        this.slots = new IItemSlot[size];
        for (int i = 0; i < size; i++) {
            this.slots[i] = new Slot(i);
        }
    }

    @Override
    public int getSlots() {
        return stacks.size();
    }

    /**
     * Same as {@link #getSlots()}
     */
    public int size() {
        return getSlots();
    }

    @Override
    public IItemSlot getSlot(int slot) {
        return slots[slot];
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return stacks.get(slot);
    }

    /**
     * Sets the stack in the slot, ignoring filters, and notifies the change
     */
    public void setStackInSlot(int slot, ItemStack stack) {
        ItemStack previous = stacks.get(slot);
        stacks.set(slot, stack);
        if (!ItemStack.matches(previous, stack)) {
            onContentsChanged(slot, previous);
        }
    }

    protected void onContentsChanged(int slot, ItemStack previousContents) {
        if (this.componentHarness != null) {
            componentHarness.markComponentDirty();
        }
        onSlotChanged.accept(getStackInSlot(slot), slot);
    }

    @Override
    public void serialize(ValueOutput output) {
        output.store(VALUE_IO_KEY, ItemStack.OPTIONAL_CODEC.listOf(), stacks);
    }

    @Override
    public void deserialize(ValueInput input) {
        input.read(VALUE_IO_KEY, ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(list -> {
            setSize(list.size());
            for (int i = 0; i < list.size(); i++) {
                stacks.set(i, list.get(i));
            }
        });
    }

    public String getName() {
        return name;
    }

    public int getXPos() {
        return xPos;
    }

    public void setXPos(int xPos) {
        this.xPos = xPos;
    }

    public int getYPos() {
        return yPos;
    }

    public void setYPos(int yPos) {
        this.yPos = yPos;
    }

    public int getXSize() {
        return xSize;
    }

    public int getYSize() {
        return ySize;
    }

    public T getComponentHarness() {
        return componentHarness;
    }

    public BiPredicate<ItemStack, Integer> getInsertPredicate() {
        return insertPredicate;
    }

    public BiPredicate<ItemStack, Integer> getExtractPredicate() {
        return extractPredicate;
    }

    public BiConsumer<ItemStack, Integer> getOnSlotChanged() {
        return onSlotChanged;
    }

    public InventoryComponent<T> setColorGuiEnabled(boolean colorGuiEnabled) {
        this.colorGuiEnabled = colorGuiEnabled;
        return this;
    }

    public boolean isColorGuiEnabled() {
        return colorGuiEnabled;
    }

    public Map<Integer, Color> getSlotToColorRenderMap() {
        return slotToColorRenderMap;
    }

    /**
     * Sets the predicate slot changed that gets triggered when a slot is changed.
     *
     * @param onSlotChanged A bi predicate where the itemstack and slot changed
     * @return itself
     */
    public InventoryComponent<T> setOnSlotChanged(BiConsumer<ItemStack, Integer> onSlotChanged) {
        this.onSlotChanged = onSlotChanged;
        return this;
    }

    /**
     * Sets the limit amount for a specific slot, this limit has priority instead of the slot limit for all the slots
     *
     * @param slot  The slot to set the limit to
     * @param limit The limit for the slot
     * @return itself
     */
    public InventoryComponent<T> setSlotLimit(int slot, int limit) {
        this.slotAmountFilter.put(slot, limit);
        return this;
    }

    /**
     * @param slot The slot to render the stack in
     * @param stack The Itemstack to render in the slot
     * @return itself
     */
    public InventoryComponent<T> setSlotToItemStackRender(int slot, ItemStack stack) {
        this.slotToStackRenderMap.put(slot, stack);
        return this;
    }

    /**
     * @param slot Slot which color should be set.
     * @param color The color of the slot
     * @return Returns the inventorycomponent
     */
    public InventoryComponent<T> setSlotToColorRender(int slot, int color) {
        if (!this.colorGuiEnabled) {
            this.setColorGuiEnabled(true);
        }
        this.slotToColorRenderMap.put(slot, new Color(color));
        return this;
    }

    /**
     * @param slot Slot which color should be set.
     * @param color The color of the slot
     * @return Returns the inventorycomponent
     */
    public InventoryComponent<T> setSlotToColorRender(int slot, DyeColor color) {
        if (!this.colorGuiEnabled) {
            this.setColorGuiEnabled(true);
        }
        this.slotToColorRenderMap.put(slot, new Color(color.getMapColor().col));
        return this;
    }

    /**
     * @param slot Slot which color should be set.
     * @param color The color of the slot
     * @return Returns the inventorycomponent
     */
    public InventoryComponent<T> setSlotToColorRender(int slot, Color color) {
        if (!this.colorGuiEnabled) {
            this.setColorGuiEnabled(true);
        }
        this.slotToColorRenderMap.put(slot, color);
        return this;
    }

    /**
     * @param slot Slot to get the Render Stack for
     * @return Returns the Itemstack to be rendered
     */
    public ItemStack getItemStackForSlotRendering(int slot) {
        return this.slotToStackRenderMap.getOrDefault(slot, ItemStack.EMPTY);
    }

    /**
     * @param slot Slot to get the Render Color for
     * @return Returns the Color to be rendered
     */
    public Color getColorForSlotRendering(int slot) {
        return this.slotToColorRenderMap.get(slot);
    }

    /**
     * Sets the default limit for all the slots
     *
     * @param limit The default limit for all the slot that don't have specific limit
     * @return itself
     */
    public InventoryComponent<T> setSlotLimit(int limit) {
        this.slotLimit = limit;
        return this;
    }

    /**
     * Gets the predicate to check if a slot is enabled
     *
     * @return predicate
     */
    public Predicate<Integer> getSlotVisiblePredicate() {
        return slotVisiblePredicate;
    }

    /**
     * Sets the slot enabled predicate that allows to disable/enable slots
     *
     * @param slotVisiblePredicate a int predicate that checks slot id
     * @return itself
     */
    public InventoryComponent<T> setSlotVisiblePredicate(Predicate<Integer> slotVisiblePredicate) {
        this.slotVisiblePredicate = slotVisiblePredicate;
        return this;
    }

    @Override
    public int getSlotLimit(int slot) {
        return slotAmountFilter.getOrDefault(slot, this.slotLimit);
    }

    /**
     * @return the max amount of the given stack the slot can hold
     */
    public int getCapacity(int slot, ItemStack stack) {
        int itemLimit = stack.isEmpty() ? slotLimit : stack.getMaxStackSize();
        return Math.min(getSlotLimit(slot), itemLimit);
    }

    public Function<Integer, Pair<Integer, Integer>> getSlotPosition() {
        return slotPosition;
    }

    public InventoryComponent<T> setSlotPosition(Function<Integer, Pair<Integer, Integer>> slotPosition) {
        this.slotPosition = slotPosition;
        return this;
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return !stack.isEmpty() && insertPredicate.test(stack, slot);
    }

    public BiPredicate<ItemStack, Integer> getInsertGUIPredicate() {
        return insertGUIPredicate;
    }

    public InventoryComponent<T> setInsertGUIPredicate(BiPredicate<ItemStack, Integer> insertGUIPredicate) {
        this.insertGUIPredicate = insertGUIPredicate;
        return this;
    }

    public BiPredicate<ItemStack, Integer> getExtractGUIPredicate() {
        return extractGUIPredicate;
    }

    public InventoryComponent<T> setExtractGUIPredicate(BiPredicate<ItemStack, Integer> extractGUIPredicate) {
        this.extractGUIPredicate = extractGUIPredicate;
        return this;
    }

    @Override
    public List<IFactory<?>> getScreenAddons() {
        List<IFactory<?>> addons = new ArrayList<>();
        addons.add(() -> new SlotsScreenAddon<>(this));
        return addons;
    }

    @Override
    public List<IFactory<? extends IContainerAddon>> getContainerAddons() {
        return Lists.newArrayList(
                () -> new SlotContainerAddon(this, xPos, yPos, slotPosition)
        );
    }

    /**
     * Slot view used by the simulate API and the loader transfer adapters
     */
    private class Slot implements IItemSlot {
        private final int index;

        private Slot(int index) {
            this.index = index;
        }

        @Override
        public ItemStack getStack() {
            return stacks.get(index);
        }

        @Override
        public void setStack(ItemStack stack) {
            stacks.set(index, stack);
        }

        @Override
        public int getCapacity(ItemStack stack) {
            return InventoryComponent.this.getCapacity(index, stack);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return isItemValid(index, stack);
        }

        @Override
        public boolean canExtract(ItemStack stack) {
            return extractPredicate.test(stack, index);
        }

        @Override
        public void onContentsChanged() {
            InventoryComponent.this.onContentsChanged(index, ItemStack.EMPTY);
        }
    }
}
