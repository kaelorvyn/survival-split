package com.agnes.survivalsplitclient.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SplitHelloPayload() implements CustomPayload {

    public static final CustomPayload.Id<SplitHelloPayload> ID =
            new CustomPayload.Id<>(Identifier.of("survivalsplit", "hello"));

    public static final PacketCodec<PacketByteBuf, SplitHelloPayload> CODEC =
            CustomPayload.<PacketByteBuf, SplitHelloPayload>codecOf((payload, buf) -> {
            }, buf -> new SplitHelloPayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
