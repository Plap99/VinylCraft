package com.radig.vinylcraft.network;

import com.radig.vinylcraft.VinylCraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Acción del Discman enviada del cliente al servidor. */
public record DiscmanActionPayload(
        int action,
        long value
) implements CustomPacketPayload {

    public static final int INSERT = 0;
    public static final int EJECT = 1;
    public static final int PLAY_PAUSE = 2;
    public static final int STOP = 3;
    public static final int PREVIOUS = 4;
    public static final int NEXT = 5;
    public static final int SYNC_TICKS = 6;
    public static final int SET_VOLUME = 7;

    public static final Type<DiscmanActionPayload> TYPE =
            new Type<>(VinylCraft.id("discman_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DiscmanActionPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    DiscmanActionPayload::action,
                    ByteBufCodecs.VAR_LONG,
                    DiscmanActionPayload::value,
                    DiscmanActionPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
