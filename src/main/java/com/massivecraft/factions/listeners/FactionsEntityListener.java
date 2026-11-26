package com.massivecraft.factions.listeners;

import com.massivecraft.factions.*;
import com.massivecraft.factions.api.LegacyUtil;
import com.massivecraft.factions.event.PowerLossEvent;
import com.massivecraft.factions.struct.FFlag;
import com.massivecraft.factions.struct.Rel;
import com.massivecraft.factions.util.MiscUtil;
import com.massivecraft.factions.zcore.Lang;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.*;


/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class FactionsEntityListener implements Listener {
    private static final Set<PotionEffectType> badPotionEffects =
            new LinkedHashSet<PotionEffectType>(Arrays
                    .asList(PotionEffectType.BLINDNESS, PotionEffectType.NAUSEA,
                            PotionEffectType.INSTANT_DAMAGE, PotionEffectType.HUNGER,
                            PotionEffectType.POISON, PotionEffectType.SLOWNESS,
                            PotionEffectType.MINING_FATIGUE, PotionEffectType.WEAKNESS,
                            PotionEffectType.WITHER));
    public Main main;


    public FactionsEntityListener(Main main) {
        this.main = main;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            if (entity instanceof LivingEntity living && living.getKiller() != null) {
                progressKill(living.getKiller(), FeatureConfig.MissionType.KILL_MOB);
            }
            return;
        }

        Player player = (Player) entity;
        FPlayer fplayer = FPlayers.i.get(player);
        if (player.getKiller() != null) {
            progressKill(player.getKiller(), FeatureConfig.MissionType.KILL_PLAYER);
        }
        progressKill(fplayer, FeatureConfig.MissionType.PLAYER_KILLED);
        Faction faction = Board.getFactionAt(new FLocation(player.getLocation()));

        PowerLossEvent powerLossEvent = new PowerLossEvent(faction, fplayer);
        // Check for no power loss conditions
        if (!faction.getFlag(FFlag.POWERLOSS)) {
            powerLossEvent.setMessage(
                    "<i>You didn't lose any power since the territory you died in works that way.");
            powerLossEvent.setCancelled(true);
        } else if (Conf.worldsNoPowerLoss.contains(player.getWorld().getName())) {
            powerLossEvent.setMessage("<i>You didn't lose any power due to the world you died in.");
            powerLossEvent.setCancelled(true);
        } else {
            powerLossEvent.setMessage("<i>Your power is now <h>%d / %d");
        }

        // call Event
        Bukkit.getPluginManager().callEvent(powerLossEvent);

        // Call player onDeath if the event is not cancelled
        if (!powerLossEvent.isCancelled()) {
            fplayer.onDeath();
        }
        // Send the message from the powerLossEvent
        final String msg = powerLossEvent.getMessage();
        if (msg != null && !msg.isEmpty()) {
            fplayer.msg(msg, fplayer.getPowerRounded(), fplayer.getPowerMaxRounded());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.isCancelled())
            return;

        if (event.getEntity() instanceof Animals) {
            Faction animalFaction = Board.getFactionAt(new FLocation(event.getEntity().getLocation()));
            if (!animalFaction.getFlag(FFlag.ANIMALS)) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getEntity() instanceof Player && event.getCause() != null
                && (event.getCause() == EntityDamageEvent.DamageCause.FIRE
                || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK)) {
            Faction fireFaction = Board.getFactionAt(new FLocation(event.getEntity().getLocation()));
            if (!fireFaction.getFlag(FFlag.FIRE_FRIENDLY)) {
                event.setCancelled(true);
                return;
            }
        }

        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent sub = (EntityDamageByEntityEvent) event;
            if (!this.canDamagerHurtDamagee(sub, true)) {
                event.setCancelled(true);
            }
        }
        // TODO: Add a no damage at all flag??
		/*else if (Conf.safeZonePreventAllDamageToPlayers && isPlayerInSafeZone(event.getEntity()))
		{
			// Players can not take any damage in a Safe Zone
			event.setCancelled(true);
		}*/
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.isCancelled())
            return;

        FPlayer igniter = explosionIgniter(event.getEntity());
        Set<FLocation> explosionLocs = new HashSet<FLocation>();
        for (Block block : event.blockList()) {
            explosionLocs.add(new FLocation(block));
        }
        for (FLocation loc : explosionLocs) {
            Faction faction = Board.getFactionAt(loc);
            if (!canExplodeAt(faction, igniter, event.getEntity())) {
                // faction has this explosion type disabled or is shielded
                event.setCancelled(true);
                return;
            }
        }

        // TNT in water/lava doesn't normally destroy any surrounding blocks, which is usually desired behavior, but...
        // this optional change below provides workaround for waterwalling providing perfect protection,
        // and makes cheap (non-obsidian) TNT cannons require minor maintenance between shots
        Block center = event.getLocation().getBlock();
        if (event.getEntity() instanceof TNTPrimed && Conf.handleExploitTNTWaterlog && center
                .isLiquid()) {
            // a single surrounding block in all 6 directions is broken if the material is weak enough
            List<Block> targets = new ArrayList<Block>();
            targets.add(center.getRelative(0, 0, 1));
            targets.add(center.getRelative(0, 0, -1));
            targets.add(center.getRelative(0, 1, 0));
            targets.add(center.getRelative(0, -1, 0));
            targets.add(center.getRelative(1, 0, 0));
            targets.add(center.getRelative(-1, 0, 0));
            for (Block target : targets) {
                if (!LegacyUtil.MATERIALS_WITH_HIGH_BLAST_RESISTANCE.contains(target.getType()))
                    continue;
                Faction targetFaction = Board.getFactionAt(new FLocation(target));
                if (canExplodeAt(targetFaction, igniter, event.getEntity()))
                    target.breakNaturally();
            }
        }
    }

    private FPlayer explosionIgniter(Entity entity) {
        if (!(entity instanceof TNTPrimed))
            return null;
        Entity source = ((TNTPrimed) entity).getSource();
        return source instanceof Player ? FPlayers.i.get((Player) source) : null;
    }

    private boolean canExplodeAt(Faction faction, FPlayer igniter, Entity entity) {
        if (faction == null)
            return true;
        if (this.main.shieldService != null) {
            boolean bypass = igniter != null && (igniter.hasAdminMode()
                    || Conf.playersWhoBypassAllProtection.contains(igniter.getName())
                    || Conf.playersWhoBypassAllProtection.contains(igniter.getId()));
            if (!bypass && !this.main.shieldService.canAttack(faction, igniter))
                return false;
        }
        FFlag explosionFlag = entity instanceof TNTPrimed ? FFlag.TNT : FFlag.EXPLOSIONS;
        return faction.getFlag(explosionFlag) && !faction.hasOfflineExplosionProtection();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityMove(EntityMoveEvent event) {
        Entity entity = event.getEntity();
        if (!Conf.monsters.contains(entity.getType()))
            return;
        if (!event.hasChangedBlock())
            return;

        FLocation from = new FLocation(event.getFrom());
        FLocation to = new FLocation(event.getTo());
        if (from.equals(to))
            return;

        Faction fromFaction = Board.getFactionAt(from);
        Faction toFaction = Board.getFactionAt(to);
        if (fromFaction == toFaction || toFaction.getFlag(FFlag.MONSTERS))
            return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Faction faction = Board.getFactionAt(new FLocation(event.getSpawner().getLocation()));
        if (!faction.getFlag(FFlag.SPAWNERS)) {
            event.setCancelled(true);
        }
    }

    // mainly for flaming arrows; don't want allies or people in safe zones to be ignited even after damage event is cancelled
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityCombustByEntity(
            EntityCombustByEntityEvent event) {
        if (event.isCancelled())
            return;

        if (!this.canDamagerHurtDamagee(event.getCombuster(), event.getEntity(), 0, false))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPotionSplashEvent(PotionSplashEvent event) {
        if (event.isCancelled())
            return;

        // see if the potion has a harmful effect
        boolean badjuju = false;
        for (PotionEffect effect : event.getPotion().getEffects()) {
            if (badPotionEffects.contains(effect.getType())) {
                badjuju = true;
                break;
            }
        }
        if (!badjuju)
            return;

        ProjectileSource thrower = event.getPotion().getShooter();
        if (!(thrower instanceof Entity)) {
            return;
        }
        if (thrower instanceof Player) {
            Player player = (Player) thrower;
            FPlayer fPlayer = FPlayers.i.get(player);
            if (badjuju && fPlayer.getFaction().getFlag(FFlag.PEACEFUL)) {
                event.setCancelled(true);
                return;
            }
        }
        // scan through affected entities to make sure they're all valid targets
        Iterator<LivingEntity> iter = event.getAffectedEntities().iterator();
        while (iter.hasNext()) {
            LivingEntity target = iter.next();
            if (!this.canDamagerHurtDamagee((Entity) thrower, target, 0, true))
                event.setIntensity(target,
                        0.0);  // affected entity list doesn't accept modification (iter.remove() is a no-go), but this works
        }
    }

    public boolean canDamagerHurtDamagee(EntityDamageByEntityEvent sub) {
        return canDamagerHurtDamagee(sub, true);
    }

    public boolean canDamagerHurtDamagee(EntityDamageByEntityEvent sub, boolean notify) {
        return canDamagerHurtDamagee(sub.getDamager(), sub.getEntity(), sub.getDamage(), notify);
    }

    public boolean canDamagerHurtDamagee(Entity damager, Entity damagee, double damage,
                                         boolean notify) {
        if (!(damagee instanceof Player))
            return true;

        FPlayer defender = FPlayers.i.get((Player) damagee);

        if (defender == null || defender.getPlayer() == null)
            return true;

        Location defenderLoc = defender.getPlayer().getLocation();

        Faction defLocFaction = Board.getFactionAt(new FLocation(defenderLoc));

        // for damage caused by projectiles, getDamager() returns the projectile... what we need to know is the source
        if (damager instanceof Projectile) {
            if (((Projectile) damager).getShooter() instanceof Entity) {
                damager = (Entity) ((Projectile) damager).getShooter();
            } else {
                return true;
            }
        }

        if (damager == damagee)  // ender pearl usage and other self-inflicted damage
            return true;

        // Players can not take attack damage in a SafeZone, or possibly peaceful territory

        if (defLocFaction.getFlag(FFlag.PVP) == false) {
            if (damager instanceof Player) {
                if (notify) {
                    FPlayer attacker = FPlayers.i.get((Player) damager);
                    attacker.msg("<i>PVP is disabled in %s.", defLocFaction.describeTo(attacker));
                }
                return false;
            }
            return defLocFaction.getFlag(FFlag.MONSTERS);
        }

        if (!(damager instanceof Player))
            return true;

        FPlayer attacker = FPlayers.i.get((Player) damager);

        if (attacker == null || attacker.getPlayer() == null)
            return true;

        String name = damager.getName();
        String uuid = damager.getUniqueId().toString();
        if (Conf.playersWhoBypassAllProtection.contains(name) || Conf.playersWhoBypassAllProtection
                .contains(uuid))
            return true;

        if (this.main.shieldService != null && !this.main.shieldService.canAttack(defLocFaction, attacker)) {
            if (notify) {
                attacker.msg(Lang.shieldProtected, defLocFaction.describeTo(attacker));
            }
            return false;
        }

        if (attacker.hasLoginPvpDisabled()) {
            if (notify)
                attacker.msg("<i>You can't hurt other players for "
                        + Conf.noPVPDamageToOthersForXSecondsAfterLogin + " seconds after logging in.");
            return false;
        }

        Faction locFaction = Board.getFactionAt(new FLocation(attacker));

        // so we know from above that the defender isn't in a safezone... what about the attacker, sneaky dog that he might be?
        if (locFaction.getFlag(FFlag.PVP) == false) {
            if (notify)
                attacker.msg("<i>PVP is disabled in %s.", locFaction.describeTo(attacker));
            return false;
        }

        if (Conf.worldsIgnorePvP.contains(defenderLoc.getWorld().getName()))
            return true;

        Faction defendFaction = defender.getFaction();
        Faction attackFaction = attacker.getFaction();

        if (attackFaction.isNone() && Conf.disablePVPForFactionlessPlayers) {
            if (notify)
                attacker.msg("<i>You can't hurt other players until you join a faction.");
            return false;
        } else if (defendFaction.isNone()) {
            if (defLocFaction == attackFaction && Conf.enablePVPAgainstFactionlessInAttackersLand) {
                // Allow PVP vs. Factionless in attacker's faction territory
                return true;
            } else if (Conf.disablePVPForFactionlessPlayers) {
                if (notify)
                    attacker.msg("<i>You can't hurt players who are not currently in a faction.");
                return false;
            }
        }

        Rel relation = defendFaction.getRelationTo(attackFaction);

        // Check the relation
        if (defender.hasFaction() && relation.isAtLeast(Conf.friendlyFireFromRel)
                && defLocFaction.getFlag(FFlag.FRIENDLYFIRE) == false) {
            if (notify)
                attacker.msg("<i>You can't hurt %s<i>.", relation.getDescPlayerMany());
            return false;
        }

        // You can not hurt neutrals in their own territory.
        boolean ownTerritory = defender.isInOwnTerritory();
        if (defender.hasFaction() && ownTerritory && relation == Rel.NEUTRAL) {
            if (notify) {
                attacker.msg(
                        "<i>You can't hurt %s<i> in their own territory unless you declare them as an enemy.",
                        defender.describeTo(attacker));
                defender.msg("%s<i> tried to hurt you.", attacker.describeTo(defender, true));
            }
            return false;
        }
/*
		// Damage will be dealt. However check if the damage should be reduced.
		if (damage > 0.0 && defender.hasFaction() && ownTerritory && Conf.territoryShieldFactor > 0)
		{
			int newDamage = (int)Math.ceil(damage * (1D - Conf.territoryShieldFactor));
			sub.setDamage(newDamage);

			// Send message
			if (notify)
			{
				String perc = MessageFormat.format("{0,number,#%}", (Conf.territoryShieldFactor)); // TODO does this display correctly??
				defender.msg("<i>Enemy damage reduced by <rose>%s<i>.", perc);
			}
		}*/

        return true;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.isCancelled())
            return;
        if (event.getLocation() == null)
            return;

        FLocation floc = new FLocation(event.getLocation());
        Faction faction = Board.getFactionAt(floc);

        if (faction.getFlag(FFlag.MONSTERS))
            return;
        if (!Conf.monsters.contains(event.getEntityType()))
            return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.isCancelled())
            return;

        // if there is a target
        Entity target = event.getTarget();
        if (target == null)
            return;

        // We are interested in blocking targeting for certain mobs:
        if (!Conf.monsters.contains(MiscUtil.creatureTypeFromEntity(event.getEntity())))
            return;

        FLocation floc = new FLocation(target.getLocation());
        Faction faction = Board.getFactionAt(floc);

        if (faction.getFlag(FFlag.MONSTERS))
            return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPaintingBreak(HangingBreakEvent event) {
        if (event.isCancelled())
            return;

        if (event.getCause() == RemoveCause.EXPLOSION) {
            Faction faction = Board.getFactionAt(new FLocation(event.getEntity().getLocation()));
            if (faction.getFlag(FFlag.EXPLOSIONS) == false) {  // faction has explosions disabled
                event.setCancelled(true);
                return;
            }
        }

        if (!(event instanceof HangingBreakByEntityEvent)) {
            return;
        }

        Entity breaker = ((HangingBreakByEntityEvent) event).getRemover();
        if (!(breaker instanceof Player)) {
            return;
        }

        if (!FactionsBlockListener.playerCanBuildDestroyBlock((Player) breaker,
                event.getEntity().getLocation().getBlock(), "remove paintings", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPaintingPlace(HangingPlaceEvent event) {
        if (event.isCancelled())
            return;

        if (!FactionsBlockListener.playerCanBuildDestroyBlock(event.getPlayer(),
                event.getBlock().getLocation().getBlock(), "place paintings", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.isCancelled())
            return;

        Entity entity = event.getEntity();

        // for now, only interested in Enderman and Wither boss tomfoolery
        if (!(entity instanceof Enderman) && !(entity instanceof Wither))
            return;

        FLocation floc = new FLocation(event.getBlock());
        Faction faction = Board.getFactionAt(floc);

        if (entity instanceof Enderman) {
            if (!faction.getFlag(FFlag.ENDERGRIEF))
                event.setCancelled(true);
        } else if (entity instanceof Wither) {
            if (!faction.getFlag(FFlag.EXPLOSIONS))
                event.setCancelled(true);
        }
    }

    private void progressKill(Player player, FeatureConfig.MissionType type) {
        progressKill(player == null ? null : FPlayers.i.get(player), type);
    }

    private void progressKill(FPlayer fplayer, FeatureConfig.MissionType type) {
        if (fplayer == null || Main.main == null || Main.main.featureService == null) {
            return;
        }
        Faction faction = fplayer.getFaction();
        if (faction != null && faction.isNormal()) {
            Main.main.featureService.progress(faction, type, 1);
        }
    }
}
