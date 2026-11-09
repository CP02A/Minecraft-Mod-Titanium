/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.platform.services;

import com.hrznstudio.titanium.fluid.TitaniumFluidStack;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;

/**
 * Client only platform helpers.
 */
public interface IClientHelper {

    TextureAtlasSprite getStillFluidSprite(TitaniumFluidStack stack);

    /**
     * @return the ARGB tint of the fluid when rendered as a stack
     */
    int getFluidTintColor(TitaniumFluidStack stack);

    Component getFluidName(TitaniumFluidStack stack);
}
