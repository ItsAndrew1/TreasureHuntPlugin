//Developed by _ItsAndrew_
package me.andrew.EchoesOfGold;

import me.andrew.EchoesOfGold.Economy.*;
import me.andrew.EchoesOfGold.TreasureManagerGUIs.*;

import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class EchoesOfGold extends JavaPlugin implements Listener{
    private YMLfiles treasures;
    private YMLfiles playerdata;
    private EventScoreboard scoreboardManager;
    private EventProgress eventProgressManager;
    private int savedDuration;
    private final Map<UUID, Consumer<String>> chatInput = new HashMap<>();
    private TreasureManager treasureManager;
    private EventBossBar bossBar;

    //Variables for the Event Progress
    private boolean eventActive;
    private long eventDuration;

    //Defining the Economy Objects
    private EconomyProvider economyProvider;
    private Economy vaultInstance;
    private ShopGUI shopGUI;
    private ShopGuiFinalChoice shopGuiFinalChoice;
    private DatabaseManager dbManager;
    private PlaceholdersManager placeholderManager;

    //Defining the GUIs
    private MainManageGUI manageGUI;
    private RewardsChoiceGUI rewardsChoiceGUI;
    private ManageTreasuresGUI manageTreasuresGUI;
    private AllTreasuresGUI allTreasuresGUI;
    private AddRewardsGUI addRewardsGUI;
    private HintsGUI hintsGUI;
    private String treasureManagerChoice;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        //Defining the YML files and main objects
        treasures = new YMLfiles(this, "treasures.yml");
        treasureManager = new TreasureManager(this);
        bossBar = new EventBossBar(this);
        playerdata = new YMLfiles(this, "playerdata.yml");
        scoreboardManager = new EventScoreboard(this);
        eventProgressManager = new EventProgress(this);
        dbManager = new DatabaseManager(this);
        ShopItem shopItem = new ShopItem(this);
        placeholderManager = new PlaceholdersManager();

        //Defining the GUIs
        manageTreasuresGUI = new ManageTreasuresGUI(this);
        rewardsChoiceGUI = new RewardsChoiceGUI(this);
        manageGUI = new MainManageGUI(this);
        allTreasuresGUI = new AllTreasuresGUI(this);
        addRewardsGUI = new AddRewardsGUI(this);
        hintsGUI = new HintsGUI(this);
        shopGuiFinalChoice = new ShopGuiFinalChoice(this);
        shopGUI = new ShopGUI(this);

        reloadConfig();
        getTreasures().reloadConfig();
        getPlayerData().reloadConfig();

        //Setting the commands and their tabs
        getCommand("eog").setExecutor(new CommandManager(this));
        getCommand("eog").setTabCompleter(new CommandTabs(this));

        boolean toggleInternalEconomy = getConfig().getBoolean("economy.internal-economy.toggle", true);
        if(toggleInternalEconomy) getCommand("thshop").setExecutor(new CommandManager(this));

        boolean toggleHints = getConfig().getBoolean("hints-gui.toggle-hints", false);
        if(toggleHints) getCommand("hints").setExecutor(new CommandManager(this));

        getServer().getPluginManager().registerEvents(manageGUI, this); //Events of mainManageGUI
        getServer().getPluginManager().registerEvents(manageTreasuresGUI, this); //Events of manageTreasuresGUI
        getServer().getPluginManager().registerEvents(rewardsChoiceGUI, this); //Events of rewardsChoiceGUI
        getServer().getPluginManager().registerEvents(allTreasuresGUI, this); //Events of allTreasuresGUI
        getServer().getPluginManager().registerEvents(hintsGUI, this); //Events of hints GUI
        getServer().getPluginManager().registerEvents(new TreasureClickEvent(this), this); //Events of treasure click
        getServer().getPluginManager().registerEvents(this, this); //Events of the main plugin class
        getServer().getPluginManager().registerEvents(eventProgressManager, this); //Events of EventProgress class
        getServer().getPluginManager().registerEvents(addRewardsGUI, this); //Events of AddRewards GUI
        getServer().getPluginManager().registerEvents(shopGUI, this); //Events of Shop GUI
        getServer().getPluginManager().registerEvents(shopGuiFinalChoice, this);
        getServer().getPluginManager().registerEvents(shopItem, this);

        //Regaining data after a reload
        savedDuration = getConfig().getInt("saved-duration");
        if(savedDuration > 0){
            eventDuration = savedDuration;
            getEventProgressManager().startEvent(formatTime(eventDuration));
            eventActive = true;

            //Restart the boss bar (if it is toggled)
            boolean toggleBossBar = getConfig().getBoolean("boss-bar");
            if(toggleBossBar) bossBar.startBossBar();

            //Restart the scoreboard (if it is toggled)
            boolean toggleScoreboard = getConfig().getBoolean("scoreboard");
            if(toggleScoreboard) scoreboardManager.startScoreboard();

            treasureManager.spawnTreasures();
            treasureManager.spawnChestParticles();

            //Putting the saved player items in the map (if the hints item is toggled)
            boolean toggleHintsItem = getConfig().getBoolean("hints-gui.hints-item.toggle", false);
            if(toggleHintsItem){
                for(OfflinePlayer p : Bukkit.getOfflinePlayers()){
                    FileConfiguration playerData = playerdata.getConfig();

                    ConfigurationSection targetPlayerSection = playerData.getConfigurationSection("players."+p.getUniqueId());
                    if(targetPlayerSection == null) continue; //Skips the players that aren't a part of the event

                    //Getting the item stack and saving it in the map
                    ItemStack savedItem = playerData.getItemStack("players."+p.getUniqueId()+".saved-item");
                    if(savedItem == null || savedItem.getType().equals(Material.AIR)) continue; //Skips the players that didn't save anything

                    getEventProgressManager().putItemsInMap(p.getUniqueId(), savedItem);
                }
            }

            getLogger().warning("[E.O.G] Resumed event!");
        }
        else{
            eventActive = false;
            for(Player p : Bukkit.getOnlinePlayers()){
                p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            }
        }

        //Setting up the economy (if it is toggled)
        boolean toggleEconomy = getConfig().getBoolean("economy.toggle-using-economy", false);
        if(toggleEconomy){
            if(!toggleInternalEconomy && setupVault()){
                economyProvider = new VaultEconomyProvider(vaultInstance, this);
                getLogger().info("[E.O.G] Enabled using Vault economy!");
            }
            else{
                //Setting up the PlayerBalance custom placeholder
                if(getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    new EogPlaceholders(this).register();
                    getLogger().info("[E.O.G] Enabled the placeholder for player balance!");
                }

                try {
                    dbManager.connectDB();
                    economyProvider = new InternalEconomyProvider(dbManager, this);
                    getLogger().info("Enabled using internal economy!");
                } catch (SQLException e) {
                    getLogger().warning("[E.O.G] There was an error connecting to the database! "+e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean setupVault(){
        if(getServer().getPluginManager().getPlugin("Vault") == null){
            getLogger().warning("[E.O.G] Vault is not installed! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }

        vaultInstance = rsp.getProvider();
        return true;
    }

    //Saving data after shutting down the server
    @Override
    public void onDisable(){
        saveConfig();
        playerdata.saveConfig();
        treasures.saveConfig();

        Bukkit.getLogger().info("Echoes of Gold shut down successfully!");
    }

    public void waitForPlayerInput(Player player, Consumer<String> callback){
        chatInput.put(player.getUniqueId(), callback);
    }

    public String ParsePP(Player player, String text){
        if(text == null) return null;

        if(!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return text;

        return PlaceholderAPI.setPlaceholders(player, text);
    }

    private String formatTime(long seconds){
        long days = seconds / 86000;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();

        if(days > 0) sb.append(days).append("d ");
        if(hours > 0 || days > 0) sb.append(hours).append("h ");
        if(minutes > 0 || hours > 0 || days > 0) sb.append(minutes).append("m ");
        sb.append(secs).append("s ");

        return sb.toString().trim();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event){
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if(!chatInput.containsKey(playerUUID)) return;
        event.setCancelled(true);

        String input = event.getMessage();
        Consumer<String> callback = chatInput.remove(playerUUID);
        Bukkit.getScheduler().runTask(this, () -> callback.accept(input));
    }

    //Getters
    public YMLfiles getTreasures(){
        return treasures;
    }
    public TreasureManager getTreasureManager(){
        return treasureManager;
    }
    public YMLfiles  getPlayerData(){
        return playerdata;
    }
    public EventBossBar getBossBar(){
        return bossBar;
    }
    public EventScoreboard getScoreboardManager(){
        return scoreboardManager;
    }
    public EventProgress getEventProgressManager(){
        return eventProgressManager;
    }

    //Getter and setter for eventActive boolean
    public boolean isEventActive(){
        return eventActive;
    }
    public void setEventActive(boolean value){
        eventActive = value;
    }

    //Getter and setter for eventDuration
    public long getEventDuration(){
        return eventDuration;
    }
    public void setEventDuration(long duration){
        eventDuration = duration;
    }

    //Getters for the GUIs
    public MainManageGUI getManageGUI(){
        return manageGUI;
    }
    public ManageTreasuresGUI getManageTreasuresGUI(){
        return manageTreasuresGUI;
    }
    public AllTreasuresGUI getAllTreasuresGUI(){
        return allTreasuresGUI;
    }
    public AddRewardsGUI getRewardsGUI(){
        return addRewardsGUI;
    }
    public RewardsChoiceGUI getRewardsChoiceGUI(){
        return rewardsChoiceGUI;
    }
    public HintsGUI getHintsGUI(){
        return hintsGUI;
    }
    public ShopGUI getShopGUI(){
        return shopGUI;
    }
    public ShopGuiFinalChoice getShopGuiFinalChoice(){
        return shopGuiFinalChoice;
    }

    //Setter and getter for treasureManagerChoice
    public void setTreasureManagerChoice(String choice){
        treasureManagerChoice = choice;
    }
    public String getTreasureManagerChoice(){
        return treasureManagerChoice;
    }

    //Getters for Economy objects
    public EconomyProvider getEconomyProvider(){
        return economyProvider;
    }
    public DatabaseManager getDbManager(){
        return dbManager;
    }
    public PlaceholdersManager getPlaceholdersManager(){
        return placeholderManager;
    }
}