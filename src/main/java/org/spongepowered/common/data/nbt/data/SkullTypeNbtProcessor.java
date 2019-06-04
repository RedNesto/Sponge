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
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.immutable.*;
import org.spongepowered.api.data.manipulator.mutable.*;
import org.spongepowered.api.data.type.SkullType;
import org.spongepowered.common.data.manipulator.mutable.*;
import org.spongepowered.common.data.nbt.AbstractSpongeNbtProcessor;
import org.spongepowered.common.data.nbt.NbtDataTypes;
import org.spongepowered.common.data.processor.common.SkullUtils;
import org.spongepowered.common.data.type.SpongeSkullType;
import org.spongepowered.common.data.util.NbtDataUtil;

import java.util.Optional;

public class SkullTypeNbtProcessor extends AbstractSpongeNbtProcessor<SkullData, ImmutableSkullData> implements
        NbtDataProcessor<SkullData, ImmutableSkullData> {

    public SkullTypeNbtProcessor() {
        super(NbtDataTypes.TILE_ENTITY);
    }

    @Override
    public boolean isCompatible(NBTTagCompound nbtDataType) {
        return nbtDataType.hasKey(NbtDataUtil.Minecraft.SKULL_TYPE);
    }

    @Override
    public Optional<SkullData> readFrom(NBTTagCompound compound) {
        final NBTBase tag = compound.getTag(NbtDataUtil.Minecraft.SKULL_TYPE);
        if (tag != null) {
            return Optional.of(new SpongeSkullData(SkullUtils.getSkullType(((NBTTagByte) tag).getByte())));
        }
        return Optional.empty();
    }

    @Override
    public Optional<SkullData> readFrom(DataView view) {
        return view.getCatalogType(Keys.SKULL_TYPE.getQuery(), SkullType.class).map(SpongeSkullData::new);
    }

    @Override
    public Optional<NBTTagCompound> storeToCompound(NBTTagCompound compound, SkullData manipulator) {
        compound.setByte(NbtDataUtil.Minecraft.SKULL_TYPE, (byte) (((SpongeSkullType) manipulator.type().get()).getByteId() & 255));
        return Optional.of(compound);
    }

    @Override
    public Optional<DataView> storeToView(DataView view, SkullData manipulator) {
        view.set(Keys.SKULL_TYPE, manipulator.type().get());
        return Optional.of(view);
    }

    @Override
    public DataTransactionResult remove(NBTTagCompound data) {
        return DataTransactionResult.failNoData();
    }

    @Override
    public DataTransactionResult remove(DataView data) {
        return DataTransactionResult.failNoData();
    }
}
