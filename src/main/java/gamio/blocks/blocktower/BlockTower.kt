package gamio.blocks.blocktower

import org.bukkit.*
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.Team
import org.bukkit.material.Wool
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.server.MapInitializeEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.map.*
import org.bukkit.scheduler.BukkitTask
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Score


class BlockTower : JavaPlugin(), Listener, CommandExecutor {

    private val TEAM_RED_NAME = "team_red"
    var teamRed: Team? = null
    private val TEAM_BLUE_NAME = "team_blue"
    var teamBlue: Team? = null

    private val OBJECTIVE_NAME = "count_donw"

    var redLoc: Location? = null
    var blueLoc: Location? = null
    var initLoc: Location? = null

    var world: World? = null

    var time: Score? = null

    var isStart: Boolean = false

    var counter: Int = 1800
    var task: BukkitTask? = null

    override fun onEnable() {
        getCommand("blocktower").executor = this

        world = Bukkit.getServer().getWorld("world")

        val manager = Bukkit.getScoreboardManager()
        val board = manager.mainScoreboard

        teamRed = board.getTeam(TEAM_RED_NAME)
        if (teamRed != null) {
            teamRed!!.unregister()
        }
        teamRed = board.registerNewTeam(TEAM_RED_NAME)
        teamRed!!.displayName = "Red Team"
        teamRed!!.setAllowFriendlyFire(false)

        teamBlue = board.getTeam(TEAM_BLUE_NAME)
        if (teamBlue != null) {
            teamBlue!!.unregister()
        }
        teamBlue = board.registerNewTeam(TEAM_BLUE_NAME)
        teamBlue!!.displayName = "Blue Team"
        teamBlue!!.setAllowFriendlyFire(false)

        teamRed!!.prefix = ChatColor.RED.toString()
        teamBlue!!.prefix = ChatColor.BLUE.toString()

        var objective: Objective? = board.getObjective(OBJECTIVE_NAME)
        if (objective != null) {
            objective!!.unregister()
        }

        objective = board.registerNewObjective(OBJECTIVE_NAME, "dummy")
        objective!!.setDisplayName("制限時間(秒)")
        objective!!.displaySlot = DisplaySlot.SIDEBAR
        time = objective!!.getScore("残り秒数:")
        time!!.score = counter
        Bukkit.getPluginManager().registerEvents(this,this);

    }

    override fun onDisable() {
        // Plugin shutdown logic

    }

    override fun onCommand(sender: CommandSender?, command: Command?, label: String?, args: Array<out String>?): Boolean {
        if (sender is Player){
            var p : Player = sender
            when(command?.name){
                "blocktower" -> {
                    if (args?.isEmpty()!!) {
                        p.sendMessage("Command not found!")
                        return true
                    }
                    when (args?.get(0)) {
                        "start" -> {
                            val stonePicx = ItemStack(Material.STONE_PICKAXE)
                            val itemData = stonePicx.itemMeta
                            itemData.displayName = "すっごーい石ピッケル"
                            itemData.isUnbreakable = true
                            stonePicx.itemMeta = itemData
                            for (p in Bukkit.getServer().onlinePlayers) {
                                p.inventory.clear()
                                p.inventory.addItem(stonePicx)
                                p.inventory.addItem(ItemStack(Material.WOOD, 10))
                                p.inventory.addItem(ItemStack(Material.COOKED_BEEF, 10))
                                p.inventory.addItem(ItemStack(Material.TORCH, 64))
                                p.inventory.addItem(ItemStack(Material.EMPTY_MAP))
                                p.gameMode = GameMode.SURVIVAL
                                p.sendTitle("試合開始", "制限時間は" + (counter / 60) + "分です")
                            }
                            task = Bukkit.getScheduler().runTaskTimer(this, CountDownTimer(), 0, 20)
                            isStart = true
                            return true
                        }
                        "init" -> {
                            initLoc = p.location
                            redLoc = Location(world, (initLoc!!.x - 4), (initLoc!!.y - 1), initLoc!!.z)
                            blueLoc = Location(world, (initLoc!!.x + 4), (initLoc!!.y - 1), initLoc!!.z)
                            InitWorld()
                            return true
                        }
                        "time" -> {
                            if (args?.lastIndex != 1) {
                                p.sendMessage("指定時間を入力してください。")
                                return true
                            }
                            counter = args?.get(1).toInt()
                            time!!.score = counter
                            return true
                        }
                        "result" -> {
                            val redResult = CountBlock(redLoc)
                            val blueResult = CountBlock(blueLoc)
                            var resultMessage: String? = null
                            if (redResult > blueResult) {
                                resultMessage = "優勝: Red Team"
                            } else if(redResult == blueResult) {
                                resultMessage = "引き分け"
                            } else {
                                resultMessage = "優勝: Blue Team"
                            }
                            for (p in Bukkit.getServer().onlinePlayers) {
                                p.sendTitle(resultMessage, "おめでとうございます！")
                            }
                            Bukkit.getServer().broadcastMessage("総ポイント数 Red:" + redResult + "    Blue:" + blueResult)
                            return true
                        }
                        "stop" -> {
                            isStart = false
                            task!!.cancel()
                            counter = 1800
                            for (p in Bukkit.getServer().onlinePlayers) {
                                p.teleport(initLoc)
                                p.gameMode = GameMode.SPECTATOR
                                p.sendTitle("試合中止", "お疲れさまでした。")
                            }
                            return true
                        }
                        else -> {
                            p.sendMessage("Command not found!")
                            return true
                        }
                    }

                }

                else -> {
                    p.sendMessage("Command not found!")
                    return true
                }
            }


        } else {
            sender?.sendMessage("Your not a player! ")
            return true
        }
    }

