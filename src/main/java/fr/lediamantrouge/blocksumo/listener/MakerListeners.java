package fr.lediamantrouge.blocksumo.listener;

import fr.lediamantrouge.blocksumo.manager.MakerManager;
import fr.lediamantrouge.blocksumo.util.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MakerListeners implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        if(MakerManager.getInstance().isInMaker(e.getPlayer())) {
            MakerManager.getInstance().getPlayers().remove(e.getPlayer());
            e.getPlayer().getInventory().clear();
        }
    }

    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent e) {
        if(!MakerManager.getInstance().isInMaker(e.getPlayer())) return;
        if(PlayerUtils.checkNameHandItem(e.getPlayer(), "§aAjouter un point de spawn")) {
            Location loc = e.getBlock().getLocation();
            loc.setYaw(e.getPlayer().getLocation().getYaw());
            MakerManager.getInstance().addPoint(e.getPlayer(), loc);
            return;
        }
        if(PlayerUtils.checkNameHandItem(e.getPlayer(), "§aDéfinir le centre")) {
            MakerManager.getInstance().setCenter(e.getPlayer(), e.getBlock());
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if(!MakerManager.getInstance().isInMaker(e.getPlayer())) return;
        if(PlayerUtils.checkNameHandItem(e.getPlayer(), "§eDéfinir un nom & créer")) {
            MakerManager.getInstance().saveMap(e.getPlayer());
            e.setCancelled(true);
        }
    }


    @EventHandler
    public void onBreakBlock(BlockBreakEvent e) {
        if(!MakerManager.getInstance().isPreviewBlock(e.getBlock()) && MakerManager.getInstance().isInMaker(e.getPlayer())) {
            e.setCancelled(true);
            return;
        }
        MakerManager.getInstance().removePoint(e.getPlayer(), e.getBlock());
    }
}
