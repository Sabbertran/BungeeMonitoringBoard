package de.sabbertran.bungeemonitoringboard;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class Events implements Listener
{

    private BungeeMonitoringBoard main;

    public Events(BungeeMonitoringBoard main)
    {
        this.main = main;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent ev)
    {
        Player p = ev.getPlayer();
        if (p.hasPermission("bungeemonitoringboard.show") && main.getCurrentBoard() != null)
        {
            p.setScoreboard(main.getCurrentBoard());
        }
    }
}
