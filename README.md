
<img width="1080" height="720" alt="BannerTreasureHuntFinal" src="https://github.com/user-attachments/assets/23e69c68-bf3c-49d1-83fc-d85b72820651" />

<p align = "center">
  <img src="https://img.shields.io/badge/Plugin%20Version-1.3.4-blue?style=for-the-badge"> <img src="https://img.shields.io/badge/Minecraft%20Version-1.18%2B-green?style=for-the-badge"><br>
  <a href = "https://modrinth.com/plugin/echoes-of-gold"><img src = "https://img.shields.io/modrinth/dt/Sc8tOoum?style=for-the-badge&label=Modrinth%20Downloads&color=dark%20green"></a> <a href = "https://www.spigotmc.org/resources/echoes-of-gold-a-treasure-hunt-plugin-1-20-1-21-11-minor-bug-fixes.130639/"><img src = "https://img.shields.io/spiget/downloads/130639?style=for-the-badge&label=Spigot%20Downloads&color=yellow"></a>
</p>

**Echoes of Gold** is a fully customizable treasure-hunting minigame plugin designed to bring exploration and adventure to your Minecraft server.  
Players search for hidden treasures, collect rewards, and compete for the top spot on the leaderboard!

---

## ⚙️ Features
- 💎 Fully configurable treasures, via interactive GUIs
- ✨ Fully configurable particles
- 📜 Custom hints with clues or lore
- 💰 Internal Economy (via `SQLite` or `MySQL`) or External Economy (via `Vault` + `Economy Plugin`)
- 💰 Fully configurable **Shop GUI** (only for `Internal Economy`)
- 📦 PlaceholderAPI and Vault support
- 🧭 Customizable particles
- 🧮 Optional dynamic top 3 leaderboard (auto-updating)  
- ⏱️ Optional boss bar timer for event duration  
- 🧰 Simple setup and clean configuration files  
And **much more**!
---

## 🪄 Commands
| Command                             | Description | Permission |
|-------------------------------------|--------------|-------------|
| `/eog enable`              | Starts the event | `eog.commands.enable` |
| `/eog disable`             | Stops the event | `eog.commands.disable` |
| `/eog reload`              | Reloads the event | `eog.commands.reload` |
| `/eog treasures` | Opens the  treasure manager | `eog.commands.treasures` |
| `/eog event ...` | Manages main event settings | `eog.commands.event. ...` |
| `/eog event shop ...` | Handles the main shop commands | `eog.commands.shop. ...` |
| `/eog help` | Helps with information on commands | `eog.commands.help` |
| `/thshop`| Opens the Shop GUI | `eog.shop.use` |
| `/hints`                            | Opens the hints GUI | `eog.hints.use` |

---

## 🔐 Permissions
| Permission                            | Description | 
|-------------------------------------|--------------|
| `eog.commands`              | Gives access to commands |
| `eog.commands.enable`             | Gives access to enable the event |
| `eog.commands.disable`              | Gives access to disable the event | 
| `eog.commands.reload` | Gives access to the reload command | 
| `eog.commands.help` | Gives access to the help command | 
| `eog.commands.treasures` | Gives access to the treasure manager | 
| `eog.commands.event` | Gives access to the commands related to the event | 
| `eog.commands.event.setstartposition` | Gives access to modify the start position | 
| `eog.commands.event.setstarttitle` | Gives access to modify the start title |
| `eog.commands.event.setstartsubtitle` | Gives access to modify the start subtitle |
| `eog.commands.event.settreasurenr` | Gives access to set the number of treasures | 
| `eog.commands.shop` | Gives access to the shop commands |
| `eog.commands.shop.additem` | Gives access to add items in the shop | 
| `eog.commands.shop.removeitem` | Gives access to remove items from the shop |
| `eog.treasures.create` | Gives access to create treasures | 
| `eog.treasures.delete` | Gives access to delete treasures |
| `eog.treasures.setlocation` | Gives access to set the location of a treasure |
| `eog.treasures.setworld` | Gives access to set the world of a treasure |
| `eog.treasures.setfacing` | Gives access to set the facing of a treasure | 
| `eog.treasures.setcoins` | Gives access to set the coins of a treasure |
| `eog.treasures.sethint` | Gives access to set the hint of a treasure |
| `eog.hints.use` | Gives access to the Hints GUI |
| `eog.shop.use` | Gives access to the Shop GUI | 

---

## 📁 Configuration Files
**Echoes of Gold** uses 3 different *configuration file*, each with it's own use.

### `config.yml`
Contains *important settings* for the plugin:
- Configuring the **sounds** and their *volume* and *pitch*
- Configuring the scoreboard
- Configuring the boss bar
- Configure the messages  
  And much more!

### `treasures.yml`
Stores all the treasures that you may create. 

### `playerdata.yml`
Tracks player progress and the treasures they’ve discovered.

---

## ❤️ Credits
Developed and tested by **\_ItsAndrew_**  
Special thanks to everyone who help, test and give feedback!  
My discord: *\_itsandrew_*
