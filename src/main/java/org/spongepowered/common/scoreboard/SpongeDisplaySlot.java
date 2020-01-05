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
package org.spongepowered.common.scoreboard;

import com.google.common.base.MoreObjects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.CatalogKey;
import org.spongepowered.api.scoreboard.displayslot.DisplaySlot;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.common.SpongeCatalogType;

import java.util.Optional;

public final class SpongeDisplaySlot extends SpongeCatalogType implements DisplaySlot {

    @Nullable private TextColor color;
    private int index;

    public SpongeDisplaySlot(CatalogKey key, int index) {
        super(key);
        this.index = index;
    }

    @Override
    public void setTeamColor(@Nullable TextColor color) {
        this.color = color;
    }

    @Override
    public Optional<TextColor> getTeamColor() {
        return Optional.ofNullable(this.color);
    }

    public int getIndex() {
        return this.index;
    }

    @Override
    protected MoreObjects.ToStringHelper toStringHelper() {
        return super.toStringHelper()
            .add("color", this.color);
    }
}
