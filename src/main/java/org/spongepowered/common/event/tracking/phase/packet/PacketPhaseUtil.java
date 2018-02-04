/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.event.tracking.phase.packet;

import static org.spongepowered.common.event.tracking.phase.packet.PacketState.processEntities;
import static org.spongepowered.common.event.tracking.phase.packet.PacketState.processSpawnedEntities;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraft.util.EnumHand;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.entity.projectile.source.ProjectileSource;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.entity.projectile.LaunchProjectileEvent;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.interfaces.IMixinContainer;
import org.spongepowered.common.item.inventory.adapter.impl.slots.SlotAdapter;
import org.spongepowered.common.item.inventory.util.ContainerUtil;
import org.spongepowered.common.item.inventory.util.ItemStackUtil;
import org.spongepowered.common.registry.type.ItemTypeRegistryModule;
import org.spongepowered.common.registry.type.event.InternalSpawnTypes;

import java.util.List;

public final class PacketPhaseUtil {

    public static void handleSlotRestore(EntityPlayer player, Container openContainer, List<SlotTransaction> slotTransactions, boolean eventCancelled) {
        for (SlotTransaction slotTransaction : slotTransactions) {

            if ((!slotTransaction.getCustom().isPresent() && slotTransaction.isValid()) && !eventCancelled) {
                continue;
            }
            final SlotAdapter slot = (SlotAdapter) slotTransaction.getSlot();
            final int slotNumber = slot.slotNumber;
            ItemStackSnapshot snapshot = eventCancelled || !slotTransaction.isValid() ? slotTransaction.getOriginal() : slotTransaction.getCustom().get();
            final ItemStack originalStack = ItemStackUtil.fromSnapshotToNative(snapshot);
            if (openContainer == null) {
                slot.set(((org.spongepowered.api.item.inventory.ItemStack) originalStack));
            } else {
                final Slot nmsSlot = openContainer.getSlot(slotNumber);
                if (nmsSlot != null) {
                    nmsSlot.putStack(originalStack);
                }
            }
        }
        if (openContainer != null) {
            openContainer.detectAndSendChanges();
            // If event is cancelled, always resync with player
            // we must also validate the player still has the same container open after the event has been processed
            if (eventCancelled && player.openContainer == openContainer && player instanceof EntityPlayerMP) {
                ((EntityPlayerMP) player).sendContainerToPlayer(openContainer);
            }
        }
    }

    public static void handleCustomCursor(EntityPlayerMP player, ItemStackSnapshot customCursor) {
        ItemStack cursor = ItemStackUtil.fromSnapshotToNative(customCursor);
        player.inventory.setItemStack(cursor);
        player.connection.sendPacket(new SPacketSetSlot(-1, -1, cursor));
    }

    public static void validateCapturedTransactions(int slotId, Container openContainer, List<SlotTransaction> capturedTransactions) {
        if (capturedTransactions.size() == 0 && slotId >= 0 && slotId < openContainer.inventorySlots.size()) {
            final Slot slot = openContainer.getSlot(slotId);
            if (slot != null) {
                ItemStackSnapshot snapshot = slot.getHasStack() ? ((org.spongepowered.api.item.inventory.ItemStack) slot.getStack()).createSnapshot() : ItemStackSnapshot.NONE;
                final SlotTransaction slotTransaction = new SlotTransaction(ContainerUtil.getSlot(openContainer, slotId), snapshot, snapshot);
                capturedTransactions.add(slotTransaction);
            }
        }
    }

    public static void handlePlayerSlotRestore(EntityPlayerMP player, ItemStack itemStack, EnumHand hand) {
        if (itemStack.isEmpty() || itemStack == ItemTypeRegistryModule.NONE) {
            return;
        }

        player.isChangingQuantityOnly = false;
        int slotId = 0;
        if (hand == EnumHand.OFF_HAND) {
            player.inventory.offHandInventory.set(0, itemStack);
            slotId = (player.inventory.mainInventory.size() + InventoryPlayer.getHotbarSize());
        } else {
            player.inventory.mainInventory.set(player.inventory.currentItem, itemStack);
            final Slot slot = player.openContainer.getSlotFromInventory(player.inventory, player.inventory.currentItem);
            slotId = slot.slotNumber;
        }

        player.openContainer.detectAndSendChanges();
        player.isChangingQuantityOnly = false;
        player.connection.sendPacket(new SPacketSetSlot(player.openContainer.windowId, slotId, itemStack));
    }

    // Check if all transactions are invalid
    public static boolean allTransactionsInvalid(List<SlotTransaction> slotTransactions) {
        if (slotTransactions.size() == 0) {
            return false;
        }

        for (SlotTransaction slotTransaction : slotTransactions) {
            if (slotTransaction.isValid()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Common method to fire a LaunchProjectileEvent and populate its context, then calls SpawnEntityEvent.
     *
     * This must be called in the main thread.
     *
     * @param frame a frame to populate the context and use for the event.
     * @param player the player who launched the projectiles.
     * @param projectiles the projectiles to fire.
     */
    public static void fireProjectileLaunchEvent(CauseStackManager.StackFrame frame, EntityPlayerMP player, List<Projectile> projectiles) {
        if(!SpongeImpl.isMainThread()) {
            SpongeImpl.getLogger().error("handleProjectileLaunch called outside of the main thread");
            return;
        }

        frame.addContext(EventContextKeys.SPAWN_TYPE, InternalSpawnTypes.PROJECTILE);
        frame.addContext(EventContextKeys.PROJECTILE_SOURCE, (ProjectileSource) player);
        frame.addContext(EventContextKeys.THROWER, (ProjectileSource) player);

        // TODO fix the Event Impl Generator to not have to put two times the same list
        LaunchProjectileEvent launchProjectileEvent = SpongeEventFactory.createLaunchProjectileEvent(Sponge.getCauseStackManager().getCurrentCause(),
                projectiles, projectiles);
        if (SpongeImpl.postEvent(launchProjectileEvent)) {
            projectiles.clear();
            IMixinContainer playerMixinContainer = (IMixinContainer) player.openContainer;
            handleSlotRestore(player, (Container) playerMixinContainer, playerMixinContainer.getCapturedTransactions(), true);
        } else {
            if(ShouldFire.SPAWN_ENTITY_EVENT) {
                SpawnEntityEvent spawnEntityEvent = SpongeEventFactory.createSpawnEntityEvent(frame.getCurrentCause(), projectiles);
                if(!SpongeImpl.postEvent(spawnEntityEvent)) {
                    processSpawnedEntities(player, spawnEntityEvent);
                }
            } else {
                processEntities(player, (List<Entity>) (List<?>) projectiles);
            }
        }
    }
}
