/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium._impl.creative.tile;

import com.hrznstudio.titanium.api.transfer.IEnergyStorage;
import com.hrznstudio.titanium.platform.Services;
import com.hrznstudio.titanium._impl.creative.CreativeFEGeneratorBlock;
import com.hrznstudio.titanium.block.BasicTileBlock;
import com.hrznstudio.titanium.block.tile.PoweredTile;
import com.hrznstudio.titanium.component.energy.EnergyStorageComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

public class CreativeFEGeneratorTile extends PoweredTile<CreativeFEGeneratorTile> {

    private static final IEnergyStorage INFINITE_ENERGY = new IEnergyStorage() {
        @Override
        public int getEnergyStored() {
            return Integer.MAX_VALUE;
        }

        @Override
        public int getMaxEnergyStored() {
            return Integer.MAX_VALUE;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return maxExtract;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    };

    public CreativeFEGeneratorTile(BlockPos pos, BlockState state) {
        super((BasicTileBlock<CreativeFEGeneratorTile>) CreativeFEGeneratorBlock.INSTANCE.getBlock(),CreativeFEGeneratorBlock.INSTANCE.type().get(), pos, state);
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state, CreativeFEGeneratorTile blockEntity) {
        super.serverTick(level, pos, state, blockEntity);
        this.getEnergyStorage().setEnergyStored(Integer.MAX_VALUE);
        for (Direction direction : Direction.values()) {
            Services.TRANSFER.pushEnergy(this.level, this.getBlockPos().relative(direction), direction.getOpposite(), INFINITE_ENERGY, Integer.MAX_VALUE);
        }
        markForUpdate();
    }

    @Override
    @Nonnull
    public CreativeFEGeneratorTile getSelf() {
        return this;
    }

    @Override
    @ParametersAreNonnullByDefault
    public InteractionResult onActivated(Player player, InteractionHand hand, Direction facing, double hitX, double hitY, double hitZ) {
        if (super.onActivated(player, hand, facing, hitX, hitY, hitZ) == InteractionResult.SUCCESS) {
            openGui(player);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    @Nonnull
    @Override
    public EnergyStorageComponent<CreativeFEGeneratorTile> createEnergyStorage() {
        return new EnergyStorageComponent<>(Integer.MAX_VALUE, 0, 0);
    }
}
