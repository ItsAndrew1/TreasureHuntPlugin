package me.andrew.EchoesOfGold;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EventProgress implements Listener {
    private final EchoesOfGold plugin;
    private long duration;
    private long fullDuration;
    private int countDownValue;
    private BukkitRunnable task;

    private final NamespacedKey hintsKey;

    private final Map<UUID, ItemStack> savedPlayerItems = new HashMap<>();

    public EventProgress(EchoesOfGold plugin){
        this.plugin = plugin;
        hintsKey = new NamespacedKey(plugin, "hints-item");
    }

    public void startEvent(String durationString){
        duration = parseDuration(durationString);

        String fullDurationString = plugin.getConfig().getString("event-duration", "1m");
        fullDuration = parseDuration(fullDurationString);
        plugin.setEventDuration(duration);

        //Setting the tournament End Date Placeholder
        plugin.getPlaceholdersManager().setEogEndDate(formatDate(System.currentTimeMillis() + fullDuration * 1000));

        countDownValue = plugin.getConfig().getInt("event-final-time.final-countdown.seconds-to-start");
        eventProgress();
    }

    public void removeHintsItem(){
        boolean toggleHintsItem = plugin.getConfig().getBoolean("hints-gui.hints-item.toggle", false);
        if(toggleHintsItem){
            for(Player p : Bukkit.getOnlinePlayers()){
                UUID playerUUID = p.getUniqueId();
                PlayerInventory inv = p.getInventory();
                int hiHotbarSlot = plugin.getConfig().getInt("hints-gui.hints-item.hotbar-slot", 8);
                inv.setItem(hiHotbarSlot, savedPlayerItems.getOrDefault(playerUUID, null));
            }
        }
    }

    private void eventProgress(){
        FileConfiguration playerData = plugin.getPlayerData().getConfig();

        //Giving the hints item to the players in the hotbar (if it is toggled)
        boolean toggleHintsItem = plugin.getConfig().getBoolean("hints-gui.hints-item.toggle", false);
        if(toggleHintsItem && thereAreHints()){
            //Building the item
            ItemStack hintsItem = createHintsItem();

            //Giving the item to the players
            int hotbarSlot = plugin.getConfig().getInt("hints-gui.hints-item.hotbar-slot", 8);
            for(Player p : Bukkit.getOnlinePlayers()){
                PlayerInventory inv = p.getInventory();

                //If the slot isn't empty, save the item stack in a map
                if(inv.getItem(hotbarSlot) != null){
                    savedPlayerItems.put(p.getUniqueId(), inv.getItem(hotbarSlot));

                    /*Saving the player's item in the 'playerdata.yml'
                    (this allows the plugin to put each item stack into the map in case of a restart) */
                    playerData.set("players." + p.getUniqueId() + ".saved-item", inv.getItem(hotbarSlot));
                    plugin.getPlayerData().saveConfig();
                }

                inv.setItem(hotbarSlot, hintsItem);
            }
        }

        task = new BukkitRunnable() {
            @Override
            public void run() {
                duration--;
                plugin.setEventDuration(duration);
                plugin.getConfig().set("saving-duration", duration);
                plugin.saveConfig();

                //Starts the cooldown event
                if(duration <= countDownValue && duration > 0){
                    //Getting the sound and sound's volume/pitch
                    Sound countdownSound = Registry.SOUNDS.get(NamespacedKey.minecraft(plugin.getConfig().getString("countdown-sound").toLowerCase()));
                    float csVolume = plugin.getConfig().getInt("cs-volume");
                    float csPitch = plugin.getConfig().getInt("cs-pitch");

                    //Getting the title/subtitle and replacing the %% placeholder
                    String countdownTitle = plugin.getConfig().getString("event-final-time.final-countdown.title", "&d&l%cooldown_time%")
                            .replace("%cooldown_time%", String.valueOf(duration));

                    String countdownSubtitle = plugin.getConfig().getString("event-final-time.final-countdown.subtitle", "")
                            .replace("%cooldown_time%", String.valueOf(duration));

                    for(Player p : Bukkit.getOnlinePlayers()){
                        //Parsing any PAPI placeholders
                        countdownTitle = plugin.ParsePP(p, countdownTitle);
                        countdownSubtitle = plugin.ParsePP(p, countdownSubtitle);

                        p.sendTitle(ChatColor.translateAlternateColorCodes('&', countdownTitle), ChatColor.translateAlternateColorCodes('&', countdownSubtitle));
                        p.playSound(p.getLocation(), countdownSound, csVolume, csPitch);
                    }
                }

                //Finishes the event
                if(duration == 0){
                    //Displaying the final chat message
                    eventFinishChatMessage();

                    //Removing the treasures and their particles
                    plugin.getTreasureManager().cancelParticles();
                    plugin.getTreasureManager().removeTreasures();

                    //Removing the hints item and giving the player's saved item
                    removeHintsItem();

                    //Stops the boss bar/scoreboard
                    long bossBarDelay = plugin.getConfig().getLong("stop-boss-bar-interval");
                    new BukkitRunnable(){
                        @Override
                        public void run() {
                            plugin.getBossBar().stopBossBar();
                        }
                    }.runTaskLater(plugin, bossBarDelay*20L);
                    for(Player p : Bukkit.getOnlinePlayers()){
                        plugin.getScoreboardManager().stopScoreboard(p);
                    }

                    //Removing the End Date placeholder
                    plugin.getPlaceholdersManager().setEogEndDate(" ");

                    //Saving the gathered coins in the database
                    for(OfflinePlayer p : Bukkit.getOfflinePlayers()){
                        double coinsGathered = plugin.getPlayerData().getConfig().getDouble("players." + p.getUniqueId() + ".coins-gathered");
                        plugin.getEconomyProvider().addBalance(coinsGathered, p);
                    }

                    //Clearing the item map
                    savedPlayerItems.clear();

                    //Removing the player data
                    plugin.getPlayerData().getConfig().set("players", null);
                    plugin.getPlayerData().saveConfig();

                    task.cancel();
                    plugin.setEventActive(false);
                }

                //Displays messages in chat based on the timer
                showEventChatMessages();
            }
        };
        task.runTaskTimer(plugin, 0L, 20L); //Runs the task every second
    }

    private void showEventChatMessages(){
        //Check if these messages are toggled.
        boolean toggleMessages = plugin.getConfig().getBoolean("toggle-event-messages", true);
        if(!toggleMessages) return;

        long threeQuartersTime = (fullDuration * 3)/4;
        long halfTime = fullDuration/2;
        long oneQuarterTime = fullDuration/4;

        if(plugin.getEventDuration() == threeQuartersTime){
            //Check if the threeQuarterTime message is toggled
            boolean toggle3Qmessage = plugin.getConfig().getBoolean("event-three-quarters-time.toggle", true);
            if(!toggle3Qmessage) return;

            Sound tqtSound = Registry.SOUNDS.get(NamespacedKey.minecraft(plugin.getConfig().getString("event-three-quarter-time-sound").toLowerCase()));
            float tqtsVolume = plugin.getConfig().getInt("etqts-volume");
            float tqtsPitch = plugin.getConfig().getInt("etqts-pitch");

            List<String> threeQmessageLines = plugin.getConfig().getStringList("event-three-quarters-time.chat-message-lines");
            for(Player p : Bukkit.getOnlinePlayers()){
                for(String messageLine : threeQmessageLines){ //Displays the message
                    //Replaces the %% placeholder if the line contains it
                    String phMessage = ChatColor.translateAlternateColorCodes('&', messageLine.replace("%three_quarters_time%", String.valueOf(threeQuartersTime)));
                    phMessage = plugin.ParsePP(p, phMessage);
                    p.sendMessage(phMessage);
                }

                //Sends the players a title/subtitle if they are toggled
                boolean toggleTitleSubtitle = plugin.getConfig().getBoolean("event-three-quarters-time.toggle-title-subtitle", true);
                if(toggleTitleSubtitle){
                    String title, subtitle;
                    title = plugin.getConfig().getString("event-three-quarters-time.title.message", "&f&lHURRY!").replace("%three_quarters_time%", String.valueOf(threeQuartersTime));
                    subtitle = plugin.getConfig().getString("event-three-quarters-time.subtitle.message", "&aOnly &l%three_quarters_time% &aseconds left!").replace("%three_quarters_time%", String.valueOf(threeQuartersTime));

                    //Parsing the PAPI placeholders
                    title = plugin.ParsePP(p, title);
                    subtitle = plugin.ParsePP(p, subtitle);

                    p.sendTitle(ChatColor.translateAlternateColorCodes('&', title), ChatColor.translateAlternateColorCodes('&', subtitle)); //Sends the title/subtitle
                }
                p.playSound(p.getLocation(), tqtSound, tqtsVolume, tqtsPitch);
            }
        }

        if(plugin.getEventDuration() == halfTime){
            //Check if the halfTime message is toggled
            boolean toggleHalfTimeMessage = plugin.getConfig().getBoolean("event-half-time.toggle", true);
            if(!toggleHalfTimeMessage) return;

            Sound thtSound = Registry.SOUNDS.get(NamespacedKey.minecraft(plugin.getConfig().getString("event-half-time-sound").toLowerCase()));
            float thtsVolume = plugin.getConfig().getInt("ehts-volume");
            float thtsPitch = plugin.getConfig().getInt("ehts-pitch");

            List<String> messageLines = plugin.getConfig().getStringList("event-half-time.chat-message-lines");
            for(Player p : Bukkit.getOnlinePlayers()){
                for(String messageLine : messageLines){ //Displays the message
                    String phMessage = ChatColor.translateAlternateColorCodes('&', messageLine.replace("%half_time%", String.valueOf(halfTime)));
                    phMessage = plugin.ParsePP(p, phMessage);
                    p.sendMessage(phMessage);
                }

                //Send the players title/subtitle if they are toggled
                boolean toggleTitleSubtitle = plugin.getConfig().getBoolean("event-half-time.toggle-title-subtitle", true);
                if(toggleTitleSubtitle){
                    String title, subtitle;
                    title = plugin.getConfig().getString("event-half-time.title.message", "&f&lHURRY!").replace("%half_time%", String.valueOf(halfTime));
                    subtitle = plugin.getConfig().getString("event-half-time.subtitle.message", "&aOnly &l%half_time% &aseconds left!").replace("%half_time%", String.valueOf(halfTime));

                    //Parsing the PAPI placeholders
                    title = plugin.ParsePP(p, title);
                    subtitle = plugin.ParsePP(p, subtitle);

                    p.sendTitle(ChatColor.translateAlternateColorCodes('&', title), ChatColor.translateAlternateColorCodes('&', subtitle));
                }

                p.playSound(p.getLocation(), thtSound, thtsVolume, thtsPitch);
            }
        }

        if(plugin.getEventDuration() == oneQuarterTime){
            //Check if the oneQuarterTime message is toggled
            boolean oqtToggle = plugin.getConfig().getBoolean("event-one-quarter-time.toggle", true);
            if(!oqtToggle) return;

            Sound oqtSound = Registry.SOUNDS.get(NamespacedKey.minecraft(plugin.getConfig().getString("event-one-quarter-time-sound").toLowerCase()));
            float oqtsVolume = plugin.getConfig().getInt("eoqts-volume");
            float oqtsPitch = plugin.getConfig().getInt("eoqts-pitch");

            List<String> chatMessageLines = plugin.getConfig().getStringList("event-one-quarter-time.chat-message-lines");
            for(Player p : Bukkit.getOnlinePlayers()){
                for(String messageLine : chatMessageLines){
                    String phMessage = ChatColor.translateAlternateColorCodes('&', messageLine.replace("%one_quarter_time%", String.valueOf(oneQuarterTime)));
                    phMessage = plugin.ParsePP(p, phMessage);
                    p.sendMessage(phMessage);
                }

                //Send the players title and subtitle if they are toggled
                boolean toggleTitleSubtitle = plugin.getConfig().getBoolean("event-one-quarter-time.toggle-title-subtitle", true);
                if(toggleTitleSubtitle){
                    String title = plugin.getConfig().getString("event-one-quarter-time.title.message", "&f&lHURRY").replace("%one_quarter_time%", String.valueOf(oneQuarterTime));
                    String subtitle = plugin.getConfig().getString("event-one-quarter-time.subtitle.message", "&aOnly &a&l%one_quarter_time% &aseconds left!").replace("%one_quarter_time%", String.valueOf(oneQuarterTime));

                    title = plugin.ParsePP(p, title);
                    subtitle = plugin.ParsePP(p, subtitle);

                    p.sendTitle(ChatColor.translateAlternateColorCodes('&', title), ChatColor.translateAlternateColorCodes('&', subtitle));
                }

                p.playSound(p.getLocation(), oqtSound, oqtsVolume, oqtsPitch); //Plays the sound to the players
            }
        }
    }

    private void eventFinishChatMessage(){
        //Check if the message is toggled
        boolean toggleMessage = plugin.getConfig().getBoolean("event-final-time.finish-event-message.toggle", true);
        if(!toggleMessage) return;

        //Sends the chat message
        List<String> messageLines = plugin.getConfig().getStringList("event-final-time.finish-event-message.message-lines");
        for(Player p : Bukkit.getOnlinePlayers()){
            List<Map.Entry<UUID, Integer>> top = plugin.getTreasureManager().getTopPlayers();
            UUID top1UUID = !top.isEmpty() ? top.getFirst().getKey() : null;
            UUID top2UUID = top.size() > 1 ? top.get(1).getKey() : null;
            UUID top3UUID = top.size() > 2 ? top.get(2).getKey() : null;
            int top1count = !top.isEmpty() ? top.getFirst().getValue() : 0;
            int top2count = top.size() > 1 ? top.get(1).getValue() : 0;
            int top3count = top.size() > 2 ? top.get(2).getValue() : 0;

            String top1name = top1UUID != null ? Bukkit.getOfflinePlayer(top1UUID).getName() : "None";
            String top2name = top2UUID != null ? Bukkit.getOfflinePlayer(top2UUID).getName() : "None";
            String top3name = top3UUID != null ? Bukkit.getOfflinePlayer(top3UUID).getName() : "None";

            Sound eventFinishSound = Registry.SOUNDS.get(NamespacedKey.minecraft(plugin.getConfig().getString("event-finish-sound").toLowerCase()));
            float efsVolume = plugin.getConfig().getInt("efs-volume");
            float efsPitch = plugin.getConfig().getInt("efs-pitch");

            String title = plugin.getConfig().getString("event-final-time.finish-event-title", "&e&lTREASURE HUNT IS FINISHED!");
            String subtitle = plugin.getConfig().getString("event-final-time.finish-event-subtitle", "&aCongrats %player_name% for your progress.");

            title = plugin.ParsePP(p, title);
            subtitle = plugin.ParsePP(p, subtitle);

            for(String messageLine : messageLines){
                title = title.replace("%player_name%", p.getName());
                subtitle = subtitle.replace("%player_name%", p.getName());
                String phLine = messageLine
                        .replace("%top1_name%", top1name)
                        .replace("%top2_name%", top2name)
                        .replace("%top3_name%", top3name)
                        .replace("%top1_count%", String.valueOf(top1count))
                        .replace("%top2_count%", String.valueOf(top2count))
                        .replace("%top3_count%", String.valueOf(top3count));

                String coloredParsed = ChatColor.translateAlternateColorCodes('&', phLine);
                coloredParsed = plugin.ParsePP(p, coloredParsed);
                p.sendMessage(coloredParsed);
            }

            p.sendTitle(ChatColor.translateAlternateColorCodes('&', title),  ChatColor.translateAlternateColorCodes('&', subtitle));
            p.playSound(p.getLocation(), eventFinishSound, efsVolume, efsPitch);
        }
    }

    public void stopEvent(){
        if(task!=null) task.cancel();
    }

    public long getDuration(){
        return duration;
    }

    //Parses the duration of the event from config.yml (bar-timer)
    private long parseDuration(String input){
        long seconds = 0;
        Matcher m = Pattern.compile("(\\d+)([dhms])").matcher(input.toLowerCase());
        while (m.find()) {
            int value = Integer.parseInt(m.group(1));
            switch (m.group(2)) {
                case "d" -> seconds += value * 86400L;
                case "h" -> seconds += value * 3600L;
                case "m" -> seconds += value * 60L;
                case "s" -> seconds += value;
            }
        }
        return seconds;
    }

    private String formatDate(long millis){
        Instant instant = Instant.ofEpochMilli(millis);
        LocalDateTime date = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

        //Setting the pattern
        String pattern = plugin.getConfig().getString("end-date-placeholder-format", "dd MMM yyyy HH:mm:ss");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

        return date.format(formatter);
    }

    @EventHandler
    public void onDropEvent(PlayerDropItemEvent e){
        FileConfiguration mainConfig = plugin.getConfig();
        if(!plugin.isEventActive() || !thereAreHints()) return; //This event will only trigger when the event is active

        boolean toggleHintsItem = mainConfig.getBoolean("hints-gui.hints-item.toggle", true);
        if(!toggleHintsItem) return; //If the hints item is not toggled.

        ItemStack item = e.getItemDrop().getItemStack();
        ItemMeta itemMeta = item.getItemMeta();

        String hintsItemDN = ChatColor.translateAlternateColorCodes('&', mainConfig.getString("hints-gui.hints-item.display-name", "&6&lHINTS"));
        Material hintsItemMaterial = Material.matchMaterial(mainConfig.getString("hints-gui.hints-item.material", "enchanted_book").toUpperCase());
        if(itemMeta.getDisplayName().equals(hintsItemDN) && item.getType().equals(hintsItemMaterial)) e.setCancelled(true); //Makes it impossible to drop the item
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e){
        FileConfiguration mainConfig = plugin.getConfig();
        if(!plugin.isEventActive() || !thereAreHints()) return; //Again, this event triggers only when the event is active

        boolean toggleHintsItem = mainConfig.getBoolean("hints-gui.hints-item.toggle", true);
        if(!toggleHintsItem) return; //If the hints item is not toggled.

        if(!(e.getWhoClicked() instanceof Player)) return;

        ItemStack clickedItem = e.getCurrentItem();
        if(clickedItem == null) return;

        ItemMeta clickedMeta = clickedItem.getItemMeta();
        if(clickedMeta == null) return;

        String hintsItemDN = ChatColor.translateAlternateColorCodes('&', mainConfig.getString("hints-gui.hints-item.display-name", "&6&lHINTS"));
        Material hintsItemMaterial = Material.matchMaterial(mainConfig.getString("hints-gui.hints-item.material", "enchanted_book").toUpperCase());
        if(clickedMeta.getDisplayName().equals(hintsItemDN) && clickedItem.getType().equals(hintsItemMaterial)) e.setCancelled(true); //Makes it impossible for the player to move the item
    }

    @EventHandler
    public void onItemInteract(PlayerInteractEvent e){
        FileConfiguration mainConfig = plugin.getConfig();
        if(!plugin.isEventActive() || !thereAreHints()) return; //This event runs only when the event is active and if there are hints.

        boolean toggleHintsItem = mainConfig.getBoolean("hints-gui.hints-item.toggle", true);
        if(!toggleHintsItem) return; //If the hints item is not toggled.

        ItemStack interactedItem = e.getItem();
        if(interactedItem == null) return;

        ItemMeta interactedMeta = interactedItem.getItemMeta();
        if(interactedMeta == null) return;

        //If it's not a left/right click, it returns
        Action action = e.getAction();
        if(!action.isLeftClick() && !action.isRightClick()) return;

        //Getting the player
        Player targetPlayer = e.getPlayer();

        //If the player interacts with the hints item, it opens the hints gui
        if(interactedMeta.getPersistentDataContainer().has(hintsKey, PersistentDataType.INTEGER)){
            targetPlayer.playSound(targetPlayer.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            plugin.getHintsGUI().hintsGUI(targetPlayer, 1);
        }
    }

    //If a player joins during the event
    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        if (!plugin.isEventActive()) return; //This event runs only when the event is active.
        Player targetPlayer = event.getPlayer();
        UUID targetPlayerUUID = targetPlayer.getUniqueId();

        //Check if the boss bar and scoreboard are toggled
        boolean toggleBossBar = plugin.getConfig().getBoolean("boss-bar", true);
        boolean toggleScoreboard = plugin.getConfig().getBoolean("scoreboard", true);
        if (toggleBossBar) plugin.getBossBar().addPlayer(targetPlayer);
        if (toggleScoreboard) plugin.getScoreboardManager().updateScoreboard(targetPlayer);

        //Spawns the treasures and their particles for the target player
        plugin.getTreasureManager().spawnTreasures();
        plugin.getTreasureManager().spawnChestParticles();

        ConfigurationSection players = plugin.getPlayerData().getConfig().getConfigurationSection("players");
        FileConfiguration data = plugin.getPlayerData().getConfig();
        FileConfiguration treasuresConfig = plugin.getTreasures().getConfig();

        //Initializing the player in 'playerData.yml' if he wasn't already
        if (players != null && !players.contains(targetPlayer.getUniqueId().toString())) {
            String path = "players." + targetPlayer.getUniqueId();
            data.set(path + ".treasures-found", 0);

            //Setting up the economy for the player (if it is toggled)
            if(plugin.getEconomyProvider() != null) Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getEconomyProvider().setupAccount(targetPlayerUUID));

            if (treasuresConfig.isConfigurationSection("treasures")) {
                for (String key : treasuresConfig.getConfigurationSection("treasures").getKeys(false)) {
                    data.set(path + ".found." + key, false);
                }
            }

            plugin.getPlayerData().saveConfig();
        }

        //Giving the player the Hints item if it is toggled
        boolean toggleHintsItem = plugin.getConfig().getBoolean("hints-gui.hints-item.toggle", false);
        if(toggleHintsItem && thereAreHints()){
            ItemStack hintsItem = createHintsItem();

            int hotbarSlot = plugin.getConfig().getInt("hints-gui.hints-item.slot", 8);
            PlayerInventory inv = targetPlayer.getInventory();
            if(inv.getItem(hotbarSlot) != null){
                savedPlayerItems.put(targetPlayer.getUniqueId(), inv.getItem(hotbarSlot));

                /*Saving the player's item in the 'playerdata.yml'
                 (this allows the plugin to put each item stack into the map in case of a restart) */
                plugin.getPlayerData().getConfig().set("players." + targetPlayer.getUniqueId() + ".saved-item", inv.getItem(hotbarSlot));
                plugin.getPlayerData().saveConfig();
            }
            inv.setItem(hotbarSlot, hintsItem);
        }
    }

    private ItemStack createHintsItem() {
        ItemStack hintsItem = new ItemStack(Material.getMaterial(plugin.getConfig().getString("hints-gui.hints-item.material", "enchanted_book").toUpperCase()));
        ItemMeta hiMeta = hintsItem.getItemMeta();

        String displayName = plugin.getConfig().getString("hints-gui.hints-item.display-name", "&6&lHINTS");
        hiMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));

        List<String> coloredLore = new ArrayList<>();
        for(String line : plugin.getConfig().getStringList("hints-gui.hints-item.lore")){
            coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }

        //Setting a special key which will help me to identify it in the interact event
        hiMeta.getPersistentDataContainer().set(hintsKey, PersistentDataType.INTEGER, 1);

        hiMeta.setLore(coloredLore);
        hintsItem.setItemMeta(hiMeta);

        return hintsItem;
    }

    private boolean thereAreHints(){
        FileConfiguration treasures = plugin.getTreasures().getConfig();
        ConfigurationSection treasuresSection = treasures.getConfigurationSection("treasures");

        if(treasuresSection == null) return false;

        for(String key : treasuresSection.getKeys(false)) {
            ConfigurationSection treasureHint = treasures.getConfigurationSection("treasures."+key+".hint");
            if(treasureHint != null) return true;
        }

        return false;
    }

    public void putItemsInMap(UUID uuid, ItemStack item){
        savedPlayerItems.put(uuid, item);
    }
}
