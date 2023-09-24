package fr.lediamantrouge.blocksumo;

import fr.lediamantrouge.blocksumo.command.CommandHandler;
import fr.lediamantrouge.blocksumo.config.ConfigManager;
import fr.lediamantrouge.blocksumo.listener.MakerListeners;
import fr.lediamantrouge.blocksumo.manager.MapManager;
import fr.lediamantrouge.blocksumo.server.ServerType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    private static Main instance;

    public static String PREFIX = "§c§lBLOCKSUMO §8» §f";

    public static Location LOBBY_LOCATION;
    public static String DATA_SOURCE = "file";

    public static ServerType serverType;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        String sT = getConfig().getString("server-type");
        if(sT.equalsIgnoreCase("HOST")) serverType = ServerType.HOST;
        if(sT.equalsIgnoreCase("MULTI-INSTANCE")) serverType = ServerType.MULTI_INSTANCE;
        MapManager.getInstance().unLoadAllWorlds();
        getCommand("blocksumo").setExecutor(new CommandHandler());
        Bukkit.getPluginManager().registerEvents(new MakerListeners(), this);
        ConfigManager.loadMaps();
        LOBBY_LOCATION = new Location(Bukkit.getWorld("waiting"), 0, 151, 0);
    }

    @Override
    public void onDisable() {
        ConfigManager.saveMaps();
    }

    public static Main getInstance() {
        return instance;
    }
}
