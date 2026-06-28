package net.azisaba.mythicerrorfixer;

public class ConfigManager {

    private final MythicErrorFixer plugin;

    public ConfigManager(MythicErrorFixer plugin) {
        this.plugin = plugin;
    }

    public boolean isAutoFixOnReload() {
        return plugin.getConfig().getBoolean("auto_fix_on_reload", true);
    }

    public void setAutoFixOnReload(boolean value) {
        plugin.getConfig().set("auto_fix_on_reload", value);
        plugin.saveConfig();
    }

    public boolean isFixEnabled(String fixName) {
        if (!plugin.getConfig().contains("fixes." + fixName)) {
            plugin.getConfig().set("fixes." + fixName, true);
            plugin.saveConfig();
            return true;
        }
        return plugin.getConfig().getBoolean("fixes." + fixName, true);
    }

    public void setFixEnabled(String fixName, boolean value) {
        plugin.getConfig().set("fixes." + fixName, value);
        plugin.saveConfig();
    }

    public boolean toggleFix(String fixName) {
        boolean current = isFixEnabled(fixName);
        setFixEnabled(fixName, !current);
        return !current;
    }
}
