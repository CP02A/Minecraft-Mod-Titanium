/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.fabric.clientgametest;

import com.hrznstudio.titanium._impl.test.MachineTestBlock;
import com.hrznstudio.titanium._impl.test.TestBlock;
import com.hrznstudio.titanium._impl.test.tile.TestTile;
import com.hrznstudio.titanium.api.client.AssetTypes;
import com.hrznstudio.titanium.api.client.IScreenAddon;
import com.hrznstudio.titanium.block.tile.ActiveTile;
import com.hrznstudio.titanium.client.screen.addon.BasicScreenAddon;
import com.hrznstudio.titanium.client.screen.addon.StateButtonAddon;
import com.hrznstudio.titanium.client.screen.addon.TankScreenAddon;
import com.hrznstudio.titanium.client.screen.asset.IAssetProvider;
import com.hrznstudio.titanium.client.screen.container.BasicAddonScreen;
import com.hrznstudio.titanium.api.transfer.IFluidStorage;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.function.Predicate;

/**
 * Opens Titanium GUIs in the real client and clicks them like a player would. Covers regressions that only show up
 * with real input: clicks swallowed by overlapping addons, SDL3 mouse button ids, and slots or buttons placed outside
 * the GUI background. Screenshots of each GUI are saved to {@code runs/clientGametest/screenshots} for a visual check.
 */
public class TitaniumGuiClientGameTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            // A small platform in the air, so the terrain of the test world doesn't matter
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("fill -3 199 -3 3 199 3 minecraft:stone");
            world.getServer().runCommand("fill -3 200 -3 3 203 3 minecraft:air");
            world.getServer().runCommand("tp @a 0.5 200 0.5 0 0");
            context.waitTicks(20);
            BlockPos pos = new BlockPos(0, 200, 2);

            testBlockGui(context, world, pos);
            machineTestGui(context, world, pos);
        }
    }

    /**
     * Left-clicking the tank with a water bucket fills it, and the button left of the background reacts. Before the
     * fixes the background addon swallowed the click and left clicks arrived as right clicks.
     */
    private static void testBlockGui(ClientGameTestContext context, TestSingleplayerContext world, BlockPos pos) {
        give(world, Items.WATER_BUCKET);
        openGui(context, world, pos, TestBlock.TEST.getBlock());
        context.takeScreenshot("titanium_test_block_gui");

        pickUpFromPlayerInventory(context, Items.WATER_BUCKET);
        clickGui(context, onScreen(context, screen -> addonCenter(screen, TankScreenAddon.class, addon -> true)), InputConstants.MOUSE_BUTTON_LEFT);
        world.getServer().waitFor(server -> tankAmount(server, pos) == 1000);
        // The emptied bucket stays on the cursor
        context.waitTicks(5);
        assertNothingDropped(world, pos, "when filling the tank from the cursor");
        context.waitFor(client -> withScreen(client, screen -> screen.getMenu().getCarried().is(Items.BUCKET)));

        clickGui(context, onScreen(context, screen -> addonCenter(screen, StateButtonAddon.class, addon -> addon.getPosX() < 0)), InputConstants.MOUSE_BUTTON_LEFT);
        world.getServer().waitFor(server -> testTileState(server, pos) == 1);
        context.waitTicks(5);
        assertNothingDropped(world, pos, "when clicking the button outside of the background with an item on the cursor");

        closeGui(context);
    }

    /**
     * Items can be put into and taken out of the tank bundle slots, which are outside of the GUI background. Before
     * the fix vanilla treated those clicks as clicks outside of the screen and threw the item out.
     */
    private static void machineTestGui(ClientGameTestContext context, TestSingleplayerContext world, BlockPos pos) {
        give(world, Items.BUCKET);
        openGui(context, world, pos, MachineTestBlock.TEST.getBlock());
        context.takeScreenshot("titanium_machine_test_gui");

        // The tank bundle input: outside of the background and accepting buckets (unlike the augment slots next to it)
        int slotIndex = onScreen(context, screen -> {
            int backgroundWidth = IAssetProvider.getAsset(screen.getAssetProvider(), AssetTypes.BACKGROUND).getArea().width;
            return screen.getMenu().slots.stream()
                .filter(slot -> !(slot.container instanceof Inventory) && slot.x >= backgroundWidth && slot.mayPlace(new ItemStack(Items.BUCKET)))
                .min(Comparator.comparingInt(slot -> slot.y))
                .orElseThrow(() -> new AssertionError("No slot accepting buckets outside of the GUI background"))
                .index;
        });

        assertNothingDropped(world, pos, "before using the slot outside of the background");
        pickUpFromPlayerInventory(context, Items.BUCKET);
        clickGui(context, onScreen(context, screen -> slotCenter(screen, screen.getMenu().getSlot(slotIndex))), InputConstants.MOUSE_BUTTON_LEFT);
        context.waitFor(client -> withScreen(client, screen -> screen.getMenu().getSlot(slotIndex).getItem().is(Items.BUCKET) && screen.getMenu().getCarried().isEmpty()));
        context.waitTicks(5);
        assertNothingDropped(world, pos, "while using the slot outside of the background");

        clickGui(context, onScreen(context, screen -> slotCenter(screen, screen.getMenu().getSlot(slotIndex))), InputConstants.MOUSE_BUTTON_LEFT);
        context.waitFor(client -> withScreen(client, screen -> screen.getMenu().getCarried().is(Items.BUCKET)));

        closeGui(context);
    }

    private static void assertNothingDropped(TestSingleplayerContext world, BlockPos pos, String when) {
        world.getServer().runOnServer(server -> {
            var dropped = server.overworld().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(8));
            if (!dropped.isEmpty()) {
                throw new AssertionError("Items were thrown out " + when + ": " + dropped.stream().map(entity -> entity.getItem().toString()).toList());
            }
        });
    }

    /**
     * Closes the GUI with Escape like a player, so the server closes the menu too and returns the carried item
     */
    private static void closeGui(ClientGameTestContext context) {
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitFor(client -> client.gui.screen() == null);
        context.waitTicks(5);
    }

    private static void openGui(ClientGameTestContext context, TestSingleplayerContext world, BlockPos pos, Block block) {
        world.getServer().runOnServer(server -> server.overworld().setBlockAndUpdate(pos, block.defaultBlockState()));
        context.waitTicks(5);
        context.getInput().lookAt(pos);
        context.waitTick();
        context.takeScreenshot("titanium_before_opening_" + BuiltInRegistries.BLOCK.getKey(block).getPath());
        context.getInput().pressKey(options -> options.keyUse);
        context.waitForScreen(BasicAddonScreen.class);
        context.waitTicks(5);
    }

    /**
     * Puts the item in the main inventory, not the hotbar: right-clicking the block while holding a fluid container
     * would interact with its tank instead of opening the GUI.
     */
    private static void give(TestSingleplayerContext world, Item item) {
        world.getServer().runOnServer(server -> player(server).getInventory().setItem(9, new ItemStack(item)));
    }

    private static void pickUpFromPlayerInventory(ClientGameTestContext context, Item item) {
        clickGui(context, onScreen(context, screen -> slotCenter(screen, screen.getMenu().slots.stream()
            .filter(slot -> slot.container instanceof Inventory && slot.getItem().is(item))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No " + item + " in the player inventory")))), InputConstants.MOUSE_BUTTON_LEFT);
        context.waitFor(client -> withScreen(client, screen -> screen.getMenu().getCarried().is(item)));
    }

    /**
     * Moves the cursor to the GUI position and clicks, converting GUI coordinates to window coordinates.
     */
    private static void clickGui(ClientGameTestContext context, int[] guiPos, int button) {
        double scale = context.computeOnClient(client -> (double) client.getWindow().getScreenWidth() / client.getWindow().getGuiScaledWidth());
        context.getInput().setCursorPos(guiPos[0] * scale, guiPos[1] * scale);
        context.getInput().pressMouse(button);
        context.waitTick();
    }

    private static int[] slotCenter(BasicAddonScreen screen, Slot slot) {
        return new int[]{screen.leftPos + slot.x + 8, screen.topPos + slot.y + 8};
    }

    private static <T extends BasicScreenAddon> int[] addonCenter(BasicAddonScreen screen, Class<T> type, Predicate<T> filter) {
        for (IScreenAddon addon : screen.getAddons()) {
            if (type.isInstance(addon) && filter.test(type.cast(addon))) {
                BasicScreenAddon basic = type.cast(addon);
                return new int[]{screen.leftPos + basic.getPosX() + basic.getXSize() / 2, screen.topPos + basic.getPosY() + basic.getYSize() / 2};
            }
        }
        throw new AssertionError("No " + type.getSimpleName() + " in the GUI");
    }

    private static <T> T onScreen(ClientGameTestContext context, java.util.function.Function<BasicAddonScreen, T> function) {
        return context.computeOnClient(client -> {
            if (!(client.gui.screen() instanceof BasicAddonScreen screen)) {
                throw new AssertionError("Expected a Titanium GUI to be open, found " + client.gui.screen());
            }
            return function.apply(screen);
        });
    }

    private static boolean withScreen(Minecraft client, Predicate<BasicAddonScreen> predicate) {
        return client.gui.screen() instanceof BasicAddonScreen screen && predicate.test(screen);
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static int tankAmount(MinecraftServer server, BlockPos pos) {
        BlockEntity blockEntity = server.overworld().getBlockEntity(pos);
        if (!(blockEntity instanceof ActiveTile<?> tile)) return -1;
        IFluidStorage storage = tile.getFluidHandler(null);
        return storage == null || storage.getTanks() == 0 ? -1 : storage.getFluidInTank(0).getAmount();
    }

    private static int testTileState(MinecraftServer server, BlockPos pos) {
        if (!(server.overworld().getBlockEntity(pos) instanceof TestTile tile)) return -1;
        try {
            Field state = TestTile.class.getDeclaredField("state");
            state.setAccessible(true);
            return state.getInt(tile);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Can't read the state of the test tile", e);
        }
    }
}
