/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.fluid;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Rarity;

import javax.annotation.Nullable;

/**
 * Loader independent fluid attributes. Mapped to a NeoForge {@code FluidType} and to Fabric fluid variant attributes
 * by the platform modules. Defaults match the NeoForge FluidType defaults.
 */
public class TitaniumFluidProperties {

    @Nullable
    private String descriptionId;
    private int lightLevel = 0;
    private int density = 1000;
    private int temperature = 300;
    private int viscosity = 1000;
    private Rarity rarity = Rarity.COMMON;
    private double motionScale = 0.014D;
    private float fallDistanceModifier = 0.5F;
    private boolean canPushEntity = true;
    private boolean canSwim = true;
    private boolean canDrown = true;
    private boolean canExtinguish = false;
    private boolean canHydrate = false;
    private boolean supportsBoating = false;
    private SoundEvent bucketFillSound = SoundEvents.BUCKET_FILL;
    private SoundEvent bucketEmptySound = SoundEvents.BUCKET_EMPTY;

    public static TitaniumFluidProperties create() {
        return new TitaniumFluidProperties();
    }

    public TitaniumFluidProperties descriptionId(String descriptionId) {
        this.descriptionId = descriptionId;
        return this;
    }

    public TitaniumFluidProperties lightLevel(int lightLevel) {
        this.lightLevel = lightLevel;
        return this;
    }

    public TitaniumFluidProperties density(int density) {
        this.density = density;
        return this;
    }

    public TitaniumFluidProperties temperature(int temperature) {
        this.temperature = temperature;
        return this;
    }

    public TitaniumFluidProperties viscosity(int viscosity) {
        this.viscosity = viscosity;
        return this;
    }

    public TitaniumFluidProperties rarity(Rarity rarity) {
        this.rarity = rarity;
        return this;
    }

    public TitaniumFluidProperties motionScale(double motionScale) {
        this.motionScale = motionScale;
        return this;
    }

    public TitaniumFluidProperties fallDistanceModifier(float fallDistanceModifier) {
        this.fallDistanceModifier = fallDistanceModifier;
        return this;
    }

    public TitaniumFluidProperties canPushEntity(boolean canPushEntity) {
        this.canPushEntity = canPushEntity;
        return this;
    }

    public TitaniumFluidProperties canSwim(boolean canSwim) {
        this.canSwim = canSwim;
        return this;
    }

    public TitaniumFluidProperties canDrown(boolean canDrown) {
        this.canDrown = canDrown;
        return this;
    }

    public TitaniumFluidProperties canExtinguish(boolean canExtinguish) {
        this.canExtinguish = canExtinguish;
        return this;
    }

    public TitaniumFluidProperties canHydrate(boolean canHydrate) {
        this.canHydrate = canHydrate;
        return this;
    }

    public TitaniumFluidProperties supportsBoating(boolean supportsBoating) {
        this.supportsBoating = supportsBoating;
        return this;
    }

    public TitaniumFluidProperties bucketFillSound(SoundEvent sound) {
        this.bucketFillSound = sound;
        return this;
    }

    public TitaniumFluidProperties bucketEmptySound(SoundEvent sound) {
        this.bucketEmptySound = sound;
        return this;
    }

    @Nullable
    public String getDescriptionId() {
        return descriptionId;
    }

    public int getLightLevel() {
        return lightLevel;
    }

    public int getDensity() {
        return density;
    }

    public int getTemperature() {
        return temperature;
    }

    public int getViscosity() {
        return viscosity;
    }

    public Rarity getRarity() {
        return rarity;
    }

    public double getMotionScale() {
        return motionScale;
    }

    public float getFallDistanceModifier() {
        return fallDistanceModifier;
    }

    public boolean canPushEntity() {
        return canPushEntity;
    }

    public boolean canSwim() {
        return canSwim;
    }

    public boolean canDrown() {
        return canDrown;
    }

    public boolean canExtinguish() {
        return canExtinguish;
    }

    public boolean canHydrate() {
        return canHydrate;
    }

    public boolean supportsBoating() {
        return supportsBoating;
    }

    public SoundEvent getBucketFillSound() {
        return bucketFillSound;
    }

    public SoundEvent getBucketEmptySound() {
        return bucketEmptySound;
    }
}
