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
package org.spongepowered.common.data.nbt.value;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntitySkull;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.common.data.manipulator.mutable.*;
import org.spongepowered.common.data.nbt.NbtDataTypes;
import org.spongepowered.common.data.processor.common.SkullUtils;
import org.spongepowered.common.data.util.DataQueries;
import org.spongepowered.common.data.util.NbtDataUtil;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeValue;
import org.spongepowered.common.data.value.mutable.SpongeValue;

import java.util.Optional;

public class SkullRepresentedPlayerNbtValueProcessor extends AbstractSpongeNbtValueProcessor<GameProfile, Value<GameProfile>> {

    public SkullRepresentedPlayerNbtValueProcessor() {
        super(NbtDataTypes.TILE_ENTITY);
    }

    @Override
    public Optional<Value<GameProfile>> readFrom(NBTTagCompound compound) {
        return readValue(compound).map(value -> new SpongeValue<>(Keys.REPRESENTED_PLAYER, SpongeRepresentedPlayerData.NULL_PROFILE, value));
    }

    @Override
    public Optional<Value<GameProfile>> readFrom(DataView view) {
        return readValue(view).map(value -> new SpongeValue<>(Keys.REPRESENTED_PLAYER, SpongeRepresentedPlayerData.NULL_PROFILE, value));
    }

    @Override
    public Optional<GameProfile> readValue(NBTTagCompound compound) {
        final NBTBase tag = compound.getTag(NbtDataUtil.Minecraft.SKULL_OWNER);
        if (tag != null) {
            return Optional.ofNullable((GameProfile) NBTUtil.readGameProfileFromNBT((NBTTagCompound) tag));
        }
        return Optional.empty();
    }

    @Override
    public Optional<GameProfile> readValue(DataView view) {
        return view.getView(DataQueries.SKULL_OWNER)
                .flatMap(SkullUtils::readProfileFrom);
    }

    @Override
    public DataTransactionResult offer(NBTTagCompound compound, GameProfile value) {
        final NBTBase skullTypeTag = compound.getTag(NbtDataUtil.Minecraft.SKULL_TYPE);
        if (skullTypeTag == null || ((NBTTagByte) skullTypeTag).getByte() != 3) { // 3 is SkullTypes.PLAYER
            return DataTransactionResult.failResult(new ImmutableSpongeValue<>(Keys.REPRESENTED_PLAYER, SpongeRepresentedPlayerData.NULL_PROFILE, value));
        }

        if (SpongeRepresentedPlayerData.NULL_PROFILE.equals(value)) {
            return remove(compound);
        }

        Optional<GameProfile> oldValue = readValue(compound);
        NBTTagCompound ownerTag = new NBTTagCompound();
        NBTUtil.writeGameProfile(ownerTag, TileEntitySkull.updateGameProfile((com.mojang.authlib.GameProfile) value));
        compound.setTag(NbtDataUtil.Minecraft.SKULL_OWNER, ownerTag);

        ImmutableSpongeValue<GameProfile> resultValue = new ImmutableSpongeValue<>(Keys.REPRESENTED_PLAYER, SpongeRepresentedPlayerData.NULL_PROFILE, value);
        if (oldValue.isPresent()) {
            ImmutableSpongeValue<GameProfile> oldImmutableVal = new ImmutableSpongeValue<>(Keys.REPRESENTED_PLAYER, SpongeRepresentedPlayerData.NULL_PROFILE, oldValue.get());
            return DataTransactionResult.successReplaceResult(resultValue, oldImmutableVal);
        } else {
            return DataTransactionResult.successResult(resultValue);
        }
    }

    @Override
    public DataTransactionResult offer(DataView view, GameProfile value) {
        Optional<Byte> skullType = view.getByte(DataQueries.SKULL_TYPE);
        if (!skullType.isPresent() || skullType.get() != 3) {
            return DataTransactionResult.failResult(new ImmutableSpongeValue<>(Keys.REPRESENTED_PLAYER, SpongeRepresentedPlayerData.NULL_PROFILE, value));
        }

        if (SpongeRepresentedPlayerData.NULL_PROFILE.equals(value)) {
            return remove(view);
        }

        Optional<GameProfile> oldValue = readValue(view);
        SkullUtils.writeProfileTo(view.createView(DataQueries.SKULL_OWNER), value);

        ImmutableSpongeValue<GameProfile> resultValue = new ImmutableSpongeValue<>(Keys.REPRESENTED_PLAYER, SpongeRepresentedPlayerData.NULL_PROFILE, value);
        if (oldValue.isPresent()) {
            ImmutableSpongeValue<GameProfile> oldImmutableVal = new ImmutableSpongeValue<>(Keys.REPRESENTED_PLAYER, SpongeRepresentedPlayerData.NULL_PROFILE, oldValue.get());
            return DataTransactionResult.successReplaceResult(resultValue, oldImmutableVal);
        } else {
            return DataTransactionResult.successResult(resultValue);
        }
    }

    @Override
    public DataTransactionResult remove(NBTTagCompound compound) {
        Optional<GameProfile> oldValue = readValue(compound);
        compound.removeTag(NbtDataUtil.Minecraft.SKULL_OWNER);

        if (oldValue.isPresent()) {
            return DataTransactionResult.successRemove(new ImmutableSpongeValue<>(Keys.REPRESENTED_PLAYER, SpongeRepresentedPlayerData.NULL_PROFILE, oldValue.get()));
        } else {
            return DataTransactionResult.successNoData();
        }
    }

    @Override
    public DataTransactionResult remove(DataView view) {
        Optional<GameProfile> oldValue = readValue(view);
        view.remove(DataQueries.SKULL_OWNER);

        if (oldValue.isPresent()) {
            return DataTransactionResult.successRemove(new ImmutableSpongeValue<>(Keys.REPRESENTED_PLAYER, SpongeRepresentedPlayerData.NULL_PROFILE, oldValue.get()));
        } else {
            return DataTransactionResult.successNoData();
        }
    }
}
