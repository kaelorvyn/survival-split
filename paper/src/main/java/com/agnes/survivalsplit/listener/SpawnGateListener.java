package com.agnes.survivalsplit.listener;

import com.agnes.survivalsplit.KaelorvynTitleBridge;
import com.agnes.survivalsplit.gui.SelectionGui;
import com.agnes.survivalsplit.model.SplitMode;
import com.agnes.survivalsplit.model.SplitPlatform;
import com.agnes.survivalsplit.model.SplitProfile;
import com.agnes.survivalsplit.store.ProfileStore;
import com.agnes.survivalsplit.visual.VisualSyncService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class SpawnGateListener implements Listener {

    private final JavaPlugin plugin;
    private final ProfileStore store;
    private final SelectionGui gui;
    private final VisualSyncService visual;

    public SpawnGateListener(JavaPlugin plugin, ProfileStore store, SelectionGui gui, VisualSyncService visual) {
        this.plugin = plugin;
        this.store = store;
        this.gui = gui;
        this.visual = visual;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        int wait = plugin.getConfig().getInt("gui.mod-detect-wait-ticks", 20);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                decideJoin(player);
            }
        }, Math.max(1, wait));
    }

    private void decideJoin(Player player) {
        ReachManager.apply(player, store, plugin.getConfig());
        UUID uuid = player.getUniqueId();
        if (visual.isNeteasePlayer(player)) {
            visual.forceLegacy(player);
            return;
        }
        if (visual.isVisualModClient(uuid)) {
            SplitProfile profile = store.get(uuid);
            if (profile == null || plugin.getConfig().getBoolean("gui.always-show", true)) {
                gui.openGate(player);
            } else {
                visual.onJoin(player);
            }
            return;
        }

        // 握手失败：未装模组，自动按 legacy 电脑战斗玩家处理，不弹窗、无提示。
        visual.markLegacy(player);
        SplitProfile legacy = new SplitProfile(SplitMode.COMBAT, SplitPlatform.PC, true);
        SplitProfile current = store.get(uuid);
        if (current == null || !current.equals(legacy)) {
            store.set(uuid, legacy);
            ReachManager.apply(player, store, plugin.getConfig());
            visual.onFactionChanged(player);
            KaelorvynTitleBridge.refresh(uuid);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        ReachManager.apply(player, store, plugin.getConfig());
        if (gui.isPending(player.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && gui.isPending(player.getUniqueId())) {
                    gui.open(player);
                }
            }, 2L);
        }
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        ReachManager.apply(player, store, plugin.getConfig());
        if (gui.isPending(player.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && gui.isPending(player.getUniqueId())) {
                    gui.open(player);
                }
            }, 2L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        visual.onQuit(event.getPlayer());
    }

    public void reapplyAll() {
        ReachManager.applyAllOnline(plugin, store, plugin.getConfig());
    }
}
