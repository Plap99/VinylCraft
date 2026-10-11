package com.radig.vinylcraft.component;

import com.mojang.serialization.Codec;

import com.radig.vinylcraft.VinylCraft;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

public final class ModDataComponents {

    public static final DataComponentType<String> ALBUM_ID =
            Registry.register(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    VinylCraft.id("album_id"),
                    DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(
                                    ByteBufCodecs.STRING_UTF8
                            )
                            .build()
            );


    public static final DataComponentType<String> ALBUM_TRACK_DURATIONS =
            Registry.register(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    VinylCraft.id("album_track_durations"),
                    DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(
                                    ByteBufCodecs.STRING_UTF8
                            )
                            .build()
            );

    public static final DataComponentType<Integer> ALBUM_WALL_SIZE =
            Registry.register(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    VinylCraft.id("album_wall_size"),
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.intRange(1, 10))
                            .networkSynchronized(
                                    ByteBufCodecs.VAR_INT
                            )
                            .build()
            );



    public static final DataComponentType<Boolean> DISCMAN_EQUIPPED =
            Registry.register(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    VinylCraft.id("discman_equipped"),
                    DataComponentType.<Boolean>builder()
                            .persistent(Codec.BOOL)
                            .networkSynchronized(ByteBufCodecs.BOOL)
                            .build()
            );

    public static final DataComponentType<Integer> DISCMAN_STATE =
            Registry.register(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    VinylCraft.id("discman_state"),
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.intRange(0, 2))
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .build()
            );

    public static final DataComponentType<Long> DISCMAN_PLAYBACK_TICKS =
            Registry.register(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    VinylCraft.id("discman_playback_ticks"),
                    DataComponentType.<Long>builder()
                            .persistent(Codec.LONG)
                            .networkSynchronized(ByteBufCodecs.VAR_LONG)
                            .build()
            );

    public static final DataComponentType<Integer> DISCMAN_VOLUME =
            Registry.register(
                    BuiltInRegistries.DATA_COMPONENT_TYPE,
                    VinylCraft.id("discman_volume"),
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.intRange(0, 100))
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .build()
            );

    private ModDataComponents() {
    }

    public static void registerModDataComponents() {

        VinylCraft.LOGGER.info(
                "Registering VinylCraft data components..."
        );
    }
}