/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.neoforge;

import com.hrznstudio.titanium.Titanium;
import com.hrznstudio.titanium.TitaniumClient;
import com.hrznstudio.titanium.client.fluid.TitaniumFluidModelRegistrar;
import com.hrznstudio.titanium.client.screen.container.BasicAddonScreen;
import com.hrznstudio.titanium.container.BasicAddonContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Titanium.MODID, dist = Dist.CLIENT)
public class TitaniumNeoForgeClient {

    @SuppressWarnings("unchecked")
    public TitaniumNeoForgeClient(IEventBus modBus) {
        TitaniumClient.init();
        modBus.addListener((RegisterFluidModelsEvent event) -> TitaniumFluidModelRegistrar.registerModels(event));
        modBus.addListener((RegisterClientExtensionsEvent event) -> TitaniumFluidModelRegistrar.registerClientExtensions(event));
        modBus.addListener((FMLClientSetupEvent event) -> TitaniumClient.clientSetup());
        modBus.addListener((RegisterMenuScreensEvent event) ->
            event.register((MenuType<BasicAddonContainer>) (MenuType<?>) BasicAddonContainer.TYPE.get(), BasicAddonScreen::new));
        NeoForge.EVENT_BUS.addListener(TitaniumNeoForgeClient::onExtractBlockOutline);
    }

    private static void onExtractBlockOutline(ExtractBlockOutlineRenderStateEvent event) {
        VoxelShape shape = TitaniumClient.getCustomOutlineShape(event.getLevel(), event.getBlockState(), event.getHitResult());
        if (shape != null) {
            BlockPos pos = event.getBlockPos();
            event.addCustomRenderer((renderState, submitNodeCollector, poseStack, levelRenderState) -> {
                TitaniumClient.submitOutline(submitNodeCollector, poseStack, levelRenderState.cameraRenderState.pos, pos, shape, renderState.highContrast(), renderState.isTranslucent());
                return true;
            });
        }
    }
}
