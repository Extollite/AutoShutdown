package pl.extollite.AutoShutdown;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.Listener;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import cn.nukkit.scheduler.ServerScheduler;

import java.util.*;

public class AutoShutdown extends PluginBase implements Listener {

    public Config pluginConfig;

    protected Timer backgroundTimer = null;
    protected Timer shutdownTimer = null;
    protected ServerScheduler scheduler = null;
    protected boolean shutdownImminent = false;
    protected TreeSet<Calendar> shutdownTimes = new TreeSet();
    protected ArrayList<Integer> warnTimes = new ArrayList();
    protected String shutdownMessage;

    @Override
    public void onDisable() {
        shutdownImminent = false;

        if (backgroundTimer != null) {
            backgroundTimer.cancel();
            backgroundTimer.purge();
            backgroundTimer = null;
        }

        if (shutdownTimer != null) {
            shutdownTimer.cancel();
            shutdownTimer.purge();
            shutdownTimer = null;
        }

        this.getLogger().info(TextFormat.DARK_RED + "I've been disabled!");
    }

    @Override
    public void onEnable() {
        this.getLogger().info(TextFormat.DARK_GREEN + "I've been enabled!");
        this.getLogger().info(TextFormat.DARK_GREEN + "Original plugin page: https://dev.bukkit.org/projects/auto-shutdown/");
        this.saveDefaultConfig();

        scheduler = getServer().getScheduler();
        shutdownImminent = false;
        shutdownTimes.clear();
        pluginConfig = getConfig();

        this.shutdownMessage = pluginConfig.getString("shutdownmessage");

        scheduleAll();

        if (backgroundTimer != null) {
            backgroundTimer.cancel();
            backgroundTimer.purge();
            backgroundTimer = null;
        }

        backgroundTimer = new Timer();

        if (shutdownTimer != null) {
            shutdownTimer.cancel();
            shutdownTimer.purge();
            shutdownTimer = null;
        }

        Calendar now = Calendar.getInstance();
        now.set(Calendar.SECOND, 0);
        now.add(Calendar.MINUTE, 1);

        now.add(Calendar.MILLISECOND, 50);
        try {
            backgroundTimer.scheduleAtFixedRate(new ShutdownScheduleTask(this), now.getTime(), 60000L);
        } catch (Exception e) {
            this.getLogger().info(TextFormat.RED + "Failed to schedule AutoShutdownTask: " + e.getMessage() );
        }

        saveConfig();

        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().toLowerCase().equals("as")) return true;

