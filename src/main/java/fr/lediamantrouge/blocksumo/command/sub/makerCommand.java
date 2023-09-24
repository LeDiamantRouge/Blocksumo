package fr.lediamantrouge.blocksumo.command.sub;

import fr.lediamantrouge.blocksumo.command.Args;
import fr.lediamantrouge.blocksumo.command.SubCommand;
import fr.lediamantrouge.blocksumo.manager.MakerManager;
import org.bukkit.entity.Player;

public class makerCommand extends SubCommand {
    @Override
    public String getName() {
        return "maker";
    }

    @Override
    public String getDescription() {
        return "Command to manage maps";
    }

    @Override
    public String getSyntax() {
        return "/bs maker toggle";
    }

    @Override
    public String getPermission() {
        return "bs.maker";
    }

    @Args(arg = "toggle", length = 2)
    public void toggle(Player player, String[] args) {
        if(!MakerManager.getInstance().isInMaker(player)) {
            MakerManager.getInstance().addInMaker(player);
        } else {
            MakerManager.getInstance().remInMaker(player);
        }
    }
}
