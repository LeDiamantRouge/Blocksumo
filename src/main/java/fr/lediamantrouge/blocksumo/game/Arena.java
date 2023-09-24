package fr.lediamantrouge.blocksumo.game;

import fr.kotlini.supragui.classes.builders.ItemBuilder;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.*;
import java.util.stream.Collectors;


@Getter
@Setter
public class Arena {

    private Map map;
    private State state;
    private final String id;
    private final HashMap<UUID, FPlayer> players;
    private final HashMap<TeamType, Flag> flags;
    private final HashMap<TeamType, Integer> points;
    private final HashMap<Integer, Boolean> gapState;
    private final LinkedList<UUID> randomTeam;
    private HashMap<Integer, Integer> tasks;
    private int timer;

    public Arena(fr.kotlini.capturetheflag.model.Map map) {
        this.map = map;
        this.state = State.WAITING;
        this.players = new HashMap<>();
        this.flags = new HashMap<>();
        this.tasks = new HashMap<>();
        this.gapState = new HashMap<>();
        this.randomTeam = new LinkedList<>();
        this.id = Algorithm.generateShortId();
        this.points = new HashMap<>();
        timer = CaptureTheFlag.TOTAL_STARTING_TIME;
        initialize();
    }

    public void initialize() {
        for (TeamType type : getMap().getPlayers().keySet()) {
            points.put(type, 0);
            flags.put(type, new Flag(type, new ItemStack(Material.WOOL, 1, TeamsHandler.getTeam(type).getData()),
                    map.getDirections().get(type)));
        }

        for (Location location : getMap().getGap()) {
            gapState.put(getMap().getGap().indexOf(location), true);
        }
    }

