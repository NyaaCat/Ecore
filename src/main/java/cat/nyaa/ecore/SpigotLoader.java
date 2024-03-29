package cat.nyaa.ecore;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class SpigotLoader extends JavaPlugin {
    private final Logger logger = getLogger();
    private Economy economyProvided = null;
    private Config config;
    private EconomyCoreProvider eCoreProvider = null;

    @Override
    public void onEnable() {
        //initialize api
        var configFile = new File(getDataFolder(), "config.toml");
        if (getDataFolder().mkdir()) {
            logger.info("Created data folder.");
        }

        var tomlWriter = new TomlWriter();
        try {
            if (configFile.createNewFile() || configFile.length() == 0) {
                var defaultConfig = new Config();
                tomlWriter.write(defaultConfig, configFile);
                logger.info("Created config file.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            this.getServer().getPluginManager().disablePlugin(this);
        }

        var toml = new Toml();
        config = toml.read(configFile).to(Config.class);
        try {
            tomlWriter.write(config, configFile);
        } catch (IOException e) {
            e.printStackTrace();
            this.getServer().getPluginManager().disablePlugin(this);
            logger.severe("Error occurred while loading config file.");
        }
        logger.info("Config loaded.");

        if (!setupEconomy()) {
            logger.warning("Vault or economy provider(implementation of vault api) not found, keep trying...");
            this.getServer().getPluginManager().registerEvents(new PluginEnableListener(this), this);
            this.getServer().getScheduler().runTaskLater(this, () -> {
                if (economyProvided == null) {
                    this.logger.severe("Vault or economy provider(implementation of vault api) not found, disabling plugin.");
                    this.getServer().getPluginManager().disablePlugin(this);
                } else {
                    PluginEnableEvent.getHandlerList().unregister(this);
                }
            }, 0);
        } else {
            if (!setupEcoreProvider()) {
                logger.severe("Failed to setup ecore provider.");
                this.getPluginLoader().disablePlugin(this);
            }
        }

    }

    protected boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        // ';[cvfp[000000000000000000000000000000000000000'
        // By Companion Object -- The cat
        RegisteredServiceProvider<Economy> economyRegisteredServiceProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyRegisteredServiceProvider == null) {
            return false;
        } else {
            economyProvided = economyRegisteredServiceProvider.getProvider();
            return true;
        }
    }

    protected boolean setupEcoreProvider() {
        if (config == null || economyProvided == null) {
            return false;
        }

        // create ecore instance & register instance as a service provider to service manager
        try {
            eCoreProvider = new EconomyCoreProvider(config, economyProvided, this);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        this.getServer().getServicesManager().register(EconomyCore.class, eCoreProvider, this, ServicePriority.Normal);
        return true;
    }

    @Override
    public void onDisable() {
        if (eCoreProvider != null) {
            eCoreProvider.onDisable();
        }
    }
}
