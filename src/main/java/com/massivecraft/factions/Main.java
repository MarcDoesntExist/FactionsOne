package com.massivecraft.factions;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.GsonBuilder;
import com.massivecraft.factions.adapters.FFlagTypeAdapter;
import com.massivecraft.factions.adapters.FPermTypeAdapter;
import com.massivecraft.factions.adapters.LocationTypeAdapter;
import com.massivecraft.factions.adapters.RelTypeAdapter;
import com.massivecraft.factions.bank.BankCompensationTask;
import com.massivecraft.factions.bank.BankJournal;
import com.massivecraft.factions.bank.BankService;
import com.massivecraft.factions.bank.IdempotencyStore;
import com.massivecraft.factions.bank.StorageRetention;
import com.massivecraft.factions.cmd.CmdAutoHelp;
import com.massivecraft.factions.cmd.FCmdAdminRoot;
import com.massivecraft.factions.cmd.FCmdRoot;
import com.massivecraft.factions.config.YamlConfig;
import com.massivecraft.factions.economy.FactionTaxService;
import com.massivecraft.factions.economy.FactionTaxTask;
import com.massivecraft.factions.economy.TaxPeriodJournal;
import com.massivecraft.factions.gui.GuiConfig;
import com.massivecraft.factions.gui.MenuManager;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.integration.Essentials;
import com.massivecraft.factions.integration.Worldguard;
import com.massivecraft.factions.integration.map.MapIntegration;
import com.massivecraft.factions.listeners.FactionsBlockListener;
import com.massivecraft.factions.listeners.FactionsChatListener;
import com.massivecraft.factions.listeners.FactionsEntityListener;
import com.massivecraft.factions.listeners.FactionsExploitListener;
import com.massivecraft.factions.listeners.FactionsPlayerListener;
import com.massivecraft.factions.listeners.FactionsServerListener;
import com.massivecraft.factions.mailbox.FactionMailbox;
import com.massivecraft.factions.mailbox.MailboxExpiryTask;
import com.massivecraft.factions.rank.FactionRankingService;
import com.massivecraft.factions.shield.ShieldService;
import com.massivecraft.factions.struct.FFlag;
import com.massivecraft.factions.struct.FPerm;
import com.massivecraft.factions.struct.Rel;
import com.massivecraft.factions.struct.TerritoryAccess;
import com.massivecraft.factions.util.AutoLeaveTask;
import com.massivecraft.factions.util.BetrayalTracker;
import com.massivecraft.factions.util.StuckService;
import com.massivecraft.factions.util.TeleportWarmupService;
import com.massivecraft.factions.value.ClaimValueListener;
import com.massivecraft.factions.value.ClaimValueService;
import com.massivecraft.factions.util.EconLandRewardTask;
import com.massivecraft.factions.util.LazyLocation;
import com.massivecraft.factions.util.MissionResetTask;
import com.massivecraft.factions.zcore.MPlugin;
import com.massivecraft.factions.zcore.util.DataVersionService;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;


/**
 * @author drtshock
 * @author externo6
 * @author Olof Cayorion Larsson
 * @author Brett Flannigan
 * @author Daniel Saukel
 * @author MarcDoesntExists
 */
public class Main extends MPlugin implements CommandExecutor, TabCompleter {
    private static final long MAP_AUTO_UPDATE_TICKS = 20L;

    // Our single plugin instance
    public static Main main;

