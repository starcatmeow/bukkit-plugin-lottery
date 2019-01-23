package top.starcatmeow.lottery

import net.milkbowl.vault.economy.Economy
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.java.JavaPlugin
import top.starcatmeow.lottery.commands.Lottery

import java.util.*

class LotteryPlugin : JavaPlugin() {
    private var econ: Economy? = null
    private var ll: LotteryLogic? = null
    override fun onEnable() {
        if (!setupEconomy()) {
            logger.severe(String.format("[%s] - Disabled due to no Vault dependency found!", description.name))
            server.pluginManager.disablePlugin(this)
            return
        }

        //从配置文件读取
        saveDefaultConfig()
        LotteryConfig.price = config.getDouble("price")
        LotteryConfig.firstprize = config.getDouble("firstprize")
        LotteryConfig.secondprize = config.getDouble("secondprize")
        LotteryConfig.thirdprize = config.getDouble("thirdprize")
        LotteryConfig.max = config.getInt("max")
        LotteryConfig.roundsecond = config.getLong("roundsecond")

        ll = LotteryLogic(server, config.getInt("lotteryid"), econ!!)
        this.getCommand("lottery").executor = Lottery(ll!!, this)
        logger.info("Lottery Plugin Enabled!")
    }

    override fun onDisable() {
        // Plugin shutdown logic
        val backmoneylist = HashMap<OfflinePlayer, Double>()
        for (lt in ll!!.tickets) {
            if (backmoneylist.containsKey(lt.player)) {
                backmoneylist[lt.player] = backmoneylist.getValue(lt.player) + LotteryConfig.price
            } else {
                backmoneylist[lt.player] = LotteryConfig.price
            }
        }
        backmoneylist.forEach { player, money -> econ!!.depositPlayer(player, money) }
        config.set("lotteryid", ll!!.id)
        saveConfig()
        logger.info("Lottery Plugin Disabled!")
    }

    private fun setupEconomy(): Boolean {
        if (server.pluginManager.getPlugin("Vault") == null) {
            return false
        }
        val rsp = server.servicesManager.getRegistration(Economy::class.java) ?: return false
        econ = rsp.provider
        return econ != null
    }
}
