/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.FluidState;

import javax.annotation.Nullable;

/**
 * Loader independent client properties of a {@link TitaniumFluidInstance}: the textures (sprite ids in the block atlas)
 * and the ARGB tint. Turned into fluid models / render handlers by the platform modules.
 */
public interface TitaniumFluidRenderProperties {

    Identifier getStillTexture();

    Identifier getFlowingTexture();

    @Nullable
    default Identifier getOverlayTexture() {
        return null;
    }

    /**
     * @return the default ARGB tint, -1 for none
     */
    default int getTintColor() {
        return -1;
    }

    /**
     * @return the ARGB tint used when the fluid is rendered as a stack (GUIs, items)
     */
    default int getTintColor(TitaniumFluidStack stack) {
        return getTintColor();
    }

    /**
     * @return the ARGB tint used when the fluid is rendered in the world
     */
    default int getTintColor(FluidState state, @Nullable BlockGetter level, @Nullable BlockPos pos) {
        return getTintColor(new TitaniumFluidStack(state.getType(), TitaniumFluidStack.BUCKET_VOLUME));
    }
}
