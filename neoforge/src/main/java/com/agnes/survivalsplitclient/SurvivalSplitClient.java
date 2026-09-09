package com.agnes.survivalsplitclient;

import com.agnes.survivalsplitclient.network.SplitPayloads;
import com.google.gson.Gson;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.List;

@Mod("survivalsplitclient")
public final class SurvivalSplitClient {
    private static final Gson GSON = new Gson();

    public SurvivalSplitClient() {
    }

    @EventBusSubscriber(modid = "survivalsplitclient")
    public static final class ModEvents {
        @SubscribeEvent
        public static void registerPayloads(RegisterPayloadHandlersEvent event) {
            var registrar = event.registrar("1");
            registrar.playToServer(SplitPayloads.Hello.TYPE, SplitPayloads.Hello.CODEC, (payload, context) -> {});
            registrar.playToServer(SplitPayloads.Platform.TYPE, SplitPayloads.Platform.CODEC, (payload, context) -> {});
            registrar.playToServer(SplitPayloads.ModList.TYPE, SplitPayloads.ModList.CODEC, (payload, context) -> {});
            registrar.playToClient(SplitPayloads.Sync.TYPE, SplitPayloads.Sync.CODEC, (payload, context) -> {});
        }

        @SubscribeEvent
        public static void registerClientPayloads(RegisterClientPayloadHandlersEvent event) {
            event.register(SplitPayloads.Sync.TYPE, (payload, context) -> context.enqueueWork(() ->
                    FactionStore.INSTANCE.loadJson(payload.value())));
        }
    }

    @EventBusSubscriber(modid = "survivalsplitclient", value = Dist.CLIENT)
    public static final class ClientEvents {
        @SubscribeEvent
        public static void loggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
            FactionStore.INSTANCE.clear();
            EntityIdIndex.clear();
            ClientPacketDistributor.sendToServer(new SplitPayloads.Hello());
            ClientPacketDistributor.sendToServer(new SplitPayloads.Platform(PlatformDetector.detect()));
            List<ModInfo> mods = ModList.get().getMods().stream().map(meta -> {
                String description = meta.getDescription();
                if (description.length() > 200) description = description.substring(0, 200);
                return new ModInfo(meta.getModId(), meta.getDisplayName(), meta.getVersion().toString(), description);
            }).toList();
            ClientPacketDistributor.sendToServer(new SplitPayloads.ModList(GSON.toJson(mods)));
        }

        @SubscribeEvent
        public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            FactionStore.INSTANCE.clear();
            EntityIdIndex.clear();
        }
    }
}
