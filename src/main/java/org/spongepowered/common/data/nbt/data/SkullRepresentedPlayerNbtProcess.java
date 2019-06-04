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
package org.spongepowered.common.data.nbt.data;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntitySkull;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.*;
import org.spongepowered.api.data.manipulator.mutable.*;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.common.data.builder.authlib.SpongeGameProfileBuilder;
import org.spongepowered.common.data.manipulator.mutable.*;
import org.spongepowered.common.data.nbt.AbstractSpongeNbtProcessor;
import org.spongepowered.common.data.nbt.NbtDataTypes;
import org.spongepowered.common.data.util.DataQueries;
import org.spongepowered.common.data.util.NbtDataUtil;

import java.util.Optional;

public class SkullRepresentedPlayerNbtProcess extends AbstractSpongeNbtProcessor<RepresentedPlayerData, ImmutableRepresentedPlayerData> {

    public SkullRepresentedPlayerNbtProcess() {
        super(NbtDataTypes.TILE_ENTITY);
    }

    @Override
    public boolean isCompatible(NBTTagCompound nbtDataType) {
        return nbtDataType.hasKey(NbtDataUtil.Minecraft.SKULL_TYPE);
    }

    @Override
    public Optional<RepresentedPlayerData> readFrom(NBTTagCompound compound) {
        final NBTBase tag = compound.getTag(NbtDataUtil.Minecraft.SKULL_OWNER);
        if (tag != null) {
            GameProfile profile = (GameProfile) NBTUtil.readGameProfileFromNBT((NBTTagCompound) tag);
            if (profile != null) {
                return Optional.of(new SpongeRepresentedPlayerData(profile));
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<RepresentedPlayerData> readFrom(DataView view) {
        Optional<GameProfile> profile = new SpongeGameProfileBuilder().build(view);
        return profile.map(SpongeRepresentedPlayerData::new);
    }

    @Override
    public Optional<NBTTagCompound> storeToCompound(NBTTagCompound compound, RepresentedPlayerData manipulator) {
        final NBTBase skullTypeTag = compound.getTag(NbtDataUtil.Minecraft.SKULL_TYPE);
        if (skullTypeTag == null || ((NBTTagByte) skullTypeTag).getByte() != 3) { // 3 is SkullTypes.PLAYER
            return Optional.of(compound);
        }

        GameProfile profile = manipulator.owner().get();
        if (SpongeRepresentedPlayerData.NULL_PROFILE.equals(profile)) {
            remove(compound);
            return Optional.of(compound);
        }

        NBTTagCompound ownerTag = new NBTTagCompound();
        NBTUtil.writeGameProfile(ownerTag, TileEntitySkull.updateGameProfile((com.mojang.authlib.GameProfile) profile));
        compound.setTag(NbtDataUtil.Minecraft.SKULL_OWNER, ownerTag);
        return Optional.of(compound);
    }

    @Override
    public Optional<DataView> storeToView(DataView view, RepresentedPlayerData manipulator) {
        Optional<Byte> skullType = view.getByte(DataQueries.SKULL_TYPE);
        if (!skullType.isPresent() || skullType.get() != 3) {
            return Optional.of(view);
        }

        GameProfile profile = manipulator.owner().get();
        if (SpongeRepresentedPlayerData.NULL_PROFILE.equals(profile)) {
            remove(view);
            return Optional.of(view);
        }

        view.set(Keys.REPRESENTED_PLAYER.getQuery(), TileEntitySkull.updateGameProfile((com.mojang.authlib.GameProfile) profile));
        return Optional.of(view);
    }

    @Override
    public DataTransactionResult remove(NBTTagCompound compound) {
        Optional<RepresentedPlayerData> oldValue = readFrom(compound);
        compound.removeTag(NbtDataUtil.Minecraft.SKULL_OWNER);

        if (oldValue.isPresent()) {
            return DataTransactionResult.successRemove(oldValue.get().owner().asImmutable());
        } else {
            return DataTransactionResult.successNoData();
        }
    }

    @Override
    public DataTransactionResult remove(DataView view) {
        Optional<RepresentedPlayerData> oldValue = readFrom(view);
        view.remove(Keys.REPRESENTED_PLAYER.getQuery());

        if (oldValue.isPresent()) {
            return DataTransactionResult.successRemove(oldValue.get().owner().asImmutable());
        } else {
            return DataTransactionResult.successNoData();
        }
    }
}
