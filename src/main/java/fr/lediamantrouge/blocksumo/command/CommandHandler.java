package fr.lediamantrouge.blocksumo.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


public class CommandHandler implements CommandExecutor {

    private final List<SubCommand> subcommands = new ArrayList<>();

    public CommandHandler() {
        for (Class<?> clazz : new Reflections("fr.lediamantrouge.").getSubTypesOf(SubCommand.class)) {
            try {
                subcommands.add((SubCommand) clazz.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            final Player p = (Player) sender;

            if (args.length > 0) {
                for (int i = 0; i < getSubcommands().size(); i++) {
                    if (args[0].equalsIgnoreCase(getSubcommands().get(i).getName())) {
                        if(getSubcommands().get(i).getPermission() == null || p.hasPermission(getSubcommands().get(i).getPermission())) {
                            final Class<?> clazz = getSubcommands().get(i).getClass();
                            for (Method method : clazz.getMethods()) {
                                if (!method.isAnnotationPresent(Args.class)) continue;
                                final Args annotation = method.getAnnotation(Args.class);

                                if(args.length == 1) {
                                    p.sendMessage("§6§m| §b" + getSubcommands().get(i).getSyntax().replace("\n", " §6| §b") + " §7- §f" +
                                            getSubcommands().get(i).getDescription());
                                    continue;
                                }
                                if (!args[1].equalsIgnoreCase(annotation.arg())) continue;
                                if (args.length != annotation.length()) {
                                    p.sendMessage("§6§m| §b§l" + getSubcommands().get(i).getSyntax().replace("\n", " | ") + " §7- §f" +
                                            getSubcommands().get(i).getDescription());
                                    continue;
                                }

                                try {
                                    method.invoke(getSubcommands().get(i), p, args);
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            p.sendMessage("§cVous n'avez pas accès à cela...");
                        }
                    }
                }
            } else {
                p.sendMessage("§8§m-------------------------------------------");
                if (p.hasPermission("capturetheflag.showHelpMessage")) {
                    for (int i = 0; i < getSubcommands().size(); i++) {
                        p.sendMessage("§6§m| §b§l" + getSubcommands().get(i).getSyntax().replace("\n", " | ") + " §7- §f" +
                                getSubcommands().get(i).getDescription());
                    }
                } else {
                    p.sendMessage("§cPlugin created by §nLeDiamantRouge§c with §6§l❤");
                }
                p.sendMessage("§8§m-------------------------------------------");
            }

        }


        return true;
    }

    public List<SubCommand> getSubcommands() {
        return subcommands;
    }
}
