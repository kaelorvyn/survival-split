package com.agnes.survivalsplitclient.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SplitSyncPayload(String json) implements CustomPayload {

    public static final CustomPayload.Id<SplitSyncPayload> ID =
            new CustomPayload.Id<>(Identifier.of("survivalsplit", "sync"));

    public static final PacketCodec<PacketByteBuf, SplitSyncPayload> CODEC =
            CustomPayload.<PacketByteBuf, SplitSyncPayload>codecOf(
                    (payload, buf) -> buf.writeString(payload.json()),
                    buf -> new SplitSyncPayload(buf.readString()));

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

}
