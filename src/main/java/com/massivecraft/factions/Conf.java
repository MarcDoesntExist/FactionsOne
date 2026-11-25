package com.massivecraft.factions;

import com.massivecraft.factions.api.LegacyUtil;
import com.massivecraft.factions.struct.FFlag;
import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.struct.Rel;
import com.massivecraft.factions.util.Color;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.*;

/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 */
public class Conf {
    public final static transient List<String> baseCommandAliases = new ArrayList<String>();
    public final transient static Set<Material> materialsEditOnInteract =
            EnumSet.noneOf(Material.class);
    public final transient static Set<Material> materialsEditTools = EnumSet.noneOf(Material.class);
    public final transient static Set<Material> materialsDoor = EnumSet.noneOf(Material.class);
    public final transient static Set<Material> materialsContainer = EnumSet.noneOf(Material.class);
    // -------------------------------------------- //
    // Persistance
    // -------------------------------------------- //
    private static final transient Conf i = new Conf();
    // Colors
    public static Color colorMember = Color.GREEN;
    public static Color colorAlly = Color.DARK_PURPLE;
    //public static Color colorWilderness = Color.DARK_GREEN;
    public static Color colorTruce = Color.LIGHT_PURPLE;
    public static Color colorNeutral = Color.WHITE;
    public static Color colorEnemy = Color.RED;
    public static Color colorNoPVP = Color.GOLD;
    public static Color colorFriendlyFire = Color.DARK_RED;
    public static Map<FFlag, Boolean> factionFlagDefaults;
    public static Map<FFlag, Boolean> factionFlagIsChangeable;
    public static Map<FPerm, Set<Rel>> factionPermDefaults;
    public static boolean coleaderPermsMigrated = false;
    public static boolean extendedPermsMigrated = false;
    public static Rel factionRankDefault = Rel.RECRUIT;
    // Power
    public static double powerPlayerMax = 10.0;
    public static double powerPlayerMin = -10.0;
    public static double powerPlayerStarting = 10.0; // New players start out with this power level
    public static double powerPerMinute = 0.2;
    // Default health rate... it takes 5 min to heal one power
    public static double powerPerDeath = 4.0; // A death makes you lose 4 power
    public static boolean scaleNegativePower = false;
    // if greater than 0, the cap on how much power a faction can have (additional power from players beyond that will act as a "buffer" of sorts)
    // Power regeneration rate increase as power decreases
    public static double scaleNegativeDivisor = 40.0;
    // Divisor for inverse power regeneration curve
    public static boolean powerRegenOffline = false;
    // does player power regenerate even while they're offline?
    public static double powerOfflineLossPerDay = 0.0;
    // players will lose this much power per day offline
    public static double powerOfflineLossLimit = 0.0;
    // players will no longer lose power from being offline once their power drops to this amount or less
    public static double powerFactionMax = 0.0;
    public static String prefixLeader = "**";
    public static String prefixColeader = "***";
    public static String prefixOfficer = "*";
    public static String prefixMember = "+";
    public static String prefixRecruit = "-";
    public static int factionTagLengthMin = 3;

