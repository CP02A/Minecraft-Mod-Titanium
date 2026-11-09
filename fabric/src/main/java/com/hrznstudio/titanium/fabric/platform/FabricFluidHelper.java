/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.fabric.platform;

import com.hrznstudio.titanium.fluid.TitaniumFluid;
import com.hrznstudio.titanium.fluid.TitaniumFluidInstance;
import com.hrznstudio.titanium.fluid.TitaniumFluidProperties;
import com.hrznstudio.titanium.module.DeferredRegistryHelper;
import com.hrznstudio.titanium.platform.services.IFluidHelper;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributeHandler;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Optional;

public class FabricFluidHelper implements IFluidHelper {

    /**
     * Same translation key NeoForge uses for fluid types, so both loaders share the lang entries
     */
    public static String getDescriptionId(TitaniumFluidInstance instance) {
        String descriptionId = instance.getProperties().getDescriptionId();
        return descriptionId != null ? descriptionId : "fluid_type." + instance.getModId() + "." + instance.getFluid();
    }

    @Override
    public TitaniumFluid.Source createSourceFluid(TitaniumFluidInstance instance) {
        return new TitaniumFluid.Source(instance);
    }

    @Override
    public TitaniumFluid.Flowing createFlowingFluid(TitaniumFluidInstance instance) {
        return new TitaniumFluid.Flowing(instance);
    }

    @Override
    public void registerFluidAttributes(DeferredRegistryHelper helper, TitaniumFluidInstance instance) {
        TitaniumFluidProperties properties = instance.getProperties();
        FluidVariantAttributeHandler handler = new FluidVariantAttributeHandler() {
            @Override
            public Component getName(FluidVariant variant) {
                return Component.translatable(getDescriptionId(instance));
            }

            @Override
            public Optional<SoundEvent> getFillSound(FluidVariant variant) {
                return Optional.of(properties.getBucketFillSound());
            }

            @Override
            public Optional<SoundEvent> getEmptySound(FluidVariant variant) {
                return Optional.of(properties.getBucketEmptySound());
            }

            @Override
            public int getLightEmission(FluidVariant variant) {
                return properties.getLightLevel();
            }

            @Override
            public int getTemperature(FluidVariant variant) {
                return properties.getTemperature();
            }

            @Override
            public int getViscosity(FluidVariant variant, @Nullable Level level) {
                return properties.getViscosity();
            }

            @Override
            public boolean isLighterThanAir(FluidVariant variant) {
                return properties.getDensity() <= 0;
            }
        };
        // Fabric registers right away, so the fluids already exist
        FluidVariantAttributes.register(instance.getSourceFluid().get(), handler);
        FluidVariantAttributes.register(instance.getFlowingFluid().get(), handler);
    }
}
