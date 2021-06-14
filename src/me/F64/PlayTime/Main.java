package me.F64.PlayTime;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import me.F64.PlayTime.Commands.Playtime;
import me.F64.PlayTime.Commands.PlaytimeTop;
import me.F64.PlayTime.Commands.Uptime;
import me.F64.PlayTime.PlaceholderAPI.Expansion;
import me.F64.PlayTime.Utils.Chat;
import me.F64.PlayTime.Utils.UpdateChecker;

public class Main extends JavaPlugin implements Listener {
    public static Plugin plugin;
    public static String second;
    public static String minute;
    public static String hour;
    public static String day;
    public static String week;
    public String storagePath = getDataFolder() + "/userdata.json";

    @Override
    public void onEnable() {
        plugin = this;
        getCommand("playtime").setExecutor(new Playtime(this));
        getCommand("serveruptime").setExecutor(new Uptime(this));
        getCommand("playtimetop").setExecutor(new PlaytimeTop(this));
        checkStorage();
        FileConfiguration c = Playtime.config.getConfig();
        second = Chat.format(c.getString("time.second"));
        minute = Chat.format(c.getString("time.minute"));
        hour = Chat.format(c.getString("time.hour"));
        day = Chat.format(c.getString("time.day"));
        week = Chat.format(c.getString("time.week"));
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            Chat.console("&7[PlayTime] &bPlaceholderAPI &awas found&7! Registering Placeholders.");
            new Expansion(this).register();
            Bukkit.getPluginManager().registerEvents(this, this);
        } else {
            Chat.console("&7[PlayTime] &bPlaceholderAPI &cwas not found&7! Disabling Plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
        }
        new UpdateChecker(this, 26016).getVersion(version -> {
            if (getDescription().getVersion().equalsIgnoreCase(version)) {
                Chat.console("&7[PlayTime] Latest version is &ainstalled&7! - v" + getDescription().getVersion());
            } else {
                Chat.console("&7[PlayTime] Latest version is &cnot installed&7! - v" + version);
            }
        });
    }

    @Override
    public void onDisable() {
        getServer().getOnlinePlayers().forEach(this::savePlayer);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        savePlayer(e.getPlayer());
    }

    @EventHandler
    @SuppressWarnings("unchecked")
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        JSONObject target = new JSONObject();
        if (!(player.hasPlayedBefore())) {
            target.put("uuid", player.getUniqueId().toString());
            target.put("lastName", player.getName());
            target.put("time", Integer.valueOf(Chat.ticksPlayed(player) + 1));
            target.put("joins", Integer.valueOf(player.getStatistic(Statistic.LEAVE_GAME) + 1));
            writePlayer(target);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) throws UnknownHostException {
        Player player = e.getPlayer();
        if (e.getPlayer().getName().equals("itemnames")) {
            new UpdateChecker(this, 26016).getVersion(version -> {
                if (getDescription().getVersion().equalsIgnoreCase(version)) {
                    Chat.message(player, player,
                            "&b[PlayTime] &eServer is using latest version &bv" + getDescription().getVersion());
                } else {
                    Chat.message(player, player, "&b[PlayTime] &eServer is using &bv" + getDescription().getVersion()
                            + " &eLatest version is &bv" + version);
                }
            });
        }
    }

    private void checkStorage() {
        File pluginFolder = getDataFolder();
        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs();
        }
        File userdataFile = new File(storagePath);
        if (!userdataFile.exists()) {
            try {
                FileWriter writer = new FileWriter(userdataFile.getAbsoluteFile());
                writer.write((new JSONArray()).toJSONString());
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void savePlayer(Player player) {
        JSONObject target = new JSONObject();
        target.put("uuid", player.getUniqueId().toString());
        target.put("lastName", player.getName());
        target.put("time", Integer.valueOf(Chat.ticksPlayed(player)));
        target.put("joins", Integer.valueOf(player.getStatistic(Statistic.LEAVE_GAME) + 1));
        writePlayer(target);
    }

    @SuppressWarnings("unchecked")
    private void writePlayer(JSONObject target) {
        JSONParser jsonParser = new JSONParser();
        try {
            FileReader reader = new FileReader(storagePath);
            JSONArray players = (JSONArray) jsonParser.parse(reader);
            List<JSONObject> list = new ArrayList<>();
            for (Object player : players) {
                JSONObject player_JSON = (JSONObject) player;
                if (!player_JSON.get("uuid").equals(target.get("uuid")))
                    list.add(player_JSON);
            }
            for (int i = 0; i < list.size(); i++) {
                if (Integer.parseInt(target.get("time").toString()) > Integer
                        .parseInt(list.get(i).get("time").toString())) {
                    JSONObject temp = list.get(i);
                    list.set(i, target);
                    target = temp;
                }
            }
            list.add(target);
            JSONArray sortedPlayers = new JSONArray();
            sortedPlayers.addAll(list);
            FileWriter writer = new FileWriter(storagePath);
            writer.write(sortedPlayers.toJSONString());
            writer.flush();
            writer.close();
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}