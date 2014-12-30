package de.sabbertran.bungeemonitoringboard;

import de.sabbertran.bungeemonitoringboard.commands.BungeeMonitoringBoardCommand;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

public class BungeeMonitoringBoard extends JavaPlugin
{

    private char[] colorcodes = new char[]
    {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'l', 'n', 'r'
    };

    private Logger log = Bukkit.getLogger();

    private File messagesFile;
    private HashMap<String, String> messages;

    private String servername;

    private ArrayList<String> sql;
    private Connection sql_connection;
    private int currentServerInfo;
    private long starttime = 0;
    private long endTime = 0;

    private int updateTime;

    private Scoreboard currentBoard = null;

    @Override
    public void onEnable()
    {
        getConfig().addDefault("BungeeMonitoringBoard.ServerName", "Name");
        getConfig().addDefault("BungeeMonitoringBoard.UpdateTime", 30);
        getConfig().addDefault("BungeeMonitoringBoard.SQL", new String[]
        {
            "Adress", "Port", "Database", "User", "Password"
        });
        getConfig().options().copyDefaults(true);
        saveConfig();

        servername = getConfig().getString("BungeeMonitoringBoard.ServerName");
        updateTime = getConfig().getInt("BungeeMonitoringBoard.UpdateTime");
        sql = (ArrayList<String>) getConfig().getStringList("BungeeMonitoringBoard.SQL");

        if (!sql.get(0).equalsIgnoreCase("Adress") && !sql.get(1).equalsIgnoreCase("Port") && !sql.get(2).equalsIgnoreCase("Database") && !sql.get(3).equalsIgnoreCase("User") && !sql.get(4).equalsIgnoreCase("Password"))
        {
            try
            {
                getSqlConnection().createStatement().execute("CREATE TABLE IF NOT EXISTS bungeemonitoringboard (id INT AUTO_INCREMENT, name text NOT NULL, tps text NOT NULL, players text NOT NULL, lastupdate text NOT NULL, PRIMARY KEY (id))");
            } catch (SQLException ex)
            {
                Logger.getLogger(BungeeMonitoringBoard.class.getName()).log(Level.SEVERE, null, ex);
            }

            getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
            {
                @Override
                public void run()
                {
                    updateServerInfo();
                }
            }, 0L, (long) updateTime * 20);

            getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
            {
                @Override
                public void run()
                {
                    updateScoreboards();
                }
            }, 100L, (long) updateTime * 20);
        } else
        {
            log.info("Please set your database details in the config");
        }

        //messages
        try
        {
            messages = new HashMap<String, String>();

            messagesFile = new File("plugins/BungeeMonitoringBoard/messages.yml");
            if (!messagesFile.exists())
            {
                messagesFile.getParentFile().mkdirs();
                copy(getResource("messages.yml"), messagesFile);
            }
            BufferedReader read_messages = new BufferedReader(new FileReader(messagesFile));
            String line_messages;
            while ((line_messages = read_messages.readLine()) != null)
            {
                String[] split = line_messages.split(": ");
                if (split.length == 2)
                {
                    messages.put(split[0], split[1]);
                } else if (split.length > 2)
                {
                    String message = "";
                    for (int i = 1; i < split.length; i++)
                    {
                        message = message + split[i] + ": ";
                    }
                    message = message.substring(0, message.length() - 2);
                    messages.put(split[0], message);
                }
            }
            read_messages.close();
        } catch (FileNotFoundException ex)
        {
            Logger.getLogger(BungeeMonitoringBoard.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex)
        {
            Logger.getLogger(BungeeMonitoringBoard.class.getName()).log(Level.SEVERE, null, ex);
        }

        currentServerInfo = 0;

        getServer().getPluginManager().registerEvents(new Events(this), this);;

        getCommand("bungeemonitoringboard").setExecutor(new BungeeMonitoringBoardCommand(this));

        log.info("BungeeMonitoringBoard enabled");
    }

    @Override
    public void onDisable()
    {
        log.info("BungeeMonitoringBoard disabled");
    }

    public void updateServerInfo()
    {
        double endstart = endTime - starttime;
        double dtps = 20000 / endstart;
        if (dtps > 20)
        {
            dtps = 20;
        }
        DecimalFormat df = new DecimalFormat("00.00");
        String tps = df.format(dtps);
        String players = getServer().getOnlinePlayers().length + "/" + getServer().getMaxPlayers();
        try
        {
            ResultSet rs = getSqlConnection().createStatement().executeQuery("SELECT * FROM bungeemonitoringboard WHERE name = '" + servername + "'");
            if (!rs.next())
            {
                getSqlConnection().createStatement().execute("INSERT INTO bungeemonitoringboard (name, tps, players, lastupdate) VALUES ('" + servername + "', '" + tps + "', '" + players + "', '" + new Date().getTime() + "')");
            } else
            {
                getSqlConnection().createStatement().execute("UPDATE bungeemonitoringboard SET tps = '" + tps + "' WHERE name = '" + servername + "'");
                getSqlConnection().createStatement().execute("UPDATE bungeemonitoringboard SET players = '" + players + "' WHERE name = '" + servername + "'");
                getSqlConnection().createStatement().execute("UPDATE bungeemonitoringboard SET lastupdate = '" + new Date().getTime() + "' WHERE name = '" + servername + "'");
            }
            rs.close();
        } catch (SQLException ex)
        {
            Logger.getLogger(BungeeMonitoringBoard.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void updateScoreboards()
    {
        if (getServer().getOnlinePlayers().length > 0)
        {
            int currentChar = 0;
            Scoreboard board = getServer().getScoreboardManager().getNewScoreboard();
            Objective serverInfo = board.registerNewObjective("Server Info", "dummy");
            serverInfo.setDisplaySlot(DisplaySlot.SIDEBAR);
            serverInfo.setDisplayName(getMessage("scoreboardname"));
            int allPlayers = 0;

            try
            {
                ResultSet count = getSqlConnection().createStatement().executeQuery("SELECT * FROM bungeemonitoringboard");
                while (count.next())
                {
                    if (new Date().getTime() - count.getLong("lastupdate") < 60000)
                    {
                        allPlayers = allPlayers + Integer.parseInt(count.getString("players").split("/")[0]);
                    }
                }
                count.last();
                int amount = count.getRow();
                ResultSet rs;
                if (amount <= 7)
                {
                    rs = getSqlConnection().createStatement().executeQuery("SELECT * FROM bungeemonitoringboard ORDER BY id");
                } else
                {
                    rs = getSqlConnection().createStatement().executeQuery("SELECT * FROM bungeemonitoringboard ORDER BY id LIMIT " + currentServerInfo + ",7");
                    if (currentServerInfo + 7 == amount)
                    {
                        currentServerInfo = 0;
                    } else
                    {
                        currentServerInfo++;
                    }
                }

                int order = 14;
                while (rs.next())
                {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    String players = rs.getString("players");
                    double tps = Double.parseDouble(rs.getString("tps"));
                    String tpsString = "";
                    if (players.length() <= 5)
                    {
                        tpsString = new DecimalFormat("00.00").format(tps);
                    } else if (players.length() == 6)
                    {
                        tpsString = new DecimalFormat("00.0").format(tps);
                    } else if (players.length() == 7 || players.length() == 8)
                    {
                        tpsString = new DecimalFormat("00").format(tps);
                    }
                    long lastupdate = rs.getLong("lastupdate");

                    if (order > 0)
                    {
                        if (servername.equals(name))
                        {
                            String text = getMessage("currentservername").replace("%server", name);
                            if (text.length() > 16)
                            {
                                text = text.substring(text.length() - 16, text.length());
                            }
                            Score s = serverInfo.getScore(text);
                            s.setScore(order);
                        } else
                        {
                            String text = getMessage("servername").replace("%server", name);
                            if (text.length() > 16)
                            {
                                text = text.substring(text.length() - 16, text.length());
                            }
                            Score s = serverInfo.getScore(text);
                            s.setScore(order);
                        }

                        order--;

                        if (new Date().getTime() - lastupdate < 60000)
                        {
                            if (tps >= 15)
                            {
                                Score s = serverInfo.getScore(" " + players + " " + ChatColor.GREEN + tpsString + "§" + colorcodes[currentChar]);
                                s.setScore(order);
                            } else if (tps >= 10)
                            {
                                Score s = serverInfo.getScore(" " + players + " " + ChatColor.YELLOW + tpsString + "§" + colorcodes[currentChar]);
                                s.setScore(order);
                            } else
                            {
                                Score s = serverInfo.getScore(" " + players + " " + ChatColor.RED + tpsString + "§" + colorcodes[currentChar]);
                                s.setScore(order);
                            }
                        } else
                        {
                            Score s = serverInfo.getScore(getMessage("offline") + "§" + colorcodes[currentChar]);
                            s.setScore(order);
                        }
                        currentChar++;
                        order--;
                    }
                }
            } catch (SQLException ex)
            {
                Logger.getLogger(BungeeMonitoringBoard.class.getName()).log(Level.SEVERE, null, ex);
            }

            Score pls = serverInfo.getScore(getMessage("onlineplayers").replace("%players", "" + allPlayers));
            pls.setScore(15);
            currentBoard = board;
            for (Player p : getServer().getOnlinePlayers())
            {
                if (p.hasPermission("bungeemonitoringboard.show"))
                {
                    p.setScoreboard(board);
                } else
                {
                    p.setScoreboard(getServer().getScoreboardManager().getNewScoreboard());
                }
            }
        }
    }

    public String getMessage(String key)
    {
        if (messages.containsKey(key))
        {
            return ChatColor.translateAlternateColorCodes('&', messages.get(key));
        } else
        {
            return "Error";
        }
    }

    public Connection getSqlConnection()
    {
        try
        {
            if (sql_connection == null || sql_connection.isClosed())
            {
                Class.forName("com.mysql.jdbc.Driver");
                String url = "jdbc:mysql://" + sql.get(0) + ":" + sql.get(1) + "/" + sql.get(2);
                sql_connection = DriverManager.getConnection(url, sql.get(3), sql.get(4));
            }
        } catch (SQLException | ClassNotFoundException ex)
        {
            Logger.getLogger(BungeeMonitoringBoard.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sql_connection;
    }

    private void copy(InputStream in, File file)
    {
        try
        {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0)
            {
                out.write(buf, 0, len);
            }
            out.close();
            in.close();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void setStartTime(long starttime)
    {
        this.starttime = starttime;
    }

    public void setEndTime(long endTime)
    {
        this.endTime = endTime;
    }

    public Scoreboard getCurrentBoard()
    {
        return currentBoard;
    }
}
