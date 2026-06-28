package net.azisaba.mythicerrorfixer;

import org.bukkit.plugin.java.JavaPlugin;

public class MythicErrorFixer extends JavaPlugin {

    private static MythicErrorFixer instance;
    private ConfigManager configManager;
    private ErrorParser errorParser;
    private LogCatcher logCatcher;
    private FileFixer fileFixer;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.errorParser = new ErrorParser(this);
        this.fileFixer = new FileFixer(this);
        
        this.logCatcher = new LogCatcher();
        this.logCatcher.attach();

        getCommand("mythicfixer").setExecutor(new FixCommand(this));
        
        getServer().getPluginManager().registerEvents(new MythicEventListener(this), this);

        getLogger().info("MythicErrorFixer has been enabled.");
    }

    @Override
    public void onDisable() {
        if (this.logCatcher != null) {
            this.logCatcher.detach();
        }
        getLogger().info("MythicErrorFixer has been disabled.");
    }

    public static MythicErrorFixer getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ErrorParser getErrorParser() {
        return errorParser;
    }
    
    public LogCatcher getLogCatcher() {
        return logCatcher;
    }
    
    public FileFixer getFileFixer() {
        return fileFixer;
    }
}