        if (args.length == 0) {
            sender.sendMessage(TextFormat.GREEN + "-- AutoShutdown " + this.getDescription().getVersion() + " --");
            sender.sendMessage(TextFormat.GREEN + "/as cancel - Cancels the currently executing shutdown");
            sender.sendMessage(TextFormat.GREEN + "/as set HH:MM - Sets a new scheduled shutdown time");
            sender.sendMessage(TextFormat.GREEN + "/as set now - Orders the server to shutdown immediately");
            sender.sendMessage(TextFormat.GREEN + "/as list - Lists the currently scheduled shutdowns");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "cancel":
                if (shutdownTimer != null) {
                    shutdownTimer.cancel();
                    shutdownTimer.purge();
                    shutdownTimer = null;
                    shutdownImminent = false;

                    sender.sendMessage(TextFormat.GREEN +"Shutdown was aborted.");
                } else {
                    sender.sendMessage(TextFormat.RED +"There is no impending shutdown. If you wish to remove");
                    sender.sendMessage(TextFormat.RED +"a scheduled shutdown, remove it from the configuration");
                    sender.sendMessage(TextFormat.RED +"and reload.");
                }
                break;
            case "set":
                if (args.length < 2) {
                    sender.sendMessage(TextFormat.RED +"Usage:");
                    sender.sendMessage(TextFormat.RED +"   /as set <time>");
                    sender.sendMessage(TextFormat.RED +"<time> can be either 'now' or a 24h time in HH:MM format.");
                    break;
                }

                Calendar stopTime = null;
                try {
                    stopTime = scheduleShutdownTime(args[1]);
                } catch (Exception e) {
                    sender.sendMessage(TextFormat.RED +"Usage:");
                    sender.sendMessage(TextFormat.RED +"   /as set <time>");
                    sender.sendMessage(TextFormat.RED +"<time> can be either 'now' or a 24h time in HH:MM format.");
                    break;
                }
                if (stopTime != null) {
                    sender.sendMessage(TextFormat.GREEN +"Shutdown scheduled for "+stopTime.getTime().toString() );
                }
                String timeString = "";

                for (Calendar shutdownTime : shutdownTimes) {
                    if ((shutdownTimes.first()).equals(shutdownTime))
                        timeString = timeString
                                .concat(String.format("%d:%02d", new Object[] { Integer.valueOf(shutdownTime.get(11)),
                                        Integer.valueOf(shutdownTime.get(12)) }));
                    else {
                        timeString = timeString
                                .concat(String.format(",%d:%02d", new Object[] { Integer.valueOf(shutdownTime.get(11)),
                                        Integer.valueOf(shutdownTime.get(12)) }));
                    }
                }

                pluginConfig.set("shutdowntimes", timeString);
                try {
                    this.saveConfig();
                } catch (Exception e) {
                    sender.sendMessage(TextFormat.RED +"Unable to save configuration: " + e.getMessage() );
                }
                break;
            case "list":
                if (shutdownTimes.size() != 0) {
                    sender.sendMessage(TextFormat.GREEN +"Shutdowns scheduled at");
                    for (Calendar shutdownTime : shutdownTimes)
                        sender.sendMessage( TextFormat.GREEN +shutdownTime.getTime().toString()  );
                } else {
                    sender.sendMessage(TextFormat.RED +"No shutdowns scheduled.");
                }
                break;
                default:
                    sender.sendMessage(TextFormat.RED +"Unknown command, use /as to list available commands");
        }
        return true;
    }

    protected void scheduleAll() {
        shutdownTimes.clear();
        warnTimes.clear();

        String[] shutdownTimeStrings;
        try {
            shutdownTimeStrings = getConfig().getString("shutdowntimes").split(",");
        } catch (Exception e) {
            shutdownTimeStrings = new String[1];
            shutdownTimeStrings[0] = getConfig().getString("shutdowntimes");
        }
        try {
            if(!shutdownTimeStrings[0].equals("null")){
                for (String timeString : shutdownTimeStrings) {
                    Calendar cal = scheduleShutdownTime(timeString);
                    this.getLogger().info(TextFormat.GREEN + "Shutdown scheduled for "+cal.getTime().toString() );
                }
            }
            String[] strings = getConfig().getString("warntimes").split(",");
            for (String warnTime : strings)
                warnTimes.add(Integer.decode(warnTime));
        } catch (Exception e) {
            this.getLogger().info(TextFormat.RED + "Unable to configure Auto Shutdown using the configuration file.");
            this.getLogger().info(TextFormat.RED + "Is the format of shutdowntimes correct? It should be only HH:MM.");
            this.getLogger().info(TextFormat.RED + "Error: "+ e.getMessage() );
        }
    }

    protected Calendar scheduleShutdownTime(String timeSpec) throws Exception {
        if (timeSpec == null) {
            return null;
        }
        if (timeSpec.matches("^now$")) {
            Calendar now = Calendar.getInstance();
            int secondsToWait = getConfig().getInt("gracetime", 30);
            now.add(Calendar.SECOND, secondsToWait);

            shutdownImminent = true;
            shutdownTimer = new Timer();

            for (Integer warnTime : warnTimes) {
                long longWarnTime = warnTime.longValue() * 1000L;

                if (longWarnTime <= secondsToWait * 1000) {
                    shutdownTimer.schedule(new WarnTask(this, warnTime.longValue()), secondsToWait * 1000
                            - longWarnTime);
                }

            }

            shutdownTimer.schedule(new ShutdownTask(this), now.getTime());
            this.getLogger().info(TextFormat.GREEN + "The server has been scheduled for immediate shutdown.");

            return now;
        }

        if (!timeSpec.matches("^[0-9]{1,2}:[0-9]{2}$")) {
            throw new Exception(TextFormat.RED +"Incorrect time specification. The format is HH:MM in 24h time.");
        }

        Calendar now = Calendar.getInstance();
        Calendar shutdownTime = Calendar.getInstance();

        String[] timecomponent = timeSpec.split(":");
        shutdownTime.set(Calendar.HOUR_OF_DAY, Integer.valueOf(timecomponent[0]).intValue());
        shutdownTime.set(Calendar.MINUTE, Integer.valueOf(timecomponent[1]).intValue());
        shutdownTime.set(Calendar.SECOND, 0);
        shutdownTime.set(Calendar.MILLISECOND, 0);

        if (now.compareTo(shutdownTime) >= 0) {
            shutdownTime.add(Calendar.DATE, 1);
        }

        shutdownTimes.add(shutdownTime);

        return shutdownTime;
    }

    protected void kickAll() {
        if (!(getConfig().getBoolean("kickonshutdown", true))) {
            return;
        }

        this.getLogger().info(TextFormat.GREEN + "Kicking all players ...");

        String kickReason = pluginConfig.getString("kickreason");
        for (Player p : getServer().getOnlinePlayers().values()) {
            this.getLogger().info(TextFormat.GREEN + "Kicking player " + p.getName()+".");
            p.kick(kickReason);
        }
    }

}
