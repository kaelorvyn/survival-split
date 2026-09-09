package com.agnes.survivalsplit.listener;

import com.agnes.survivalsplit.model.DamageRule;
import com.agnes.survivalsplit.model.SplitProfile;
import com.agnes.survivalsplit.store.ProfileStore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.block.Action;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DamageIsolationListener implements Listener {

    private static final long ANCHOR_CLICK_TTL_MS = 5000L;
    private static final long FALL_ATTRIBUTION_MS = 5000L;

    private final JavaPlugin plugin;
    private final ProfileStore store;
    private final Map<UUID, UUID> explosiveOwners = new HashMap<>();
    private final Map<BlockKey, AnchorClick> anchorClicks = new HashMap<>();
    private final Map<BlockKey, UUID> pendingTntOwners = new HashMap<>();
    private final Map<BlockKey, UUID> pendingTntMinecartOwners = new HashMap<>();
    private final Map<BlockKey, UUID> pendingCrystalOwners = new HashMap<>();
    private final Map<UUID, RecentHit> recentHits = new HashMap<>();

    public DamageIsolationListener(JavaPlugin plugin, ProfileStore store) {
        this.plugin = plugin;
        this.store = store;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, this::cleanup, 600L, 600L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = resolveAttacker(event);
        UUID victimId = victim.getUniqueId();
        if (attacker != null) {
            recentHits.put(victimId, new RecentHit(attacker.getUniqueId(), System.currentTimeMillis()));
        }
        SplitProfile victimProfile = store.get(victimId);
        SplitProfile attackerProfile = attacker != null ? store.get(attacker.getUniqueId()) : null;
        if (attackerProfile == null && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            attackerProfile = resolveRecentAttacker(victimId);
        }
        if (attackerProfile == null) {
            return;
        }
        if (DamageRule.shouldCancel(victimProfile, attackerProfile)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPlace(EntityPlaceEvent event) {
        if (event.getEntity() instanceof EnderCrystal) {
            explosiveOwners.put(event.getEntity().getUniqueId(), event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block.getType() == Material.RESPAWN_ANCHOR) {
            anchorClicks.put(BlockKey.of(block), new AnchorClick(player.getUniqueId(), System.currentTimeMillis()));
        } else if (event.getItem() != null && event.getItem().getType() == Material.END_CRYSTAL) {
            pendingCrystalOwners.put(BlockKey.of(block.getRelative(BlockFace.UP)), player.getUniqueId());
        } else if (event.getItem() != null && event.getItem().getType() == Material.TNT_MINECART) {
            pendingTntMinecartOwners.put(BlockKey.of(block), player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof ExplosiveMinecart minecart) {
            explosiveOwners.put(minecart.getUniqueId(), event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTntPrime(TNTPrimeEvent event) {
        if (event.getPrimingEntity() instanceof Player player) {
            pendingTntOwners.put(BlockKey.of(event.getBlock()), player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof TNTPrimed tnt) {
            BlockKey key = BlockKey.of(event.getLocation().getBlock());
            UUID owner = pendingTntOwners.remove(key);
            if (owner != null) {
                Player player = Bukkit.getPlayer(owner);
                if (player != null) {
                    tnt.setSource(player);
                }
                explosiveOwners.put(tnt.getUniqueId(), owner);
            }
        } else if (event.getEntity() instanceof ExplosiveMinecart minecart) {
            BlockKey key = BlockKey.of(event.getLocation().getBlock());
            UUID owner = pendingTntMinecartOwners.remove(key);
            if (owner != null) {
                explosiveOwners.put(minecart.getUniqueId(), owner);
            }
        } else if (event.getEntity() instanceof EnderCrystal crystal) {
            BlockKey key = BlockKey.of(event.getLocation().getBlock());
            UUID owner = pendingCrystalOwners.remove(key);
            if (owner != null) {
                explosiveOwners.put(crystal.getUniqueId(), owner);
            }
        }
    }

    private Player resolveAttacker(EntityDamageEvent event) {
        DamageSource source = event.getDamageSource();
        Player player = asPlayerOwner(source != null ? source.getCausingEntity() : null);
        if (player != null) {
            return player;
        }
        player = asPlayerOwner(source != null ? source.getDirectEntity() : null);
        if (player != null) {
            return player;
        }
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            player = asPlayerOwner(byEntity.getDamager());
            if (player != null) {
                return player;
            }
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            Location location = source != null ? source.getSourceLocation() : null;
            if (location == null && source != null) {
                location = source.getDamageLocation();
            }
            if (location != null) {
                return resolveAnchorOwner(location);
            }
        }
        return null;
    }

    private Player asPlayerOwner(Entity entity) {
        if (entity == null) {
            return null;
        }
        if (entity instanceof Player player) {
            return player;
        }
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        if (entity instanceof TNTPrimed tnt) {
            if (tnt.getSource() instanceof Player player) {
                return player;
            }
            return ownerPlayer(tnt.getUniqueId());
        }
        if (entity instanceof ExplosiveMinecart) {
            return ownerPlayer(entity.getUniqueId());
        }
        if (entity instanceof EnderCrystal) {
            return ownerPlayer(entity.getUniqueId());
        }
        if (entity instanceof AreaEffectCloud cloud && cloud.getSource() instanceof Player player) {
            return player;
        }
        return null;
    }

    private Player ownerPlayer(UUID owner) {
        if (owner == null) {
            return null;
        }
        return Bukkit.getPlayer(owner);
    }

    private Player resolveAnchorOwner(Location location) {
        long now = System.currentTimeMillis();
        anchorClicks.entrySet().removeIf(entry -> now - entry.getValue().time > ANCHOR_CLICK_TTL_MS);
        AnchorClick click = anchorClicks.get(BlockKey.of(location.getBlock()));
        return click == null ? null : Bukkit.getPlayer(click.owner);
    }

    private SplitProfile resolveRecentAttacker(UUID victimId) {
        RecentHit hit = recentHits.get(victimId);
        if (hit == null) {
            return null;
        }
        if (System.currentTimeMillis() - hit.time > FALL_ATTRIBUTION_MS) {
            recentHits.remove(victimId);
            return null;
        }
        return store.get(hit.attacker);
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        recentHits.entrySet().removeIf(entry -> now - entry.getValue().time > FALL_ATTRIBUTION_MS);
        explosiveOwners.keySet().removeIf(uuid -> {
            Entity entity = Bukkit.getEntity(uuid);
            return entity == null || !entity.isValid();
        });
    }

    private record BlockKey(String world, int x, int y, int z) {

        static BlockKey of(Block block) {
            return new BlockKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        }
    }

    private record AnchorClick(UUID owner, long time) {
    }

    private record RecentHit(UUID attacker, long time) {
    }
}
