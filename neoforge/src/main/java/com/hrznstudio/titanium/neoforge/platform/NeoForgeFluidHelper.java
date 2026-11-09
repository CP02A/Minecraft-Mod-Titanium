/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.neoforge.platform;

import com.hrznstudio.titanium.fluid.TitaniumFluid;
import com.hrznstudio.titanium.fluid.TitaniumFluidInstance;
import com.hrznstudio.titanium.fluid.TitaniumFluidProperties;
import com.hrznstudio.titanium.module.DeferredRegistryHelper;
import com.hrznstudio.titanium.platform.services.IFluidHelper;
import com.hrznstudio.titanium.registry.RegistryEntry;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class NeoForgeFluidHelper implements IFluidHelper {

    private static final Map<TitaniumFluidInstance, RegistryEntry<FluidType, FluidType>> FLUID_TYPES = new ConcurrentHashMap<>();

    /**
     * @return the NeoForge fluid type registered for the instance
     */
    public static RegistryEntry<FluidType, FluidType> getFluidType(TitaniumFluidInstance instance) {
        return Objects.requireNonNull(FLUID_TYPES.get(instance), () -> "No fluid type registered for " + instance.getFluid());
    }

    public static FluidType.Properties toNeoForge(TitaniumFluidProperties properties) {
        FluidType.Properties neoProperties = FluidType.Properties.create()
            .lightLevel(properties.getLightLevel())
            .density(properties.getDensity())
            .temperature(properties.getTemperature())
            .viscosity(properties.getViscosity())
            .rarity(properties.getRarity())
            .motionScale(properties.getMotionScale())
            .fallDistanceModifier(properties.getFallDistanceModifier())
            .canPushEntity(properties.canPushEntity())
            .canSwim(properties.canSwim())
            .canDrown(properties.canDrown())
            .canExtinguish(properties.canExtinguish())
            .canHydrate(properties.canHydrate())
            .supportsBoating(properties.supportsBoating())
            .sound(SoundActions.BUCKET_FILL, properties.getBucketFillSound())
            .sound(SoundActions.BUCKET_EMPTY, properties.getBucketEmptySound());
        if (properties.getDescriptionId() != null) {
            neoProperties.descriptionId(properties.getDescriptionId());
        }
        return neoProperties;
    }

    @Override
    public TitaniumFluid.Source createSourceFluid(TitaniumFluidInstance instance) {
        return new TitaniumFluid.Source(instance) {
            @Override
            public FluidType getFluidType() {
                return NeoForgeFluidHelper.getFluidType(instance).get();
            }
        };
    }

    @Override
    public TitaniumFluid.Flowing createFlowingFluid(TitaniumFluidInstance instance) {
        return new TitaniumFluid.Flowing(instance) {
            @Override
            public FluidType getFluidType() {
                return NeoForgeFluidHelper.getFluidType(instance).get();
            }
        };
    }

    @Override
    public void registerFluidAttributes(DeferredRegistryHelper helper, TitaniumFluidInstance instance) {
        FLUID_TYPES.put(instance, helper.registerGeneric(NeoForgeRegistries.Keys.FLUID_TYPES, instance.getFluid(), () -> new FluidType(toNeoForge(instance.getProperties()))));
    }
}
