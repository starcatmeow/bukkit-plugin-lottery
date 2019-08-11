package top.starcatmeow.lottery.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import top.starcatmeow.lottery.LotteryConfig
import top.starcatmeow.lottery.LotteryLogic

class Lottery(private val lotteryLogic: LotteryLogic, private val plugin: JavaPlugin) : CommandExecutor {
    init {
        plugin.server.scheduler.runTaskTimerAsynchronously(plugin, Runnable { lotteryLogic.showLotteryResult() }, 0L, (LotteryConfig.roundsecond + 23) * 20)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        //特判无参数
        if (args.isEmpty()) {
            showHelp(sender)
            return true
        }

        if (args[0].equals("buy", ignoreCase = true) && args.size >= 2 && args.size <= 3) {
            if (sender is Player) {
                val amount: Int = if (args.size == 3) {
                    try {
                        Integer.valueOf(args[2])
                    } catch (e: Exception) {
                        sender.sendMessage("§l§5彩票系统 §l§7> §4命令格式不正确！数量必须为正整数！")
                        return true
                    }
                } else
                    1
                if (amount <= 0) {
                    sender.sendMessage("§l§5彩票系统 §l§7> §4命令格式不正确！数量必须为正整数！")
                    return true
                }
                Thread { lotteryLogic.buyLottery(sender, args[1], amount) }.start()
            } else {
                sender.sendMessage("§l§5彩票系统 §l§7> §4该命令必须由玩家执行！")
            }
        } else if (args[0].equals("get", ignoreCase = true)) {
            Thread { lotteryLogic.sendBuyLotterytoCommandSender(sender) }.start()
        } else if (args[0].equals("next", ignoreCase = true)) {
            if (!sender.hasPermission("lottery.admin")) {
                sender.sendMessage("§l§5彩票系统 §l§7> §4你没有权限执行该命令！")
                return true
            }
            showResult()
        } else
            showHelp(sender)
        return true
    }

    fun showResult(){
        plugin.server.scheduler.cancelTasks(plugin)
        Thread { lotteryLogic.showLotteryResult() }.start()
        plugin.server.scheduler.runTaskTimerAsynchronously(plugin, Runnable { lotteryLogic.showLotteryResult() }, (LotteryConfig.roundsecond + 23) * 20, (LotteryConfig.roundsecond + 23) * 20)
    }

    private fun showHelp(sender: CommandSender) {
        sender.sendMessage("§l§5彩票系统 §l§7> 使用方式：")
        sender.sendMessage("                      买彩票：/lt buy [三位数] [数量]")
        sender.sendMessage("                      三位数可用random代替以随机选择；数量为1时可省略")
        sender.sendMessage("                      查看开奖时间：/lt get")
        if (sender.hasPermission("lottery.admin")) {
            sender.sendMessage("                      立即开奖：/lt next")
        }
    }
}
