package com.agnes.survivalsplitclient.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class SplitPayloads {
    private static final String NS = "survivalsplit";
    private static StreamCodec<RegistryFriendlyByteBuf, String> textCodec() {
        return StreamCodec.of((buf, value) -> buf.writeUtf(value), buf -> buf.readUtf(32767));
    }
    public record Hello() implements CustomPacketPayload {
        public static final Type<Hello> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NS, "hello"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Hello> CODEC = StreamCodec.unit(new Hello());
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Platform(String value) implements CustomPacketPayload {
        public static final Type<Platform> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NS, "plat"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Platform> CODEC = textCodec().map(Platform::new, Platform::value);
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record ModList(String value) implements CustomPacketPayload {
        public static final Type<ModList> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NS, "modlist"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ModList> CODEC = textCodec().map(ModList::new, ModList::value);
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Sync(String value) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NS, "sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC = textCodec().map(Sync::new, Sync::value);
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    private SplitPayloads() {}
}
