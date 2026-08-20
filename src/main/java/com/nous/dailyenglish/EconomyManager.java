package com.nous.dailyenglish;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {
    private Economy economy;

    public EconomyManager() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp != null) this.economy = rsp.getProvider();
    }

    public boolean isReady() { return economy != null; }
    public String getName() { return economy != null ? economy.getName() : "none"; }

    public boolean deposit(OfflinePlayer player, double dollars) {
        if (economy == null) return false;
        economy.depositPlayer(player, dollars);
        return true;
    }
}
