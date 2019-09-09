package pl.extollite.AutoShutdown;

import cn.nukkit.utils.TextFormat;

import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class WarnTask extends TimerTask {
    protected final AutoShutdown plugin;
    protected long seconds;

    public WarnTask(AutoShutdown plugin, long seconds) {
        this.plugin = plugin;
        this.seconds = seconds;
    }

    public void run() {
        plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
            if (TimeUnit.SECONDS.toMinutes(seconds) > 0L) {
                if (TimeUnit.SECONDS.toMinutes(seconds) == 1L) {
                    if (seconds - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(seconds)) == 0L)
                        plugin.getServer().broadcastMessage(TextFormat.RED + plugin.shutdownMessage + " in 1 minute...");
                    else {
                        plugin.getServer().broadcastMessage(TextFormat.RED + plugin.shutdownMessage + " in 1 minute"+
                                Long.valueOf(seconds - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(seconds))) +" seconds...");
                    }

                } else if (seconds - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(seconds)) == 0L) {
                    plugin.getServer().broadcastMessage(TextFormat.RED + plugin.shutdownMessage + " in "+
                            Long.valueOf(TimeUnit.SECONDS.toMinutes(seconds)) +" minutes...");
                } else {
                    plugin.getServer().broadcastMessage(TextFormat.RED + plugin.shutdownMessage + " in "+
                            Long.valueOf(TimeUnit.SECONDS.toMinutes(seconds)) +" minutes "+
                            Long.valueOf(seconds - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(seconds))) +" seconds...");
                }

            } else if (TimeUnit.SECONDS.toSeconds(seconds) == 1L)
                plugin.getServer().broadcastMessage(TextFormat.RED + plugin.shutdownMessage + " NOW!");
            else
                plugin.getServer().broadcastMessage(TextFormat.RED + plugin.shutdownMessage + " in "+ Long.valueOf(seconds) +" seconds...");
        }, false);
    }
}
