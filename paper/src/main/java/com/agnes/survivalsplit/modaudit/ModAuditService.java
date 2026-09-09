package com.agnes.survivalsplit.modaudit;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class ModAuditService implements PluginMessageListener {

    public static final String MODLIST_CHANNEL = "survivalsplit:modlist";
    private static final Gson GSON = new Gson();

    private final JavaPlugin plugin;
    private final ModAuditStore store;
    private final HttpClient http;
    private final Set<UUID> auditing = ConcurrentHashMap.newKeySet();

    private boolean enabled;
    private boolean onlyOnChange;
    private String tavilyUrl;
    private String tavilyKey;
    private int maxResults;
    private int searchTimeoutSeconds;
    private String aiBaseUrl;
    private String aiModel;
    private String aiApiKey;
    private int aiTimeoutSeconds;
    private double minCheatConfidence;
    private String cheatAction;
    private String suspiciousAction;
    private String unknownAction;
    private int banHours;
    private final Set<String> safeMods = new HashSet<>();
    private final Set<String> cheatMods = new HashSet<>();

    public ModAuditService(JavaPlugin plugin, ModAuditStore store) {
        this.plugin = plugin;
        this.store = store;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        reload();
    }

    public void start() {
        reload();
        plugin.getServer().getMessenger()
                .registerIncomingPluginChannel(plugin, MODLIST_CHANNEL, this);
    }

    public void stop() {
        plugin.getServer().getMessenger()
                .unregisterIncomingPluginChannel(plugin, MODLIST_CHANNEL, this);
    }

    public void reload() {
        var config = plugin.getConfig();
        enabled = config.getBoolean("modaudit.enabled", true);
        onlyOnChange = config.getBoolean("modaudit.only-on-change", true);
        tavilyUrl = config.getString("modaudit.search.tavily-url",
                "https://api.tavily.com/search");
        tavilyKey = config.getString("modaudit.search.tavily-api-key", "");
        if (tavilyKey == null || tavilyKey.isBlank()) {
            tavilyKey = System.getenv("TAVILY_API_KEY");
        }
        if (tavilyKey == null) {
            tavilyKey = "";
        }
        maxResults = Math.max(1, Math.min(10, config.getInt("modaudit.search.max-results", 5)));
        searchTimeoutSeconds = Math.max(5, config.getInt("modaudit.search.timeout-seconds", 15));
        aiBaseUrl = config.getString("modaudit.ai.base-url", "https://api.agnes-ai.cn");
        aiModel = config.getString("modaudit.ai.model", "agnes-2.5-flash");
        aiApiKey = config.getString("modaudit.ai.api-key", "");
        if (aiApiKey == null || aiApiKey.isBlank()) {
            aiApiKey = System.getenv("AGNES_API_KEY");
        }
        if (aiApiKey == null || aiApiKey.isBlank()) {
            aiApiKey = kaelorvynAiKey();
        }
        if (aiApiKey == null) {
            aiApiKey = "";
        }
        aiTimeoutSeconds = Math.max(10, config.getInt("modaudit.ai.timeout-seconds", 60));
        minCheatConfidence = Math.max(0.5, Math.min(1.0,
                config.getDouble("modaudit.actions.min-cheat-confidence", 0.9)));
        cheatAction = config.getString("modaudit.actions.cheat", "ban");
        suspiciousAction = config.getString("modaudit.actions.suspicious", "warn");
        unknownAction = config.getString("modaudit.actions.unknown", "warn");
        banHours = Math.max(1, config.getInt("modaudit.actions.ban-hours", 168));
        safeMods.clear();
        cheatMods.clear();
        for (String id : config.getStringList("modaudit.safe-mods")) {
            if (id != null && !id.isBlank()) safeMods.add(id.trim().toLowerCase(java.util.Locale.ROOT));
        }
        for (String id : config.getStringList("modaudit.cheat-mods")) {
            if (id != null && !id.isBlank()) cheatMods.add(id.trim().toLowerCase(java.util.Locale.ROOT));
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!MODLIST_CHANNEL.equals(channel) || player == null) {
            return;
        }
        String raw = new String(message, StandardCharsets.UTF_8).trim();
        ModInfo[] mods;
        try {
            mods = GSON.fromJson(raw, ModInfo[].class);
        } catch (Exception e) {
            plugin.getLogger().warning("无法解析 " + player.getName() + " 的模组列表: " + e.getMessage());
            return;
        }
        if (mods == null) {
            return;
        }
        plugin.getLogger().info("收到模组上报: " + player.getName() + " 共 " + mods.length + " 个模组");
        handle(player, List.of(mods));
    }

    private void handle(Player player, List<ModInfo> mods) {
        UUID uuid = player.getUniqueId();
        String fingerprint = ModAuditFingerprint.fingerprint(mods);
        if (!enabled) {
            store.save(uuid, fingerprint, mods);
            return;
        }
        // 黑名单优先于指纹去重：带已知作弊模组每次进服都直接拦截，
        // 不因上次指纹相同而跳过。
        ModVerdict blacklisted = firstKnownCheat(mods);
        if (blacklisted != null) {
            plugin.getLogger().warning("模组黑名单命中: " + player.getName()
                    + " -> " + blacklisted.mod() + "（" + blacklisted.purpose() + "）");
            apply(player, mods, fingerprint, Map.of(blacklisted.mod(), blacklisted), List.of());
            return;
        }
        ModAuditStore.Entry previous = store.get(uuid);
        if (previous != null && previous.fingerprint().equals(fingerprint)) {
            store.touch(uuid);
            return;
        }
        if (!auditing.add(uuid)) {
            return;
        }

        List<ModInfo> candidates = onlyOnChange && previous != null
                ? ModAuditFingerprint.changedOnly(mods, previous.mods())
                : mods;
        if (candidates.isEmpty()) {
            store.save(uuid, fingerprint, mods);
            auditing.remove(uuid);
            return;
        }

        Map<String, ModVerdict> local = new LinkedHashMap<>();
        List<ModInfo> pending = new ArrayList<>();
        for (ModInfo mod : candidates) {
            if (isSafe(mod)) {
                continue;
            }
            pending.add(mod);
        }
        if (pending.isEmpty()) {
            apply(player, mods, fingerprint, local, List.of());
            auditing.remove(uuid);
            return;
        }
        auditAsync(player, mods, fingerprint, pending, local);
    }

    private void auditAsync(Player player, List<ModInfo> mods, String fingerprint,
                            List<ModInfo> pending, Map<String, ModVerdict> local) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, String> searchByMod = new LinkedHashMap<>();
            for (ModInfo mod : pending) {
                try {
                    searchByMod.put(mod.id(), tavilySearch(queryFor(mod)));
                } catch (Exception e) {
                    plugin.getLogger().warning("Tavily 搜索失败 " + mod.id() + ": " + e.getMessage());
                    searchByMod.put(mod.id(), "");
                }
            }
            List<ModVerdict> ai = new ArrayList<>();
            try {
                ai.addAll(aiReview(pending, searchByMod)
                        .get(aiTimeoutSeconds, TimeUnit.SECONDS));
            } catch (Exception e) {
                plugin.getLogger().warning("Agnes 模组审计失败: " + e.getMessage());
            }
            List<ModVerdict> verdicts = ai;
            if (!plugin.isEnabled()) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                apply(player, mods, fingerprint, local, verdicts);
                auditing.remove(player.getUniqueId());
            });
        });
    }

    private void apply(Player player, List<ModInfo> mods, String fingerprint,
                       Map<String, ModVerdict> local, List<ModVerdict> ai) {
        Map<String, ModVerdict> verdicts = new LinkedHashMap<>(local);
        for (ModVerdict verdict : ai) {
            if (verdict.mod() == null || verdict.mod().isBlank()) {
                continue;
            }
            verdicts.putIfAbsent(verdict.mod(), verdict);
        }
        store.save(player.getUniqueId(), fingerprint, mods);

        ModVerdict cheat = verdicts.values().stream()
                .filter(v -> v.isCheat() && v.confidence() >= minCheatConfidence)
                .findFirst()
                .orElse(null);
        if (cheat != null) {
            String reason = "检测到作弊模组 " + cheat.mod() + (cheat.purpose().isBlank() ? "" : "（" + cheat.purpose() + "）")
                    + (cheat.reason().isBlank() ? "" : "，依据：" + cheat.reason());
            alertAdmins(player, "&c模组审计：&f" + player.getName() + " &c携带作弊模组 &f" + cheat.mod());
            plugin.getLogger().warning(player.getName() + " 模组审计判定 cheat: " + reason);
            applyAction(player, cheatAction, "作弊模组", reason);
            return;
        }

        for (ModVerdict verdict : verdicts.values()) {
            String action = switch (verdict.verdict() == null ? "" : verdict.verdict().toUpperCase(java.util.Locale.ROOT)) {
                case "SUSPICIOUS" -> suspiciousAction;
                case "UNKNOWN" -> unknownAction;
                default -> "none";
            };
            if ("none".equalsIgnoreCase(action)) {
                continue;
            }
            String reason = "模组 " + verdict.mod() + " 用途不明/存疑（" + verdict.purpose() + "），"
                    + verdict.reason();
            plugin.getLogger().warning(player.getName() + " 模组审计: " + verdict.verdict() + " " + verdict.mod()
                    + " " + verdict.purpose() + " " + verdict.reason());
            applyAction(player, action, "可疑模组", reason);
        }
    }

    private void applyAction(Player player, String action, String label, String reason) {
        if (player == null || !player.isOnline()) {
            return;
        }
        switch (action == null ? "" : action.toLowerCase(java.util.Locale.ROOT)) {
            case "guard", "path" -> submitToGuard(player, "CLIENT_MOD", 1.0, reason);
            case "ban" -> banPlayer(player, reason, banHours);
            case "kick" -> player.kickPlayer(ChatColor.translateAlternateColorCodes('&',
                    "&cKaelorvyn 安全中心\n&7" + label + "：&f" + reason));
            case "warn" -> {
                player.sendTitle(ChatColor.translateAlternateColorCodes('&', "&cKaelorvyn 安全中心"),
                        ChatColor.translateAlternateColorCodes('&', "&e" + label + "：" + reason),
                        5, 100, 10);
                alertAdmins(player, "&e模组审计：" + player.getName() + " " + reason);
            }
            default -> plugin.getLogger().info("模组审计已记录：" + player.getName() + " " + reason);
        }
    }

    private void submitToGuard(Player player, String category, double confidence, String reason) {
        Object guard = Bukkit.getPluginManager().getPlugin("KaelorvynGuard");
        if (guard == null) {
            plugin.getLogger().warning("KaelorvynGuard 未加载，模组审计只能直接告警: " + player.getName() + " " + reason);
            player.sendTitle(ChatColor.translateAlternateColorCodes('&', "&cKaelorvyn 安全中心"),
                    ChatColor.translateAlternateColorCodes('&', "&e" + reason), 5, 100, 10);
            return;
        }
        try {
            guard.getClass().getMethod("onModAuditKick", Player.class, String.class)
                    .invoke(guard, player, reason);
        } catch (Throwable e) {
            plugin.getLogger().warning("提交模组审计到 KaelorvynGuard 失败: " + e.getMessage());
        }
    }

    private void banPlayer(Player player, String reason, int hours) {
        String colored = ChatColor.translateAlternateColorCodes('&', reason);
        Object guard = Bukkit.getPluginManager().getPlugin("KaelorvynGuard");
        if (guard != null) {
            try {
                Object punish = guard.getClass().getMethod("punishService").invoke(guard);
                punish.getClass().getMethod("banPlayer", Player.class, String.class, int.class)
                        .invoke(punish, player, colored, hours);
                return;
            } catch (Throwable e) {
                plugin.getLogger().warning("调用 KaelorvynGuard 封禁失败，回退 Bukkit 封禁: " + e.getMessage());
            }
        }
        Date expire = new Date(System.currentTimeMillis() + hours * 3600000L);
        Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), colored, expire, "KaelorvynGuard ModAudit");
        if (player.isOnline()) {
            player.kickPlayer(ChatColor.translateAlternateColorCodes('&',
                    "&c你已被 Kaelorvyn 服务器封禁\n&7原因：&f" + colored));
        }
    }

    private String tavilySearch(String query) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("query", query);
        body.addProperty("search_depth", "basic");
        body.addProperty("include_answer", true);
        body.addProperty("max_results", maxResults);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tavilyUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + tavilyKey)
                .timeout(Duration.ofSeconds(searchTimeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        return parseTavily(response.body());
    }

    private String parseTavily(String body) {
        StringBuilder out = new StringBuilder();
        try {
            JsonObject root = GSON.fromJson(body, JsonObject.class);
            if (root == null) {
                return "";
            }
            if (root.has("answer") && !root.get("answer").isJsonNull()) {
                out.append("answer: ").append(root.get("answer").getAsString()).append('\n');
            }
            if (root.has("results") && root.get("results").isJsonArray()) {
                JsonArray results = root.getAsJsonArray("results");
                int count = Math.min(3, results.size());
                for (int i = 0; i < count; i++) {
                    JsonObject item = results.get(i).getAsJsonObject();
                    out.append("[").append(i + 1).append("] ")
                            .append(str(item, "title")).append(" ")
                            .append(str(item, "url")).append('\n')
                            .append(ModAuditFingerprint.truncate(str(item, "content"), 400))
                            .append('\n');
                }
            }
        } catch (Exception e) {
            return "";
        }
        return ModAuditFingerprint.truncate(out.toString(), 1800);
    }

    private CompletableFuture<List<ModVerdict>> aiReview(
            List<ModInfo> mods, Map<String, String> searchByMod) {
        JsonObject body = new JsonObject();
        body.addProperty("model", aiModel);
        body.addProperty("max_tokens", 2000);
        JsonArray messages = new JsonArray();

        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", """
                你是 Kaelorvyn 服务器的客户端模组审计员。玩家上报了 Fabric 模组，
                下面是 Tavily 真实联网搜索到的资料。请判断每个模组的用途。
                每个模组只能给出一个 verdict：safe、cheat、suspicious 或 unknown。
                safe = 原版或正常辅助；cheat = 提供任何游戏外优势的作弊功能
                （杀戮光环、飞行、透视、连点、自动瞄准、自动搭路、无后座等）；
                suspicious = 有作弊能力但可能被正常使用；unknown = 没有可靠证据。
                宁可漏放也不误封，没有可靠证据就写 unknown。
                只输出 JSON：{"verdicts":[{"mod":"模组id","verdict":"cheat",
                "confidence":0.9,"purpose":"中文用途","reason":"中文依据"}]}
                """);
        messages.add(system);

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", buildPrompt(mods, searchByMod));
        messages.add(user);
        body.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(aiBaseUrl + "/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiApiKey)
                .timeout(Duration.ofSeconds(aiTimeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build();
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> parseVerdicts(response.body()))
                .exceptionally(e -> List.of());
    }

    private String buildPrompt(List<ModInfo> mods, Map<String, String> searchByMod) {
        StringBuilder out = new StringBuilder();
        out.append("玩家模组：\n");
        for (ModInfo mod : mods) {
            out.append("- id=").append(mod.id())
                    .append(" name=").append(mod.name())
                    .append(" version=").append(mod.version())
                    .append(" description=").append(ModAuditFingerprint.truncate(mod.description(), 120))
                    .append('\n');
        }
        out.append("\n联网搜索资料：\n");
        for (Map.Entry<String, String> entry : searchByMod.entrySet()) {
            out.append("--- ").append(entry.getKey()).append(" ---\n")
                    .append(entry.getValue()).append('\n');
        }
        return ModAuditFingerprint.truncate(out.toString(), 12000);
    }

    private List<ModVerdict> parseVerdicts(String body) {
        List<ModVerdict> verdicts = new ArrayList<>();
        try {
            body = stripJsonFence(body);
            JsonElement root = GSON.fromJson(body, JsonElement.class);
            JsonArray array = null;
            if (root != null && root.isJsonObject() && root.getAsJsonObject().has("verdicts")) {
                array = root.getAsJsonObject().getAsJsonArray("verdicts");
            } else if (root != null && root.isJsonArray()) {
                array = root.getAsJsonArray();
            }
            if (array == null) {
                return verdicts;
            }
            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject obj = element.getAsJsonObject();
                String mod = str(obj, "mod");
                if (mod.isBlank()) {
                    continue;
                }
                String verdict = str(obj, "verdict").toUpperCase(java.util.Locale.ROOT);
                if (!Set.of("SAFE", "CHEAT", "SUSPICIOUS", "UNKNOWN").contains(verdict)) {
                    continue;
                }
                double confidence = obj.has("confidence") ? obj.get("confidence").getAsDouble() : 0.5;
                verdicts.add(new ModVerdict(mod, verdict, confidence,
                        str(obj, "purpose"), str(obj, "reason")));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Agnes 模组审计响应解析失败: " + e.getMessage());
        }
        return verdicts;
    }

    private static String stripJsonFence(String body) {
        if (body == null) {
            return "";
        }
        int objectStart = body.indexOf('{');
        int arrayStart = body.indexOf('[');
        int start = objectStart;
        if (arrayStart != -1 && (objectStart == -1 || arrayStart < objectStart)) {
            start = arrayStart;
        }
        int objectEnd = body.lastIndexOf('}');
        int arrayEnd = body.lastIndexOf(']');
        int end = Math.max(objectEnd, arrayEnd);
        if (start >= 0 && end > start) {
            return body.substring(start, end + 1);
        }
        return body;
    }

    private boolean isSafe(ModInfo mod) {
        String id = mod.id().toLowerCase(java.util.Locale.ROOT);
        if (safeMods.contains(id) || id.startsWith("fabric-")) {
            return true;
        }
        return false;
    }

    private ModVerdict knownCheat(ModInfo mod) {
        String id = mod.id().toLowerCase(java.util.Locale.ROOT);
        if (!cheatMods.contains(id)) {
            return null;
        }
        return new ModVerdict(mod.id(), "CHEAT", 1.0, "已知作弊模组", "命中服务器内置作弊模组名单");
    }

    private ModVerdict firstKnownCheat(List<ModInfo> mods) {
        for (ModInfo mod : mods) {
            ModVerdict verdict = knownCheat(mod);
            if (verdict != null) {
                return verdict;
            }
        }
        return null;
    }

    private String queryFor(ModInfo mod) {
        return "Minecraft mod \"" + mod.id() + "\" \"" + mod.name() + "\" "
                + mod.version() + " cheat hack client what is it";
    }

    private void alertAdmins(Player target, String message) {
        String colored = ChatColor.translateAlternateColorCodes('&', message);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("kg.alerts") || online.hasPermission("survivalsplit.admin")) {
                online.sendMessage(colored);
            }
        }
    }

    private String kaelorvynAiKey() {
        Object guard = Bukkit.getPluginManager().getPlugin("KaelorvynGuard");
        if (guard == null) {
            return "";
        }
        try {
            Object settings = guard.getClass().getMethod("settings").invoke(guard);
            return (String) settings.getClass().getMethod("apiKey").invoke(settings);
        } catch (Exception e) {
            return "";
        }
    }

    private static String str(JsonObject obj, String key) {
        JsonElement value = obj.get(key);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }
}
