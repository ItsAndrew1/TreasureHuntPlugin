//Developed by _ItsAndrew_
package me.andrew.EchoesOfGold.Economy;

import me.andrew.EchoesOfGold.EchoesOfGold;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.*;

public class DatabaseManager {
    private final EchoesOfGold plugin;
    private Connection dbConnection;

    public DatabaseManager(EchoesOfGold plugin){
        this.plugin = plugin;
    }

    //Main method for making the connection with the database
    public void connectDB() throws SQLException{
        FileConfiguration mainConfig = plugin.getConfig();

        //Getting the database type
        String databaseType = mainConfig.getString("economy.internal-economy.database-system.type", "sqlite");
        if(!databaseType.equals("sqlite") && !databaseType.equals("mysql")) databaseType = "sqlite";

        //If the type is 'sqlite', it creates a .db file inside the EchoesOfGold folder (located in the plugins folder)
        if(databaseType.equals("sqlite")){
            String fileName = mainConfig.getString("economy.internal-economy.database-system.file-name", "database.db");
            File dbFile = new File(plugin.getDataFolder(), fileName);

            String url = "jdbc:sqlite:"+dbFile.getAbsolutePath();
            dbConnection = DriverManager.getConnection(url);
        }

        //If the type is 'mysql', it sets up the connection with the data from the config file
        else{
            String host = mainConfig.getString("economy.internal-economy.database-system.host");
            String port = mainConfig.getString("economy.internal-economy.database-system.port");
            String database = mainConfig.getString("economy.internal-economy.database-system.database");
            String username = mainConfig.getString("economy.internal-economy.database-system.username");
            String password = mainConfig.getString("economy.internal-economy.database-system.password");

            //If one of them is null, shuts down the plugin
            if(host == null || port == null || database == null || username == null || password == null){
                plugin.getLogger().warning("[E.O.G] The database is enabled but there is missing data! Disabling plugin...");
                plugin.getServer().getPluginManager().disablePlugin(plugin);
                return;
            }

            String url = "jdbc:mysql://"+host+":"+port+"/"+database +
                    "?useSSL=false&autoReconnect=true&characterEncoding=utf8";
            dbConnection = DriverManager.getConnection(url, username, password);
        }

        //Creating the players table
        String playersTable = """
                CREATE TABLE IF NOT EXISTS players (
                crt INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid TEXT UNIQUE NOT NULL,
                balance DECIMAL(12,2) DEFAULT 0.00
            );
        """;
        try(PreparedStatement statement = dbConnection.prepareStatement(playersTable)){
            statement.executeUpdate();
        }

        plugin.getLogger().info("[E.O.G] Database connection established!");
    }

    public boolean isPlayerInDatabase(String uuid){
        String query = "SELECT * FROM players WHERE uuid = ?";
        try(PreparedStatement statement = dbConnection.prepareStatement(query)){
            statement.setString(1, uuid);
            return statement.executeQuery().next();
        } catch (SQLException e){
            plugin.getLogger().warning("[E.O.G] There was an error checking if the player is in the database!");
            return false;
        }
    }

    public double getPlayerBalance(String uuid){
        String query = "SELECT balance FROM players WHERE uuid = ?";
        try(PreparedStatement statement = dbConnection.prepareStatement(query)){
            statement.setString(1, uuid);
            try(ResultSet resultSet = statement.executeQuery()){
                if(resultSet.next()) return resultSet.getDouble("balance");
            }
        } catch (SQLException e){
            plugin.getLogger().warning("[E.O.G] There was an error getting the player balance!");
        }

        return 0;
    }

    public void insertIntoPlayerBalance(String uuid, double amount){
        double playerBalance = 0;

        //Getting the player values
        String sql = "SELECT balance FROM players WHERE uuid = ?";
        try(PreparedStatement ps = dbConnection.prepareStatement(sql)){
             ps.setString(1, uuid);
             try(ResultSet rs = ps.executeQuery()){
                 if(rs.next()) playerBalance = rs.getDouble("balance");
             }
        } catch (SQLException e){
            plugin.getLogger().warning("[E.O.G] There was an error inserting the player balance!");
        }

        //Updating the values with the amount
        playerBalance += amount;

        //Updating the values in the database
        String updateQuery = "UPDATE players SET balance = ? WHERE uuid = ?";
        try(PreparedStatement ps = dbConnection.prepareStatement(updateQuery)){
            ps.setDouble(1, playerBalance);
            ps.setString(2, uuid);
            ps.executeUpdate();
        } catch (SQLException e){
            plugin.getLogger().warning("[E.O.G] There was an error inserting the player balance!");
        }
    }

    public void withdrawFromPlayer(String uuid, double amount){
        String query = "UPDATE players SET balance = balance - ? WHERE uuid = ?";
        try(PreparedStatement statement = dbConnection.prepareStatement(query)){
            statement.setDouble(1, amount);
            statement.setString(2, uuid);
            statement.executeUpdate();
        } catch (SQLException e){
            plugin.getLogger().warning("[E.O.G] There was an error withdrawing from the player!");
        }
    }

    public Connection getDbConnection(){
        return dbConnection;
    }
}