    //public static boolean showMapFactionKey = true;
    //public static boolean showNeutralFactionsOnMap = true;
    //public static boolean showEnemyFactionsOnMap = true;
    public static int factionTagLengthMax = 10;
    public static boolean factionTagForceUpperCase = false;
    public static boolean newFactionsDefaultOpen = false;
    // when faction membership hits this limit, players will no longer be able to join using /f join; default is 0, no limit
    public static int factionMemberLimit = 0;
    // what faction ID to start new players in when they first join the server; default is 0, "no faction"
    public static String newPlayerStartingFactionID = "0";
    // Disallow joining/leaving/kicking while power is negative
    public static boolean canLeaveWithNegativePower = true;
    // Configuration on the Faction tag in chat messages.
    public static boolean chatSetFormat = false;
    public static String chatSetFormatTo =
            "<{faction_relcolor}\u00A7l{faction_roleprefix}\u00A7lr{faction_relcolor}{faction_tag_pr}"
                    + Color.WHITE.toString() + "%s> %s";
    // This one is almost deprecated now right? or is it?
    public static boolean chatParseTags = true;
    public static boolean chatParseTagsColored = false;
    public static Map<String, String> chatSingleFormats = new HashMap<String, String>();
    public static transient boolean chatTagHandledByAnotherPlugin = false;
    // Why do we need this? (Olof asks)
    public static String chatTagFormat = "%s" + Color.WHITE;
    public static boolean broadcastDescriptionChanges = false;
    public static double saveToFileEveryXMinutes = 30.0;
    public static double autoLeaveAfterDaysOfInactivity = 10.0;
    public static double autoLeaveRoutineRunsEveryXMinutes = 5.0;
    public static int autoLeaveRoutineMaxMillisecondsPerTick = 5;
    // 1 server tick is roughly 50ms, so default max 10% of a tick
    public static boolean removePlayerDataWhenBanned = true;
    public static boolean worldGuardChecking = false;
    //LWC
    public static boolean lwcIntegration = false;
    public static boolean onUnclaimResetLwcLocks = false;
    public static boolean onCaptureResetLwcLocks = false;
    // server logging options
    public static boolean logFactionCreate = true;
    public static boolean logFactionDisband = true;
    public static boolean logFactionJoin = true;
    public static boolean logFactionKick = true;
    public static boolean logFactionLeave = true;
    public static boolean logLandClaims = true;
    public static boolean logLandUnclaims = true;
    public static boolean logMoneyTransactions = true;
    public static boolean logPlayerCommands = true;
    // prevent some potential exploits
    public static boolean handleExploitObsidianGenerators = true;
    public static boolean handleExploitEnderPearlClipping = true;
    public static boolean handleExploitInteractionSpam = true;
    public static boolean handleExploitTNTWaterlog = false;
    public static boolean homesEnabled = true;
    public static boolean homesMustBeInClaimedTerritory = true;
    public static boolean homesTeleportToOnDeath = true;
    public static boolean homesRespawnFromNoPowerLossWorlds = true;
    public static boolean homesTeleportCommandEnabled = true;
    public static boolean homesTeleportCommandEssentialsIntegration = true;
    public static boolean homesTeleportCommandSmokeEffectEnabled = true;
    public static boolean homesTeleportAllowedFromEnemyTerritory = true;
    public static boolean homesTeleportAllowedFromDifferentWorld = true;
    public static double homesTeleportAllowedEnemyDistance = 32.0;

    //public static boolean peacefulMembersDisablePowerLoss = true;
    public static boolean homesTeleportIgnoreEnemiesIfInOwnTerritory = true;
    public static Rel friendlyFireFromRel = Rel.TRUCE;
    public static boolean disablePVPForFactionlessPlayers = false;
    public static boolean enablePVPAgainstFactionlessInAttackersLand = false;
    public static int noPVPDamageToOthersForXSecondsAfterLogin = 3;
    public static boolean permanentFactionsDisableLeaderPromotion = false;
    public static boolean claimsMustBeConnected = false;

    //public static double considerFactionsReallyOfflineAfterXMinutes = 0.0;
    public static boolean claimingFromOthersAllowed = true;
    public static boolean claimsCanBeUnconnectedIfOwnedByOtherFaction = true;
    public static int claimsRequireMinFactionMembers = 1;
    public static int claimedLandsMax = 0;
    // if someone is doing a radius claim and the process fails to claim land this many times in a row, it will exit
    public static int radiusClaimFailureLimit = 9;
    public static boolean protectOfflineFactionsFromExplosions = false;


    // for claimed areas where further faction-member ownership can be defined
    public static double offlineExplosionProtectionDelay = 2.0;
    public static int actionDeniedPainAmount = 2;
    // commands which will be prevented if the player is a member of a permanent faction
    public static Set<String> permanentFactionMemberDenyCommands = new LinkedHashSet<String>();
    // commands which will be prevented when in claimed territory of another faction
    public static Set<String> territoryNeutralDenyCommands = new LinkedHashSet<String>();
    public static Set<String> territoryEnemyDenyCommands = new LinkedHashSet<String>();

