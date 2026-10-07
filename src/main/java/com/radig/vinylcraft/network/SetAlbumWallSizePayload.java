package com.radig.vinylcraft.network;

import com.radig.vinylcraft.VinylCraft;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetAlbumWallSizePayload(
        BlockPos pos,
        int size
) implements CustomPacketPayload {

    public static final Type<SetAlbumWallSizePayload> TYPE =
            new Type<>(VinylCraft.id("set_album_wall_size"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetAlbumWallSizePayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    SetAlbumWallSizePayload::pos,
                    ByteBufCodecs.VAR_INT,
                    SetAlbumWallSizePayload::size,
                    SetAlbumWallSizePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
