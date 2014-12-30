package de.sabbertran.bungeemonitoringboard.commands;

import de.sabbertran.bungeemonitoringboard.BungeeMonitoringBoard;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BungeeMonitoringBoardCommand implements CommandExecutor
{

    private BungeeMonitoringBoard main;

    public BungeeMonitoringBoardCommand(BungeeMonitoringBoard main)
    {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
    {
        sender.sendMessage("No features implemented yet. Just reserved this spot.");
        return true;
    }
}