     fun InitWorld() {
         if (blueLoc == null || redLoc == null) return
         val WOOL_RED = Wool(DyeColor.RED)
         val WOOL_BLUE = Wool(DyeColor.BLUE)
         world!!.getBlockAt(redLoc!!.x.toInt() + 1, redLoc!!.y.toInt(), redLoc!!.z.toInt()).type = WOOL_RED.itemType
         world!!.getBlockAt(redLoc!!.x.toInt() + 1, redLoc!!.y.toInt(), redLoc!!.z.toInt()).data = WOOL_RED.data
         world!!.getBlockAt(blueLoc!!.x.toInt() - 1, blueLoc!!.y.toInt(), blueLoc!!.z.toInt()).type = WOOL_BLUE.itemType
         world!!.getBlockAt(blueLoc!!.x.toInt() - 1, blueLoc!!.y.toInt(), blueLoc!!.z.toInt()).data = WOOL_BLUE.data
         world!!.getBlockAt(redLoc!!.x.toInt() - 1, redLoc!!.y.toInt(), redLoc!!.z.toInt()).type = WOOL_RED.itemType
         world!!.getBlockAt(redLoc!!.x.toInt() - 1, redLoc!!.y.toInt(), redLoc!!.z.toInt()).data = WOOL_RED.data
         world!!.getBlockAt(blueLoc!!.x.toInt() + 1, blueLoc!!.y.toInt(), blueLoc!!.z.toInt()).type = WOOL_BLUE.itemType
         world!!.getBlockAt(blueLoc!!.x.toInt() + 1, blueLoc!!.y.toInt(), blueLoc!!.z.toInt()).data = WOOL_BLUE.data
         world!!.getBlockAt(redLoc!!.x.toInt(), redLoc!!.y.toInt(), redLoc!!.z.toInt() + 1).type = WOOL_RED.itemType
         world!!.getBlockAt(redLoc!!.x.toInt(), redLoc!!.y.toInt(), redLoc!!.z.toInt() + 1).data = WOOL_RED.data
         world!!.getBlockAt(blueLoc!!.x.toInt(), blueLoc!!.y.toInt(), blueLoc!!.z.toInt() + 1).type = WOOL_BLUE.itemType
         world!!.getBlockAt(blueLoc!!.x.toInt(), blueLoc!!.y.toInt(), blueLoc!!.z.toInt() + 1).data = WOOL_BLUE.data
         world!!.getBlockAt(redLoc!!.x.toInt(), redLoc!!.y.toInt(), redLoc!!.z.toInt() - 1).type = WOOL_RED.itemType
         world!!.getBlockAt(redLoc!!.x.toInt(), redLoc!!.y.toInt(), redLoc!!.z.toInt() - 1).data = WOOL_RED.data
         world!!.getBlockAt(blueLoc!!.x.toInt(), blueLoc!!.y.toInt(), blueLoc!!.z.toInt() - 1).type = WOOL_BLUE.itemType
         world!!.getBlockAt(blueLoc!!.x.toInt(), blueLoc!!.y.toInt(), blueLoc!!.z.toInt() - 1).data = WOOL_BLUE.data

         world!!.getBlockAt(redLoc!!.x.toInt(), redLoc!!.y.toInt(), redLoc!!.z.toInt()).type = Material.WOOL
         world!!.getBlockAt(blueLoc!!.x.toInt(), blueLoc!!.y.toInt(), blueLoc!!.z.toInt()).type = Material.WOOL

         val y = (redLoc!!.y.toInt() + 1)
         for (i in y..256) {
             world!!.getBlockAt(redLoc!!.x.toInt(), i, redLoc!!.z.toInt() + 2).type = Material.STONE
             world!!.getBlockAt(redLoc!!.x.toInt(), i, redLoc!!.z.toInt() + 1).type = Material.LADDER
         }

         for (i in y..256) {
             world!!.getBlockAt(blueLoc!!.x.toInt(), i, blueLoc!!.z.toInt() + 2).type = Material.STONE
             world!!.getBlockAt(blueLoc!!.x.toInt(), i, blueLoc!!.z.toInt() + 1).type = Material.LADDER
         }
     }

