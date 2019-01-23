package top.starcatmeow.lottery

import net.milkbowl.vault.economy.Economy
import org.bukkit.OfflinePlayer
import org.bukkit.Server
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

import java.util.*

class LotteryLogic(private val server: Server, internal var id: Int, private val econ: Economy) {
    internal val tickets: ArrayList<LotteryTicket> = ArrayList()
    private val firstprize: HashMap<OfflinePlayer, Int> = HashMap()
    private val secondprize: HashMap<OfflinePlayer, Int> = HashMap()
    private val thirdprize: HashMap<OfflinePlayer, Int> = HashMap()
    private var firstprizecount = 0
    private var secondprizecount = 0
    private var thirdprizecount = 0
    private var firstrun = true
    private var canbuy = false
    private val resulttime: Date = Date()
    private var lock = false
    private var lock2 = false
    private var leftticket = LotteryConfig.max

    private fun startBuyLottery() {
        server.broadcastMessage("§l§5彩票系统 §l§7> §b第 §7$id§b 期彩票已开始发售，每注$${LotteryConfig.price}，将在${LotteryConfig.roundsecond}秒后开奖")
        server.broadcastMessage("          §3一等奖（三位数字全部匹配）：§4§l$${LotteryConfig.firstprize}/注")
        server.broadcastMessage("          §3二等奖（两位数字对应匹配）：§4§l$${LotteryConfig.secondprize}/注")
        server.broadcastMessage("          §3三等奖（一位数字对应匹配）：§4§l$${LotteryConfig.thirdprize}/注")
        server.broadcastMessage("          §6还在犹豫什么，下注输入：/lottery buy [你要买的三位数]（可以用random代替来机选） [注数]（默认为1注，最大为10注）")
        resulttime.time = Date().time + LotteryConfig.roundsecond * 1000
        canbuy = true
    }

    fun sendBuyLotterytoCommandSender(target: CommandSender) {
        if (canbuy) {
            target.sendMessage("§l§5彩票系统 §l§7> §b第 §7" + id + "§b 期彩票已开始发售，每注$${LotteryConfig.price}，将在" + (resulttime.time - Date().time) / 1000 + "秒后开奖")
            target.sendMessage("          §3一等奖（三位数字全部匹配）：§4§l$${LotteryConfig.firstprize}/注")
            target.sendMessage("          §3二等奖（两位数字对应匹配）：§4§l$${LotteryConfig.secondprize}/注")
            target.sendMessage("          §3三等奖（一位数字对应匹配）：§4§l$${LotteryConfig.thirdprize}/注")
            target.sendMessage("          §6还在犹豫什么，下注输入：/lottery buy [你要买的三位数]（可以用random代替来机选） [注数]（默认为1注，最大为10注）")
        } else {
            target.sendMessage("§l§5彩票系统 §l§7> §4现在还没有彩票开售哦！")
        }
    }

    fun showLotteryResult() {
        //异步线程安全
        if (lock) {
            return
        }

        //第一次启动特判
        if (firstrun) {
            firstrun = false
            sleep(23000)
            startBuyLottery()
            return
        }

        lock = true
        //停卖
        server.broadcastMessage("§l§5彩票系统 §l§7> §b第 §7$id§b 期彩票已停止售卖！！！将在三秒后开奖！！！")
        canbuy = false
        sleep(3000)

        //开奖
        val result = Math.abs(Random().nextInt() % 1000)
        server.broadcastMessage("§l§5彩票系统 §l§7> §b第 §7$id§b 期彩票的开奖号码是：" + String.format("%03d", result))
        sleep(3000)

        //判奖逻辑
        for (ticket in tickets) {
            if (ticket.number == result) {
                if (firstprize.containsKey(ticket.player))
                    firstprize[ticket.player] = firstprize.getValue(ticket.player) + 1
                else
                    firstprize[ticket.player] = 1
                ++firstprizecount

            } else if (ticket.number / 10 == result / 10 ||        //前两位相对应
                    ticket.number % 100 == result % 100) {         //后两位相对应

                if (secondprize.containsKey(ticket.player))
                    secondprize[ticket.player] = secondprize.getValue(ticket.player) + 1
                else
                    secondprize[ticket.player] = 1
                ++secondprizecount

            } else if (ticket.number / 100 == result / 100 ||       //第一位对应第一位
                    ticket.number / 10 % 10 == result / 10 % 10 ||  //第二位对应第二位
                    ticket.number % 10 == result % 10) {            //第三位对应第三位

                if (thirdprize.containsKey(ticket.player))
                    thirdprize[ticket.player] = thirdprize.getValue(ticket.player) + 1
                else
                    thirdprize[ticket.player] = 1
                ++thirdprizecount

            }
        }

        //广播中奖信息
        server.broadcastMessage("          一等奖中奖 $firstprizecount 张：")
        firstprize.forEach { player, count -> server.broadcastMessage("              ${player.name}：$count 张") }
        sleep(3000)

        server.broadcastMessage("          二等奖中奖 $secondprizecount 张：")
        secondprize.forEach { player, count -> server.broadcastMessage("              ${player.name}：$count 张") }
        sleep(3000)

        server.broadcastMessage("          三等奖中奖 $thirdprizecount 张：")
        thirdprize.forEach { player, count -> server.broadcastMessage("              ${player.name}：$count 张") }
        sleep(3000)

        //发奖
        firstprize.forEach { player, count ->
            run {
                if (player.isOnline)
                    player.player.sendMessage("§l§5彩票系统 §l§7> §b你有 $count 张彩票获得一等奖，奖金共计 §4§l$${count * LotteryConfig.firstprize}")
                econ.depositPlayer(player, count * LotteryConfig.firstprize)
            }
        }

        secondprize.forEach { player, count ->
            run {
                if (player.isOnline)
                    player.player.sendMessage("§l§5彩票系统 §l§7> §b你有 $count 张彩票获得二等奖，奖金共计 §4§l$${count * LotteryConfig.secondprize}")
                econ.depositPlayer(player, count * LotteryConfig.secondprize)
            }
        }

        thirdprize.forEach { player, count ->
            run {
                if (player.isOnline)
                    player.player.sendMessage("§l§5彩票系统 §l§7> §b你有 $count 张彩票获得三等奖，奖金共计 §4§l$${count * LotteryConfig.thirdprize}")
                econ.depositPlayer(player, count * LotteryConfig.thirdprize)
            }
        }

        sleep(5000)
        resetVars()
        startBuyLottery()
    }

