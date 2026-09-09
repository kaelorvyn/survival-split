package com.agnes.survivalsplit.gui;

import com.agnes.survivalsplit.KaelorvynTitleBridge;
import com.agnes.survivalsplit.listener.ReachManager;
import com.agnes.survivalsplit.model.SplitMode;
import com.agnes.survivalsplit.model.SplitPlatform;
import com.agnes.survivalsplit.model.SplitProfile;
import com.agnes.survivalsplit.store.ProfileStore;
import com.agnes.survivalsplit.visual.VisualSyncService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SelectionGui implements Listener {

    private static final int SLOT_PEACE = 2;
    private static final int SLOT_COMBAT = 6;
    private static final int SLOT_PC = 11;
    private static final int SLOT_MOBILE = 15;

    private final JavaPlugin plugin;
    private final ProfileStore store;
    private final VisualSyncService visual;
    private final Set<UUID> pendingPlayers = new HashSet<>();
    private final Set<UUID> platformRequired = new HashSet<>();
    private final Map<UUID, SplitMode> chosenMode = new HashMap<>();

    public SelectionGui(JavaPlugin plugin, ProfileStore store, VisualSyncService visual) {
        this.plugin = plugin;
        this.store = store;
        this.visual = visual;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        UUID uuid = player.getUniqueId();
        boolean requirePlatform = !visual.isVisualModClient(uuid);
        if (requirePlatform) {
            platformRequired.add(uuid);
        } else {
            platformRequired.remove(uuid);
        }
        player.openInventory(buildInventory(player, requirePlatform));
    }

    public void openGate(Player player) {
        pendingPlayers.add(player.getUniqueId());
        open(player);
    }

    public boolean isPending(UUID uuid) {
        return pendingPlayers.contains(uuid);
    }

    public void onModDetected(Player player) {
        UUID uuid = player.getUniqueId();
        if (!pendingPlayers.contains(uuid) || !platformRequired.remove(uuid)) {
            return;
        }
        String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("gui.title", "&8身份选择"));
        Inventory inventory = player.getOpenInventory().getTopInventory();
        if (inventory != null && inventory.getSize() == 18 && player.getOpenInventory().getTitle().equals(title)) {
            inventory.setItem(SLOT_PC, filler());
            inventory.setItem(SLOT_MOBILE, filler());
        }
        SplitPlatform detected = visual.detectedPlatform(uuid);
        if (detected != null) {
            player.sendMessage(ChatColor.GREEN + "已自动识别设备：" + ChatColor.AQUA + detected.display());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("gui.title", "&8身份选择"));
        if (event.getView().getTopInventory() == null
                || !event.getView().getTitle().equals(title)
                || event.getClickedInventory() == null) {
            return;
        }
        event.setCancelled(true);
        UUID uuid = player.getUniqueId();
        int slot = event.getRawSlot();
        plugin.getLogger().info("GUI点击: " + player.getName() + " slot=" + slot
                + " click=" + event.getClick() + " modded=" + visual.isVisualModClient(uuid));

        if (slot == SLOT_PEACE || slot == SLOT_COMBAT) {
            SplitMode mode = slot == SLOT_PEACE ? SplitMode.PEACEFUL : SplitMode.COMBAT;
            if (!platformRequired.contains(uuid)) {
                SplitPlatform platform = visual.detectedPlatform(uuid);
                if (platform == null) {
                    SplitProfile current = store.get(uuid);
                    platform = current != null ? current.platform() : SplitPlatform.PC;
                }
                completeSelection(player, mode, platform);
                return;
            }
            chosenMode.put(uuid, mode);
            refreshModeItems(player);
            return;
        }

        if (slot == SLOT_PC || slot == SLOT_MOBILE) {
            SplitMode mode = chosenMode.get(uuid);
            if (mode == null) {
                player.sendMessage(ChatColor.YELLOW + "请先选择第一行的和平/战斗身份。");
                return;
            }
            SplitPlatform clicked = slot == SLOT_PC ? SplitPlatform.PC : SplitPlatform.MOBILE;
            SplitPlatform detected = visual.detectedPlatform(uuid);
            if (detected != null && detected != clicked) {
                player.sendMessage(ChatColor.YELLOW + "你的设备已被识别为" + detected.display()
                        + "，不能选择" + clicked.display() + "。");
                return;
            }
            completeSelection(player, mode, detected != null ? detected : clicked);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        UUID uuid = player.getUniqueId();
        if (!pendingPlayers.contains(uuid)) {
            return;
        }
        int delay = plugin.getConfig().getInt("gui.reopen-delay-ticks", 1);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && pendingPlayers.contains(uuid)) {
                open(player);
            }
        }, Math.max(1, delay));
    }

    private void completeSelection(Player player, SplitMode mode, SplitPlatform platform) {
        UUID uuid = player.getUniqueId();
        SplitProfile profile = new SplitProfile(mode, platform);
        store.set(uuid, profile);
        pendingPlayers.remove(uuid);
        chosenMode.remove(uuid);
        platformRequired.remove(uuid);
        player.closeInventory();
        ReachManager.apply(player, store, plugin.getConfig());
        visual.onFactionChanged(player);
        KaelorvynTitleBridge.refresh(uuid);
        plugin.getLogger().info(player.getName() + " 选择身份: " + profile.display());
        player.sendMessage(ChatColor.GREEN + "身份已保存：" + ChatColor.AQUA + profile.display());
    }

    private Inventory buildInventory(Player player, boolean requirePlatform) {
        int size = requirePlatform ? 18 : 9;
        String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("gui.title", "&8身份选择"));
        Inventory inventory = Bukkit.createInventory(null, size, title);
        for (int slot = 0; slot < size; slot++) {
            inventory.setItem(slot, filler());
        }
        SplitMode chosen = chosenMode.get(player.getUniqueId());
        inventory.setItem(SLOT_PEACE, modeItem(SplitMode.PEACEFUL, chosen == SplitMode.PEACEFUL));
        inventory.setItem(SLOT_COMBAT, modeItem(SplitMode.COMBAT, chosen == SplitMode.COMBAT));
        if (requirePlatform) {
            SplitPlatform detected = visual.detectedPlatform(player.getUniqueId());
            inventory.setItem(SLOT_PC, platformItem(SplitPlatform.PC, detected == SplitPlatform.PC));
            inventory.setItem(SLOT_MOBILE, platformItem(SplitPlatform.MOBILE, detected == SplitPlatform.MOBILE));
        }
        return inventory;
    }

    private void refreshModeItems(Player player) {
        Inventory inventory = player.getOpenInventory().getTopInventory();
        if (inventory == null) {
            return;
        }
        SplitMode chosen = chosenMode.get(player.getUniqueId());
        inventory.setItem(SLOT_PEACE, modeItem(SplitMode.PEACEFUL, chosen == SplitMode.PEACEFUL));
        inventory.setItem(SLOT_COMBAT, modeItem(SplitMode.COMBAT, chosen == SplitMode.COMBAT));
    }

    private static ItemStack modeItem(SplitMode mode, boolean selected) {
        Material material = mode == SplitMode.PEACEFUL ? Material.EMERALD : Material.DIAMOND_SWORD;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(mode == SplitMode.PEACEFUL
                ? ChatColor.GREEN + "和平玩家"
                : ChatColor.RED + "战斗玩家");
        List<String> lore = new ArrayList<>();
        if (mode == SplitMode.PEACEFUL) {
            lore.add(ChatColor.GRAY + "不受战斗玩家伤害");
            lore.add(ChatColor.GRAY + "仍会受怪物和陷阱伤害");
        } else {
            lore.add(ChatColor.GRAY + "可与其他战斗玩家 PvP");
        }
        meta.setLore(lore);
        if (selected) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack platformItem(SplitPlatform platform, boolean detected) {
        ItemStack item = new ItemStack(platform == SplitPlatform.PC ? Material.COMPASS : Material.MAP);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + (platform == SplitPlatform.PC ? "电脑玩家" : "手机玩家"));
        List<String> lore = new ArrayList<>();
        if (detected) {
            lore.add(ChatColor.GRAY + "已自动识别你的设备");
        } else {
            lore.add(ChatColor.GRAY + "请选择你的设备类型");
        }
        meta.setLore(lore);
        if (detected) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack filler() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }
}
