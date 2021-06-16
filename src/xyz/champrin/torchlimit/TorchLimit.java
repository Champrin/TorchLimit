package xyz.champrin.torchlimit;

import cn.nukkit.block.Block;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.math.Vector3;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class TorchLimit extends PluginBase implements Listener {

    public Config config, blocksConfig;
    private ArrayList<String> OpenWorlds = new ArrayList<>();
    public ArrayList<String> blocks = new ArrayList<>();

    @Override
    public void onEnable() {
        LoadConfig();
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    public void LoadConfig() {
        if (!new File(this.getDataFolder() + "/config.yml").exists()) {
            this.saveResource("config.yml", false);
        }
        this.config = new Config(this.getDataFolder() + "/config.yml", Config.YAML);
        int limitTime = 0;
        if (Integer.parseInt(config.getString("day")) != 0) {
            limitTime = limitTime + Integer.parseInt(config.getString("day")) * 86400000;
        }
        if (Integer.parseInt(config.getString("hour")) != 0) {
            limitTime = limitTime + Integer.parseInt(config.getString("hour")) * 3600000;
        }
        if (Integer.parseInt(config.getString("minute")) != 0) {
            limitTime = limitTime + Integer.parseInt(config.getString("minute")) * 60000;
        }
        if (Integer.parseInt(config.getString("second")) != 0) {
            limitTime = limitTime + Integer.parseInt(config.getString("second")) * 1000;
        }
        this.getServer().getScheduler().scheduleRepeatingTask(new Task(limitTime, this), 20);
        this.OpenWorlds.addAll(config.getStringList("open-worlds"));

        if (!new File(this.getDataFolder() + "/blocksConfig.yml").exists()) {
            this.saveResource("blocksConfig.yml", false);
        }
        this.blocksConfig = new Config(this.getDataFolder() + "/blocksConfig.yml", Config.YAML);
        this.blocks.addAll(blocksConfig.getStringList("blocks"));
    }

    @Override
    public void onDisable() {
        blocksConfig.set("blocks", this.blocks);
        blocksConfig.save();

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void OnBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (OpenWorlds.contains(block.getLevel().getFolderName())) {
            if (block.getId() == Block.TORCH) {
                blocks.add(block.x + "+" + block.y + "+" + block.z + "+" + block.level.getFolderName() + "+" + new Date().getTime());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void OnBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (OpenWorlds.contains(block.getLevel().getFolderName())) {
            if (block.getId() == Block.TORCH) {
                blocks.remove(block.x + "+" + block.y + "+" + block.z + "+" + block.level.getFolderName() + "+" + new Date().getTime());
            }
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String Title = "§l§dTorchLimit§f>§r";
        if (args.length < 1) {
            sender.sendMessage(Title + "  §c指令输入错误");
            return false;
        }
        switch (args[0]) {
            case "set":
                if (args.length < 3) {
                    sender.sendMessage(Title + "  §c指令输入错误");
                    sender.sendMessage(Title + "/tlt set [day/hour/minute/second] [数字]   §f设定限制的时间");
                    break;
                } else if (new ArrayList<>(Arrays.asList("day", "hour", "minute", "second")).contains(args[1])) {
                    config.set(args[1], args[2]);
                    config.save();
                    sender.sendMessage(Title + "  §a设置成功");
                    this.getServer().reload();
                    break;
                } else {
                    sender.sendMessage(Title + "  §c指令输入错误");
                    sender.sendMessage(Title + "/tlt set [day/hour/minute/second] [数字]   §f设定限制的时间");
                    break;
                }
            case "addworld":
                if (args.length >= 2) {
                    String level = args[1];
                    if (OpenWorlds.contains(level)) {
                        sender.sendMessage(Title + "  §a地图§6" + level + "§a已经开启此功能");
                        break;
                    }
                    this.OpenWorlds.add(level);
                    config.set("open-worlds", OpenWorlds);
                    config.save();
                    sender.sendMessage(Title + "  §6记步开启在世界§a" + level);
                } else {
                    sender.sendMessage(Title + "  §c未输入要添加的地图名");
                    sender.sendMessage(Title + "  §a用法: /tlt addworld [地图名]");
                }
                break;
            case "delworld":
                if (args.length >= 2) {
                    String level = args[1];
                    if (!OpenWorlds.contains(level)) {
                        sender.sendMessage(Title + "  §a地图§6" + level + "§a未开启此功能");
                        break;
                    }
                    this.OpenWorlds.remove(level);
                    config.set("open-worlds", OpenWorlds);
                    config.save();
                    sender.sendMessage(Title + "  §6记步关闭在世界§a" + level);
                } else {
                    sender.sendMessage(Title + "  §c未输入要删除的地图名");
                    sender.sendMessage(Title + "  §a用法: /tlt delworld [地图名]");
                }
                break;
            default:
                sender.sendMessage("============== -=§l§dTorchLimit§r=- ================");
                sender.sendMessage("/tlt help                             §f查看帮助");
                sender.sendMessage("/tlt set [day/hour/minute/second] [数字]   §f设定限制的时间");
                sender.sendMessage("/tlt addworld [世界名称]                §f添加§8开启火把寿命的世界");
                sender.sendMessage("/tlt delworld [世界名称]                §f移除§8开启火把寿命的世界");
                break;
        }
        return true;
    }

}

class Task extends cn.nukkit.scheduler.Task {

    private int limitTime;
    private TorchLimit plugin;

    public Task(int limitTime, TorchLimit plugin) {
        this.limitTime = limitTime;
        this.plugin = plugin;
    }

    @Override
    public void onRun(int tick) {
        for (String blocks : plugin.blocks) {
            String[] pos = blocks.split("\\+");
            int time = Integer.parseInt(pos[4]);
            if (new Date().getTime() - time > limitTime) {
                int x = Integer.parseInt(pos[0]);
                int y = Integer.parseInt(pos[1]);
                int z = Integer.parseInt(pos[2]);
                String levelName = pos[3];
                plugin.getServer().getLevelByName(levelName).setBlock(new Vector3(x, y, z), Block.get(Block.AIR));
                plugin.blocks.remove(blocks);
            }
        }
    }
}



