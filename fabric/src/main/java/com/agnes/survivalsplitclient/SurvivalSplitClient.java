package com.agnes.survivalsplitclient;

import com.agnes.survivalsplitclient.network.SplitHelloPayload;
import com.agnes.survivalsplitclient.network.SplitModListPayload;
import com.agnes.survivalsplitclient.network.SplitPlatformPayload;
import com.agnes.survivalsplitclient.network.SplitSyncPayload;
import com.agnes.survivalsplitclient.config.SplitConfig;
import com.google.gson.Gson;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;

public final class SurvivalSplitClient implements ClientModInitializer {

    private static final Gson GSON = new Gson();

    @Override
    public void onInitializeClient() {
        if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
            AutoConfig.register(SplitConfig.class, GsonConfigSerializer::new);
        }

        PayloadTypeRegistry.playS2C().register(SplitSyncPayload.ID, SplitSyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SplitHelloPayload.ID, SplitHelloPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SplitPlatformPayload.ID, SplitPlatformPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SplitModListPayload.ID, SplitModListPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(SplitSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> FactionStore.INSTANCE.loadJson(payload.json())));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            FactionStore.INSTANCE.clear();
            EntityIdIndex.clear();
            ClientPlayNetworking.send(new SplitHelloPayload());
            ClientPlayNetworking.send(new SplitPlatformPayload(PlatformDetector.detect()));
            List<ModInfo> mods = FabricLoader.getInstance().getAllMods().stream()
                    .map(ModInfo::from)
                    .toList();
            ClientPlayNetworking.send(new SplitModListPayload(GSON.toJson(mods)));
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            FactionStore.INSTANCE.clear();
            EntityIdIndex.clear();
        });
    }
}
