/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.neoforge.platform;

import com.hrznstudio.titanium.fluid.TitaniumFluidStack;
import com.hrznstudio.titanium.neoforge.transfer.FluidStorageResourceHandler;
import com.hrznstudio.titanium.platform.services.IClientHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;

public class NeoForgeClientHelper implements IClientHelper {

    private static FluidModel getModel(TitaniumFluidStack stack) {
        return Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(stack.getFluid().defaultFluidState());
    }

    @Override
    public TextureAtlasSprite getStillFluidSprite(TitaniumFluidStack stack) {
        return getModel(stack).stillMaterial().sprite();
    }

    @Override
    public int getFluidTintColor(TitaniumFluidStack stack) {
        FluidModel model = getModel(stack);
        return model.fluidTintSource() == null ? -1 : model.fluidTintSource().colorAsStack(FluidStorageResourceHandler.toNeoForge(stack));
    }

    @Override
    public Component getFluidName(TitaniumFluidStack stack) {
        FluidStack neoStack = FluidStorageResourceHandler.toNeoForge(stack);
        return neoStack.getHoverName();
    }
}
