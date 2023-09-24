package fr.lediamantrouge.blocksumo.config;

import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.plugin.config.WorldsConfig;
import com.sun.javafx.scene.traversal.Direction;
import fr.lediamantrouge.blocksumo.Main;
import fr.lediamantrouge.blocksumo.game.Map;
import fr.lediamantrouge.blocksumo.manager.MapManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigManager {

    private static final File PLUGIN_DIR = new File("plugins", "Blocksumo");

    private static final File MAPS_FILE;

    public static void loadMaps() {
        if (!MAPS_FILE.exists()) {
            if (!PLUGIN_DIR.exists()) {
                PLUGIN_DIR.mkdir();
            }
            try {
                MAPS_FILE.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        final FileConfiguration config = YamlConfiguration.loadConfiguration(MAPS_FILE);
        if (config.getConfigurationSection("maps") == null) return;

        for (String mapName : config.getConfigurationSection("maps").getKeys(false)) {
            final String key = "maps." + mapName + ".";

            final List<Location> spawns = new ArrayList<>();
            final Location center;

            for(String locName : config.getStringList(key + "spawns")) spawns.add(ConfigManager.parseStringToLoc(locName));
            center = ConfigManager.parseStringToLoc(config.getString(key + "center"));


            MapManager.getInstance().getMaps().add(new Map(mapName, spawns, center));
        }
    }

    public static void saveMaps() {
        final FileConfiguration config = YamlConfiguration.loadConfiguration(MAPS_FILE);

        for (Map map : MapManager.getInstance().getMaps()) {
            final String key = "maps." + map.getName() + ".";
            config.set(key + "spawns", map.getSpawns().stream().map(ConfigManager::parseLocToString).collect(Collectors.toList()));
            config.set(key + "center", parseLocToString(map.getCenter()));
        }

        try {
            config.save(MAPS_FILE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        MapManager.getInstance().unLoadAllWorlds();
    }


    private static String parseLocToString(Location location) {
        return location.getX() + "," + location.getY() + "," + location.getZ() + "," + location.getYaw() + ", " + location.getPitch();
    }

    private static Location parseStringToLoc(String loc) {
        final String[] parser = loc.split(",");
        return (new Location(null, Double.parseDouble(parser[0]), Double.parseDouble(parser[1]),
                Double.parseDouble(parser[2]), Float.parseFloat(parser[3]), Float.parseFloat(parser[4])));
    }

    static {
        MAPS_FILE = new File(PLUGIN_DIR, "maps.yml");
    }
}
