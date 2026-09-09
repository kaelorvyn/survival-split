package com.agnes.survivalsplitclient.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;

public record SplitPlatformPayload(String platform) implements CustomPayload {

    public static final CustomPayload.Id<SplitPlatformPayload> ID =
            new CustomPayload.Id<>(Identifier.of("survivalsplit", "plat"));

    public static final PacketCodec<PacketByteBuf, SplitPlatformPayload> CODEC =
            CustomPayload.<PacketByteBuf, SplitPlatformPayload>codecOf(
                    (payload, buf) -> buf.writeBytes(payload.platform().getBytes(StandardCharsets.UTF_8)),
                    buf -> new SplitPlatformPayload(
                            buf.readSlice(buf.readableBytes()).toString(StandardCharsets.UTF_8)));

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
