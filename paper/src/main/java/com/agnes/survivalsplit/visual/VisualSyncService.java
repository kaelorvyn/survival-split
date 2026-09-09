package com.agnes.survivalsplit.visual;

import com.agnes.survivalsplit.model.SplitMode;
import com.agnes.survivalsplit.model.SplitProfile;
import com.agnes.survivalsplit.model.SplitPlatform;
import com.agnes.survivalsplit.gui.SelectionGui;
import com.agnes.survivalsplit.listener.ReachManager;
import com.agnes.survivalsplit.store.ProfileStore;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class VisualSyncService implements PluginMessageListener {

    public static final String SYNC_CHANNEL = "survivalsplit:sync";
    public static final String HELLO_CHANNEL = "survivalsplit:hello";
    public static final String PLATFORM_CHANNEL = "survivalsplit:plat";

    private static final Gson GSON = new Gson();

    private final JavaPlugin plugin;
    private final ProfileStore store;
    private final Set<UUID> visualModClients = new HashSet<>();
    private final Set<UUID> legacyPlayers = new HashSet<>();
    private final Set<Long> glowingPairs = new HashSet<>();
    private final Map<Integer, UUID> entityIdOwners = new HashMap<>();
    private final Map<UUID, SplitPlatform> detectedPlatforms = new HashMap<>();
    private SelectionGui gui;
    private PacketListenerAbstract metadataListener;
    private boolean packetEventsMissingLogged;

    public VisualSyncService(JavaPlugin plugin, ProfileStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public void start() {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, SYNC_CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, HELLO_CHANNEL, this);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, PLATFORM_CHANNEL, this);
        int refreshTicks = plugin.getConfig().getInt("visual.refresh-ticks", 100);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, Math.max(10, refreshTicks));
        if (hasPacketEvents()) {
            registerMetadataListener();
        }
        recoverOnlinePlayers();
    }

    public void stop() {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin);
        if (metadataListener != null && hasPacketEvents()) {
            PacketEvents.getAPI().getEventManager().unregisterListener(metadataListener);
            metadataListener = null;
        }
        Bukkit.getScheduler().cancelTasks(plugin);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (PLATFORM_CHANNEL.equals(channel)) {
            handlePlatformReport(player, message);
            return;
        }
        if (!HELLO_CHANNEL.equals(channel)) {
            return;
        }
        UUID uuid = player.getUniqueId();
        if (isNeteasePlayer(player)) {
            forceLegacy(player);
            return;
        }
        SplitProfile profile = store.get(uuid);
        boolean wasLegacy = profile != null && profile.legacy();
        legacyPlayers.remove(uuid);
        if (profile == null || profile.legacy()) {
            SplitMode mode = profile != null ? profile.mode() : SplitMode.COMBAT;
            SplitPlatform platform = detectedPlatforms.getOrDefault(uuid, SplitPlatform.PC);
            store.set(uuid, new SplitProfile(mode, platform, false));
            ReachManager.apply(player, store, plugin.getConfig());
        }
        visualModClients.add(uuid);
        entityIdOwners.put(player.getEntityId(), uuid);
        plugin.getLogger().info("已识别视觉模组客户端: " + player.getName());
        removeGlowForViewer(player);
        sendSync(player);
        if (gui != null) {
            gui.onModDetected(player);
            if (wasLegacy) {
                gui.openGate(player);
            }
        }
    }

    public void onJoin(Player player) {
        entityIdOwners.put(player.getEntityId(), player.getUniqueId());
        sendSync(player);
    }

    public void onQuit(Player player) {
        visualModClients.remove(player.getUniqueId());
        legacyPlayers.remove(player.getUniqueId());
        entityIdOwners.remove(player.getEntityId());
        detectedPlatforms.remove(player.getUniqueId());
        removeGlowForViewer(player);
        removeGlowTargeting(player);
    }

    public void onFactionChanged(Player player) {
        entityIdOwners.put(player.getEntityId(), player.getUniqueId());
        removeGlowForViewer(player);
        removeGlowTargeting(player);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (visualModClients.contains(online.getUniqueId())) {
                sendSync(online);
            }
        }
        tick();
    }

    public void setGui(SelectionGui selectionGui) {
        this.gui = selectionGui;
    }

    public boolean isVisualModClient(UUID uuid) {
        return visualModClients.contains(uuid);
    }

    public void markLegacy(Player player) {
        legacyPlayers.add(player.getUniqueId());
    }

    public boolean isNeteasePlayer(Player player) {
        return player != null && isNeteaseName(player.getName());
    }

    public void forceLegacy(Player player) {
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        visualModClients.remove(uuid);
        legacyPlayers.add(uuid);
        SplitProfile legacy = new SplitProfile(SplitMode.COMBAT, SplitPlatform.PC, true);
        if (!legacy.equals(store.get(uuid))) {
            store.set(uuid, legacy);
            ReachManager.apply(player, store, plugin.getConfig());
            onFactionChanged(player);
        }
    }

    static boolean isNeteaseName(String name) {
        return NeteaseNameRules.isGeneratedName(name);
    }

    public SplitPlatform detectedPlatform(UUID uuid) {
        return detectedPlatforms.get(uuid);
    }

    private void handlePlatformReport(Player player, byte[] message) {
        if (isNeteasePlayer(player)) {
            forceLegacy(player);
            return;
        }
        String raw = new String(message, StandardCharsets.UTF_8).trim();
        SplitPlatform platform = switch (raw.toUpperCase()) {
            case "PC" -> SplitPlatform.PC;
            case "MOBILE" -> SplitPlatform.MOBILE;
            default -> null;
        };
        if (platform == null) {
            return;
        }
        detectedPlatforms.put(player.getUniqueId(), platform);
        plugin.getLogger().info("设备检测: " + player.getName() + " -> " + platform.display());

        SplitProfile profile = store.get(player.getUniqueId());
        if (profile != null && profile.platform() != platform) {
            SplitProfile updated = new SplitProfile(profile.mode(), platform);
            store.set(player.getUniqueId(), updated);
            ReachManager.apply(player, store, plugin.getConfig());
            player.sendMessage(ChatColor.YELLOW + "已按检测到的设备更新为" + platform.display() + "玩家。");
        }
    }

    private void registerMetadataListener() {
        metadataListener = new PacketListenerAbstract() {
            @Override
            public void onPacketSend(PacketSendEvent event) {
                if (event.getPacketType() != PacketType.Play.Server.ENTITY_METADATA) {
                    return;
                }
                Player viewer = event.getPlayer();
                if (viewer == null || !visualModClients.contains(viewer.getUniqueId())) {
                    return;
                }
                WrapperPlayServerEntityMetadata wrapper = new WrapperPlayServerEntityMetadata(event);
                UUID target = resolveEntityOwner(wrapper.getEntityId());
                if (target == null) {
                    return;
                }
                if (!isLegacy(target)) {
                    return;
                }
                Player targetPlayer = Bukkit.getPlayer(target);
                if (targetPlayer == null) {
                    return;
                }
                List<EntityData<?>> metadata = wrapper.getEntityMetadata();
                boolean flagsFound = false;
                for (EntityData<?> data : metadata) {
                    if (data.getIndex() == 0
                            && data.getType() == EntityDataTypes.BYTE
                            && data.getValue() instanceof Byte value) {
                        ((EntityData) data).setValue((byte) (value | 0x40));
                        flagsFound = true;
                        event.markForReEncode(true);
                        break;
                    }
                }
                if (!flagsFound) {
                    metadata.add(new EntityData<>(0, EntityDataTypes.BYTE,
                            (byte) (flagsByte(targetPlayer) | 0x40)));
                    event.markForReEncode(true);
                }
            }
        };
        PacketEvents.getAPI().getEventManager().registerListener(metadataListener);
    }

    public void onReload() {
        glowingPairs.clear();
        recoverOnlinePlayers();
        for (Player online : Bukkit.getOnlinePlayers()) {
            sendSync(online);
        }
        tick();
    }

    private void recoverOnlinePlayers() {
        int recovered = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            UUID uuid = online.getUniqueId();
            if (isNeteasePlayer(online)) {
                forceLegacy(online);
                recovered++;
                continue;
            }
            SplitProfile profile = store.get(uuid);
            if (profile == null) {
                continue;
            }
            if (profile.legacy()) {
                legacyPlayers.add(uuid);
            } else {
                visualModClients.add(uuid);
                entityIdOwners.put(online.getEntityId(), uuid);
                sendSync(online);
            }
            recovered++;
        }
        if (recovered > 0) {
            plugin.getLogger().info("已恢复在线玩家分流状态: " + recovered + " 人");
        }
    }

    private void tick() {
        FileConfiguration config = plugin.getConfig();
        boolean legacyGlow = config.getBoolean("visual.legacy-glow", true);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (visualModClients.contains(viewer.getUniqueId())) {
                sendSync(viewer);
            }
            // Glow is sent as vanilla metadata, so legacy observers need it too.
            if (legacyGlow) {
                refreshGlow(viewer);
            }
        }
    }

    private void sendSync(Player viewer) {
        SyncPayload payload = SyncPayload.fromOnline(Bukkit.getOnlinePlayers(), store, legacyPlayers);
        byte[] bytes = encodeString(GSON.toJson(payload));
        viewer.sendPluginMessage(plugin, SYNC_CHANNEL, bytes);
    }

    private static byte[] encodeString(String value) {
        byte[] utf8 = value.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream(utf8.length + 5);
        int length = utf8.length;
        while ((length & ~0x7F) != 0) {
            out.write((length & 0x7F) | 0x80);
            length >>>= 7;
        }
        out.write(length);
        out.write(utf8, 0, utf8.length);
        return out.toByteArray();
    }

    private void refreshGlow(Player viewer) {
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (viewer.getUniqueId().equals(target.getUniqueId())) {
                continue;
            }
            boolean shouldGlow = isLegacy(target.getUniqueId());
            long key = pairKey(viewer, target);
            if (shouldGlow && !glowingPairs.contains(key)) {
                sendGlow(viewer, target);
                glowingPairs.add(key);
                plugin.getLogger().info("发光描边: " + viewer.getName() + " 看见 " + target.getName());
            } else if (!shouldGlow && glowingPairs.contains(key)) {
                sendRemoveGlow(viewer, target);
                glowingPairs.remove(key);
                plugin.getLogger().info("移除发光描边: " + viewer.getName() + " 看见 " + target.getName());
            }
        }
    }

    private void sendGlow(Player viewer, Player target) {
        if (!hasPacketEvents()) {
            return;
        }
        byte flags = (byte) (flagsByte(target) | 0x40);
        WrapperPlayServerEntityMetadata metadata = new WrapperPlayServerEntityMetadata(
                target.getEntityId(),
                java.util.List.of(new EntityData<>(0, EntityDataTypes.BYTE, flags)));
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, metadata);
    }

    private void sendRemoveGlow(Player viewer, Player target) {
        if (!hasPacketEvents()) {
            return;
        }
        byte flags = (byte) (flagsByte(target) & ~0x40);
        WrapperPlayServerEntityMetadata metadata = new WrapperPlayServerEntityMetadata(
                target.getEntityId(),
                java.util.List.of(new EntityData<>(0, EntityDataTypes.BYTE, flags)));
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, metadata);
    }

    private static byte flagsByte(Player target) {
        byte flags = 0;
        if (target.getFireTicks() > 0 || target.isVisualFire()) {
            flags |= 0x01;
        }
        if (target.isSneaking()) {
            flags |= 0x02;
        }
        if (target.isSprinting()) {
            flags |= 0x08;
        }
        if (target.isSwimming()) {
            flags |= 0x10;
        }
        if (target.isInvisible()) {
            flags |= 0x20;
        }
        if (target.isGlowing()) {
            flags |= 0x40;
        }
        if (target.isGliding()) {
            flags |= 0x80;
        }
        return flags;
    }

    private void removeGlowForViewer(Player viewer) {
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (viewer.getUniqueId().equals(target.getUniqueId())) {
                continue;
            }
            long key = pairKey(viewer, target);
            if (glowingPairs.remove(key)) {
                sendRemoveGlow(viewer, target);
            }
        }
    }

    private void removeGlowTargeting(Player target) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.getUniqueId().equals(target.getUniqueId())) {
                continue;
            }
            long key = pairKey(viewer, target);
            if (glowingPairs.remove(key)) {
                sendRemoveGlow(viewer, target);
            }
        }
    }

    private boolean hasPacketEvents() {
        boolean present = Bukkit.getPluginManager().getPlugin("packetevents") != null;
        if (!present && !packetEventsMissingLogged) {
            packetEventsMissingLogged = true;
            plugin.getLogger().warning("未找到 packetevents，无分流玩家白色发光标记已禁用（客户端模组同步仍可用）。");
        }
        return present;
    }

    private static long pairKey(Player viewer, Player target) {
        return ((long) viewer.getEntityId()) << 32 | (target.getEntityId() & 0xffffffffL);
    }

    private UUID resolveEntityOwner(int entityId) {
        UUID mapped = entityIdOwners.get(entityId);
        Player mappedPlayer = mapped == null ? null : Bukkit.getPlayer(mapped);
        if (mappedPlayer != null && mappedPlayer.getEntityId() == entityId) {
            return mapped;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getEntityId() == entityId) {
                entityIdOwners.put(entityId, online.getUniqueId());
                return online.getUniqueId();
            }
        }
        entityIdOwners.remove(entityId);
        return null;
    }

    private boolean isLegacy(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        Player online = Bukkit.getPlayer(uuid);
        if (online != null && isNeteasePlayer(online)) {
            return true;
        }
        SplitProfile profile = store.get(uuid);
        return legacyPlayers.contains(uuid) || (profile != null && profile.legacy());
    }
}
