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

import com.google.common.collect.ImmutableList;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.common.data.nbt.NbtDataType;

import java.util.Optional;

public class SpongeNbtValueProcessorDelegate<E, V extends BaseValue<E>> implements NbtValueProcessor<E, V> {

    private final ImmutableList<NbtValueProcessor<E, V>> processors;
    private final NbtDataType nbtDataType;

    public SpongeNbtValueProcessorDelegate(ImmutableList<NbtValueProcessor<E, V>> processors, NbtDataType nbtDataType) {
        this.processors = processors;
        this.nbtDataType = nbtDataType;
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

    @Override
    public NbtDataType getTargetType() {
        return this.nbtDataType;
    }

    @Override
    public boolean isCompatible(NbtDataType nbtDataType) {
        for (NbtValueProcessor<E, V> processor : this.processors) {
            if (processor.isCompatible(nbtDataType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Optional<V> readFrom(NBTTagCompound compound) {
        for (NbtValueProcessor<E, V> processor : this.processors) {
            final Optional<V> returnVal = processor.readFrom(compound);
            if (returnVal.isPresent()) {
                return returnVal;
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<V> readFrom(DataView view) {
        for (NbtValueProcessor<E, V> processor : this.processors) {
            final Optional<V> returnVal = processor.readFrom(view);
            if (returnVal.isPresent()) {
                return returnVal;
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<E> readValue(NBTTagCompound compound) {
        for (NbtValueProcessor<E, V> processor : this.processors) {
            final Optional<E> returnVal = processor.readValue(compound);
            if (returnVal.isPresent()) {
                return returnVal;
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<E> readValue(DataView view) {
        for (NbtValueProcessor<E, V> processor : this.processors) {
            final Optional<E> returnVal = processor.readValue(view);
            if (returnVal.isPresent()) {
                return returnVal;
            }
        }
        return Optional.empty();
    }

    @Override
    public DataTransactionResult offer(NBTTagCompound compound, E value) {
        for (NbtValueProcessor<E, V> processor : this.processors) {
            final DataTransactionResult returnVal = processor.offer(compound, value);
            if (returnVal.isSuccessful()) {
                return returnVal;
            }
        }
        return DataTransactionResult.failNoData();
    }

    @Override
    public DataTransactionResult offer(DataView view, E value) {
        for (NbtValueProcessor<E, V> processor : this.processors) {
            final DataTransactionResult returnVal = processor.offer(view, value);
            if (returnVal.isSuccessful()) {
                return returnVal;
            }
        }
        return DataTransactionResult.failNoData();
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
