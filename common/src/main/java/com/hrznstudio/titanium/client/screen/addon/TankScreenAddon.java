/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.client.screen.addon;

import com.hrznstudio.titanium.Titanium;
import com.hrznstudio.titanium.api.transfer.IFluidStorage;
import com.hrznstudio.titanium.fluid.TitaniumFluidStack;
import com.hrznstudio.titanium.platform.ClientServices;
import com.hrznstudio.titanium.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import com.hrznstudio.titanium.api.client.assets.types.ITankAsset;
import com.hrznstudio.titanium.client.screen.asset.IAssetProvider;
import com.hrznstudio.titanium.component.fluid.FluidTankComponent;
import com.hrznstudio.titanium.network.locator.ILocatable;
import com.hrznstudio.titanium.network.messages.ButtonClickNetworkMessage;
import com.hrznstudio.titanium.util.AssetUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TankScreenAddon extends BasicScreenAddon {

    private IFluidStorage tank;
    private ITankAsset asset;
    private FluidTankComponent.Type type;

    public TankScreenAddon(int posX, int posY, IFluidStorage tank, FluidTankComponent.Type type) {
        super(posX, posY);
        this.tank = tank;
        this.type = type;
    }

    private static final TagKey<Fluid> GASEOUS = TagKey.create(Registries.FLUID, Identifier.fromNamespaceAndPath("c", "gaseous"));

    private static boolean canMove(ItemStack carried, IFluidStorage tank, boolean intoTank, int amount, boolean exact) {
        int moved = Services.TRANSFER.simulateTransferWithStack(carried, tank, intoTank, amount);
        return exact ? moved == amount : moved > 0;
    }

    @Override
    public void drawBackgroundLayer(GuiGraphicsExtractor guiGraphics, Screen screen, IAssetProvider provider, int guiX, int guiY, int mouseX, int mouseY, float partialTicks) {
        asset = IAssetProvider.getAsset(provider, type.getAssetType());
        Rectangle area = asset.getArea();
        TitaniumFluidStack displayedFluid = tank.getFluidInTank(0);
        if (!displayedFluid.isEmpty()) {
            TitaniumFluidStack fluidStack = displayedFluid;
            double stored = fluidStack.getAmount();
            double capacity = tank.getTankCapacity(0);
            int topBottomPadding = asset.getFluidRenderPadding(Direction.UP) + asset.getFluidRenderPadding(Direction.DOWN);
            int offset = (int) ((stored / capacity) * (area.height - topBottomPadding));
            TextureAtlasSprite sprite = ClientServices.CLIENT.getStillFluidSprite(fluidStack);
            int color = ClientServices.CLIENT.getFluidTintColor(fluidStack);
            guiGraphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                sprite,
                this.getPosX() + guiX + asset.getFluidRenderPadding(Direction.WEST),
                this.getPosY() + guiY + asset.getFluidRenderPadding(Direction.UP) + (fluidStack.is(GASEOUS) ? 0 : (area.height - topBottomPadding) - offset),
                (int) (area.getWidth() - asset.getFluidRenderPadding(Direction.EAST) - asset.getFluidRenderPadding(Direction.WEST)),
                offset,
                color
            );
        }
        ITankAsset asset = IAssetProvider.getAsset(provider, type.getAssetType());
        AssetUtil.drawAsset(guiGraphics, screen, asset, guiX + getPosX(), guiY + getPosY());
    }

    @Override
    public int getXSize() {
        return asset != null ? asset.getArea().width : 0;
    }

    @Override
    public int getYSize() {
        return asset != null ? asset.getArea().height : 0;
    }

    @Override
    public void drawForegroundLayer(GuiGraphicsExtractor guiGraphics, Screen screen, IAssetProvider provider, int guiX, int guiY, int mouseX, int mouseY, float partialTicks) {
    }

    @Override
    public List<Component> getTooltipLines() {
        List<Component> strings = new ArrayList<>();
        TitaniumFluidStack tankStack = tank.getFluidInTank(0);
        strings.add(Component.literal(ChatFormatting.GOLD + Component.translatable("tooltip.titanium.tank.fluid").getString()).append(tankStack.isEmpty() ? Component.translatable("tooltip.titanium.tank.empty").withStyle(ChatFormatting.WHITE) : ClientServices.CLIENT.getFluidName(tankStack)).withStyle(ChatFormatting.WHITE));
        strings.add(Component.translatable("tooltip.titanium.tank.amount").withStyle(ChatFormatting.GOLD).append(Component.literal(ChatFormatting.WHITE + new DecimalFormat().format(tankStack.getAmount()) + ChatFormatting.GOLD + "/" + ChatFormatting.WHITE + new DecimalFormat().format(tank.getTankCapacity(0)) + ChatFormatting.DARK_AQUA + "mb")));
        ItemStack carried = Minecraft.getInstance().player.containerMenu.getCarried();
        if (!carried.isEmpty() && Services.TRANSFER.isFluidContainer(carried)) {
            {
                Item carriedItem = Minecraft.getInstance().player.containerMenu.getCarried().getItem();
                boolean isBucket = carriedItem instanceof BucketItem || carriedItem == Items.MILK_BUCKET;
                int amount = isBucket ? TitaniumFluidStack.BUCKET_VOLUME : Integer.MAX_VALUE;
                boolean canFillFromItem = canMove(carried, tank, true, amount, isBucket);
                boolean canDrainFromItem = canMove(carried, tank, false, amount, isBucket);
                if (canFillFromItem)
                    strings.add(Component.translatable("tooltip.titanium.tank.can_fill_from_item").withStyle(ChatFormatting.BLUE));
                if (canDrainFromItem)
                    strings.add(Component.translatable("tooltip.titanium.tank.can_drain_from_item").withStyle(ChatFormatting.GOLD));
                if (canFillFromItem)
                    strings.add(Component.translatable("tooltip.titanium.tank.action_fill").withStyle(ChatFormatting.DARK_GRAY));
                if (canDrainFromItem)
                    strings.add(Component.translatable("tooltip.titanium.tank.action_drain").withStyle(ChatFormatting.DARK_GRAY));
                if (!canDrainFromItem && !canFillFromItem) {
                    strings.add(Component.translatable("tooltip.titanium.tank.no_action").withStyle(ChatFormatting.RED));
                }
            }
        } else {
            strings.add(Component.translatable("tooltip.titanium.tank.no_tank").withStyle(ChatFormatting.DARK_GRAY));
        }
        return strings;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ItemStack carried = Minecraft.getInstance().player.containerMenu.getCarried();
        if (!carried.isEmpty() && Services.TRANSFER.isFluidContainer(carried)) {
            Screen screen = Minecraft.getInstance().gui.screen();
            if (screen instanceof AbstractContainerScreen && ((AbstractContainerScreen) screen).getMenu() instanceof ILocatable) {
                if (!isMouseOver(mouseX, mouseY))
                    return false;
                Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 1f, 1f, RandomSource.create(), Minecraft.getInstance().player.blockPosition())); //getPosition
                ILocatable locatable = (ILocatable) ((AbstractContainerScreen) screen).getMenu();
                CompoundTag compoundNBT = new CompoundTag();
                if (tank instanceof FluidTankComponent) {
                    compoundNBT.putString("Name", ((FluidTankComponent<?>) tank).getName());
                } else {
                    compoundNBT.putBoolean("Invalid", true);
                }
                Item carriedItem = carried.getItem();
                boolean isBucket = carriedItem instanceof BucketItem || carriedItem == Items.MILK_BUCKET;
                int amount = isBucket ? TitaniumFluidStack.BUCKET_VOLUME : Integer.MAX_VALUE;
                boolean canFillFromItem = canMove(carried, tank, true, amount, isBucket);
                boolean canDrainFromItem = canMove(carried, tank, false, amount, isBucket);
                if (canFillFromItem && button == 0) compoundNBT.putBoolean("Fill", true);
                if (canDrainFromItem && button == 1) compoundNBT.putBoolean("Fill", false);
                Titanium.NETWORK.sendToServer(new ButtonClickNetworkMessage(locatable.getLocatorInstance(), -3, compoundNBT));
                return true;
            }
        }
        return false;
    }
}
