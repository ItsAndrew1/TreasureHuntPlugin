//Developed by _ItsAndrew_
package me.andrew.EchoesOfGold.Economy;

import me.andrew.EchoesOfGold.EchoesOfGold;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class ShopGUI implements Listener {
    private final EchoesOfGold plugin;
    private final NamespacedKey shopKey;

    private final Map<UUID, ShopGuiChoice> guiChoices = new HashMap<>();
    private final Map<UUID, Integer> GUIPages = new HashMap<>();
    private final Set<UUID> switchStances = new HashSet<>();

    public ShopGUI(EchoesOfGold plugin) {
        this.plugin = plugin;
        this.shopKey = new NamespacedKey(plugin, "shop-item-id");
    }

    public void showGUI(Player player, int page, ShopGuiChoice guiChoice){
        FileConfiguration mainConfig = plugin.getConfig();

        //Adding the player's choice and page in the maps
        if(!guiChoices.containsKey(player.getUniqueId())) guiChoices.put(player.getUniqueId(), guiChoice);
        if(!GUIPages.containsKey(player.getUniqueId())) GUIPages.put(player.getUniqueId(), page);

        //Getting the size of the gui
        int guiRows = mainConfig.getInt("economy.shop-gui.rows", 6);
        if(guiRows < 1 || guiRows > 6) guiRows = 6;
        int guiSize = guiRows * 9;

        //Getting the GUI's title
        String title = guiChoice.getShopGuiTitle(plugin.getConfig()) + " (Page "+page+")";

        //Creating the GUI
        Inventory shopGUI = Bukkit.createInventory(null, guiSize, title);

        //Setting the exit button
        String materialTXT = mainConfig.getString("economy.shop-gui.exit-item.material", "red_concrete"); //Default value is 'red_concrete'
        int exitButtonSlot = mainConfig.getInt("economy.shop-gui.exit-item.slot", 49);
        ItemStack exitButton = createButton(Material.matchMaterial(materialTXT.toUpperCase()), mainConfig.getString("economy.shop-gui.exit-item.display-name", "&c&lEXIT"));
        shopGUI.setItem(exitButtonSlot, exitButton);

        //Adding the decoration if it is toggled
        boolean toggleDeco = mainConfig.getBoolean("economy.shop-gui.decoration.toggle", true);
        if(toggleDeco){
            String decoMaterialTXT = mainConfig.getString("economy.shop-gui.decoration.material", "black_stained_glass_pane");
            ItemStack decoItem = createButton(Material.matchMaterial(decoMaterialTXT.toUpperCase()), mainConfig.getString("economy.shop-gui.decoration.display-name", " "));
            for(int i = 0; i<9; i++) shopGUI.setItem(i, decoItem);
        }

        //Adding the player head to the gui (if it is toggled)
        boolean togglePlayerHead = plugin.getConfig().getBoolean("economy.shop-gui.player-head-item.toggle", true);
        if(togglePlayerHead) {
            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta headMeta = (SkullMeta) playerHead.getItemMeta();

            int slot = plugin.getConfig().getInt("economy.shop-gui.player-head-item.slot", 4);
            if(slot < 0 || slot > 53) slot = 4;

            headMeta.setOwningPlayer(player);

            //Setting the display name
            String displayName = plugin.getConfig().getString("economy.shop-gui.player-head-item.display-name", "&e%player_name%")
                    .replace("%player_name%", player.getName());
            displayName = plugin.ParsePP(player, displayName);
            headMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));

            //Setting the lore
            List<String> lore = new ArrayList<>();
            for(String line : plugin.getConfig().getStringList("economy.shop-gui.player-head-item.lore")) {
                line = plugin.ParsePP(player, line);
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            headMeta.setLore(lore);

            playerHead.setItemMeta(headMeta);
            shopGUI.setItem(slot, playerHead);
        }

        //Getting data for displaying the items
        ConfigurationSection shopItems = mainConfig.getConfigurationSection("economy.shop-gui.items");
        //Displaying the 'no items' item (if there aren't any items configured)
        if(shopItems == null || shopItems.getKeys(false).isEmpty()){
            String noHintsMaterialTXT = mainConfig.getString("economy.shop-gui.no-items-item.material", "barrier");
            ItemStack noHintsItem = createButton(Material.matchMaterial(noHintsMaterialTXT.toUpperCase()), mainConfig.getString("economy.shop-gui.no-items-item.display-name", "&eThere are no items set yet!"));
            int nhiSlot = mainConfig.getInt("economy.shop-gui.no-items-item.slot", 22);
            shopGUI.setItem(nhiSlot, noHintsItem);
        }

        //Else, displays the items
        else{
            int startingSlot = toggleDeco ? 9 : 0;
            int maximumNrOfItemsPerPage = toggleDeco ? 36 : 45;
            List<String> itemList = shopItems.getKeys(false).stream().toList();
            int offset = (page - 1) * maximumNrOfItemsPerPage;
            int endIndex = Math.min(offset + maximumNrOfItemsPerPage, itemList.size());

            for(int i = 0, j = offset; i<45 && j < itemList.size(); i++, j++){
                String itemID = itemList.get(j);
                ItemStack shopItem = shopItems.getItemStack(itemID + ".item").clone();
                ItemMeta shopItemMeta = shopItem.getItemMeta();
                double shopItemPrice = shopItems.getDouble(itemID + ".price");
                String itemPriceLore = mainConfig.getString("economy.shop-gui.displaying-the-price", "&aPrice: &f%item_price%")
                        .replace("%item_price%", String.valueOf(shopItemPrice));

                //Building the item lore
                List<String> shopItemLore = shopItem.hasItemMeta() ? shopItem.getItemMeta().getLore() : new ArrayList<>();
                shopItemLore.add(" ");
                shopItemLore.add(ChatColor.translateAlternateColorCodes('&', itemPriceLore));
                shopItemLore.add(ChatColor.translateAlternateColorCodes('&', plugin.getEconomyProvider().hasEnough(player, shopItemPrice) ? "&7Click to purchase!" : "&cNot enough money!"));
                shopItemMeta.setLore(shopItemLore);

                //Saving an ID for each item
                shopItemMeta.getPersistentDataContainer().set(shopKey, PersistentDataType.STRING, itemID);

                //Displaying the item
                shopItem.setItemMeta(shopItemMeta);
                shopGUI.setItem(startingSlot+i, shopItem);
            }

            //Displaying the next page button
            if(endIndex < itemList.size()){
                ItemStack nextPageButton = createButton(Material.ARROW, "&aNext Page ▶");
                shopGUI.setItem(53, nextPageButton);
            }

            //Displaying the previous page button
            if(page > 1){
                ItemStack previousPageButton = createButton(Material.ARROW, "&a◀ Previous Page");
                shopGUI.setItem(45, previousPageButton);
            }
        }

        player.openInventory(shopGUI);
    }

    private ItemStack createButton(Material material, String displayName){
        ItemStack button = new ItemStack(material);
        ItemMeta buttonMeta = button.getItemMeta();
        buttonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));

        button.setItemMeta(buttonMeta);
        return button;
    }

    public Map<UUID, ShopGuiChoice> getGuiChoices() {
        return guiChoices;
    }
    public Set<UUID> getSwitchStances() {
        return switchStances;
    }

    public void removePlayerFromMaps(UUID playerUUID){
        guiChoices.remove(playerUUID);
        GUIPages.remove(playerUUID);
    }

    @EventHandler
    public void onGuiLeave(InventoryCloseEvent event){
        //Getting the player's choice
        Player player = (Player) event.getPlayer();
        ShopGuiChoice playerChoice = guiChoices.get(player.getUniqueId());
        if(playerChoice == null) return;

        if(!event.getView().getTitle().contains(playerChoice.getShopGuiTitle(plugin.getConfig()))) return;
        if(switchStances.remove(player.getUniqueId())) return;

        removePlayerFromMaps(player.getUniqueId());
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event){
        if(!(event.getWhoClicked() instanceof Player player)) return;

        //Getting the player's choice
        ShopGuiChoice playerChoice = guiChoices.get(player.getUniqueId());
        if(playerChoice == null) return;

        String shopGuiTitle = playerChoice.getShopGuiTitle(plugin.getConfig());
        if(!event.getView().getTitle().contains(shopGuiTitle)) return;

        //Making it impossible to take items from the GUI
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem == null) return;

        ItemMeta clickedItemMeta = clickedItem.getItemMeta();
        if(clickedItemMeta == null) return;

        Sound clickSound = Sound.UI_BUTTON_CLICK;

        //If the player clicks on the exit button
        Material exitButton = Material.matchMaterial(plugin.getConfig().getString("economy.shop-gui.exit-item.material", "red_concrete").toUpperCase());
        if(clickedItem.getType().equals(exitButton)){
            player.closeInventory();
            player.playSound(player.getLocation(), clickSound, 1f, 1f);
            return;
        }

        //If the player clicks on the next page button
        if(clickedItemMeta.getDisplayName().contains("Next Page")){
            player.playSound(player.getLocation(), clickSound, 1f, 1f);
            showGUI(player, GUIPages.get(player.getUniqueId()) + 1, guiChoices.get(player.getUniqueId()));
            return;
        }

        //If the player clicks on the previous page button
        if(clickedItemMeta.getDisplayName().contains("Previous Page")){
            player.playSound(player.getLocation(), clickSound, 1f, 1f);
            plugin.getShopGUI().showGUI(player, GUIPages.get(player.getUniqueId()) - 1, guiChoices.get(player.getUniqueId()));
            return;
        }

        //Handling the click based on the choice
        switch(playerChoice){
            case SHOP -> handleShopClick(player, clickedItemMeta);
            case REMOVE_ITEM -> handleRemoveItemClick(player, clickedItemMeta);
        }
    }

    private void handleShopClick(Player player, ItemMeta clickedItemMeta){
        String itemId = clickedItemMeta.getPersistentDataContainer().get(shopKey, PersistentDataType.STRING);
        if(itemId == null) return;

        double itemPrice = plugin.getConfig().getDouble("economy.shop-gui.items."+itemId+".price", 0);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean playerHasEnough = plugin.getEconomyProvider().hasEnough(player, itemPrice);

            if(playerHasEnough){
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.closeInventory();

                    String chatMessage = plugin.getConfig().getString("economy.shop-gui.not-enough-money-message", "&cYou don't have enough money to buy this!");
                    chatMessage = plugin.ParsePP(player, chatMessage);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatMessage));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
                });
            }
            else {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    //Checking if the player has enough inventory space
                    Inventory playerInv = player.getInventory();
                    if (playerInv.firstEmpty() == -1) {
                        player.closeInventory();

                        String chatMessage = plugin.getConfig().getString("economy.shop-gui.no-inventory-space-message", "&cYou don't have enough space in your inventory to buy this!");
                        chatMessage = plugin.ParsePP(player, chatMessage);
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', chatMessage));
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
                        return;
                    }

                    switchStances.add(player.getUniqueId());
                    plugin.getShopGuiFinalChoice().showGui(player, itemId);
                });
            }
        });
    }

    private void handleRemoveItemClick(Player player, ItemMeta clickedItemMeta){
        String itemId = clickedItemMeta.getPersistentDataContainer().get(shopKey, PersistentDataType.STRING);
        if(itemId == null) return;

        switchStances.add(player.getUniqueId());
        plugin.getShopGuiFinalChoice().showGui(player, itemId);
    }
}

