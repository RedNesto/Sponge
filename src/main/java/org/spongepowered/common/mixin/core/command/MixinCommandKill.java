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
package org.spongepowered.common.mixin.core.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandKill;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(CommandKill.class)
public abstract class MixinCommandKill extends CommandBase {

    /**
     * @author RedNesto - January 29th, 2018
     * @reason checks if the entity has been killed before sending a success message
     */
    @Overwrite
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            EntityPlayer entityplayer = getCommandSenderAsPlayer(sender);
            entityplayer.onKillCommand();
            // Sponge: check if the entity has been really killed.
            if (entityplayer.isDead || entityplayer.dead) {
                notifyCommandListener(sender, this, "commands.kill.successful", entityplayer.getDisplayName());
            }
        } else {
            Entity entity = getEntity(server, sender, args[0]);
            entity.onKillCommand();
            // Sponge: check if the entity has been really killed.
            // Yeah, EntityLivingBase#dead is a thing; Acts like a mirror of Entity#isDead and is used where Entity#isDead should be used, what a
            // odd field Mojang.
            if (entity.isDead || (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).dead)) {
                notifyCommandListener(sender, this, "commands.kill.successful", entity.getDisplayName());
            }
        }
    }
}
