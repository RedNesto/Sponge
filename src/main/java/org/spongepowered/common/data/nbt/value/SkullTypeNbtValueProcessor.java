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
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.SkullType;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.common.data.nbt.NbtDataTypes;
import org.spongepowered.common.data.processor.common.SkullUtils;
import org.spongepowered.common.data.type.SpongeSkullType;
import org.spongepowered.common.data.util.NbtDataUtil;
import org.spongepowered.common.data.value.immutable.ImmutableSpongeValue;
import org.spongepowered.common.data.value.mutable.SpongeValue;

import java.util.Optional;

public class SkullTypeNbtValueProcessor extends AbstractSpongeNbtValueProcessor<SkullType, Value<SkullType>> implements
        NbtValueProcessor<SkullType, Value<SkullType>> {

    public SkullTypeNbtValueProcessor() {
        super(NbtDataTypes.TILE_ENTITY);
    }

    @Override
    public Optional<Value<SkullType>> readFrom(NBTTagCompound compound) {
        return readValue(compound).map(value -> new SpongeValue<>(Keys.SKULL_TYPE, SkullUtils.DEFAULT_TYPE, value));
    }

    @Override
    public Optional<Value<SkullType>> readFrom(DataView view) {
        return readValue(view).map(value -> new SpongeValue<>(Keys.SKULL_TYPE, SkullUtils.DEFAULT_TYPE, value));
    }

    @Override
    public Optional<SkullType> readValue(NBTTagCompound compound) {
        final NBTBase tag = compound.getTag(NbtDataUtil.Minecraft.SKULL_TYPE);
        if (tag != null) {
            return Optional.of(SkullUtils.getSkullType(((NBTTagByte) tag).getByte()));
        }
        return Optional.empty();
    }

    @Override
    public Optional<SkullType> readValue(DataView view) {
        return view.getCatalogType(Keys.SKULL_TYPE.getQuery(), SkullType.class);
    }

    @Override
    public DataTransactionResult offer(NBTTagCompound compound, SkullType value) {
        Optional<SkullType> oldValue = readValue(compound);
        compound.setByte(NbtDataUtil.Minecraft.SKULL_TYPE, ((SpongeSkullType) value).getByteId());

        ImmutableSpongeValue<SkullType> resultValue = new ImmutableSpongeValue<>(Keys.SKULL_TYPE, SkullUtils.DEFAULT_TYPE, value);
        if (oldValue.isPresent()) {
            ImmutableSpongeValue<SkullType> oldImmutableVal = new ImmutableSpongeValue<>(Keys.SKULL_TYPE, SkullUtils.DEFAULT_TYPE, oldValue.get());
            return DataTransactionResult.successReplaceResult(resultValue, oldImmutableVal);
        } else {
            return DataTransactionResult.successResult(resultValue);
        }
    }

    @Override
    public DataTransactionResult offer(DataView view, SkullType value) {
        Optional<SkullType> oldValue = readValue(view);
        view.set(Keys.SKULL_TYPE.getQuery(), ((SpongeSkullType) value).getByteId());

        ImmutableSpongeValue<SkullType> resultValue = new ImmutableSpongeValue<>(Keys.SKULL_TYPE, SkullUtils.DEFAULT_TYPE, value);
        if (oldValue.isPresent()) {
            ImmutableSpongeValue<SkullType> oldImmutableVal = new ImmutableSpongeValue<>(Keys.SKULL_TYPE, SkullUtils.DEFAULT_TYPE, oldValue.get());
            return DataTransactionResult.successReplaceResult(resultValue, oldImmutableVal);
        } else {
            return DataTransactionResult.successResult(resultValue);
        }

    }

    @Override
    public DataTransactionResult remove(NBTTagCompound compound) {
        return DataTransactionResult.failNoData();
    }

    @Override
    public DataTransactionResult remove(DataView view) {
        return DataTransactionResult.failNoData();
    }
}
