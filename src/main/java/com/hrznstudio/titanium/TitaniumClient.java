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
import com.hrznstudio.titanium.client.fluid.TitaniumFluidModelRegistrar;
import com.hrznstudio.titanium.client.screen.addon.WidgetScreenAddon;
import com.hrznstudio.titanium.client.screen.container.BasicAddonScreen;
import com.hrznstudio.titanium.container.BasicAddonContainer;
import com.hrznstudio.titanium.event.handler.EventManager;
import com.hrznstudio.titanium.reward.RewardManager;
import com.hrznstudio.titanium.util.RayTraceUtils;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@Mod(value = Titanium.MODID, dist = Dist.CLIENT)
public class TitaniumClient {
    public static void registerModelLoader() {
        //ModelLoaderRegistry.registerLoader(new ResourceLocation(Titanium.MODID, "model_loader"),new TitaniumModelLoader());
    }

    public TitaniumClient(IEventBus bus) {
        BasicTile.registerClientInitializer(TwentyFourTestTile.class, tile ->
            tile.addGuiAddonFactory(() -> new WidgetScreenAddon(
                30,
                -25,
                new EditBox(Minecraft.getInstance().font, 0, 0, 120, 20, Component.literal(""))
            ))
        );
        bus.addListener((RegisterFluidModelsEvent event) -> TitaniumFluidModelRegistrar.registerModels(event));
        bus.addListener((RegisterClientExtensionsEvent event) -> TitaniumFluidModelRegistrar.registerClientExtensions(event));
        bus.addListener(this::clientSetup);
        bus.addListener((final RegisterMenuScreensEvent event) -> {
            event.register((MenuType<? extends BasicAddonContainer>) BasicAddonContainer.TYPE.get(), BasicAddonScreen::new);
        });
    }

    public static EntityRenderer<? super AbstractClientPlayer, ?> getPlayerRenderer(Minecraft minecraft, AbstractClientPlayer player) {
        return minecraft.getEntityRenderDispatcher().getRenderer(player);
    }

    public static void blockOverlayEvent(ExtractBlockOutlineRenderStateEvent event) {
        BlockHitResult traceResult = event.getHitResult();
        BlockState og = event.getBlockState();
        if (og.getBlock() instanceof BasicBlock && ((BasicBlock) og.getBlock()).hasIndividualRenderVoxelShape()) {
            VoxelShape shape = RayTraceUtils.rayTraceVoxelShape(traceResult, event.getLevel(), Minecraft.getInstance().player, 32, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
            BlockPos blockpos = event.getBlockPos();
            if (shape != null && !shape.isEmpty()) {
                event.addCustomRenderer((renderState, submitNodeCollector, poseStack, levelRenderState) -> {
                    Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
                    boolean highContrast = renderState.highContrast();
                    poseStack.pushPose();
                    poseStack.translate(blockpos.getX() - cameraPos.x, blockpos.getY() - cameraPos.y, blockpos.getZ() - cameraPos.z);
                    if (highContrast) {
                        submitNodeCollector.submitShapeOutline(poseStack, shape, RenderTypes.secondaryBlockOutline(), -16777216, 7.0F, renderState.isTranslucent());
                    }
                    RenderType renderType = highContrast ? RenderTypes.linesDepthBias() : RenderTypes.linesTranslucent();
                    int outlineColor = highContrast ? -11010079 : ARGB.black(102);
                    submitNodeCollector.submitShapeOutline(poseStack, shape, renderType, outlineColor,
                        Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth, renderState.isTranslucent());
                    poseStack.popPose();
                    return true;
                });
            }
        }
    }

    private void clientSetup(FMLClientSetupEvent event) {
        EventManager.forge(ExtractBlockOutlineRenderStateEvent.class).process(TitaniumClient::blockOverlayEvent).subscribe();
        registerModelLoader();
        RewardManager.get().getRewards().values().forEach(rewardGiver ->
            rewardGiver.getRewards().forEach(reward -> reward.register(Dist.CLIENT)));
    }
}
