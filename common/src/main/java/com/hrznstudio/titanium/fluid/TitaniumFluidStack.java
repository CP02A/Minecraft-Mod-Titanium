/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

/**
 * Loader independent fluid stack. Amounts are always expressed in millibuckets ({@link #BUCKET_VOLUME} per bucket),
 * the loader specific transfer adapters convert to the native unit of the platform.
 * <p>
 * The serialized form ({@code {id, amount, components}}) matches NeoForge's FluidStack so data stays compatible.
 */
public final class TitaniumFluidStack {

    public static final int BUCKET_VOLUME = 1000;
    public static final TitaniumFluidStack EMPTY = new TitaniumFluidStack(Fluids.EMPTY.builtInRegistryHolder(), 0, DataComponentPatch.EMPTY);

    private static final Codec<Holder<Fluid>> FLUID_NON_EMPTY_CODEC = BuiltInRegistries.FLUID.holderByNameCodec()
        .validate(holder -> holder.value() == Fluids.EMPTY ? DataResult.error(() -> "Fluid must not be minecraft:empty") : DataResult.success(holder));

    public static final MapCodec<TitaniumFluidStack> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        FLUID_NON_EMPTY_CODEC.fieldOf("id").forGetter(TitaniumFluidStack::getFluidHolder),
        ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(TitaniumFluidStack::getAmount),
        DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(TitaniumFluidStack::getComponentsPatch)
    ).apply(instance, TitaniumFluidStack::new));

    /**
     * Codec that does not accept empty stacks
     */
    public static final Codec<TitaniumFluidStack> CODEC = MAP_CODEC.codec();

    /**
     * Codec that accepts empty stacks, serializing them as {@code {}}
     */
    public static final Codec<TitaniumFluidStack> OPTIONAL_CODEC = ExtraCodecs.optionalEmptyMap(CODEC)
        .xmap(optional -> optional.orElse(EMPTY), stack -> stack.isEmpty() ? Optional.empty() : Optional.of(stack));

    private static final StreamCodec<RegistryFriendlyByteBuf, Holder<Fluid>> FLUID_HOLDER_STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.FLUID);

    public static final StreamCodec<RegistryFriendlyByteBuf, TitaniumFluidStack> OPTIONAL_STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TitaniumFluidStack decode(RegistryFriendlyByteBuf buf) {
            int amount = buf.readVarInt();
            if (amount <= 0) {
                return EMPTY;
            }
            Holder<Fluid> holder = FLUID_HOLDER_STREAM_CODEC.decode(buf);
            DataComponentPatch patch = DataComponentPatch.STREAM_CODEC.decode(buf);
            return new TitaniumFluidStack(holder, amount, patch);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, TitaniumFluidStack stack) {
            if (stack.isEmpty()) {
                buf.writeVarInt(0);
            } else {
                buf.writeVarInt(stack.getAmount());
                FLUID_HOLDER_STREAM_CODEC.encode(buf, stack.getFluidHolder());
                DataComponentPatch.STREAM_CODEC.encode(buf, stack.getComponentsPatch());
            }
        }
    };

    public static final StreamCodec<RegistryFriendlyByteBuf, TitaniumFluidStack> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TitaniumFluidStack decode(RegistryFriendlyByteBuf buf) {
            TitaniumFluidStack stack = OPTIONAL_STREAM_CODEC.decode(buf);
            if (stack.isEmpty()) throw new DecoderException("Empty TitaniumFluidStack not allowed");
            return stack;
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, TitaniumFluidStack stack) {
            if (stack.isEmpty()) throw new EncoderException("Empty TitaniumFluidStack not allowed");
            OPTIONAL_STREAM_CODEC.encode(buf, stack);
        }
    };

    private final Holder<Fluid> fluid;
    private final int amount;
    private final DataComponentPatch components;

    public TitaniumFluidStack(Holder<Fluid> fluid, int amount, DataComponentPatch components) {
        this.fluid = Objects.requireNonNull(fluid);
        this.amount = amount;
        this.components = Objects.requireNonNull(components);
    }

    public TitaniumFluidStack(Holder<Fluid> fluid, int amount) {
        this(fluid, amount, DataComponentPatch.EMPTY);
    }

    public TitaniumFluidStack(Fluid fluid, int amount, DataComponentPatch components) {
        this(fluid.builtInRegistryHolder(), amount, components);
    }

    public TitaniumFluidStack(Fluid fluid, int amount) {
        this(fluid.builtInRegistryHolder(), amount, DataComponentPatch.EMPTY);
    }

    public boolean isEmpty() {
        return amount <= 0 || fluid.value() == Fluids.EMPTY;
    }

    public Fluid getFluid() {
        return isEmpty() ? Fluids.EMPTY : fluid.value();
    }

    public Holder<Fluid> getFluidHolder() {
        return fluid;
    }

    public int getAmount() {
        return isEmpty() ? 0 : amount;
    }

    public DataComponentPatch getComponentsPatch() {
        return components;
    }

    @Nullable
    public <T> T get(DataComponentType<? extends T> type) {
        return components.get(DataComponentMap.EMPTY, type);
    }

    public boolean is(Fluid fluid) {
        return getFluid() == fluid;
    }

    public boolean is(TagKey<Fluid> tag) {
        return !isEmpty() && fluid.is(tag);
    }

    public TitaniumFluidStack copy() {
        return isEmpty() ? EMPTY : new TitaniumFluidStack(fluid, amount, components);
    }

    public TitaniumFluidStack copyWithAmount(int amount) {
        return isEmpty() || amount <= 0 ? EMPTY : new TitaniumFluidStack(fluid, amount, components);
    }

    public TitaniumFluidStack grow(int amount) {
        return copyWithAmount(this.amount + amount);
    }

    public TitaniumFluidStack shrink(int amount) {
        return copyWithAmount(this.amount - amount);
    }

    /**
     * Checks if both stacks have the same fluid and components, ignoring the amount
     */
    public static boolean isSameFluidSameComponents(TitaniumFluidStack first, TitaniumFluidStack second) {
        if (first.isEmpty() || second.isEmpty()) return first.isEmpty() && second.isEmpty();
        return first.getFluid() == second.getFluid() && first.components.equals(second.components);
    }

    public static boolean matches(TitaniumFluidStack first, TitaniumFluidStack second) {
        return first.getAmount() == second.getAmount() && isSameFluidSameComponents(first, second);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TitaniumFluidStack other && matches(this, other);
    }

    @Override
    public int hashCode() {
        return isEmpty() ? 0 : Objects.hash(getFluid(), amount, components);
    }

    @Override
    public String toString() {
        return isEmpty() ? "TitaniumFluidStack[EMPTY]" : amount + " " + BuiltInRegistries.FLUID.getKey(getFluid());
    }
}
