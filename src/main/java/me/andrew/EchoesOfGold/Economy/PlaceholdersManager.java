//Developed by _ItsAndrew_
package me.andrew.EchoesOfGold.Economy;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

//Class for saving different data for the placeholders to access
public class PlaceholdersManager {
    private final Map<UUID, Double> eogBalanceMap = new ConcurrentHashMap<>();
    private String eogEndDate = " ";

    //Setters for the variables
    public void setEogBalance(UUID playerUUID, double balance) {
        eogBalanceMap.put(playerUUID, balance);
    }
    public void RemoveFromEogBalance(UUID playerUUID, double balance) {
        if(!eogBalanceMap.containsKey(playerUUID)) return;
        eogBalanceMap.compute(playerUUID, (k, currentBalance) -> currentBalance - balance);
    }
    public void setEogEndDate(String eogEndDate) {
        this.eogEndDate = eogEndDate;
    }

    //Getters for the variables
    public double getEogBalance(UUID playerUUID) {
        return eogBalanceMap.getOrDefault(playerUUID, 0.0);
    }
    public String getEogEndDate() {
        return eogEndDate;
    }
}
