/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.fabric;

import com.hrznstudio.titanium.TitaniumClient;
import com.hrznstudio.titanium.client.screen.container.BasicAddonScreen;
import com.hrznstudio.titanium.container.BasicAddonContainer;
import com.hrznstudio.titanium.fabric.platform.FabricPlatformHelper;
import com.hrznstudio.titanium.fluid.TitaniumFluidInstance;
import com.hrznstudio.titanium.fluid.TitaniumFluidRenderProperties;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TitaniumFabricClient implements ClientModInitializer {

    @Override
    @SuppressWarnings("unchecked")
    public void onInitializeClient() {
        TitaniumClient.init();
        MenuScreens.register((MenuType<BasicAddonContainer>) (MenuType<?>) BasicAddonContainer.TYPE.get(), BasicAddonScreen::new);
        for (TitaniumFluidInstance instance : TitaniumFluidInstance.getInstances()) {
            registerFluidModel(instance);
        }
        LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, outlineState) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null && minecraft.hitResult instanceof BlockHitResult hitResult) {
                BlockState state = minecraft.level.getBlockState(outlineState.pos());
                VoxelShape shape = TitaniumClient.getCustomOutlineShape(minecraft.level, state, hitResult);
                if (shape != null) {
                    TitaniumClient.submitOutline(context.submitNodeCollector(), context.poseStack(), context.levelState().cameraRenderState.pos,
                        outlineState.pos(), shape, outlineState.highContrast(), outlineState.isTranslucent());
                    return false;
                }
            }
            return true;
        });
        TitaniumClient.clientSetup();
        FabricPlatformHelper.runClientSetup();
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> FabricPlatformHelper.runCommonSetup());
    }

    private static void registerFluidModel(TitaniumFluidInstance instance) {
        TitaniumFluidRenderProperties properties = instance.getRenderProperties();
        BlockTintSource tintSource = new BlockTintSource() {
            @Override
            public int color(BlockState state) {
                return properties.getTintColor(state.getFluidState(), null, null);
            }

            @Override
            public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
                return properties.getTintColor(state.getFluidState(), level, pos);
            }
        };
        FluidRenderingRegistry.register(instance.getSourceFluid().get(), instance.getFlowingFluid().get(), new FluidModel.Unbaked(
            new Material(properties.getStillTexture()),
            new Material(properties.getFlowingTexture()),
            properties.getOverlayTexture() == null ? null : new Material(properties.getOverlayTexture()),
            tintSource
        ));
    }
}
