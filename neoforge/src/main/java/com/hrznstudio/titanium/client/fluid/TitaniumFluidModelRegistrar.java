/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.client.fluid;

import com.hrznstudio.titanium.fluid.TitaniumFluidInstance;
import com.hrznstudio.titanium.fluid.TitaniumFluidRenderProperties;
import com.hrznstudio.titanium.neoforge.platform.NeoForgeFluidHelper;
import com.hrznstudio.titanium.neoforge.transfer.FluidStorageResourceHandler;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Registers the fluid models of the {@link TitaniumFluidInstance}s on NeoForge
 */
public final class TitaniumFluidModelRegistrar {

    private TitaniumFluidModelRegistrar() {
    }

    public static FluidTintSource createTintSource(TitaniumFluidRenderProperties properties) {
        return new FluidTintSource() {
            @Override
            public int color(FluidState state) {
                return properties.getTintColor(state, null, null);
            }

            @Override
            public int colorInWorld(FluidState fluidState, BlockState blockState, BlockAndTintGetter level, BlockPos pos) {
                return properties.getTintColor(fluidState, level, pos);
            }

            @Override
            public int colorAsStack(FluidStack stack) {
                return properties.getTintColor(FluidStorageResourceHandler.toTitanium(stack));
            }
        };
    }

    public static void registerModels(RegisterFluidModelsEvent event) {
        for (TitaniumFluidInstance instance : TitaniumFluidInstance.getInstances()) {
            TitaniumFluidRenderProperties properties = instance.getRenderProperties();
            event.register(
                new FluidModel.Unbaked(
                    new Material(properties.getStillTexture()),
                    new Material(properties.getFlowingTexture()),
                    properties.getOverlayTexture() == null ? null : new Material(properties.getOverlayTexture()),
                    createTintSource(properties)
                ),
                instance.getSourceFluid(),
                instance.getFlowingFluid()
            );
        }
    }

    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        for (TitaniumFluidInstance instance : TitaniumFluidInstance.getInstances()) {
            event.registerFluidType(new IClientFluidTypeExtensions() {
            }, NeoForgeFluidHelper.getFluidType(instance).get());
        }
    }
}
