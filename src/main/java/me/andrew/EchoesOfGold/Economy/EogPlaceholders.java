package me.andrew.EchoesOfGold.Economy;

import me.andrew.EchoesOfGold.EchoesOfGold;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class EogPlaceholders extends PlaceholderExpansion {
    private final EchoesOfGold plugin;

    public EogPlaceholders(EchoesOfGold plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "eog";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist(){
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params){
        if(params.equals("end_date")) return plugin.getPlaceholdersManager().getEogEndDate();

        //If the internal economy isn't toggled, returns null
        boolean toggleInternalEconomy = plugin.getConfig().getBoolean("economy.internal-economy.toggle", true);
        boolean toggleEconomy = plugin.getConfig().getBoolean("economy.toggle-using-economy", true);
        if(!toggleInternalEconomy || !toggleEconomy) return null;

        if(params.equals("balance")) return String.format("%.2f", plugin.getPlaceholdersManager().getEogBalance(player.getUniqueId()));

        return null;
    }
}
