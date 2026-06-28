package net.azisaba.mythicerrorfixer;

import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicReloadedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MythicEventListener implements Listener {
    private final MythicErrorFixer plugin;
    private boolean isFixing = false;

    public MythicEventListener(MythicErrorFixer plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMythicReload(MythicReloadedEvent event) {
        if (!plugin.getConfigManager().isAutoFixOnReload()) return;

        if (isFixing) {
            isFixing = false;
            int unfixableCount = plugin.getErrorParser().getUnfixableErrors().size();
            String msg = "§a[MythicErrorFixer] 自動修正後のリロードが完了しました §c(未解決のエラー: " + unfixableCount + "件)";
            plugin.getLogger().info("Auto-fix reload completed. Unfixable errors: " + unfixableCount);
            
            for (org.bukkit.entity.Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.hasPermission("mythicerrorfixer.notify") || player.isOp()) {
                    player.sendMessage(msg);
                    if (unfixableCount > 0) {
                        player.sendMessage("§e/mythicfixer listで未解決のエラーを確認できます。");
                    }
                }
            }
            return;
        }

        plugin.getLogger().info("MythicMobs reload detected. Parsing logs for errors...");
        for (org.bukkit.entity.Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPermission("mythicerrorfixer.notify") || player.isOp()) {
                player.sendMessage("§e[MythicErrorFixer] エラーを解析・修正中...");
            }
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getLogger().info("Starting parseAndFix()...");
                boolean fixed = plugin.getErrorParser().parseAndFix();
                plugin.getLogger().info("Finished parseAndFix(). fixed=" + fixed);
                
                plugin.getLogger().info("Starting fixGlobalSyntax()...");
                boolean globalFixed = plugin.getFileFixer().fixGlobalSyntax();
                plugin.getLogger().info("Finished fixGlobalSyntax(). globalFixed=" + globalFixed);
                
                if (fixed || globalFixed) {
                    plugin.getLogger().info("Errors were fixed automatically! Issuing another reload to apply changes...");
                    isFixing = true;
                    
                    for (org.bukkit.entity.Player player : plugin.getServer().getOnlinePlayers()) {
                        if (player.hasPermission("mythicerrorfixer.notify") || player.isOp()) {
                            player.sendMessage("§a[MythicErrorFixer] エラーを自動修正しました．設定を適用するため再度リロードしています...");
                        }
                    }
                    
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "mm reload");
                    });
                } else {
                    plugin.getLogger().info("No fixable errors found or changed during this reload.");
                    int unfixableCount = plugin.getErrorParser().getUnfixableErrors().size();
                    for (org.bukkit.entity.Player player : plugin.getServer().getOnlinePlayers()) {
                        if (player.hasPermission("mythicerrorfixer.notify") || player.isOp()) {
                            player.sendMessage("§e[MythicErrorFixer] 新たな修正可能なエラーはありませんでした。 §c(未解決のエラー: " + unfixableCount + "件)");
                            if (unfixableCount > 0) {
                                player.sendMessage("§e/mythicfixer list で未解決のエラーを確認してください。");
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                plugin.getLogger().severe("An error occurred during async fix task!");
                t.printStackTrace();
                for (org.bukkit.entity.Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player.hasPermission("mythicerrorfixer.notify") || player.isOp()) {
                        player.sendMessage("§c[MythicErrorFixer] 重大なエラーが発生したため，処理が停止しました．コンソールを確認してください．");
                        player.sendMessage("§c" + t);
                    }
                }
            }
        });
    }
}