    public void kill(FPlayer fPlayer, FPlayer fKiller, EntityDamageEvent.DamageCause damageCause) {
        final Player player = fPlayer.toBukkit();
        setSpectator(fPlayer);
        if (fPlayer.isHasFlag()) {
            fPlayer.setHasFlag(false);
            Flag flag = getFlagByUuid(player.getUniqueId());
            flag.teleportOnGround(fPlayer.toBukkit().getLocation().clone().add(0, -0.5, 0));
            flag.respawn(getMap().getFlagLocation(flag.getTeamType()).clone().add(0, -1, 0));
        }
        fPlayer.respawn(this);
        Message.setMessageOnDeath(this, damageCause, fPlayer, (player.getKiller() != null ? fKiller : null));

        if (player.getKiller() != null) {
            final Player killer = player.getKiller();
            killer.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE));
            killer.getInventory().addItem(new ItemStack(Material.ARROW, 6));
        }
    }

    public void addPoint(TeamType type) {
        this.points.replace(type, points.get(type) + 1);
    }

    public void removePlayer(FPlayer player) {
        if(!getPlayers().containsValue(player)) return;
        if(state.equals(State.STARTING) || state.equals(State.WAITING)) {
            sendMessage(CaptureTheFlag.PREFIX + " §6" + player.toBukkit().getName() + " §fà quitter §7(§c" + (getPlayersInLife().size() - 1) + "§7/§c" + (getMap().getTeamSize() * getFlags().size()) + "§7)");
        } else {
            if(!player.getTeamType().equals(TeamType.SPECTATOR))
                sendMessage(CaptureTheFlag.PREFIX + " §6" + player.toBukkit().getName() + " §fà quitter");
        }

        player.reset();

        player.toBukkit().teleport(CaptureTheFlag.LOBBY_LOCATION);

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (MapMakerHandler.getInstance().hasPlayer(target.getUniqueId())) continue;

            if (ArenaHandler.getInstance().hasPlayer(target.getUniqueId())) {
                target.hidePlayer(player.toBukkit());
                player.toBukkit().hidePlayer(target);
            } else {
                target.showPlayer(player.toBukkit());
                player.toBukkit().showPlayer(target);
            }
        }

        getPlayers().remove(player.getUuid());
        randomTeam.remove(player.getUuid());

        if(player.getTeamType().equals(TeamType.SPECTATOR)) return;

        if (!getState().equals(State.WAITING) && !getState().equals(State.ENDING) && !getState().equals(State.STARTING)) {
            if (getTeamPlayers(player.getArenaTeamType()).isEmpty()) {
                getFlagByTeam(player.getArenaTeamType()).delete();
                getFlags().remove(player.getArenaTeamType());
                getPoints().remove(player.getArenaTeamType());
            }

            if (getPlayers().values().stream().map(FPlayer::getTeamType).filter(team -> getFlags().containsKey(team)).
                    distinct().count() < 2) {
                end();
            }
        }
    }

    public void addPlayer(FPlayer player) {
        players.put(player.toBukkit().getUniqueId(), player);


        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(player.toBukkit())) continue;
            final FPlayer fTarget = getPlayers().get(target.getUniqueId());

            if (state.equals(State.WAITING) || state.equals(State.STARTING)) {
                if (hasPlayer(target.getUniqueId())) {
                    target.showPlayer(player.toBukkit());
                    player.toBukkit().showPlayer(target);
                    continue;
                }
            } else {
                if (hasPlayer(target.getUniqueId())) {
                    if (fTarget.getTeamType().equals(TeamType.SPECTATOR)) {
                        target.showPlayer(player.toBukkit());
                    }
                    player.toBukkit().showPlayer(target);
                    continue;
                }
            }

            player.hidePlayer(target);
            target.hidePlayer(player.toBukkit());
        }

        player.toBukkit().teleport(CaptureTheFlag.WAITING_LOCATION);


        if (getPlayers().size() >= getMap().getTeamSize() * getMap().getFlags().size() / 2)
            startStarting();

        if(getPlayers().size() >= getMap().getTeamSize() * getMap().getFlags().size() && ArenaHandler.getInstance().getNextArena() == null) {
            final fr.kotlini.capturetheflag.model.Map randomMap = MapManager.getInstance().getRandomWorld();
            if (randomMap != null)
                ArenaHandler.getInstance().addArena(new Arena(randomMap));
        }

        if (state.equals(State.WAITING) || state.equals(State.STARTING)) {
            getRandomTeam().add(player.toBukkit().getUniqueId());
            sendMessage(CaptureTheFlag.PREFIX + " §6" + player.toBukkit().getName() + " §fà rejoint la partie §7(§a" + getPlayersInLife().size() + "§7/§a" + (getMap().getTeamSize() * getMap().getFlags().size()) + "§7)");
            player.toBukkit().getInventory().setItem(8, new ItemBuilder(Material.BED).name("§cQuitter").build());
        } else {
            setSpectator(player);
            player.toBukkit().teleport(getMap().randomLocation());
        }
    }

    public boolean hasPlayer(UUID uuid) {
        return players.containsKey(uuid);
    }

    public void start() {
        state = State.STARTING_GAME;

        if (ArenaHandler.getInstance().getNextArena() == null) {
            final Map randomMap = MapManager.getInstance().getRandomWorld();
            if (randomMap != null)
                ArenaHandler.getInstance().addArena(new Arena(randomMap));
        }
        getPlayers().forEach((uuid, uhcPlayer) -> {
            uhcPlayer.toBukkit().sendTitle("", "§bTéléportation...");
            uhcPlayer.toBukkit().addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 2, 3), true);
        });

        map.loadWorld();

        Collections.shuffle(getRandomTeam());
        for (UUID uuid : getRandomTeam())  {
            final FPlayer player = getPlayer(uuid);
            player.setArenaTeamType(TeamsHandler.addInRandomTeam(this, player));
        }

        for (FPlayer fPlayer : getPlayers().values()) {
            if (fPlayer.getTeamType().equals(TeamType.DEFAULT)) {
                fPlayer.toBukkit().kickPlayer("§cUne erreur c'est produite.");
                continue;
            }

            if (fPlayer.getTeamType().equals(TeamType.SPECTATOR)) {
                fPlayer.toBukkit().teleport(getMap().randomLocation());
                continue;
            }

            putPlayerInGame(fPlayer);
        }

        setTimer(5);

        tasks.put(0, Bukkit.getScheduler().runTaskTimer(CaptureTheFlag.getInstance(), () -> {
            if (timer == 0) {
                state = State.PLAYING;
                getPlayers().values().forEach(uhcPlayer -> {
                    final Player player = uhcPlayer.toBukkit();
                    player.playSound(player.getLocation(), Sound.ENDERDRAGON_GROWL, 1f, 1f);
                    player.setLevel(0);
                    player.sendTitle("", "");
                });

                for (TeamType type : flags.keySet()) {
                    flags.get(type).create(getMap().getFlagLocation(type).clone().add(0, -1, 0));
                }
                Bukkit.getScheduler().cancelTask(tasks.get(0));
                tasks.remove(0);
                timer = CaptureTheFlag.PARTY_TIME;
                if (!map.getGap().isEmpty()) {
                    tasks.put(5, Bukkit.getScheduler().runTaskTimer(CaptureTheFlag.getInstance(),
                            new GoldenApplePointTask(this), 0, 5L).getTaskId());
                }
                tasks.put(3, Bukkit.getScheduler().runTaskTimer(CaptureTheFlag.getInstance(), new PointTask(this), 0, 1L).getTaskId());
                tasks.put(2, Bukkit.getScheduler().runTaskTimer(CaptureTheFlag.getInstance(), new FlagTask(this), 0, 1L).getTaskId());
                tasks.put(1, Bukkit.getScheduler().runTaskTimer(CaptureTheFlag.getInstance(), new GameTask(this), 0, 20L).getTaskId());
                return;
            }

            for (FPlayer ps : getPlayers().values()) {
                final Player player = ps.toBukkit();
                player.playSound(player.getLocation(), Sound.NOTE_PLING, 1f, 1f);
                ps.setXpBar(timer, 5);
                player.setLevel(timer);
                player.sendTitle(Message.getColorByNumber(timer) + timer, "");
            }

            timer--;
        }, 0, 20L).getTaskId());
    }

    public void startStarting() {
        if (!checkPlayers() || tasks.get(4) != null || !state.equals(State.WAITING)) return;

        final List<Integer> timers = Arrays.asList(60, 30, 15, 10, 5, 4, 3, 2, 1);
        setTimer(CaptureTheFlag.TOTAL_STARTING_TIME);
        setState(State.STARTING);
        try {
            getTasks().put(4, Bukkit.getScheduler().runTaskTimer(CaptureTheFlag.getInstance(), () -> {
                if (getPlayers().size() <= 1) {
                    for (FPlayer fPlayer : getPlayers().values()) {
                        final Player ps = fPlayer.toBukkit();
                        ps.playSound(ps.getLocation(), Sound.NOTE_BASS_DRUM, 1f, 1f);
                        ps.setLevel(0);
                    }
                    sendMessage(CaptureTheFlag.PREFIX + " §fManque de joueur pour §6commencer§f, début §6annulé§f.");
                    setTimer(CaptureTheFlag.TOTAL_STARTING_TIME);
                    setState(State.WAITING);
                    Bukkit.getScheduler().cancelTask(getTasks().get(4));
                    getTasks().remove(4);
                    return;
                }

                if (timer == 0) {
                    Bukkit.getScheduler().cancelTask(getTasks().get(4));
                    getTasks().remove(4);
                    start();
                    return;
                }

                for (FPlayer fPlayer : getPlayers().values()) {
                    final Player p = fPlayer.toBukkit();
                    if (timers.contains(getTimer())) {
                        p.sendMessage(CaptureTheFlag.PREFIX + " §fDémarrage de la partie dans §6" + getTimer() + " seconde" + (getTimer() == 1 ? "" : "s") + " §f!");
                        p.playSound(p.getLocation(), Sound.NOTE_PLING, 1f, 1f);
                    }
                    p.setLevel(getTimer());
                    fPlayer.setXpBar(p, timer, CaptureTheFlag.TOTAL_STARTING_TIME);
                }



                setTimer(getTimer() - 1);
            },0L, 20L).getTaskId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void end() {
        state = State.ENDING;

        Bukkit.getScheduler().cancelTask(tasks.get(1));

        LinkedHashMap<TeamType, Integer> sortedHashMap = points.entrySet().stream()
                .sorted(java.util.Map.Entry.<TeamType, Integer> comparingByValue().reversed()).limit(3).collect(Collectors.toMap(
                        java.util.Map.Entry::getKey, java.util.Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        final TeamType teamWin = sortedHashMap.keySet().stream().findFirst().orElse(null);

        StringBuilder message = new StringBuilder(Message.centerText("§6Fin de la partie") + "\n\n" +
                Message.centerText("§8§m----------§r§6SCORE TOTAL§8§m----------§r") + "\n\n");
        int rang = 1;
        for (java.util.Map.Entry<TeamType, Integer> entry : sortedHashMap.entrySet()) {
            message.append(Message.centerText(String.format("§f%d. %s §f- §6%d\n", rang++, TeamsHandler.getTeam(entry.getKey()).getPrefix() + entry.getKey(), entry.getValue())));
        }
        message.append("\n").append(Message.centerText("§8§m-------------------------------§r"));
        sendMessage(message.toString());

        for (FPlayer player : getPlayers().values()) {
            player.toBukkit().playSound(player.toBukkit().getLocation(), Sound.ENDERDRAGON_GROWL, 1f, 1f);
            setSpectator(player);
            if (player.getArenaTeamType().equals(teamWin)) {
                player.toBukkit().sendTitle("§6§lVictoire", "");
            } else {
                player.toBukkit().sendTitle("§cDéfaite", "");
            }
        }

        getFlags().values().forEach(flag -> {
            if(flag != null && flag.getMaster() != null) {
                Color color = TeamsHandler.getTeam(flag.getTeamType()).getColor();
                Utils.launchFirework(flag.getMaster().getLocation(), color, color, FireworkEffect.Type.BALL_LARGE, 0);
                flag.delete();
            }
        });

        Bukkit.getScheduler().runTaskLater(CaptureTheFlag.getInstance(), () -> {
            for (FPlayer fPlayer : getPlayers().values()) {
                final Player ps = fPlayer.toBukkit();
                ps.sendMessage("§aConnexion au lobby...");
                ps.playSound(ps.getLocation(), Sound.ENDERDRAGON_GROWL, 1f, 1f);
                TeamsHandler.addPlayerTeam(fPlayer, TeamType.DEFAULT);
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player != fPlayer.toBukkit() && !ArenaHandler.getInstance().hasPlayer(player.getUniqueId()) && !player.canSee(fPlayer.toBukkit())) {
                        fPlayer.toBukkit().showPlayer(player);
                        player.showPlayer(fPlayer.toBukkit());
                    }
                }
                fPlayer.reset();
                fPlayer.toBukkit().teleport(CaptureTheFlag.LOBBY_LOCATION);
            }
            reset();
            ArenaHandler.getInstance().removeArena(this);
        }, 20L * 10);
    }


    public void reset() {
        for (FPlayer fPlayer : players.values()) {
            if (fPlayer.getTask() != null) {
                fPlayer.getTask().cancel();
            }
        }
        points.clear();
        players.clear();

        for (Flag flag : flags.values()) {
            flag.delete();
        }
        flags.clear();
        final BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.cancelTask(tasks.get(3));
        scheduler.cancelTask(tasks.get(2));
        scheduler.cancelTask(tasks.get(1));
        if (!getMap().getGap().isEmpty()) {
            scheduler.cancelTask(tasks.get(5));
        }
        getMap().unloadWorld();
    }

    public boolean checkPlayers() {
        return getPlayers().values().stream().filter(player -> !player.getTeamType().equals(TeamType.SPECTATOR)).count() <=
                (long) getMap().getTeamSize() * getMap().getFlags().size();
    }

    public FPlayer getPlayer(UUID uuid) {
        for (FPlayer fPlayer : getPlayers().values()) {
            if (!fPlayer.toBukkit().getPlayer().getUniqueId().toString().equals(uuid.toString())) continue;

            return fPlayer;
        }

        return null;
    }

    public Set<FPlayer> getPlayersInLife() {
        return getPlayers().values().stream().filter(player -> !player.getTeamType().equals(TeamType.SPECTATOR))
                .collect(Collectors.toSet());
    }

    public void setSpectator(FPlayer player) {
        TeamsHandler.addPlayerTeam(player, TeamType.SPECTATOR);

        player.toBukkit().setGameMode(GameMode.ADVENTURE);

        for (FPlayer target : getPlayers().values()) {
            if (target.equals(player)) continue;

            if (target.getTeamType().equals(TeamType.SPECTATOR)) {
                player.toBukkit().showPlayer(target.toBukkit());
                continue;
            }

            target.hidePlayer(player.toBukkit());
        }

        final Player p = player.toBukkit();
        p.setAllowFlight(true);
        p.setFlying(true);
        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 1, true), true);
        p.getInventory().setHelmet(null);
        p.getInventory().setChestplate(null);
        p.getInventory().setLeggings(null);
        p.getInventory().setBoots(null);
        p.getInventory().clear();
        p.getInventory().setItem(8, new ItemBuilder(Material.BED).name("§cQuitter").build());
        p.setFoodLevel(20);
        p.setHealth(20.0);
        p.setLevel(0);
        p.setExp(0F);
    }

    public void sendMessage(String message) {
        getPlayers().values().forEach(uhcPlayer -> uhcPlayer.toBukkit().sendMessage(message));
    }
}
