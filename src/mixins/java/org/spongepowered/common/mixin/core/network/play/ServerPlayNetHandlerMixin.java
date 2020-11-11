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
package org.spongepowered.common.mixin.core.network.play;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.network.play.client.CTabCompletePacket;
import net.minecraft.network.play.server.STabCompletePacket;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.manager.CommandMapping;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.RotateEntityEvent;
import org.spongepowered.api.util.Transform;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeCommon;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.accessor.network.play.client.CPlayerPacketAccessor;
import org.spongepowered.common.bridge.entity.EntityBridge;
import org.spongepowered.common.bridge.entity.player.ServerPlayerEntityBridge;
import org.spongepowered.common.bridge.network.NetworkManagerHolderBridge;
import org.spongepowered.common.command.manager.SpongeCommandManager;
import org.spongepowered.common.command.registrar.BrigadierBasedRegistrar;
import org.spongepowered.common.command.registrar.BrigadierCommandRegistrar;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.event.tracking.PhaseTracker;
import org.spongepowered.math.vector.Vector3d;

import java.util.Locale;
import java.util.Optional;

@Mixin(ServerPlayNetHandler.class)
public abstract class ServerPlayNetHandlerMixin implements NetworkManagerHolderBridge {

    private static final String[] impl$emptyCommandArray = new String[] { "" };

    @Shadow @Final public NetworkManager netManager;
    @Shadow public ServerPlayerEntity player;
    @Nullable private Vector3d impl$lastMovePosition = null;
    private boolean impl$justTeleported = false;

    @Override
    public NetworkManager bridge$getNetworkManager() {
        return this.netManager;
    }

