//Developed by _ItsAndrew_
package me.andrew.EchoesOfGold;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.util.Map;


public class TreasureClickEvent implements Listener{
    private final EchoesOfGold plugin;
    int foundCount;

    public TreasureClickEvent(EchoesOfGold plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onTreasureClick(PlayerInteractEvent event){
        FileConfiguration treasures = plugin.getTreasures().getConfig();

        //If the event is canceled, returns
        if(!plugin.isEventActive()) return;

        if(event.getHand() != EquipmentSlot.HAND) return;
        if(event.getClickedBlock() == null) return;
        if(event.getClickedBlock().getType() != Material.PLAYER_HEAD) return;

        //Check if there are any treasures
        ConfigurationSection treasuresSection = treasures.getConfigurationSection("treasures");
        if(treasuresSection == null || treasuresSection.getKeys(false).isEmpty()) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        Location loc = block.getLocation();

        for(String key : treasures.getConfigurationSection("treasures").getKeys(false)){
            String path = "treasures." + key;
            double x = treasures.getDouble(path + ".x");
            double y = treasures.getDouble(path + ".y");
            double z = treasures.getDouble(path + ".z");
            String worldName = treasures.getString(path + ".world");

            //Correcting the X/Z coordinates
            if(x < 0) x--;
            if(z < 0) z--;

            if(loc.getWorld().getName().equalsIgnoreCase(worldName) && loc.getBlockX() == x && loc.getBlockY() == y && loc.getBlockZ() == z){
                //Handling the rewards and other data
                handleTreasureFound(player, key);

                //Refreshes the scoreboard if it is toggled
                boolean toggleScoreboard = plugin.getConfig().getBoolean("scoreboard", false);
                if(toggleScoreboard){
                    plugin.getTreasures().reloadConfig();
                    for(Player p : Bukkit.getOnlinePlayers()){
                        plugin.getScoreboardManager().updateScoreboard(p);
                    }
                }
                event.setCancelled(true);
            }
        }
    }

    private void handleTreasureFound(Player player, String treasureID){
        FileConfiguration data = plugin.getPlayerData().getConfig();
        FileConfiguration treasures = plugin.getTreasures().getConfig();
        String playerPath = "players." + player.getUniqueId();

        Map<String, BukkitTask> treasureParticleTasks = plugin.getTreasureManager().getTreasureParticleTasks().get(player.getUniqueId());
        BukkitTask taskForTreasure = treasureParticleTasks.get(treasureID);

        //If the player already found this treasure
        boolean alreadyFound = data.getBoolean(playerPath + ".found."+treasureID, false);
        if(alreadyFound){
            Sound treasureAlreadyFound = Registry.SOUNDS.get(NamespacedKey.minecraft(plugin.getConfig().getString("treasure-found-sound").toLowerCase()));
            float tafVolume = plugin.getConfig().getInt("tfs-volume");
            float tafPitch = plugin.getConfig().getInt("tfs-pitch");

            //Spawning the particle (if it is toggled)
            boolean toggleFoundParticle = plugin.getConfig().getBoolean("treasure-found-particle.toggle");
            if(toggleFoundParticle){
                double treasureX = treasures.getDouble("treasures."+treasureID+".x");
                double treasureY = treasures.getDouble("treasures."+treasureID+".y");
                double treasureZ = treasures.getDouble("treasures."+treasureID+".z");

                //Correcting the X/Z coordinates
                if(treasureX < 0) treasureX-=0.5;
                else treasureX+=0.5;

                if(treasureZ < 0) treasureZ-=0.5;
                else treasureZ+=0.5;

                Location loc = new Location(Bukkit.getWorld("treasures."+treasureID+".world"), treasureX, treasureY, treasureZ);
                Particle foundParticle;
                try{
                    String value = plugin.getConfig().getString("treasure-found-particle.particle").toUpperCase();
                    foundParticle = getParticleFromConfig(value);

                    //Getting data for the particle
                    int particleCount = plugin.getConfig().getInt("treasure-found-particle.count");
                    double particleOffsetX = plugin.getConfig().getDouble("treasure-found-particle.offsetX");
                    double particleOffsetY = plugin.getConfig().getDouble("treasure-found-particle.offsetY");
                    double particleOffsetZ = plugin.getConfig().getDouble("treasure-found-particle.offsetZ");
                    double particleExtra =  plugin.getConfig().getDouble("treasure-found-particle.extra");

                    player.spawnParticle(foundParticle, loc, particleCount, particleOffsetX, particleOffsetY, particleOffsetZ, particleExtra);
                } catch(Exception e){
                    Bukkit.getLogger().warning("[EchoesOfGold] The value for treasure-found-particle is INVALID! The particle will not show up!");
                    return;
                } finally{
                    player.playSound(player.getLocation(), treasureAlreadyFound, tafVolume, tafPitch);
                    String treasureAlreadyFoundMessage = plugin.getConfig().getString("treasure-already-found", "&cYou have &lalready found &cthis treasure!");
                    treasureAlreadyFoundMessage = plugin.ParsePP(player, treasureAlreadyFoundMessage);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', treasureAlreadyFoundMessage));
                }
                return;
            }

            String treasureAlreadyFoundMessage = plugin.getConfig().getString("treasure-already-found", "&cYou have &lalready found &cthis treasure!");
            treasureAlreadyFoundMessage = plugin.ParsePP(player, treasureAlreadyFoundMessage);
            player.playSound(player.getLocation(), treasureAlreadyFound, tafVolume, tafPitch);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', treasureAlreadyFoundMessage));
            return;
        }

        //Check if the player has enough inventory space
        String mainRewardsPath = "treasures."+treasureID+".rewards";
        ConfigurationSection treasureRewards = treasures.getConfigurationSection(mainRewardsPath);

        if(player.getInventory().firstEmpty() == -1 && (treasureRewards != null || !treasureRewards.getKeys(false).isEmpty())){
            Sound noInventorySpace = Registry.SOUNDS.get(NamespacedKey.minecraft(plugin.getConfig().getString("no-inventory-space-sound").toLowerCase()));
            float nissVolume = plugin.getConfig().getInt("niss-volume");
            float nissPitch = plugin.getConfig().getInt("niss-pitch");

            player.playSound(player.getLocation(), noInventorySpace, nissVolume, nissPitch);
            String noInvSpaceMessage = plugin.getConfig().getString("no-inventory-space-message");
            noInvSpaceMessage = plugin.ParsePP(player, noInvSpaceMessage);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', noInvSpaceMessage));
            return;
        }

        //Giving the rewards to players
        Sound treasureClick = Registry.SOUNDS.get(NamespacedKey.minecraft(plugin.getConfig().getString("treasure-click-sound").toLowerCase()));
        float tcsVolume = plugin.getConfig().getInt("tcs-volume");
        float tcsPitch = plugin.getConfig().getInt("tcs-pitch");

        //Sound and firework
        player.playSound(player.getLocation(), treasureClick, tcsVolume, tcsPitch);
        spawnFirework(player, treasureID);

        //Setting the data for the player in 'playerdata.yml'
        data.set(playerPath + ".found." + treasureID, true);
        foundCount = data.getInt(playerPath +".treasures-found", 0) + 1;
        data.set(playerPath + ".treasures-found", foundCount);
        plugin.getPlayerData().saveConfig();

        //Adding money to the player's account (if the economy is toggled and working)
        if(isEconomyWorking()){
            double treasureAmount = treasures.getDouble("treasures."+treasureID+".coins");

            //Adding the amount to 'coins-gathered' in the player's data
            double newAmount = data.getDouble(playerPath+".coins-gathered", 0) + treasureAmount;
            data.set(playerPath+".coins-gathered", newAmount);
            plugin.getPlayerData().saveConfig();

            //Saving the balance to the placeholder
            plugin.getEconomyProvider().setBalancePP(player);

            //Displaying a pop-up message above the player's xp bar (action bar)
            String actionBarMessage = plugin.getConfig().getString("economy.action-bar-message");
            player.sendActionBar(ChatColor.translateAlternateColorCodes('&', actionBarMessage
                    .replace("%coins_amount%", String.valueOf(treasureAmount))
            ));
        }

        //Canceling the particle task
        if(taskForTreasure != null) taskForTreasure.cancel();

        if(treasureRewards != null && !treasureRewards.getKeys(false).isEmpty()){
            //Giving the items
            for(String item : treasureRewards.getKeys(false)){
                int itemQuantity = treasures.getInt(mainRewardsPath+"."+item+".quantity");
                Material itemMaterial = Material.matchMaterial(item.toUpperCase());

                ItemStack reward = new ItemStack(itemMaterial, itemQuantity);
                ItemMeta rewardMeta = reward.getItemMeta();

                //Setting the enchantments for the reward (if there are any)
                ConfigurationSection enchantSection = treasures.getConfigurationSection(mainRewardsPath+"."+item+".enchantments");
                if(enchantSection != null){
                    for(String enchant : treasures.getConfigurationSection(mainRewardsPath+"."+item+".enchantments").getKeys(false)){
                        int enchantLevel = treasures.getInt(mainRewardsPath+"."+item+".enchantments."+enchant);
                        Enchantment realEnchant = Enchantment.getByName(enchant);
                        rewardMeta.addEnchant(realEnchant, enchantLevel, true);
                    }
                }

                reward.setItemMeta(rewardMeta);
                player.getInventory().addItem(reward);
            }
        }

        //If they found all rewards
        if(foundCount == treasures.getInt("nr-of-treasures")){
            String foundAllTreasuresMessage = plugin.getConfig().getString("player-found-all-treasures", "&6&lGG! &6You have found all the treasures!");
            foundAllTreasuresMessage = plugin.ParsePP(player, foundAllTreasuresMessage);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', foundAllTreasuresMessage));
            foundCount++;
            return;
        }

        String treasureFoundMessage = plugin.getConfig().getString("treasure-found", "&aYou have found a &ltreasure&a!");
        treasureFoundMessage = plugin.ParsePP(player, treasureFoundMessage);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', treasureFoundMessage));
    }

    private void spawnFirework(Player player, String treasure){
        FileConfiguration treasures = plugin.getTreasures().getConfig();

        boolean toggleFireworks = plugin.getConfig().getBoolean("toggle-fireworks-effect", true);
        String fwMainColor = plugin.getConfig().getString("fw-main-color");
        String fwSecondaryColor = plugin.getConfig().getString("fw-second-color");

        if(!toggleFireworks) return;

        Location playerLocation = new Location(Bukkit.getWorld(treasures.getString("treasures."+treasure+".world")), player.getLocation().getX(), player.getLocation().getY()+1.5, player.getLocation().getZ());
        Firework firework =  (Firework) player.getWorld().spawnEntity(playerLocation, EntityType.FIREWORK);
        FireworkMeta fwMeta = firework.getFireworkMeta();

        //Adding the colors and effects
        fwMeta.addEffect(FireworkEffect.builder()
                        .withColor(getColorFromHex(fwMainColor.toUpperCase()), getColorFromHex(fwSecondaryColor.toUpperCase()))
                        .with(FireworkEffect.Type.BALL)
                        .flicker(true)
                        .trail(true)
                        .build());
        fwMeta.setPower(0);
        firework.setFireworkMeta(fwMeta);

        //Detonating the firework
        Bukkit.getScheduler().runTaskLater(plugin, firework::detonate, 1L);
    }

    //Event to cancel the firework damage
    @EventHandler
    public void onFireworkDamage(EntityDamageByEntityEvent e) {
        if(!plugin.isEnabled()) return; //Runs only when the event is enabled

        if(!(e.getEntity() instanceof Player)) return;

        if(e.getDamager() instanceof Firework) e.setCancelled(true);
    }

    //Gets the color from 'config.yml' for the firework
    private Color getColorFromHex(String hex){
        return Color.fromRGB(
                Integer.valueOf(hex.substring(1, 3), 16),
                Integer.valueOf(hex.substring(3, 5), 16),
                Integer.valueOf(hex.substring(5, 7), 16)
        );
    }

    //Gets the particle from 'config.yml'
    private Particle getParticleFromConfig(String value){
        return Particle.valueOf(value);
    }

    private boolean isEconomyWorking(){
        return plugin.getEconomyProvider() != null;
    }
}
