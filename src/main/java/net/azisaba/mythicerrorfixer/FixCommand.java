package net.azisaba.mythicerrorfixer;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FixCommand implements CommandExecutor, TabCompleter {

    private final MythicErrorFixer plugin;
    private final List<String> fixNames = Arrays.asList(
            "anchor_quotes", "mechanic_delay", "mechanic_message", "unbalanced_braces", "itemflag_potion"
    );

    public FixCommand(MythicErrorFixer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("mythicerrorfixer.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "=== MythicErrorFixer Commands ===");
            sender.sendMessage(ChatColor.YELLOW + "/mythicfixer list" + ChatColor.WHITE + " - List unfixable errors from last run");
            sender.sendMessage(ChatColor.YELLOW + "/mythicfixer fix" + ChatColor.WHITE + " - Manually parse logs and fix errors");
            sender.sendMessage(ChatColor.YELLOW + "/mythicfixer toggle <fix_name>" + ChatColor.WHITE + " - Toggle a specific fix");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "list":
                java.util.List<ParsedError> unfixable = plugin.getErrorParser().getUnfixableErrors();
                if (unfixable.isEmpty()) {
                    sender.sendMessage("§a修正不可能なエラーは現在ありません");
                } else {
                    boolean consoleOutput = false;
                    int page = 1;
                    int itemsPerPage = 10;
                    
                    for (int i = 1; i < args.length; i++) {
                        if (args[i].equalsIgnoreCase("--console") || args[i].equalsIgnoreCase("-c")) {
                            consoleOutput = true;
                        } else {
                            try {
                                page = Integer.parseInt(args[i]);
                            } catch (NumberFormatException ignored) {}
                        }
                    }

                    if (consoleOutput) {
                        plugin.getLogger().info("=== 未解決のエラー (" + unfixable.size() + "件) ===");
                        for (ParsedError err : unfixable) {
                            String formatted;
                            switch (err.getType()) {
                                case MATERIAL:
                                    formatted = "アイテムIDが不正: " + err.getDetail();
                                    break;
                                case METASKILL:
                                    formatted = "存在しないスキル: " + err.getDetail();
                                    break;
                                case TYPE_MISSING:
                                    formatted = "Mobのタイプが不正: " + err.getDetail();
                                    break;
                                case TEXTURE:
                                    formatted = "スキンテクスチャが不正";
                                    break;
                                default:
                                    formatted = "その他: " + err.getDetail();
                                    break;
                            }
                            plugin.getLogger().info(formatted + " (ファイル: " + err.getFile() + ")");
                        }
                        sender.sendMessage("§aコンソールに " + unfixable.size() + " 件のエラーリストを出力しました。");
                        break;
                    }
                    
                    int totalPages = (int) Math.ceil((double) unfixable.size() / itemsPerPage);
                    if (page < 1) page = 1;
                    if (page > totalPages) page = totalPages;
                    
                    sender.sendMessage("§f----- §c未解決のエラー §8(" + unfixable.size() + "件) §f----- §8(ページ " + page + "/" + totalPages + ")");
                    
                    int start = (page - 1) * itemsPerPage;
                    int end = Math.min(start + itemsPerPage, unfixable.size());
                    
                    for (int i = start; i < end; i++) {
                        ParsedError err = unfixable.get(i);
                        String formatted;
                        switch (err.getType()) {
                            case MATERIAL:
                                formatted = "§cアイテムIDが不正: §e" + err.getDetail();
                                break;
                            case METASKILL:
                                formatted = "§c存在しないスキル: §e" + err.getDetail();
                                break;
                            case TYPE_MISSING:
                                formatted = "§cMobのタイプが不正: §e" + err.getDetail();
                                break;
                            case TEXTURE:
                                formatted = "§cスキンテクスチャが不正";
                                break;
                            default:
                                formatted = "§cその他: §7" + err.getDetail();
                                break;
                        }
                        
                        String shortFile = err.getFile();
                        if (shortFile != null) {
                            String lower = shortFile.toLowerCase();
                            int idx = lower.indexOf("mythicmobs");
                            if (idx != -1) {
                                shortFile = shortFile.substring(idx).replace("\\", "/");
                            } else {
                                if (shortFile.contains("/") || shortFile.contains("\\")) {
                                    String[] parts = shortFile.split("[\\\\/]");
                                    shortFile = parts[parts.length - 1];
                                }
                            }
                        }
                        
                        sender.sendMessage(formatted + " §8(ファイル: " + shortFile + ")");
                    }
                    
                    if (totalPages > 1) {
                        if (sender instanceof org.bukkit.entity.Player) {
                            org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
                            net.md_5.bungee.api.chat.TextComponent footer = new net.md_5.bungee.api.chat.TextComponent("§f----- ");
                            
                            if (page > 1) {
                                net.md_5.bungee.api.chat.TextComponent prev = new net.md_5.bungee.api.chat.TextComponent("§e◀ 前のページ");
                                prev.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/mythicfixer list " + (page - 1)));
                                footer.addExtra(prev);
                            } else {
                                footer.addExtra("§8◀ 前のページ");
                            }
                            
                            footer.addExtra(" §7| ");
                            
                            if (page < totalPages) {
                                net.md_5.bungee.api.chat.TextComponent next = new net.md_5.bungee.api.chat.TextComponent("§e次のページ ▶");
                                next.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/mythicfixer list " + (page + 1)));
                                footer.addExtra(next);
                            } else {
                                footer.addExtra("§8次のページ ▶");
                            }
                            
                            footer.addExtra(" §f-----");
                            p.spigot().sendMessage(footer);
                        } else {
                            String prevStr = (page > 1) ? "前のページ: /mythicfixer list " + (page - 1) : "";
                            String nextStr = (page < totalPages) ? "次のページ: /mythicfixer list " + (page + 1) : "";
                            sender.sendMessage("§f----- §7" + prevStr + (prevStr.isEmpty() || nextStr.isEmpty() ? "" : " | ") + nextStr + " §f-----");
                        }
                    }
                }
                break;
            case "fix":
                sender.sendMessage(ChatColor.YELLOW + "Starting manual fix process...");
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    plugin.getErrorParser().parseAndFix();
                    sender.sendMessage(ChatColor.GREEN + "Fix process completed. Check console. Run /mm reload to apply.");
                });
                break;
            case "toggle":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /mythicfixer toggle <fix_name>");
                    sender.sendMessage(ChatColor.RED + "Available: " + String.join(", ", fixNames) + ", auto_fix_on_reload");
                    return true;
                }
                String fixName = args[1].toLowerCase();
                if (fixName.equals("auto_fix_on_reload")) {
                    boolean current = plugin.getConfigManager().isAutoFixOnReload();
                    plugin.getConfigManager().setAutoFixOnReload(!current);
                    sender.sendMessage(ChatColor.GREEN + "auto_fix_on_reload set to " + (!current));
                } else if (fixNames.contains(fixName)) {
                    boolean newVal = plugin.getConfigManager().toggleFix(fixName);
                    sender.sendMessage(ChatColor.GREEN + "Fix '" + fixName + "' set to " + newVal);
                } else {
                    sender.sendMessage(ChatColor.RED + "Unknown fix name. Available: " + String.join(", ", fixNames));
                }
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand.");
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], Arrays.asList("list", "fix", "toggle"), new ArrayList<>());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {
            List<String> options = new ArrayList<>(fixNames);
            options.add("auto_fix_on_reload");
            return StringUtil.copyPartialMatches(args[1], options, new ArrayList<>());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
            return StringUtil.copyPartialMatches(args[1], Arrays.asList("--console", "-c"), new ArrayList<>());
        }
        return Collections.emptyList();
    }
}
