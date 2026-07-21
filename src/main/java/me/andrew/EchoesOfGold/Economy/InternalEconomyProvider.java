//Developed by _ItsAndrew_
package me.andrew.EchoesOfGold.Economy;

import com.google.common.util.concurrent.AtomicDouble;
import me.andrew.EchoesOfGold.EchoesOfGold;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

public class InternalEconomyProvider implements EconomyProvider{
    private final DatabaseManager dbManager;
    private final Connection dbConnection;
    private final EchoesOfGold plugin;

    public InternalEconomyProvider(DatabaseManager dbManager, EchoesOfGold plugin){
        this.dbManager = dbManager;
        this.dbConnection = dbManager.getDbConnection();
        this.plugin = plugin;
    }

    @Override
    public void setBalancePP(OfflinePlayer Player) {
        UUID playerUUID = Player.getUniqueId();
        double coinsGathered = plugin.getPlayerData().getConfig().getDouble("players."+playerUUID+".coins-gathered");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
             plugin.getPlaceholdersManager().setEogBalance(playerUUID, dbManager.getPlayerBalance(playerUUID.toString()) + coinsGathered);
        });
    }

    @Override
    public void addBalance(double amount, OfflinePlayer Player) {
        UUID playerUUID = Player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> dbManager.insertIntoPlayerBalance(playerUUID.toString(), amount));
    }

    @Override
    public void withdrawBalance(double amount, OfflinePlayer Player) {
        UUID playerUUID = Player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> dbManager.withdrawFromPlayer(playerUUID.toString(), amount));
    }

    @Override
    public void setupAccount(UUID playerUUID){
        if(!dbManager.isPlayerInDatabase(playerUUID.toString())){
            String sql = "INSERT INTO players (uuid, balance) VALUES (?, 0)";
            try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
                ps.setString(1, playerUUID.toString());
                ps.executeUpdate();
            } catch (Exception e){
                plugin.getLogger().warning("[E.O.G] There was an error setting up the account");
            }
        }
    }

    @Override
    public boolean hasEnough(OfflinePlayer player, double amount){
        String query = "SELECT balance FROM players WHERE uuid = ?";
        try(PreparedStatement statement = dbConnection.prepareStatement(query)){
            statement.setString(1, player.getUniqueId().toString());
            try(ResultSet resultSet = statement.executeQuery()){
                if(resultSet.next()) return resultSet.getDouble("balance") >= amount;
            }
        } catch (Exception e){
            plugin.getLogger().warning("[E.O.G] There was an error checking if the player has the needed amount.");
        }

        return false;
    }
}
