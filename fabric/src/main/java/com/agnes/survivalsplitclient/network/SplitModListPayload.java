package com.agnes.survivalsplitclient.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;

public record SplitModListPayload(String json) implements CustomPayload {

    public static final CustomPayload.Id<SplitModListPayload> ID =
            new CustomPayload.Id<>(Identifier.of("survivalsplit", "modlist"));

    public static final PacketCodec<PacketByteBuf, SplitModListPayload> CODEC =
            CustomPayload.<PacketByteBuf, SplitModListPayload>codecOf(
                    (payload, buf) -> buf.writeBytes(payload.json().getBytes(StandardCharsets.UTF_8)),
                    buf -> new SplitModListPayload(
                            buf.readSlice(buf.readableBytes()).toString(StandardCharsets.UTF_8)));

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
