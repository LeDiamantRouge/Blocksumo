package fr.lediamantrouge.blocksumo.manager;

import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.plugin.config.WorldsConfig;
import fr.lediamantrouge.blocksumo.Main;
import fr.lediamantrouge.blocksumo.game.Map;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MapManager {

    private final List<Map> maps = new ArrayList<>();

    public Map getRandomWorld() {
        if (!maps.isEmpty()) {
            return maps.get(new Random().nextInt(maps.size()));
        } else {
            return null;
        }
    }


    public Map getMapByName(String string) {
        for(Map map : maps) {
            if(map.getName().equalsIgnoreCase(string)) return map;
        }
        return null;
    }

    public List<Map> getMaps() {
        return maps;
    }

    public void unLoadAllWorlds() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getName().contains("_copy_")) {
                final SlimeLoader loader = ((SlimePlugin) Bukkit.getPluginManager().getPlugin("SlimeWorldManager")).
                        getLoader(Main.DATA_SOURCE);
                try {
                    if (!Bukkit.getWorld(world.getName()).getPlayers().isEmpty())
                        Bukkit.getWorld(world.getName()).getPlayers().forEach(player -> player.teleport(Main.LOBBY_LOCATION));

                    Bukkit.unloadWorld(world.getName(), false);
                    if (loader.isWorldLocked(world.getName())) {
                        loader.unlockWorld(world.getName());
                    }
                    loader.deleteWorld(world.getName());

                    final WorldsConfig cg = com.grinderwolf.swm.plugin.config.ConfigManager.getWorldConfig();
                    cg.getWorlds().remove(world.getName());
                    cg.save();
                } catch (UnknownWorldException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class Loader {
        private static final MapManager INSTANCE = new MapManager();
    }

    public static MapManager getInstance() {
        return MapManager.Loader.INSTANCE;
    }
}
