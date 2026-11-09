/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium;

import com.hrznstudio.titanium._impl.test.tile.TwentyFourTestTile;
import com.hrznstudio.titanium.block.BasicBlock;
import com.hrznstudio.titanium.block.tile.BasicTile;
import com.hrznstudio.titanium.client.screen.addon.WidgetScreenAddon;
import com.hrznstudio.titanium.reward.RewardManager;
import com.hrznstudio.titanium.util.PhysicalSide;
import com.hrznstudio.titanium.util.RayTraceUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Loader independent client logic of Titanium, called by the client entrypoint of each loader.
 */
public class TitaniumClient {

    /**
     * Called by the loader client entrypoint when the mod is constructed
     */
    public static void init() {
        BasicTile.registerClientInitializer(TwentyFourTestTile.class, tile ->
            tile.addGuiAddonFactory(() -> new WidgetScreenAddon(
                30,
                -25,
                new EditBox(Minecraft.getInstance().font, 0, 0, 120, 20, Component.literal(""))
            ))
        );
    }

    /**
     * Called by the loader during client setup
     */
    public static void clientSetup() {
        RewardManager.get().getRewards().values().forEach(rewardGiver ->
            rewardGiver.getRewards().forEach(reward -> reward.register(PhysicalSide.CLIENT)));
    }

    public static EntityRenderer<? super AbstractClientPlayer, ?> getPlayerRenderer(Minecraft minecraft, AbstractClientPlayer player) {
        return minecraft.getEntityRenderDispatcher().getRenderer(player);
    }

    /**
     * @return the outline shape of the part of a {@link BasicBlock} the player is looking at, or null if the block
     * doesn't use individual render shapes
     */
    @Nullable
    public static VoxelShape getCustomOutlineShape(Level level, BlockState state, BlockHitResult hitResult) {
        if (state.getBlock() instanceof BasicBlock basicBlock && basicBlock.hasIndividualRenderVoxelShape()) {
            VoxelShape shape = RayTraceUtils.rayTraceVoxelShape(hitResult, level, Minecraft.getInstance().player, 32, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
            if (shape != null && !shape.isEmpty()) {
                return shape;
            }
        }
        return null;
    }

    /**
     * Submits a block outline the same way vanilla does
     */
    public static void submitOutline(SubmitNodeCollector submitNodeCollector, PoseStack poseStack, Vec3 cameraPos, BlockPos pos, VoxelShape shape, boolean highContrast, boolean translucent) {
        poseStack.pushPose();
        poseStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
        if (highContrast) {
            submitNodeCollector.submitShapeOutline(poseStack, shape, RenderTypes.secondaryBlockOutline(), -16777216, 7.0F, translucent);
        }
        RenderType renderType = highContrast ? RenderTypes.linesDepthBias() : RenderTypes.linesTranslucent();
        int outlineColor = highContrast ? -11010079 : ARGB.black(102);
        submitNodeCollector.submitShapeOutline(poseStack, shape, renderType, outlineColor,
            Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth, translucent);
        poseStack.popPose();
    }
}
