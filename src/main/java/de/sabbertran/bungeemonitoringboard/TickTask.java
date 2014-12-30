package de.sabbertran.bungeemonitoringboard;

import org.bukkit.scheduler.BukkitRunnable;

public class TickTask extends BukkitRunnable
{

    private BungeeMonitoringBoard main;
    
    private long start;

    public TickTask(BungeeMonitoringBoard main)
    {
        start = System.currentTimeMillis();
        this.main = main;
    }

    @Override
    public void run()
    {
        main.setStartTime(start);
        main.setEndTime(System.currentTimeMillis());
        start = System.currentTimeMillis();
    }
}