    //public static Set<Material> territoryProtectedMaterialsWhenOffline = EnumSet.noneOf(Material.class);
    //public static Set<Material> territoryDenyUseageMaterialsWhenOffline = EnumSet.noneOf(Material.class);
    public static boolean pistonProtectionThroughDenyBuild = true;
    // TODO: Rename to monsterCreatureTypes
    public static transient Set<EntityType> monsters = EnumSet.noneOf(EntityType.class);
    // Economy settings
    public static boolean econEnabled = false;
    public static String econUniverseAccount = "";
    public static double econCostClaimWilderness = 30.0;
    public static double econCostClaimFromFactionBonus = 30.0;
    public static double econClaimAdditionalMultiplier = 0.5;
    public static double econClaimRefundMultiplier = 0.7;
    public static double econClaimUnconnectedFee = 0.0;
    public static double econCostCreate = 100.0;
    public static double econCostSethome = 30.0;
    public static double econCostJoin = 0.0;
    public static double econCostLeave = 0.0;
    public static double econCostKick = 0.0;
    public static double econCostInvite = 0.0;
    public static double econCostHome = 0.0;
    public static double econCostTag = 0.0;
    public static double econCostDesc = 0.0;
    public static double econCostTitle = 0.0;
    public static double econCostList = 0.0;
    public static double econCostMap = 0.0;
    public static double econCostPower = 0.0;
    public static double econCostShow = 0.0;
    public static double econCostOpen = 0.0;
    public static double econCostAlly = 0.0;
    public static double econCostTruce = 0.0;
    public static double econCostNeutral = 0.0;
    public static double econCostEnemy = 0.0;
    public static int econLandRewardTaskRunsEveryXMinutes = 20;
    public static double econLandReward = 0.00;
    // Periodic faction tax (tax + upkeep). Disabled by default.
    public static boolean taxEnabled = false;
    public static int taxIntervalMinutes = 1440;
    public static double taxBase = 0.0;
    public static double taxPerMember = 0.0;
    public static double taxPerChunk = 0.0;
    public static double taxCap = 0.0;
    public static int taxGracePeriods = 3;
    public static String taxInsolvencyAction = "NONE";
    public static boolean taxNotifyOfficers = true;
    // Durable bank retries and the offline payout mailbox.
    public static int bankCompensationIntervalMinutes = 5;
    public static boolean mailboxEnabled = true;
    public static int mailboxTtlDays = 30;
    public static int mailboxMaxPerPlayer = 20;
    public static int mailboxExpiryCheckMinutes = 60;
    // Old idempotency keys and terminal mailbox claims are compacted after
    // these windows. Keep the idempotency window longer than the mailbox window.
    public static int idempotencyRetentionDays = 180;
    public static int mailboxRetentionDays = 30;
    // Grace and shield windows for faction territory. Disabled by default.
    public static boolean shieldEnabled = false;
    public static int shieldGraceHours = 24;
    public static int shieldMaxHours = 168;
    // Ranking and exploration helpers
    public static int rankingCacheSeconds = 30;
    public static int nearDefaultRadiusChunks = 50;
    public static int nearMaxRadiusChunks = 200;
    public static int stuckWarmupSeconds = 3;
    //Faction banks, to pay for land claiming and other costs instead of individuals paying for them
    public static boolean bankEnabled = true;
    public static boolean warpsEnabled = true;
    public static int warpMax = 5;
    public static boolean warpMustBeInTerritory = true;
    public static int warpWarmupSeconds = 3;
    public static int warpCooldownSeconds = 10;
    public static boolean tntBankEnabled = true;
    public static int tntBankMax = 512;
    public static int factionLogLimit = 100;
    public static String chatAllyFormat = "<pink>[Ally] <white>%s: %s";
    //public static boolean bankMembersCanWithdraw = false; //Have to be at least moderator to withdraw or pay money to another faction
    public static boolean bankFactionPaysCosts = true;
    //The faction pays for land claiming costs.
    //The faction pays for faction command costs, such as sethome
    public static boolean bankFactionPaysLandCosts = true;
    // mainly for other plugins/mods that use a fake player to take actions, which shouldn't be subject to our protections
    public static Set<String> playersWhoBypassAllProtection = new LinkedHashSet<String>();
    public static Set<String> worldsNoClaiming = new LinkedHashSet<String>();
    public static Set<String> worldsNoPowerLoss = new LinkedHashSet<String>();
    // TODO: A better solution Would be to have One wilderness faction per world.
    //public static Set<String> worldsNoWildernessProtection = new LinkedHashSet<String>();
    public static Set<String> worldsIgnorePvP = new LinkedHashSet<String>();
    public static transient int mapHeight = 8;
    public static transient int mapWidth = 39;
    public static transient char[] mapKeyChrs =
            "\\/#?$%=&^ABCDEFGHJKLMNOPQRSTUVWXYZ1234567890abcdeghjmnopqrsuvwxyz".toCharArray();

