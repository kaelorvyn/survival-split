package com.agnes.survivalsplit.command;

import com.agnes.survivalsplit.KaelorvynTitleBridge;
import com.agnes.survivalsplit.gui.SelectionGui;
import com.agnes.survivalsplit.listener.ReachManager;
import com.agnes.survivalsplit.listener.SpawnGateListener;
import com.agnes.survivalsplit.modaudit.ModAuditService;
import com.agnes.survivalsplit.model.SplitMode;
import com.agnes.survivalsplit.model.SplitPlatform;
import com.agnes.survivalsplit.model.SplitProfile;
import com.agnes.survivalsplit.store.ProfileStore;
import com.agnes.survivalsplit.visual.VisualSyncService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SplitCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final ProfileStore store;
    private final SelectionGui gui;
    private final SpawnGateListener gate;
    private final VisualSyncService visual;
    private final ModAuditService modAudit;

    public SplitCommand(JavaPlugin plugin, ProfileStore store, SelectionGui gui,
                        SpawnGateListener gate, VisualSyncService visual, ModAuditService modAudit) {
        this.plugin = plugin;
        this.store = store;
        this.gui = gui;
        this.gate = gate;
        this.visual = visual;
        this.modAudit = modAudit;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "该指令只能由玩家执行。");
                return true;
            }
            gui.open(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("check")) {
            if (sender instanceof Player player) {
                SplitProfile profile = store.get(player.getUniqueId());
                if (profile == null) {
                    player.sendMessage(ChatColor.YELLOW + "你还未选择身份，使用 /split 打开选择界面。");
                } else {
                    player.sendMessage(ChatColor.GREEN + "当前身份：" + ChatColor.AQUA + profile.display());
                }
            } else {
                sender.sendMessage(ChatColor.RED + "该指令只能由玩家执行。");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "该指令只能由玩家执行。");
                return true;
            }
            if (!visual.isVisualModClient(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "未检测到客户端模组，无法直接设置身份。");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(ChatColor.YELLOW + "用法: /split set <peaceful|combat> [pc|mobile]");
                return true;
            }
            SplitMode mode = switch (args[1].toLowerCase()) {
                case "peaceful" -> SplitMode.PEACEFUL;
                case "combat" -> SplitMode.COMBAT;
                default -> null;
            };
            if (mode == null) {
                player.sendMessage(ChatColor.RED + "模式只能是 peaceful 或 combat。");
                return true;
            }
            SplitPlatform platform = null;
            if (args.length >= 3) {
                platform = switch (args[2].toLowerCase()) {
                    case "pc" -> SplitPlatform.PC;
                    case "mobile" -> SplitPlatform.MOBILE;
                    default -> null;
                };
                if (platform == null) {
                    player.sendMessage(ChatColor.RED + "设备只能是 pc 或 mobile。");
                    return true;
                }
            } else {
                platform = visual.detectedPlatform(player.getUniqueId());
                if (platform == null) {
                    SplitProfile current = store.get(player.getUniqueId());
                    platform = current != null ? current.platform() : SplitPlatform.PC;
                }
            }
            SplitProfile profile = new SplitProfile(mode, platform);
            store.set(player.getUniqueId(), profile);
            ReachManager.apply(player, store, plugin.getConfig());
            visual.onFactionChanged(player);
            KaelorvynTitleBridge.refresh(player.getUniqueId());
            plugin.getLogger().info(player.getName() + " 选择身份: " + profile.display());
            player.sendMessage(ChatColor.GREEN + "身份已保存：" + ChatColor.AQUA + profile.display());
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("survivalsplit.admin")) {
                sender.sendMessage(ChatColor.RED + "你没有权限执行该指令。");
                return true;
            }
            plugin.reloadConfig();
            store.load();
            gate.reapplyAll();
            visual.onReload();
            modAudit.reload();
            sender.sendMessage(ChatColor.GREEN + "SurvivalSplit 配置已重载。");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "用法: /split [check|set <peaceful|combat> [pc|mobile]|reload]");
        return true;
    }
}
