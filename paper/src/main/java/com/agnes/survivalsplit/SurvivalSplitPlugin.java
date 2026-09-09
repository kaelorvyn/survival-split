package com.agnes.survivalsplit;

import com.agnes.survivalsplit.command.SplitCommand;
import com.agnes.survivalsplit.gui.SelectionGui;
import com.agnes.survivalsplit.listener.DamageIsolationListener;
import com.agnes.survivalsplit.listener.SpawnGateListener;
import com.agnes.survivalsplit.modaudit.ModAuditService;
import com.agnes.survivalsplit.modaudit.ModAuditStore;
import com.agnes.survivalsplit.store.ProfileStore;
import com.agnes.survivalsplit.visual.VisualSyncService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SurvivalSplitPlugin extends JavaPlugin {

    private ProfileStore profileStore;
    private VisualSyncService visualSyncService;
    private SpawnGateListener spawnGateListener;
    private ModAuditService modAuditService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        profileStore = new ProfileStore(this);
        modAuditService = new ModAuditService(this, new ModAuditStore(this));
        modAuditService.start();
        visualSyncService = new VisualSyncService(this, profileStore);
        visualSyncService.start();

        SelectionGui selectionGui = new SelectionGui(this, profileStore, visualSyncService);
        visualSyncService.setGui(selectionGui);
        spawnGateListener = new SpawnGateListener(this, profileStore, selectionGui, visualSyncService);
        new DamageIsolationListener(this, profileStore);

        PluginCommand command = getCommand("split");
        if (command != null) {
            command.setExecutor(new SplitCommand(this, profileStore, selectionGui,
                    spawnGateListener, visualSyncService, modAuditService));
        }

        getLogger().info("SurvivalSplit 已启用：和平/战斗 × 电脑/手机");
    }

    @Override
    public void onDisable() {
        if (profileStore != null) {
            profileStore.save();
        }
        if (visualSyncService != null) {
            visualSyncService.stop();
        }
        if (modAuditService != null) {
            modAuditService.stop();
        }
    }

    public ProfileStore getProfileStore() {
        return profileStore;
    }

    public ModAuditService getModAuditService() {
        return modAuditService;
    }
}