    static {
        baseCommandAliases.add("f");

        factionFlagDefaults = new LinkedHashMap<FFlag, Boolean>();
        for (FFlag flag : FFlag.values()) {
            factionFlagDefaults.put(flag, flag.defaultDefaultValue);
        }

        factionPermDefaults = new LinkedHashMap<FPerm, Set<Rel>>();
        for (FPerm perm : FPerm.values()) {
            factionPermDefaults.put(perm, perm.defaultDefaultValue);
        }

        chatSingleFormats.put("pl", " %s");
        chatSingleFormats.put("pr", "%s ");
        chatSingleFormats.put("pb", " %s ");

        territoryEnemyDenyCommands.add("home");
        territoryEnemyDenyCommands.add("sethome");
        territoryEnemyDenyCommands.add("spawn");
        territoryEnemyDenyCommands.add("tpahere");
        territoryEnemyDenyCommands.add("tpaccept");
        territoryEnemyDenyCommands.add("tpa");
        territoryEnemyDenyCommands.add("warp");

        materialsContainer.add(Material.DISPENSER);
        materialsContainer.add(Material.CHEST);
        materialsContainer.add(Material.FURNACE);
        if (!LegacyUtil.is1_13)
            materialsContainer.add(Material.valueOf("BURNING_FURNACE"));
        materialsContainer.add(Material.JUKEBOX);
        materialsContainer.add(Material.BREWING_STAND);
        materialsContainer.add(LegacyUtil.ENCHANTING_TABLE);
        materialsContainer.add(Material.ANVIL);
        materialsContainer.add(Material.BEACON);
        materialsContainer.add(Material.TRAPPED_CHEST);
        materialsContainer.add(Material.DROPPER);
        materialsContainer.add(Material.HOPPER);

        if (!LegacyUtil.is1_13) {
            materialsEditOnInteract.add(Material.valueOf("DIODE_BLOCK_OFF"));
            materialsEditOnInteract.add(Material.valueOf("DIODE_BLOCK_ON"));
        } else
            materialsEditOnInteract.add(Material.valueOf("REPEATER"));
        materialsEditOnInteract.add(Material.NOTE_BLOCK);
        materialsEditOnInteract.add(Material.CAULDRON);
        materialsEditOnInteract.add(LegacyUtil.FARMLAND);

        for (Material type : LegacyUtil.DOORS)
            materialsDoor.add(type);

        materialsEditTools.add(LegacyUtil.FIRE_CHARGE);
        materialsEditTools.add(Material.FLINT_AND_STEEL);
        materialsEditTools.add(Material.BUCKET);
        materialsEditTools.add(Material.WATER_BUCKET);
        materialsEditTools.add(Material.LAVA_BUCKET);

        monsters.add(EntityType.BLAZE);
        monsters.add(EntityType.CAVE_SPIDER);
        monsters.add(EntityType.CREEPER);
        monsters.add(EntityType.ENDERMAN);
        monsters.add(EntityType.ENDER_DRAGON);
        monsters.add(EntityType.GHAST);
        monsters.add(EntityType.GIANT);
        monsters.add(EntityType.MAGMA_CUBE);
        monsters.add(EntityType.ZOMBIFIED_PIGLIN);
        monsters.add(EntityType.SILVERFISH);
        monsters.add(EntityType.SKELETON);
        monsters.add(EntityType.SLIME);
        monsters.add(EntityType.SPIDER);
        monsters.add(EntityType.WITCH);
        monsters.add(EntityType.WITHER);
        monsters.add(EntityType.ZOMBIE);
    }

    public static void load() {
        Main.main.persist.loadOrSaveDefault(i, Conf.class, "conf");
        if (factionPermDefaults != null) {
            factionPermDefaults.keySet().removeIf(Objects::isNull);
            factionPermDefaults.values().removeIf(Objects::isNull);
        }
        if (factionFlagDefaults != null) {
            factionFlagDefaults.keySet().removeIf(Objects::isNull);
            factionFlagDefaults.values().removeIf(Objects::isNull);
            for (FFlag flag : FFlag.values()) {
                factionFlagDefaults.putIfAbsent(flag, flag.defaultDefaultValue);
            }
        }
    }

    /**
     * Adds the co-leader role to existing configurations and faction overrides once.
     * The marker keeps later administrative removals from being reverted on restart.
     */
    public static void migrateColeaderPermissions() {
        if (coleaderPermsMigrated) {
            return;
        }

        if (factionPermDefaults == null) {
            factionPermDefaults = new LinkedHashMap<FPerm, Set<Rel>>();
        }
        for (FPerm perm : FPerm.values()) {
            Set<Rel> configured = factionPermDefaults.get(perm);
            if (configured == null) {
                configured = new HashSet<Rel>();
                configured.addAll(perm.defaultDefaultValue);
                factionPermDefaults.put(perm, configured);
            }
            if (perm.defaultDefaultValue.contains(Rel.COLEADER)) {
                configured.add(Rel.COLEADER);
            }
        }

        for (Faction faction : Factions.i.get()) {
            faction.migrateColeaderPermissions();
        }
        coleaderPermsMigrated = true;
    }

    /**
     * Adds defaults for permissions introduced after the current config was written.
     * Existing configured values are preserved.
     */
    public static void migrateExtendedPermissions() {
        if (extendedPermsMigrated) {
            return;
        }

        if (factionPermDefaults == null) {
            factionPermDefaults = new LinkedHashMap<FPerm, Set<Rel>>();
        }
        for (FPerm perm : FPerm.values()) {
            if (factionPermDefaults.get(perm) == null) {
                Set<Rel> defaults = new HashSet<Rel>();
                defaults.addAll(perm.defaultDefaultValue);
                factionPermDefaults.put(perm, defaults);
            }
        }
        extendedPermsMigrated = true;
    }

    public static void save() {
        Main.main.persist.saveAsync(i);
    }
}

