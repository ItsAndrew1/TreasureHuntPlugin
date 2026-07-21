//Developed by _ItsAndrew_
package me.andrew.EchoesOfGold.Economy;

import org.bukkit.OfflinePlayer;

import java.util.UUID;

public interface EconomyProvider {
    //Method for getting the balance of a player
    void setBalancePP(OfflinePlayer Player);

    //Method for adding
    void addBalance(double amount, OfflinePlayer Player);

    //Method for withdrawing
    void withdrawBalance(double amount, OfflinePlayer Player);

    //Method for setting up accounts
    void setupAccount(UUID playerUUID);

    boolean hasEnough(OfflinePlayer player, double amount);
}
