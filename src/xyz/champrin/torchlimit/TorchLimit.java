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
        long limitTime = 0;
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
        String Title = "??l??dTorchLimit??f>??r";
        if (args.length < 1) {
            sender.sendMessage(Title + "  ??c??????????????????");
            return false;
        }
        switch (args[0]) {
            case "set":
                if (args.length < 3) {
                    sender.sendMessage(Title + "  ??c??????????????????");
                    sender.sendMessage(Title + "/tlt set [day/hour/minute/second] [??????]   ??f?????????????????????");
                    break;
                } else if (new ArrayList<>(Arrays.asList("day", "hour", "minute", "second")).contains(args[1])) {
                    config.set(args[1], args[2]);
                    config.save();
                    sender.sendMessage(Title + "  ??a????????????");
                    this.getServer().reload();
                    break;
                } else {
                    sender.sendMessage(Title + "  ??c??????????????????");
                    sender.sendMessage(Title + "/tlt set [day/hour/minute/second] [??????]   ??f?????????????????????");
                    break;
                }
            case "addworld":
                if (args.length >= 2) {
                    String level = args[1];
                    if (OpenWorlds.contains(level)) {
                        sender.sendMessage(Title + "  ??a????????6" + level + "??a?????????????????????");
                        break;
                    }
                    this.OpenWorlds.add(level);
                    config.set("open-worlds", OpenWorlds);
                    config.save();
                    sender.sendMessage(Title + "  ??6?????????????????????????????a" + level);
                } else {
                    sender.sendMessage(Title + "  ??c??????????????????????????????");
                    sender.sendMessage(Title + "  ??a??????: /tlt addworld [?????????]");
                }
                break;
            case "delworld":
                if (args.length >= 2) {
                    String level = args[1];
                    if (!OpenWorlds.contains(level)) {
                        sender.sendMessage(Title + "  ??a????????6" + level + "??a??????????????????");
                        break;
                    }
                    this.OpenWorlds.remove(level);
                    config.set("open-worlds", OpenWorlds);
                    config.save();
                    sender.sendMessage(Title + "  ??6?????????????????????????????a" + level);
                } else {
                    sender.sendMessage(Title + "  ??c??????????????????????????????");
                    sender.sendMessage(Title + "  ??a??????: /tlt delworld [?????????]");
                }
                break;
            default:
                sender.sendMessage("============== -=??l??dTorchLimit??r=- ================");
                sender.sendMessage("/tlt help                             ??f????????????");
                sender.sendMessage("/tlt set [day/hour/minute/second] [??????]   ??f?????????????????????");
                sender.sendMessage("/tlt addworld [????????????]                ??f????????8???????????????????????????");
                sender.sendMessage("/tlt delworld [????????????]                ??f????????8???????????????????????????");
                break;
        }
        return true;
    }

}

class Task extends cn.nukkit.scheduler.Task {

    private long limitTime;
    private TorchLimit plugin;

    public Task(long limitTime, TorchLimit plugin) {
        this.limitTime = limitTime;
        this.plugin = plugin;
    }

    private ArrayList<String> cleanTorch = new ArrayList<>();

    @Override
    public void onRun(int tick) {
        for (String blocks : plugin.blocks) {
            String[] pos = blocks.split("\\+");
            long time = Long.parseLong(pos[4].trim());
            if (new Date().getTime() - time > limitTime) {
                double x = Double.parseDouble(pos[0]);
                double y = Double.parseDouble(pos[1]);
                double z = Double.parseDouble(pos[2]);
                String levelName = pos[3];
                plugin.getServer().getLevelByName(levelName).setBlock(new Vector3(x, y, z), Block.get(Block.AIR));
                cleanTorch.add(blocks);
            }
        }
        plugin.blocks.removeAll(cleanTorch);
    }
}



