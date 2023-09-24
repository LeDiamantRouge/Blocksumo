package fr.lediamantrouge.blocksumo.manager;

import fr.kotlini.supragui.classes.builders.ItemBuilder;
import fr.lediamantrouge.blocksumo.Main;
import fr.lediamantrouge.blocksumo.game.Map;
import fr.lediamantrouge.blocksumo.util.PlayerUtils;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MakerManager {

    private List<Player> players = new ArrayList<>();
    private HashMap<Player, List<Location>> playersPreview = new HashMap<>();
    private HashMap<Player, Location> centerPreview = new HashMap<>();

    ItemStack[] items = new ItemStack[] {
            new ItemBuilder(Material.WOOL).name("§aAjouter un point de spawn").build(),
            new ItemBuilder(Material.REDSTONE_BLOCK).name("§aDéfinir le centre").build(),
            new ItemBuilder(Material.BLAZE_POWDER).name("§eDéfinir un nom & créer").build()
    };

    public List<Player> getPlayers() {
        return players;
    }

    public HashMap<Player, List<Location>> getPlayersPreviewHolograms() {
        return playersPreview;
    }

    public boolean isInMaker(Player player) {
        return players.contains(player);
    }

    public void addInMaker(Player player) {
        players.add(player);
        centerPreview.remove(player);
        playersPreview.remove(player);
        playersPreview.put(player, new ArrayList<>());
        centerPreview.put(player, null);
        player.sendMessage(Main.PREFIX + "§7Vous avez §aactivé §7l'§eéditeur de monde §7!");
        PlayerUtils.resetPlayer(player);
        player.setGameMode(GameMode.CREATIVE);
        player.getInventory().addItem(items);
    }

    public void remInMaker(Player player) {
        players.remove(player);
        player.getInventory().clear();
        PlayerUtils.resetPlayer(player);
        player.sendMessage(Main.PREFIX + "§7Vous avez §cdésactivé §7l'§eéditeur de monde §7!");
        player.teleport(Main.LOBBY_LOCATION);
    }

    public boolean isPreviewBlock(Block block) {
        AtomicBoolean success = new AtomicBoolean(false);
        playersPreview.values().forEach(bs -> {
            for (Location b : bs) {
                Location bl = block.getLocation();
                if(bl.getBlockX() == b.getBlockX() && bl.getBlockY() == b.getBlockY() && bl.getBlockZ() == b.getBlockZ() && bl.getWorld() == b.getWorld()) {
                    success.set(true);
                }
            }
        });
        return success.get();
    }

    // ---------------------------- MAKER --------------------------------

    public void addPoint(Player player, Location loc) {
        if(!isInMaker(player)) return;
        playersPreview.get(player).add(loc);
        player.sendMessage("§aLe point à été ajouter ! §7(" + playersPreview.get(player).size() + ")");
    }

    public void removePoint(Player player, Block b) {
        if(!isInMaker(player)) return;
        new ArrayList<>(playersPreview.get(player)).forEach(bl -> {
            if(bl.getBlockX() == b.getX() && bl.getBlockY() == b.getY() && bl.getBlockZ() == b.getZ() && bl.getWorld() == b.getWorld()) playersPreview.get(player).remove(bl);
        });
        player.sendMessage("§aLe point à été supprimer ! §7(" + playersPreview.get(player).size() + ")");
    }

    public void setCenter(Player player, Block block) {
        if(!isInMaker(player)) return;
        if(centerPreview.get(player) != null) centerPreview.get(player).getBlock().setType(Material.AIR);
        centerPreview.put(player, block.getLocation());
        player.sendMessage("§aLa position du centre à été défini !");
    }

    public void saveMap(Player player) {
        if(!isInMaker(player)) return;
        new AnvilGUI.Builder()
                .onClick((slot, stateSnapshot) -> {
                    if(slot != AnvilGUI.Slot.OUTPUT || stateSnapshot.getText().contains(" ")) return Collections.emptyList();

                    if(playersPreview.get(player).isEmpty() || centerPreview.get(player) == null) {
                        player.sendMessage("§cVous n'avez pas défini tous les prérequis");
                        return Collections.singletonList(AnvilGUI.ResponseAction.close());
                    }

                    playersPreview.values().forEach(bs -> {
                        for (Location b : bs) {
                            b.getBlock().setType(Material.AIR);
                        }
                    });
                    centerPreview.get(player).getBlock().setType(Material.AIR);

                    MapManager.getInstance().getMaps().add(new Map(stateSnapshot.getText(), playersPreview.get(player), centerPreview.get(player)));

                    player.sendMessage(Main.PREFIX + "§aLa map " + stateSnapshot.getText() + " à été créer !");
                    remInMaker(player);
                    player.setAllowFlight(true);
                    player.setFlying(true);
                    return Collections.singletonList(AnvilGUI.ResponseAction.close());
                }).plugin(Main.getInstance()).text("MapName").open(player);
    }

    // -------------------------------------------------------------------

    private static class Loader {
        private static final MakerManager INSTANCE = new MakerManager();
    }

    public static MakerManager getInstance() {
        return MakerManager.Loader.INSTANCE;
    }
}