    // Listeners
    public final FactionsPlayerListener playerListener;
    public final FactionsChatListener chatListener;
    private com.massivecraft.factions.integration.placeholder.FactionsPlaceholderExpansion placeholderExpansion;
    public final FactionsEntityListener entityListener;
    public final FactionsExploitListener exploitListener;
    public final FactionsBlockListener blockListener;
    public final FactionsServerListener serverListener;
    // Commands
    public FCmdRoot cmdBase;
    public FCmdAdminRoot cmdAdminBase;
    public CmdAutoHelp cmdAutoHelp;
    // GUI
    public GuiConfig guiConfig;
    public MenuManager menuManager;
    // Faction features
    public FeatureConfig featureConfig;
    public FeatureService featureService;
    public WarpService warpService;
    public FlyService flyService;
    public ShieldService shieldService;
    public FactionRankingService rankingService;
    public ClaimValueService claimValueService;
    public TeleportWarmupService teleportWarmupService;
    public StuckService stuckService;
    public TntBankService tntBankService;
    public FactionTaxService factionTaxService;
    public TaxPeriodJournal taxPeriodJournal;
    public BankJournal bankJournal;
    public IdempotencyStore idempotencyStore;
    public FactionMailbox mailbox;
    public MapIntegration mapIntegration;
    public BetrayalTracker betrayalTracker;
    // Persistance related
    /**
     * Gson must skip static final constants such as baseCommandAliases, but it
     * still has to persist mutable static settings and migration markers.
     */
    private static final ExclusionStrategy STATIC_FINAL_FIELDS = new ExclusionStrategy() {
        @Override
        public boolean shouldSkipField(FieldAttributes field) {
            return field.hasModifier(Modifier.STATIC) && field.hasModifier(Modifier.FINAL);
        }

        @Override
        public boolean shouldSkipClass(Class<?> type) {
            return false;
        }
    };

    private boolean locked = false;
    private Integer AutoLeaveTask = null;
    private Integer econLandRewardTaskID = null;
    private Integer factionTaxTaskID = null;
    private Integer bankCompensationTaskID = null;
    private Integer mailboxExpiryTaskID = null;
    private Integer missionResetTaskID = null;
    private Integer mapAutoUpdateTask = null;

    public Main() {
        main = this;
        this.playerListener = new FactionsPlayerListener(this);
        this.chatListener = new FactionsChatListener(this);
        this.entityListener = new FactionsEntityListener(this);
        this.exploitListener = new FactionsExploitListener();
        this.blockListener = new FactionsBlockListener(this);
        this.serverListener = new FactionsServerListener(this);
    }

    public boolean getLocked() {
        return this.locked;
    }

    public void setLocked(boolean val) {
        this.locked = val;
        this.setAutoSave(val);
    }

    @Override
    protected boolean prepareData() {
        return DataVersionService.prepare(getDataFolder().toPath(), getLogger());
    }