    fun CountBlock(loc: Location?) :Int {
        if (blueLoc == null || redLoc == null || loc == null) return 0
        var count: Int = 0

        val y = (loc!!.y.toInt() + 1)
        for (i in y..256) {
            var blockType: Material = world!!.getBlockAt(loc!!.x.toInt(), i, loc!!.z.toInt()).type
            when(blockType) {
                Material.COAL_BLOCK -> count += 1
                Material.IRON_BLOCK -> count += 5
                Material.LAPIS_BLOCK -> count += 3
                Material.DIAMOND_BLOCK -> count += 50
                Material.EMERALD_BLOCK -> count += 100
                Material.GOLD_BLOCK -> count += 10
                Material.REDSTONE_BLOCK -> count += 2
            }
        }
        return count

    }

    fun CountDownTimer()  : Runnable {
        var r: Runnable = Runnable {
            if (counter == 0){
                time!!.score = counter
                isStart = false
                task!!.cancel()
                counter = 1800
                for (p in Bukkit.getServer().onlinePlayers) {
                    p.teleport(initLoc)
                    p.gameMode = GameMode.SPECTATOR
                    p.sendTitle("試合終了", "お疲れさまでした！")
                }
            } else {
                time!!.score = counter
                counter--
            }
        }
        return r
    }

    @EventHandler
    public fun joinEvent(event: PlayerJoinEvent){
        val p = event.player
        if (isStart) {
            if (teamRed!!.hasEntry(p.name) || teamBlue!!.hasEntry(p.name)) { return }
            p.gameMode = GameMode.SPECTATOR
            p.sendTitle("試合中です", "観戦モードに切り替えました")
        } else {
            p.inventory.clear()
            if (initLoc != null){
                p.teleport(initLoc)
            }
            p.gameMode = GameMode.ADVENTURE
            if (teamRed!!.entries.size > teamBlue!!.entries.size){
                teamBlue?.addEntry(p.name)
            } else if (teamRed!!.entries.size == teamBlue!!.entries.size) {
                teamRed?.addEntry(p.name)
            } else {
                teamRed?.addEntry(p.name)
            }
        }

    }

    @EventHandler
    public fun initMap(event: MapInitializeEvent){
        if (initLoc == null){
            Bukkit.getServer().broadcastMessage("活動拠点をセットしてください。")
            return
        }
        val mapview = event.map
        mapview.isUnlimitedTracking = true
        mapview.centerX = initLoc!!.x.toInt()
        mapview.centerZ = initLoc!!.z.toInt()
        mapview.scale = MapView.Scale.CLOSE
        mapview.world = world
        mapview.addRenderer(PointRender())
    }

}

class PointRender: MapRenderer() {
    public override fun render(map: MapView?, canvas: MapCanvas?, player: Player?) {
        var s = "×"
        val width = MinecraftFont.Font.getWidth(s)
        val height = MinecraftFont.Font.height
        canvas!!.drawText(64 - (width / 2), 64 - (height / 2), MinecraftFont.Font, s)
    }
}

