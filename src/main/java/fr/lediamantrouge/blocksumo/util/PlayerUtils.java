package fr.lediamantrouge.blocksumo.util;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlayerUtils {

    public static void resetPlayer(Player player) {
        player.getInventory().clear();
        player.getInventory().setHelmet(null);
        player.getInventory().setChestplate(null);
        player.getInventory().setLeggings(null);
        player.getInventory().setBoots(null);
        player.setGameMode(GameMode.ADVENTURE);
        player.closeInventory();
        player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setExp(0f);
        player.setLevel(0);
        player.setHealth(20.0);
        player.setFoodLevel(20);
    }

    public static boolean checkNameHandItem(Player player, String displayName) {
        ItemStack it = player.getInventory().getItemInHand();
        return it != null
                && it.hasItemMeta()
                && it.getItemMeta().hasDisplayName()
                && it.getItemMeta().getDisplayName().equalsIgnoreCase(displayName);
    }
}