    @Inject(method = "processTabComplete", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/network/PacketThreadUtil;"
                    + "checkThreadAndEnqueue(Lnet/minecraft/network/IPacket;Lnet/minecraft/network/INetHandler;Lnet/minecraft/world/server/ServerWorld;)V"),
            cancellable = true)
    private void impl$getSuggestionsFromNonBrigCommand(final CTabCompletePacket p_195518_1_, final CallbackInfo ci) {
        final String rawCommand = p_195518_1_.getCommand();
        final String[] command = this.impl$extractCommandString(rawCommand);
        try (final CauseStackManager.StackFrame frame = PhaseTracker.getCauseStackManager().pushCauseFrame()) {
            frame.pushCause(this.player);
            final CommandCause cause = CommandCause.create();
            final SpongeCommandManager manager = ((SpongeCommandManager) Sponge.getCommandManager());
            if (!rawCommand.contains(" ")) {
                final SuggestionsBuilder builder = new SuggestionsBuilder(command[0], 0);
                if (command[0].isEmpty()) {
                    manager.getAliasesForCause(cause).forEach(builder::suggest);
                } else {
                    manager.getAliasesThatStartWithForCause(cause, command[0]).forEach(builder::suggest);
                }
                this.netManager.sendPacket(new STabCompletePacket(p_195518_1_.getTransactionId(), builder.build()));
                ci.cancel();
            } else {
                final Optional<CommandMapping> mappingOptional =
                        manager.getCommandMapping(command[0].toLowerCase(Locale.ROOT)).filter(x -> !(x.getRegistrar() instanceof BrigadierBasedRegistrar));
                if (mappingOptional.isPresent()) {
                    final CommandMapping mapping = mappingOptional.get();
                    if (mapping.getRegistrar().canExecute(cause, mapping)) {
                        try {
                            final SuggestionsBuilder builder = new SuggestionsBuilder(rawCommand, rawCommand.lastIndexOf(" ") + 1);
                            mapping.getRegistrar().suggestions(cause, mapping, command[0], command[1]).forEach(builder::suggest);
                            this.netManager.sendPacket(new STabCompletePacket(p_195518_1_.getTransactionId(), builder.build()));
                        } catch (final CommandException e) {
                            cause.sendMessage(Identity.nil(), Component.text("Unable to create suggestions for your tab completion"));
                            this.netManager.sendPacket(new STabCompletePacket(p_195518_1_.getTransactionId(), Suggestions.empty().join()));
                        }
                    } else {
                        this.netManager.sendPacket(new STabCompletePacket(p_195518_1_.getTransactionId(), Suggestions.empty().join()));
                    }
                    ci.cancel();
                }
            }
        }
    }

    @Redirect(method = "processTabComplete",
        at = @At(value = "INVOKE",
            target = "Lcom/mojang/brigadier/CommandDispatcher;parse(Lcom/mojang/brigadier/StringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/ParseResults;",
            remap = false
        )
    )
    private ParseResults<CommandSource> impl$informParserThisIsASuggestionCheck(final CommandDispatcher<CommandSource> commandDispatcher,
            final StringReader command,
            final Object source) {
        return BrigadierCommandRegistrar.INSTANCE.getDispatcher().parse(command, (CommandSource) source, true);
    }

    private String[] impl$extractCommandString(final String commandString) {
        if (commandString.isEmpty()) {
            return ServerPlayNetHandlerMixin.impl$emptyCommandArray;
        }
        if (commandString.startsWith("/")) {
            return commandString.substring(1).split(" ", 2);
        }
        return commandString.split(" ", 2);
    }

    /**
     * A workaround to resolve <a href="https://bugs.mojang.com/browse/MC-107103">MC-107103</a>
     * since the server will expect the client is trying to interact with the "eyes" of the entity.
     * If the check is desired, {@link #impl$getPlatformReach(double)}
     * is where the seen check should be done.
     */
    @Redirect(
        method = "processUseEntity",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/ServerPlayerEntity;canEntityBeSeen(Lnet/minecraft/entity/Entity;)Z"
        ),
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/entity/player/ServerPlayerEntity;markPlayerActive()V"
            ),
            to = @At(
                value = "CONSTANT",
                args = "doubleValue=36.0D"
            )
        )
    )
    private boolean impl$preventMC_107103(final ServerPlayerEntity serverPlayerEntity, final Entity entityIn) {
        this.impl$targetedEntity = entityIn;
        return true;
    }
    private @Nullable Entity impl$targetedEntity = null;

    /**
     * Specifically hooks the reach distance to use the forge hook.
     */
    @ModifyConstant(
        method = "processUseEntity",
        constant = @Constant(doubleValue = 36.0D)
    )
    private double impl$getPlatformReach(final double thirtySix) {
        final Entity targeted = this.impl$targetedEntity;
        this.impl$targetedEntity = null;
        return SpongeImplHooks.getEntityReachDistanceSq(this.player, targeted);
    }

    @Redirect(method = "processPlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/ServerPlayerEntity;queuedEndExit:Z"))
    private boolean impl$callMoveEvents(final ServerPlayerEntity playerMP, final CPlayerPacket packetIn) {
        if (playerMP.queuedEndExit) {
            return true;
        }
        // If the movement is modified we pretend that the player has queuedEndExit = true
        // so that vanilla wont process that packet further

        final CPlayerPacketAccessor packetInAccessor = (CPlayerPacketAccessor) packetIn;
        // During login, minecraft sends a packet containing neither the 'moving' or 'rotating' flag set - but only once.
        // We don't fire an event to avoid confusing plugins.
        if (!packetInAccessor.accessor$getMoving() && !packetInAccessor.accessor$getRotating()) {
            return false;
        }

        final ServerPlayer player = (ServerPlayer) this.player;
        final ServerPlayerEntityBridge mixinPlayer = (ServerPlayerEntityBridge) this.player;
        final Vector3d fromRotation = player.getRotation();
        // Use the position of the last movement with an event or the current player position if never called
        // We need this because we ignore very small position changes as to not spam as many move events.
        final Vector3d fromPosition = this.impl$lastMovePosition != null ? this.impl$lastMovePosition : player.getPosition();

        Vector3d toPosition = new Vector3d(packetInAccessor.accessor$getX(), packetInAccessor.accessor$getY(), packetInAccessor.accessor$getZ());
        Vector3d toRotation = new Vector3d(packetInAccessor.accessor$getPitch(), packetInAccessor.accessor$getYaw(), 0);

        // If we have zero movement, we have rotation only, we might as well note that now.
        final boolean zeroMovement = !packetInAccessor.accessor$getMoving() || toPosition.equals(fromPosition);

        // Minecraft does the same with rotation when it's only a positional update
        // Branch executed for CPacketPlayer.Position
        boolean fireMoveEvent = packetInAccessor.accessor$getMoving() && !packetInAccessor.accessor$getRotating();
        if (fireMoveEvent) {
            // Correct the new rotation to match the old rotation
            toRotation = fromRotation;
            fireMoveEvent = !zeroMovement && ShouldFire.MOVE_ENTITY_EVENT;
        }

        // Minecraft sends a 0, 0, 0 position when rotation only update occurs, this needs to be recognized and corrected
        // Branch executed for CPacketPlayer.Rotation
        boolean fireRotationEvent = !packetInAccessor.accessor$getMoving() && packetInAccessor.accessor$getRotating();
        if (fireRotationEvent) {
            toPosition = fromPosition; // Rotation only update
            fireRotationEvent = ShouldFire.ROTATE_ENTITY_EVENT;
        }

        // Branch executed for CPacketPlayer.PositionRotation
        if (packetInAccessor.accessor$getMoving() && packetInAccessor.accessor$getRotating()) {
            fireMoveEvent = !zeroMovement && ShouldFire.MOVE_ENTITY_EVENT;
            fireRotationEvent = ShouldFire.ROTATE_ENTITY_EVENT;
        }

        mixinPlayer.bridge$setVelocityOverride(toPosition.sub(fromPosition));

        // These magic numbers are sad but help prevent excessive lag from this event.
        // eventually it would be nice to not have them
        final boolean significantMovement = !zeroMovement && toPosition.distanceSquared(fromPosition)  > ((1f / 16) * (1f / 16));
        final boolean significantRotation = fromRotation.distanceSquared(toRotation) > (.15f * .15f);

        final Transform originalToTransform = Transform.of(toPosition, toRotation);

        // Call move & rotate event as needed...
        if (significantMovement && fireMoveEvent) {
            final MoveEntityEvent event = SpongeEventFactory.createMoveEntityEvent(PhaseTracker.SERVER.getCurrentCause(), player, fromPosition, toPosition, toPosition);
            if (SpongeCommon.postEvent(event)) {
                this.impl$lastMovePosition = event.getOriginalPosition();
                toPosition = fromPosition;
            } else {
                toPosition = event.getDestinationPosition();
            }
        }

        if (significantRotation && fireRotationEvent) {
            final RotateEntityEvent event = SpongeEventFactory.createRotateEntityEvent(PhaseTracker.SERVER.getCurrentCause(), player, fromRotation, toRotation);
            if (SpongeCommon.postEvent(event)) {
                toRotation = fromRotation;
            } else {
                toRotation = event.getToRotation();
            }
        }

        final Transform toTransform = Transform.of(toPosition, toRotation);

        // Handle event results
        if (!toTransform.equals(originalToTransform)) {
            // event changed position or rotation (includes cancel)
            ((EntityBridge) mixinPlayer).bridge$setLocationAndAngles(toTransform);
            this.impl$lastMovePosition = toPosition;
            mixinPlayer.bridge$setVelocityOverride(null);
            return true;
        } else if (!fromPosition.equals(player.getPosition()) && this.impl$justTeleported) {
            // Teleport during move event - prevent vanilla from causing odd behaviors after this
            this.impl$justTeleported = false;
            this.impl$lastMovePosition = player.getLocation().getPosition();
            mixinPlayer.bridge$setVelocityOverride(null);
            return true;
        } else { // Normal movement - Just update lastMovePosition
            this.impl$lastMovePosition = toPosition;
        }
        // TODO ??? this.bridge$resendLatestResourcePackRequest();
        return false; // Continue with vanilla movement processing
    }

}
