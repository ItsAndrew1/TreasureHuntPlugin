//Developed by _ItsAndrew_
package me.andrew.EchoesOfGold.Economy;

import me.andrew.EchoesOfGold.EchoesOfGold;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class VaultEconomyProvider implements EconomyProvider{
    private final Economy vault;
    private final EchoesOfGold plugin;

    public VaultEconomyProvider(Economy vault, EchoesOfGold plugin){
        this.vault = vault;
        this.plugin = plugin;
    }

    @Override
    public void setBalancePP(OfflinePlayer player){
        int coins_gathered = plugin.getPlayerData().getConfig().getInt("players."+player.getUniqueId()+".coins-gathered");
        plugin.getPlaceholdersManager().setEogBalance(player.getUniqueId(), coins_gathered + vault.getBalance(player));
    }

    @Override
    public void addBalance(double amount, OfflinePlayer player) {
        vault.depositPlayer(player, amount);
    }

    @Override
    public void withdrawBalance(double amount, OfflinePlayer player) {
        vault.withdrawPlayer(player, amount);
    }

    @Override
    public void setupAccount(UUID playerUUID){
        OfflinePlayer player = Bukkit.getPlayer(playerUUID);
        if(!vault.hasAccount(player)) vault.createPlayerAccount(player);
    }

    @Override
    public boolean hasEnough(OfflinePlayer player, double amount){
        return vault.has(player, amount);
    }
}