    @Override
    public void onEnable() {
        if (!preEnable()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.loadSuccessful = false;

        // Load Conf from disk
        Conf.load();
        boolean playersLoaded = FPlayers.i.loadFromDisc();
        boolean factionsLoaded = Factions.i.loadFromDisc();
        if (!playersLoaded || !factionsLoaded) {
            getLogger().severe("Core player or faction data could not be read; "
                + "disabling FactionsOne instead of overwriting it with empty state.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        Conf.migrateColeaderPermissions();
        Conf.migrateExtendedPermissions();
        if (YamlConfig.wasGeneratedOnLoad()) {
            YamlConfig.save(this);
        }
        if (!Board.load()) {
            getLogger().severe("Core board data could not be read; "
                + "disabling FactionsOne instead of overwriting it with empty state.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.guiConfig = new GuiConfig(this);
        this.menuManager = new MenuManager(this, this.guiConfig);
        this.featureConfig = new FeatureConfig(this);
        this.featureService = new FeatureService(this, this.featureConfig);
        this.teleportWarmupService = new TeleportWarmupService(this);
        this.warpService = new WarpService(this, this.featureService, this.teleportWarmupService);
        this.flyService = new FlyService();
        this.shieldService = new ShieldService(Clock.systemUTC());
        this.rankingService = new FactionRankingService(Clock.systemUTC(), Conf.rankingCacheSeconds,
                FactionRankingService::collectLive);
        this.claimValueService = new ClaimValueService(this);
        this.stuckService = new StuckService(this, this.teleportWarmupService);
        this.tntBankService = new TntBankService(this.featureService);
        Path storage = getDataFolder().toPath();
        this.bankJournal = new BankJournal(storage.resolve("bank-journal.jsonl"), getLogger());
        this.idempotencyStore = new IdempotencyStore(storage.resolve("idempotency.json"), getLogger());
        this.taxPeriodJournal = new TaxPeriodJournal(storage.resolve("tax-journal.jsonl"),
                getLogger());
        this.mailbox = new FactionMailbox(storage.resolve("mailbox.json"), this.idempotencyStore,
                Clock.systemUTC(), Conf.mailboxMaxPerPlayer,
                TimeUnit.DAYS.toMillis(Math.max(1, Conf.mailboxTtlDays)), getLogger());
        compactStorage();

        this.betrayalTracker = new BetrayalTracker();

        // Add Base Commands
        this.cmdAutoHelp = new CmdAutoHelp();
        this.cmdBase = new FCmdRoot();
        this.cmdAdminBase = new FCmdAdminRoot();

        // Keep the existing plugin.yml registration, but wire suggestions into
        // the same FCommand tree used for execution.
        PluginCommand command = this.getCommand("factions");
        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        }
        PluginCommand adminCommand = this.getCommand("fa");
        if (adminCommand != null) {
            adminCommand.setExecutor(this);
            adminCommand.setTabCompleter(this);
        }
        for (String chatCommand : new String[]{"fc", "ac"}) {
            PluginCommand root = this.getCommand(chatCommand);
            if (root != null) {
                root.setExecutor(this);
                root.setTabCompleter(this);
            }
        }
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.placeholderExpansion =
                    new com.massivecraft.factions.integration.placeholder.FactionsPlaceholderExpansion(
                            this);
            this.placeholderExpansion.register();
        }

        Essentials.setup();
        Econ.setup();
        this.factionTaxService = new FactionTaxService(BankService.get(), Clock.systemUTC(),
                this.idempotencyStore, this.taxPeriodJournal);
        BankService.get().retryPendingCompensations();

        if (Conf.worldGuardChecking) {
            Worldguard.init(this);
        }

        // start up task which runs the autoLeaveAfterDaysOfInactivity routine
        startAutoLeaveTask(false);

        // start up task which runs the econLandRewardRoutine
        startEconLandRewardTask(false);

        // start up task which charges the periodic faction tax
        startFactionTaxTask(false);
        startBankCompensationTask(false);
        startMailboxExpiryTask(false);
        startMissionResetTask(false);

        // Register Event Handlers
        getServer().getPluginManager().registerEvents(this.playerListener, this);
        getServer().getPluginManager().registerEvents(this.chatListener, this);
        getServer().getPluginManager().registerEvents(this.entityListener, this);
        getServer().getPluginManager().registerEvents(this.exploitListener, this);
        getServer().getPluginManager().registerEvents(this.blockListener, this);
        getServer().getPluginManager().registerEvents(this.serverListener, this);
        getServer().getPluginManager().registerEvents(this.menuManager, this);
        getServer().getPluginManager().registerEvents(this.teleportWarmupService, this);
        getServer().getPluginManager().registerEvents(this.flyService, this);
        getServer().getPluginManager().registerEvents(
                new ClaimValueListener(this.claimValueService), this);

        this.claimValueService.start();

        this.mapIntegration = new MapIntegration(this);
        this.mapIntegration.start();

        startMapAutoUpdateTask();

        postEnable();
        this.loadSuccessful = true;
    }

    @Override
    public GsonBuilder getGsonBuilder() {
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping()
            .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.VOLATILE)
            .addSerializationExclusionStrategy(STATIC_FINAL_FIELDS)
            .addDeserializationExclusionStrategy(STATIC_FINAL_FIELDS)
            .registerTypeAdapter(LazyLocation.class, new LocationTypeAdapter())
            .registerTypeAdapter(TerritoryAccess.class, new TerritoryAccess())
            .registerTypeAdapter(Rel.class, new RelTypeAdapter())
            .registerTypeAdapter(FPerm.class, new FPermTypeAdapter())
            .registerTypeAdapter(FFlag.class, new FFlagTypeAdapter());
    }

    @Override
    public void onDisable() {
        if (this.claimValueService != null) {
            this.claimValueService.close();
            this.claimValueService = null;
        }
        if (this.mapIntegration != null) {
            this.mapIntegration.close();
            this.mapIntegration = null;
        }
        if (this.placeholderExpansion != null) {
            this.placeholderExpansion.unregister();
            this.placeholderExpansion = null;
        }
        // only save data if plugin actually completely loaded successfully
        if (this.loadSuccessful) {
            Board.save();
            Conf.save();
        }
        if (AutoLeaveTask != null) {
            this.getServer().getScheduler().cancelTask(AutoLeaveTask);
            AutoLeaveTask = null;
        }
        if (mapAutoUpdateTask != null) {
            this.getServer().getScheduler().cancelTask(mapAutoUpdateTask);
            mapAutoUpdateTask = null;
        }
        if (factionTaxTaskID != null) {
            this.getServer().getScheduler().cancelTask(factionTaxTaskID);
            factionTaxTaskID = null;
        }
        if (bankCompensationTaskID != null) {
            this.getServer().getScheduler().cancelTask(bankCompensationTaskID);
            bankCompensationTaskID = null;
        }
        if (mailboxExpiryTaskID != null) {
            this.getServer().getScheduler().cancelTask(mailboxExpiryTaskID);
            mailboxExpiryTaskID = null;
        }
        if (missionResetTaskID != null) {
            this.getServer().getScheduler().cancelTask(missionResetTaskID);
            missionResetTaskID = null;
        }
        super.onDisable();
    }

    public void startAutoLeaveTask(boolean restartIfRunning) {
        if (AutoLeaveTask != null) {
            if (!restartIfRunning)
                return;
            this.getServer().getScheduler().cancelTask(AutoLeaveTask);
        }

        if (Conf.autoLeaveRoutineRunsEveryXMinutes > 0.0) {
            long ticks = (long) (20 * 60 * Conf.autoLeaveRoutineRunsEveryXMinutes);
            AutoLeaveTask = getServer().getScheduler()
                    .scheduleSyncRepeatingTask(this, new AutoLeaveTask(), ticks, ticks);
        }
    }

    public void startEconLandRewardTask(boolean restartIfRunning) {
        if (econLandRewardTaskID != null) {
            if (!restartIfRunning)
                return;
            this.getServer().getScheduler().cancelTask(econLandRewardTaskID);
        }

        if (Conf.econEnabled && Conf.econLandRewardTaskRunsEveryXMinutes > 0.0
                && Conf.econLandReward > 0.0) {
            long ticks = 20L * 60 * Conf.econLandRewardTaskRunsEveryXMinutes;
            econLandRewardTaskID = getServer().getScheduler()
                    .scheduleSyncRepeatingTask(this, new EconLandRewardTask(), ticks, ticks);
        }
    }

    public void startFactionTaxTask(boolean restartIfRunning) {
        if (factionTaxTaskID != null) {
            if (!restartIfRunning)
                return;
            this.getServer().getScheduler().cancelTask(factionTaxTaskID);
            factionTaxTaskID = null;
        }

        if (Conf.taxEnabled && Conf.taxIntervalMinutes > 0) {
            long ticks = Math.min(Integer.MAX_VALUE, 20L * 60L * Conf.taxIntervalMinutes);
            factionTaxTaskID = getServer().getScheduler()
                    .scheduleSyncRepeatingTask(this, new FactionTaxTask(), ticks, ticks);
        }
    }

    public void startBankCompensationTask(boolean restartIfRunning) {
        if (bankCompensationTaskID != null) {
            if (!restartIfRunning) {
                return;
            }
            this.getServer().getScheduler().cancelTask(bankCompensationTaskID);
            bankCompensationTaskID = null;
        }
        if (Conf.bankCompensationIntervalMinutes <= 0) {
            return;
        }
        long ticks = Math.min(Integer.MAX_VALUE, 20L * 60L * Conf.bankCompensationIntervalMinutes);
        bankCompensationTaskID = this.getServer().getScheduler()
                .scheduleSyncRepeatingTask(this, new BankCompensationTask(), ticks, ticks);
    }

    public void startMailboxExpiryTask(boolean restartIfRunning) {
        if (mailboxExpiryTaskID != null) {
            if (!restartIfRunning) {
                return;
            }
            this.getServer().getScheduler().cancelTask(mailboxExpiryTaskID);
            mailboxExpiryTaskID = null;
        }
        if (Conf.mailboxExpiryCheckMinutes <= 0) {
            return;
        }
        long ticks = Math.min(Integer.MAX_VALUE, 20L * 60L * Conf.mailboxExpiryCheckMinutes);
        mailboxExpiryTaskID = this.getServer().getScheduler()
                .scheduleSyncRepeatingTask(this, new MailboxExpiryTask(), ticks, ticks);
    }

    public void startMissionResetTask(boolean restartIfRunning) {
        if (missionResetTaskID != null) {
            if (!restartIfRunning) {
                return;
            }
            this.getServer().getScheduler().cancelTask(missionResetTaskID);
            missionResetTaskID = null;
        }
        if (Conf.missionResetCheckMinutes <= 0) {
            return;
        }
        long ticks = Math.min(Integer.MAX_VALUE,
                20L * 60L * Conf.missionResetCheckMinutes);
        missionResetTaskID = this.getServer().getScheduler()
                .scheduleSyncRepeatingTask(this, new MissionResetTask(), ticks, ticks);
    }

    public boolean refundMailboxClaim(FactionMailbox.Claim claim) {
        return claim != null && claim.sourceAccount() != null && !claim.sourceAccount().isBlank()
                && BankService.get().creditAccount(claim.sourceAccount(), claim.amount());
    }

    /**
     * Compacts the durable bank/mailbox state. Called at enable and by the
     * expiry task; it never removes pending or ambiguous work.
     */
    public void compactStorage() {
        StorageRetention.compact(this);
    }

    private void startMapAutoUpdateTask() {
        if (mapAutoUpdateTask != null) {
            this.getServer().getScheduler().cancelTask(mapAutoUpdateTask);
        }
        mapAutoUpdateTask = getServer().getScheduler()
                .scheduleSyncRepeatingTask(this, this::updateAutoMaps,
                        MAP_AUTO_UPDATE_TICKS, MAP_AUTO_UPDATE_TICKS);
    }

    private void updateAutoMaps() {
        for (Player player : getServer().getOnlinePlayers()) {
            FPlayer fplayer = FPlayers.i.get(player);
            if (fplayer == null || !fplayer.isMapAutoUpdating()) {
                continue;
            }
            player.sendActionBar(Board.getActionBarMap(fplayer, new FLocation(player)));
        }
    }

    @Override
    public void postAutoSave() {
        Board.save();
        Conf.save();
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, Command command, String label, String[] split) {
        if ("fc".equalsIgnoreCase(command.getName())) {
            this.cmdBase.cmdChat.cmdChatFaction.execute(sender,
                    new ArrayList<String>(Arrays.asList(split)));
            return true;
        }
        if ("ac".equalsIgnoreCase(command.getName())) {
            this.cmdBase.cmdChat.cmdChatAlly.execute(sender,
                    new ArrayList<String>(Arrays.asList(split)));
            return true;
        }
        if ("fa".equalsIgnoreCase(command.getName())) {
            this.cmdAdminBase.execute(sender, new ArrayList<String>(Arrays.asList(split)));
            return true;
        }
        this.cmdBase.execute(sender, new ArrayList<String>(Arrays.asList(split)));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NonNull CommandSender sender, Command command, String alias,
                                      String[] args) {
        List<String> tokens = new ArrayList<>(Arrays.asList(args));
        tokens.removeIf(String::isEmpty);
        // A trailing space must stay meaningful: it asks for the next argument.
        if (args.length > 0 && args[args.length - 1].isEmpty()) {
            tokens.add("");
        }
        if ("fc".equalsIgnoreCase(command.getName())) {
            return this.cmdBase.cmdChat.cmdChatFaction.tabComplete(sender, tokens);
        }
        if ("ac".equalsIgnoreCase(command.getName())) {
            return this.cmdBase.cmdChat.cmdChatAlly.tabComplete(sender, tokens);
        }
        if ("fa".equalsIgnoreCase(command.getName())) {
            return this.cmdAdminBase.tabComplete(sender, tokens);
        }
        return this.cmdBase.tabComplete(sender, tokens);
    }

    // -------------------------------------------- //
    // Functions for other plugins to hook into
    // -------------------------------------------- //

    // This value will be updated whenever new hooks are added
    public int hookSupportVersion() {
        return 3;
    }

    // If another plugin is handling insertion of chat tags, this should be used to notify Factions
    public void handleFactionTagExternally(boolean notByFactions) {
        Conf.chatTagHandledByAnotherPlugin = notByFactions;
    }

    // Simply put, should this chat event be left for Factions to handle? For now, that means players with Faction Chat
    // enabled or use of the Factions f command without a slash; combination of isPlayerFactionChatting() and isFactionsCommand()


    public boolean shouldLetFactionsHandleThisChat(AsyncChatEvent event) {
        if (event == null)
            return false;
        return (isPlayerFactionChatting(event.getPlayer()) || isFactionsCommand(
                PlainTextComponentSerializer.plainText().serialize(event.message())));
    }

    // Does player have Faction Chat enabled? If so, chat plugins should preferably not do channels,
    // local chat, or anything else which targets individual recipients, so Faction Chat can be done

    /**
     * @deprecated As of release 1.8, there is no built in faction chat.
     */
    @Deprecated
    public boolean isPlayerFactionChatting(Player player) {
        return false;
    }

    // Is this chat message actually a Factions command, and thus should be left alone by other plugins?

    /**
     * @deprecated As of release 1.8.1 the normal Bukkit command-handling is used.
     */
    @Deprecated
    public boolean isFactionsCommand(String check) {
        return false;
    }

    // Get a player's faction tag (faction name), mainly for usage by chat plugins for local/channel chat
    public String getPlayerFactionTag(Player player) {
        return getPlayerFactionTagRelation(player, null);
    }

    // Same as above, but with relation (enemy/neutral/ally) coloring potentially added to the tag
    public String getPlayerFactionTagRelation(Player speaker, Player listener) {
        String tag = "~";

        if (speaker == null)
            return tag;

        FPlayer me = FPlayers.i.get(speaker);
        if (me == null)
            return tag;

        // if listener isn't set, or config option is disabled, give back uncolored tag
        if (listener == null || !Conf.chatParseTagsColored) {
            tag = me.getChatTag().trim();
        } else {
            FPlayer you = FPlayers.i.get(listener);
            if (you == null)
                tag = me.getChatTag().trim();
            else  // everything checks out, give the colored tag
                tag = me.getChatTag(you).trim();
        }
        if (tag.isEmpty())
            tag = "~";

        return tag;
    }

    // Get a player's title within their faction, mainly for usage by chat plugins for local/channel chat
    public String getPlayerTitle(Player player) {
        if (player == null)
            return "";

        FPlayer me = FPlayers.i.get(player);
        if (me == null)
            return "";

        return me.getTitle().trim();
    }

    // Get a list of all faction tags (names)
    public Set<String> getFactionTags() {
        Set<String> tags = new HashSet<String>();
        for (Faction faction : Factions.i.get()) {
            tags.add(faction.getTagPlain());
        }
        return tags;
    }

    // Get a list of all players in the specified faction
    public Set<String> getPlayersInFaction(String factionTag) {
        Set<String> players = new HashSet<String>();
        Faction faction = Factions.i.getByTag(factionTag);
        if (faction != null) {
            for (FPlayer fplayer : faction.getFPlayers()) {
                players.add(fplayer.getName());
            }
        }
        return players;
    }

    // Get a list of all online players in the specified faction
    public Set<String> getOnlinePlayersInFaction(String factionTag) {
        Set<String> players = new HashSet<String>();
        Faction faction = Factions.i.getByTag(factionTag);
        if (faction != null) {
            for (FPlayer fplayer : faction.getFPlayersWhereOnline(true)) {
                players.add(fplayer.getName());
            }
        }
        return players;
    }

    // check if player is allowed to build/destroy in a particular location
    public boolean isPlayerAllowedToBuildHere(Player player, Location location) {
        return FactionsBlockListener
                .playerCanBuildDestroyBlock(player, location.getBlock(), "", true);
    }

    // check if player is allowed to interact with the specified block (doors/chests/whatever)
    public boolean isPlayerAllowedToInteractWith(Player player, Block block) {
        return FactionsPlayerListener.canPlayerUseBlock(player, block, true);
    }

    // check if player is allowed to use a specified item (flint&steel, buckets, etc) in a particular location
    public boolean isPlayerAllowedToUseThisHere(Player player, Location location,
                                                Material material) {
        return FactionsPlayerListener.playerCanUseItemHere(player, location, material, true);
    }
}
