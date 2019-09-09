package pl.extollite.AutoShutdown;

import cn.nukkit.Server;
import cn.nukkit.utils.TextFormat;

import java.util.TimerTask;

public class ShutdownTask extends TimerTask {
    protected AutoShutdown plugin;

    ShutdownTask(AutoShutdown instance) {
        plugin = instance;
    }

    public void run() {
        plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
            plugin.getLogger().info(TextFormat.GREEN + "Shutdown in progress!");

            plugin.kickAll();

            Server server = plugin.getServer();

            server.doAutoSave();

            server.shutdown();
        }, false);
    }
}