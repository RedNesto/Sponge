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
package org.spongepowered.common.mixin.core.block;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.ImmutableDataManipulator;
import org.spongepowered.api.data.manipulator.immutable.block.ImmutableGrowthData;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.data.ImmutableDataCachingUtil;
import org.spongepowered.common.data.manipulator.immutable.block.ImmutableSpongeGrowthData;

import java.util.Optional;

@Mixin(BlockCrops.class)
public abstract class MixinBlockCrops extends MixinBlock {

    @Shadow protected abstract PropertyInteger getAgeProperty();
    @Shadow public abstract int getMaxAge();
    @Shadow protected abstract int getAge(IBlockState state);
    @Shadow public abstract IBlockState withAge(int age);

    @Override
    public ImmutableList<ImmutableDataManipulator<?, ?>> getManipulators(IBlockState blockState) {
        return ImmutableList.<ImmutableDataManipulator<?, ?>>of(getGrowthData(blockState));
    }

    @Override
    public boolean supports(Class<? extends ImmutableDataManipulator<?, ?>> immutable) {
        return ImmutableGrowthData.class.isAssignableFrom(immutable);
    }

    @Override
    public Optional<BlockState> getStateWithData(IBlockState blockState, ImmutableDataManipulator<?, ?> manipulator) {
        if (manipulator instanceof ImmutableGrowthData) {
            int growth = ((ImmutableGrowthData) manipulator).growthStage().get();
            if (growth > getMaxAge()) {
                growth = getMaxAge();
            }
            return Optional.of((BlockState) blockState.withProperty(getAgeProperty(), growth));
        }
        return super.getStateWithData(blockState, manipulator);
    }

    @Override
    public <E> Optional<BlockState> getStateWithValue(IBlockState blockState, Key<? extends BaseValue<E>> key, E value) {
        if (key.equals(Keys.GROWTH_STAGE)) {
            int growth = (Integer) value;
            if (growth > getMaxAge()) {
                growth = getMaxAge();
            }
            return Optional.of((BlockState) blockState.withProperty(getAgeProperty(), growth));
        }

        return super.getStateWithValue(blockState, key, value);
    }

    private ImmutableGrowthData getGrowthData(IBlockState blockState) {
        return ImmutableDataCachingUtil.getManipulator(ImmutableSpongeGrowthData.class, blockState.getValue(getAgeProperty()), 0, getMaxAge());
    }

    //@Redirect(method = "updateTick", at = @At(value = "INVOKE",
    //        target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Z"))
    //public boolean onUpdateTick(World world, BlockPos pos, IBlockState newState, int flags) {
    //    // TODO find a good default thingy
    //    Transaction<BlockSnapshot> transaction = new Transaction<>(
    //            BlockUtil.fromNative(newState)
    //                    .snapshotFor(new Location<>((org.spongepowered.api.world.World) world, pos.getX(), pos.getY(), pos.getZ())),
    //            BlockUtil.fromNative(world.getBlockState(pos).withProperty(getAgeProperty(), this.getAge(newState)))
    //                    .snapshotFor(new Location<>((org.spongepowered.api.world.World) world, pos.getX(), pos.getY(), pos.getZ())));

    //    ChangeBlockEvent.Modify growEvent = SpongeEventFactory.createChangeBlockEventGrow(Sponge.getCauseStackManager().getCurrentCause(),
    //            ImmutableList.of(transaction));
    //    if (!SpongeImpl.postEvent(growEvent) && transaction.isValid()) {
    //        if(transaction.getCustom().isPresent()) {
    //            BlockSnapshot result = transaction.getCustom().get();
    //            return world.setBlockState(pos, this.withAge(this.getAge(BlockUtil.toNative(result.getState())) + 1), 2);
    //        } else {
    //            return world.setBlockState(pos, this.withAge(this.getAge(newState) + 1), 2);
    //        }
    //    }

    //    return false;
    //}
}
