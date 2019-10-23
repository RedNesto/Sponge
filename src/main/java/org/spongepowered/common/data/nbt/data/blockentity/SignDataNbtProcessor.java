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
package org.spongepowered.common.data.nbt.data.blockentity;

import com.google.common.collect.Lists;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.manipulator.immutable.tileentity.ImmutableSignData;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.common.data.manipulator.mutable.tileentity.SpongeSignData;
import org.spongepowered.common.text.SpongeTexts;
import org.spongepowered.common.util.Constants;

import java.util.List;
import java.util.Optional;

public class SignDataNbtProcessor extends AbstractBlockEntityNbtProcessor<SignData, ImmutableSignData> {
    @Override
    public boolean isCompatible(final NBTTagCompound compound) {
        if (compound.hasKey(Constants.Item.BLOCK_ENTITY_TAG)) {
            return hasSignText(compound.getCompoundTag(Constants.Item.BLOCK_ENTITY_TAG));
        }

        return hasSignText(compound);
    }

    private boolean hasSignText(NBTTagCompound compound) {
        return compound.hasKey("Text1") || compound.hasKey("Text2") || compound.hasKey("Text3") || compound.hasKey("Text4");
    }

    @Override
    public Optional<SignData> readFrom(final NBTTagCompound mainCompound) {
        NBTTagCompound tileCompound = mainCompound;
        if (mainCompound.hasKey(Constants.Item.BLOCK_ENTITY_TAG)) {
            tileCompound = mainCompound.getCompoundTag(Constants.Item.BLOCK_ENTITY_TAG);
        }

        if (!hasSignText(tileCompound)) {
            return Optional.empty();
        }

        final List<Text> texts = Lists.newArrayListWithCapacity(4);
        for (int i = 0; i < 4; i++) {
            texts.add(TextSerializers.JSON.deserialize(tileCompound.getString("Text" + (i + 1))));
        }

        return Optional.of(new SpongeSignData(texts));
    }

    @Override
    public Optional<NBTTagCompound> storeToCompound(final NBTTagCompound compound, final SignData manipulator) {
        final ListValue<Text> lines = manipulator.lines();
        for (int i = 0; i < 4; i++) {
            final String line = SpongeTexts.toLegacy(lines.get(i));
            compound.setTag("Text" + (i + 1), new NBTTagString(line));
        }
        return Optional.of(compound);
    }

    @Override
    public DataTransactionResult remove(final NBTTagCompound data) {
        final Optional<SignData> signData = readFrom(data);
        if (signData.isPresent()) {
            for (int i = 0; i < 4; i++) {
                data.removeTag("Text" + (i + 1));
            }
            return DataTransactionResult.successRemove(signData.get().asImmutable().lines());
        }
        return DataTransactionResult.successNoData();
    }
}
