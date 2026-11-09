/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium;

import com.hrznstudio.titanium._impl.creative.CreativeFEGeneratorBlock;
import com.hrznstudio.titanium._impl.test.AssetTestBlock;
import com.hrznstudio.titanium._impl.test.MachineTestBlock;
import com.hrznstudio.titanium._impl.test.TestBlock;
import com.hrznstudio.titanium._impl.test.TwentyFourTestBlock;
import com.hrznstudio.titanium._impl.test.recipe.TestSerializableRecipe;
import com.hrznstudio.titanium.attachment.StoredEnergyAttachment;
import com.hrznstudio.titanium.block_network.NetworkManager;
import com.hrznstudio.titanium.command.RewardCommand;
import com.hrznstudio.titanium.command.RewardGrantCommand;
import com.hrznstudio.titanium.container.BasicAddonContainer;
import com.hrznstudio.titanium.item.AugmentWrapper;
import com.hrznstudio.titanium.module.ModuleController;
import com.hrznstudio.titanium.network.NetworkHandler;
import com.hrznstudio.titanium.network.locator.LocatorTypes;
import com.hrznstudio.titanium.network.messages.ButtonClickNetworkMessage;
import com.hrznstudio.titanium.network.messages.TileFieldNetworkMessage;
import com.hrznstudio.titanium.platform.Services;
import com.hrznstudio.titanium.recipe.serializer.GenericSerializer;
import com.hrznstudio.titanium.reward.Reward;
import com.hrznstudio.titanium.reward.RewardManager;
import com.hrznstudio.titanium.reward.RewardSyncMessage;
import com.hrznstudio.titanium.reward.storage.RewardWorldStorage;
import com.hrznstudio.titanium.util.PhysicalSide;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.Codec;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Loader independent core of Titanium, constructed by the entrypoint of each loader.
 */
public class Titanium extends ModuleController {

    public static final String MODID = "titanium";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static NetworkHandler NETWORK = new NetworkHandler(MODID);

    public Titanium() {
        super(MODID);
        NETWORK.registerMessage("button_click", ButtonClickNetworkMessage.class);
        NETWORK.registerMessage("reward_sync", RewardSyncMessage.class);
        NETWORK.registerMessage("tile_field", TileFieldNetworkMessage.class);
        Services.PLATFORM.onCommonSetup(MODID, this::commonSetup);
    }

    @Override
    protected void initModules() {
        BasicAddonContainer.TYPE = getRegistries().registerGeneric(Registries.MENU, "addon_container", () -> (MenuType) Services.REGISTRY.createMenuType(BasicAddonContainer::create));
        StoredEnergyAttachment.TYPE = getRegistries()
            .registerTyped(Registries.DATA_COMPONENT_TYPE, "stored_energy", () -> DataComponentType.<StoredEnergyAttachment>builder()
                .persistent(StoredEnergyAttachment.CODEC)
                .build());
        AugmentWrapper.ATTACHMENT = getRegistries()
            .registerTyped(Registries.DATA_COMPONENT_TYPE, "augments", () -> DataComponentType.<Map<String, Float>>builder()
                .persistent(Codec.unboundedMap(Codec.STRING, Codec.FLOAT))
                .build());
        if (Services.PLATFORM.isDevelopmentEnvironment()) { //ENABLE IN DEV
            // Registered before the serializer, objects are registered right away on Fabric
            TestSerializableRecipe.RECIPE_TYPE = getRegistries().registerGeneric(Registries.RECIPE_TYPE, "test_recipe_type", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return MODID + ":test_recipe_type";
                }
            });
            TestSerializableRecipe.SERIALIZER = getRegistries().registerGeneric(Registries.RECIPE_SERIALIZER, "test_serializer", () -> GenericSerializer.create(TestSerializableRecipe.class, () -> TestSerializableRecipe.RECIPE_TYPE.value(), TestSerializableRecipe.CODEC));
            TestBlock.TEST = getRegistries().registerBlockWithTile("block_test", () -> (TestBlock) new TestBlock(), null);
            TwentyFourTestBlock.TEST = getRegistries().registerBlockWithTile("block_twenty_four_test", () -> (TwentyFourTestBlock) new TwentyFourTestBlock(), null);
            AssetTestBlock.TEST = getRegistries().registerBlockWithTile("block_asset_test", () -> (AssetTestBlock) new AssetTestBlock(), null);
            MachineTestBlock.TEST = getRegistries().registerBlockWithTile("machine_test", () -> (MachineTestBlock) new MachineTestBlock(), null);
            CreativeFEGeneratorBlock.INSTANCE = getRegistries().registerBlockWithTile("creative_generator", () -> new CreativeFEGeneratorBlock(), null);
        }
    }

    private void commonSetup() {
        RewardManager.get().getRewards().values().forEach(rewardGiver -> rewardGiver.getRewards().forEach(reward -> reward.register(PhysicalSide.DEDICATED_SERVER)));
        LocatorTypes.register();
    }

    /**
     * Called by the loader at the end of every server level tick
     */
    public static void onLevelTickPost(Level level) {
        if (!level.isClientSide()) {
            NetworkManager.get((net.minecraft.server.level.ServerLevel) level).getNetworks().forEach(network -> network.update(level));
        }
    }

    /**
     * Called by the loader when a player joins the server
     */
    public static void onPlayerLoggedIn(ServerPlayer player) {
        player.level().getServer().execute(() -> {
            RewardWorldStorage storage = RewardWorldStorage.get(player.level().getServer().getLevel(Level.OVERWORLD));
            if (!storage.getConfiguredPlayers().contains(player.getUUID())) {
                for (Identifier collectRewardsIdentifier : RewardManager.get().collectRewardsIdentifiers(player.getUUID())) {
                    Reward reward = RewardManager.get().getReward(collectRewardsIdentifier);
                    storage.add(player.getUUID(), reward.getIdentifier(), reward.getOptions()[0]);
                }
                storage.getConfiguredPlayers().add(player.getUUID());
                storage.setDirty();
            }
            CompoundTag nbt = storage.serializeSimple(player.level().registryAccess());
            player.level().getServer().getPlayerList().getPlayers().forEach(serverPlayerEntity -> Titanium.NETWORK.sendTo(new RewardSyncMessage(nbt), serverPlayerEntity));
        });
    }

    /**
     * Called by the loader when the server commands are registered
     */
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        RewardCommand.register(dispatcher);
        RewardGrantCommand.register(dispatcher);
    }
}