    private fun resetVars() {
        tickets.clear()
        firstprize.clear()
        secondprize.clear()
        thirdprize.clear()
        firstprizecount = 0
        secondprizecount = 0
        thirdprizecount = 0
        id++
        lock = false
        canbuy = true
        leftticket = LotteryConfig.max
    }

    private fun sleep(millis: Long) {
        Thread.sleep(millis)
    }

    fun buyLottery(player: Player, number: String, amount: Int) {
        while (lock2) {
            sleep(100)
        }
        lock2 = true
        var lt: LotteryTicket
        val finalstr = StringBuilder()

        //判断彩票开售
        if (!canbuy) {
            player.sendMessage("§l§5彩票系统 §l§7> §4现在还没有彩票开售哦！")
            lock2 = false
            return
        }

        //判断剩余彩票
        if (amount > leftticket) {
            player.sendMessage("§l§5彩票系统 §l§7> §4只剩 $leftticket 张彩票了，无法购买")
            lock2 = false
            return
        }

        //判断玩家钱
        if (LotteryConfig.price * amount > econ.getBalance(player)) {
            player.sendMessage("§l§5彩票系统 §l§7> §4你没有足够的钱购买 $amount 张彩票")
            lock2 = false
            return
        }

        player.sendMessage("§l§5彩票系统 §l§7> §8正在处理你的请求...")
        if (number == "random") {
            leftticket -= amount
            econ.withdrawPlayer(player, LotteryConfig.price * amount)
            if (amount > 20) {
                for (i in 0 until 20) {
                    lt = LotteryTicket(player, Math.abs(Random().nextInt() % 1000))
                    tickets.add(lt)
                    finalstr.append("            §b彩票号码 ${lt.number}\n")
                }
                for (i in 20 until amount) {
                    lt = LotteryTicket(player, Math.abs(Random().nextInt() % 1000))
                    tickets.add(lt)
                    //finalstr.append("            §b彩票号码 ${lt.number}\n")
                }
            } else {
                for (i in 0 until amount) {
                    lt = LotteryTicket(player, Math.abs(Random().nextInt() % 1000))
                    tickets.add(lt)
                    finalstr.append("            §b彩票号码 ${lt.number}\n")
                }
            }
            player.sendMessage(finalstr.toString())
            player.sendMessage("          §b共计 $amount 张，购买成功！")
        } else {
            try {
                val num = Integer.valueOf(number)
                if (num < 0 || num > 999)
                    throw Exception()
                leftticket -= amount
                econ.withdrawPlayer(player, LotteryConfig.price * amount)
                for (i in 0 until amount) {
                    lt = LotteryTicket(player, num)
                    tickets.add(lt)
                }
                player.sendMessage("          §b彩票号码 $num * $amount")
                player.sendMessage("          §b共计 $amount 张，购买成功！")
            } catch (e: Exception) {
                player.sendMessage("§l§5彩票系统 §l§7> §4命令格式不正确！彩票号码不在正确范围中！")
                lock2 = false
                return
            }
        }
        lock2 = false
    }
}
