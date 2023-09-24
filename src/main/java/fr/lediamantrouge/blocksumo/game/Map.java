package fr.lediamantrouge.blocksumo.game;

import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.exceptions.*;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.plugin.SWMPlugin;
import com.grinderwolf.swm.plugin.config.ConfigManager;
import com.grinderwolf.swm.plugin.config.WorldData;
import com.grinderwolf.swm.plugin.config.WorldsConfig;
import fr.lediamantrouge.blocksumo.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class Map {

    private final String name;
    private final List<Location> spawns;
    private final Location center;
    private String tempWorld;

    public Map(String name, List<Location> spawns, org.bukkit.Location center) {
        this.name = name;
        this.spawns = spawns;
        this.center = center;
    }

    public void loadWorld() {
        tempWorld = name + "_copy_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        final SlimeLoader loader = ((SlimePlugin) Bukkit.getPluginManager().getPlugin("SlimeWorldManager")).
                getLoader(Main.DATA_SOURCE);
        final WorldsConfig config = ConfigManager.getWorldConfig();
        final WorldData worldData = config.getWorlds().get(name);

        try {
            SWMPlugin.getInstance().generateWorld(SWMPlugin.getInstance().loadWorld(loader, name, true,
                    worldData.toPropertyMap()).clone(tempWorld, loader));
            config.getWorlds().put(tempWorld, worldData);
            config.save();
        } catch (WorldAlreadyExistsException | IOException | UnknownWorldException | CorruptedWorldException |
                NewerFormatException | WorldInUseException e) {
            e.printStackTrace();
        }

        center.setWorld(Bukkit.getWorld(tempWorld));

        for (Location location : spawns) {
            location.setWorld(Bukkit.getWorld(tempWorld));
        }

        Bukkit.getWorld(tempWorld).setGameRuleValue("naturalRegeneration", "false");
    }

    public void unloadWorld() {
        if (tempWorld == null) return;

        final SlimeLoader loader = ((SlimePlugin) Bukkit.getPluginManager().getPlugin("SlimeWorldManager")).getLoader(Main.DATA_SOURCE);
        try {
            Bukkit.unloadWorld(tempWorld, false);
            if (loader.isWorldLocked(tempWorld)) {
                loader.unlockWorld(tempWorld);
            }
            loader.deleteWorld(tempWorld);

            final WorldsConfig config = ConfigManager.getWorldConfig();
            config.getWorlds().remove(tempWorld);
            config.save();
        } catch (UnknownWorldException | IOException e) {
            e.printStackTrace();
        }
    }


    public String getName() {
        return name;
    }

    public List<Location> getSpawns() {
        return spawns;
    }

    public Location getCenter() {
        return center;
    }

    public String getTempWorld() {
        return tempWorld;
    }
}
