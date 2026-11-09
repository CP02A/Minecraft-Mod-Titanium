/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.fabric.client;

import com.hrznstudio.titanium.fabric.transfer.FabricFluidStorage;
import com.hrznstudio.titanium.fluid.TitaniumFluidStack;
import com.hrznstudio.titanium.platform.services.IClientHelper;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;

public class FabricClientHelper implements IClientHelper {

    @Override
    public TextureAtlasSprite getStillFluidSprite(TitaniumFluidStack stack) {
        return Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(stack.getFluid().defaultFluidState()).stillMaterial().sprite();
    }

    @Override
    public int getFluidTintColor(TitaniumFluidStack stack) {
        return FluidVariantRendering.getColor(FabricFluidStorage.toVariant(stack));
    }

    @Override
    public Component getFluidName(TitaniumFluidStack stack) {
        return FluidVariantAttributes.getName(FabricFluidStorage.toVariant(stack));
    }
}
