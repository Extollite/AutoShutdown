package pl.extollite.AutoShutdown;

import cn.nukkit.utils.TextFormat;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class ShutdownScheduleTask extends TimerTask {
    protected AutoShutdown plugin;

    ShutdownScheduleTask(AutoShutdown instance) {
        plugin = instance;
    }

    public void run() {
        plugin.getServer().getScheduler().scheduleTask(plugin, ShutdownScheduleTask.this::runTask, false);
    }

    private void runTask() {
        if (plugin.shutdownImminent) {
            return;
        }
        Calendar now = Calendar.getInstance();

        if(plugin.warnTimes.isEmpty()){
            return;
        }
        long firstWarning = ( plugin.warnTimes.get(0) ).intValue() * 1000;

        for (Calendar cal : plugin.shutdownTimes)
            if (cal.getTimeInMillis() - now.getTimeInMillis() <= firstWarning) {
                plugin.shutdownImminent = true;
                plugin.shutdownTimer = new Timer();

                for (Integer warnTime : plugin.warnTimes) {
                    long longWarnTime = warnTime.longValue() * 1000L;

                    if (longWarnTime <= cal.getTimeInMillis() - now.getTimeInMillis()) {
                        plugin.shutdownTimer.schedule(new WarnTask(plugin, warnTime.longValue()), cal.getTimeInMillis()
                                - now.getTimeInMillis() - longWarnTime);
                    }

                }

                plugin.shutdownTimer.schedule(new ShutdownTask(plugin), cal.getTime());

                plugin.getServer().broadcastMessage(TextFormat.RED + plugin.shutdownMessage + " at " + cal.getTime().toString() );

                break;
            }
    }
}
