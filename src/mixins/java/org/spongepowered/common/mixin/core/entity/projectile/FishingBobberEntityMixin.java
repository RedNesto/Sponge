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
package org.spongepowered.common.mixin.core.entity.projectile;

import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootParameterSets;
import net.minecraft.world.storage.loot.LootParameters;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTables;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.projectile.FishingBobber;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.projectile.source.ProjectileSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.entity.projectile.ProjectileSourceSerializer;
import org.spongepowered.common.item.util.ItemStackUtil;
import org.spongepowered.common.mixin.core.entity.EntityMixin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

@Mixin(FishingBobberEntity.class)
public abstract class FishingBobberEntityMixin extends EntityMixin {

    @Shadow @Nullable private PlayerEntity angler;
    @Shadow @Nullable public net.minecraft.entity.Entity caughtEntity;
    @Shadow private int ticksCatchable;
    @Shadow private int luck;
    @Shadow private boolean inGround;

    @Shadow protected abstract void bringInHookedEntity();

    @Nullable private ProjectileSource impl$projectileSource;

    @Inject(method = "setHookedEntity", at = @At("HEAD"), cancellable = true)
    private void onSetHookedEntity(CallbackInfo ci) {
        if (SpongeCommon
            .postEvent(SpongeEventFactory.createFishingEventHookEntity(PhaseTracker.getCauseStackManager().getCurrentCause(), (Entity) this.caughtEntity, (FishingBobber) this))) {
            this.caughtEntity = null;
            ci.cancel();
        }
    }

    /**
     * @author Aaron1011 - February 6th, 2015
     * @author Minecrell - December 24th, 2016 (Updated to Minecraft 1.11.2)
     * @author Minecrell - June 14th, 2017 (Rewritten to handle cases where no items are dropped)
     * @reason This needs to handle for both cases where a fish and/or an entity is being caught.
     */
    @Overwrite
    public int handleHookRetraction(ItemStack stack) {
        if (!this.world.isRemote && this.angler != null) {
            int i = 0;

            // Sponge start
            final List<Transaction<ItemStackSnapshot>> transactions;
            if (this.ticksCatchable > 0) {
                // Moved from below
                final LootContext.Builder lootcontext$builder = new LootContext.Builder((ServerWorld)this.world)
                        .withParameter(LootParameters.POSITION, new BlockPos((FishingBobberEntity) (Object) this))
                        .withParameter(LootParameters.TOOL, stack)
                        .withRandom(this.rand)
                        .withLuck((float)this.luck + this.angler.getLuck());
                final LootTable lootTable = this.world.getServer().getLootTableManager().getLootTableFromLocation(LootTables.GAMEPLAY_FISHING);
                final List<ItemStack> list = lootTable.generate(lootcontext$builder.build(LootParameterSets.FISHING));
                transactions = list.stream().map(ItemStackUtil::snapshotOf)
                        .map(snapshot -> new Transaction<>(snapshot, snapshot))
                        .collect(Collectors.toList());
            } else {
                transactions = new ArrayList<>();
            }
            PhaseTracker.getCauseStackManager().pushCause(this.angler);

            if (SpongeCommon.postEvent(SpongeEventFactory.createFishingEventStop(PhaseTracker.getCauseStackManager().getCurrentCause(), ((FishingBobber) this), transactions))) {
                // Event is cancelled
                return -1;
            }

            if (this.caughtEntity != null) {
                this.bringInHookedEntity();
                this.world.setEntityState((net.minecraft.entity.Entity) (Object) this, (byte) 31);
                i = this.caughtEntity instanceof ItemEntity ? 3 : 5;
            } // Sponge: Remove else

            // Sponge start - Moved up to event call
            if (!transactions.isEmpty()) { // Sponge: Check if we have any transactions instead
                //LootContext.Builder lootcontext$builder = new LootContext.Builder((WorldServer) this.world);
                //lootcontext$builder.withLuck((float) this.field_191518_aw + this.angler.getLuck());

                // Use transactions
                for (Transaction<ItemStackSnapshot> transaction : transactions) {
                    if (!transaction.isValid()) {
                        continue;
                    }
                    ItemStack itemstack = (ItemStack) (Object) transaction.getFinal().createStack();
                    // Sponge end

                    ItemEntity entityitem = new ItemEntity(this.world, this.posX, this.posY, this.posZ, itemstack);
                    double d0 = this.angler.posX - this.posX;
                    double d1 = this.angler.posY - this.posY;
                    double d2 = this.angler.posZ - this.posZ;
                    double d3 = MathHelper.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                    //double d4 = 0.1D;
                    entityitem.setMotion(d0 * 0.1D, d1 * 0.1D + MathHelper.sqrt(d3) * 0.08D, d2 * 0.1D);
                    this.world.addEntity(entityitem);
                    this.angler.world.addEntity(new ExperienceOrbEntity(this.angler.world, this.angler.posX, this.angler.posY + 0.5D, this.angler.posZ + 0.5D,
                            this.rand.nextInt(6) + 1));
                    Item item = itemstack.getItem();

                    if (item.isIn(ItemTags.FISHES)) {
                        this.angler.addStat(Stats.FISH_CAUGHT, 1);
                    }
                }
                PhaseTracker.getCauseStackManager().popCause();

                i = Math.max(i, 1); // Sponge: Don't lower damage if we've also caught an entity
            }

            if (this.inGround) {
                i = 2;
            }

            this.shadow$remove();
            return i;
        } else {
            return 0;
        }
    }

    @Override
    public void impl$readFromSpongeCompound(CompoundNBT compound) {
        super.impl$readFromSpongeCompound(compound);
        ProjectileSourceSerializer.readSourceFromNbt(compound, ((FishingBobber) this));
    }

    @Override
    public void impl$writeToSpongeCompound(CompoundNBT compound) {
        super.impl$writeToSpongeCompound(compound);
        ProjectileSourceSerializer.writeSourceToNbt(compound, ((FishingBobber) this).shooter().get(), this.angler);
    }
}
