package com.radig.vinylcraft.network;

import com.radig.vinylcraft.VinylCraft;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record StartVinylRecordingPayload(
        BlockPos pos,
        String albumId,
        String trackDurations
) implements CustomPacketPayload {

    public static final Type<StartVinylRecordingPayload> TYPE =
            new Type<>(
                    VinylCraft.id("start_vinyl_recording")
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            StartVinylRecordingPayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    StartVinylRecordingPayload::pos,
                    ByteBufCodecs.STRING_UTF8,
                    StartVinylRecordingPayload::albumId,
                    ByteBufCodecs.STRING_UTF8,
                    StartVinylRecordingPayload::trackDurations,
                    StartVinylRecordingPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
