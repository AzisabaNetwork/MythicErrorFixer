package net.azisaba.mythicerrorfixer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class FileFixer {
    private final MythicErrorFixer plugin;
    private static final java.util.regex.Pattern MATERIAL_PATTERN = java.util.regex.Pattern.compile("(?i)^(\\s*)(Id|Type|Block|Material):\\s*['\"]?(.*?)['\"]?\\s*$");

    public FileFixer(MythicErrorFixer plugin) {
        this.plugin = plugin;
    }

    private File getActualFile(String pathStr) {
        String normalized = pathStr.replace("\\", "/");
        int idx = normalized.indexOf("plugins/MythicMobs/");
        if (idx != -1) {
            String relative = normalized.substring(idx + 19);
            File pluginsFolder = plugin.getDataFolder().getParentFile();
            File mmFolder = new File(pluginsFolder, "MythicMobs");
            return new File(mmFolder, relative);
        }
        return new File(pathStr);
    }

    private void backupFile(File file) {
        try {
            File backupFolder = new File(plugin.getDataFolder(), "backup");
            File pluginsFolder = plugin.getDataFolder().getParentFile();
            File mmFolder = new File(pluginsFolder, "MythicMobs");
            
            String relative = mmFolder.toURI().relativize(file.toURI()).getPath();
            if (relative == null || relative.isEmpty() || relative.equals(file.getPath())) {
                relative = file.getName();
            }
            File backupFile = new File(backupFolder, relative);
            backupFile.getParentFile().mkdirs();
            if (!backupFile.exists()) {
                Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to backup file: " + file.getPath());
        }
    }

    public boolean fixAnchorQuotes(String currentFile, int lineNum) {
        File file = getActualFile(currentFile);
        if (!file.exists()) return false;
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            if (lineNum > 0 && lineNum <= lines.size()) {
                String line = lines.get(lineNum - 1);
                int dashIdx = line.indexOf('-');
                int colonIdx = line.indexOf(':');
                if (dashIdx != -1 && (colonIdx == -1 || dashIdx < colonIdx)) {
                    int ampIdx = line.indexOf('&');
                    if (ampIdx != -1 && ampIdx > dashIdx) {
                        String before = line.substring(0, ampIdx);
                        String after = line.substring(ampIdx);
                        if (!after.startsWith("\"")) {
                            lines.set(lineNum - 1, before + "\"" + after.trim() + "\"");
                            backupFile(file);
                            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
                            return true;
                        }
                    }
                } else if (colonIdx != -1) {
                    int ampIdx = line.indexOf('&', colonIdx);
                    if (ampIdx != -1) {
                        String before = line.substring(0, ampIdx);
                        String after = line.substring(ampIdx);
                        if (!after.startsWith("\"")) {
                            lines.set(lineNum - 1, before + "\"" + after.trim() + "\"");
                            backupFile(file);
                            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
                            return true;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean fixDelayMechanic(String currentFile, String mechLineStr) {
        File file = getActualFile(currentFile);
        if (!file.exists()) return false;
        return replaceInFile(file, mechLineStr, "delay 1");
    }

    public boolean fixMessageMechanic(String currentFile, String mechLineStr) {
        File file = getActualFile(currentFile);
        if (!file.exists()) return false;
        String replacement = mechLineStr;
        if (mechLineStr.contains("{") && !mechLineStr.contains("}")) {
            replacement = mechLineStr + "}";
        } else if (mechLineStr.startsWith("message ") || mechLineStr.startsWith("m ")) {
            String msg = mechLineStr.substring(mechLineStr.indexOf(" ") + 1).replace("\"", "").replace("'", "");
            replacement = "message{m=\"" + msg + "\"}";
        }
        if (!replacement.equals(mechLineStr)) {
            return replaceInFile(file, mechLineStr, replacement);
        }
        return false;
    }

    public boolean fixUnbalancedBraces(String currentFile, String mechLineStr) {
        if (currentFile != null) {
            File file = getActualFile(currentFile);
            if (file.exists()) {
                return replaceInFile(file, mechLineStr, mechLineStr + "}");
            }
        }
        File pluginsFolder = plugin.getDataFolder().getParentFile();
        File mmFolder = new File(pluginsFolder, "MythicMobs");
        return searchAndReplaceGlobally(mmFolder, (f, line) -> line.replace(mechLineStr, mechLineStr + "}"));
    }

    public boolean fixGlobalItemFlags() {
        File pluginsFolder = plugin.getDataFolder().getParentFile();
        File mmFolder = new File(pluginsFolder, "MythicMobs");
        return searchAndReplaceGlobally(mmFolder, (f, line) -> line.replace("HIDE_POTION_EFFECTS", "HIDE_ADDITIONAL_TOOLTIP"));
    }

    private boolean replaceInFile(File file, String target, String replacement) {
        return searchAndReplaceGlobally(file, (f, line) -> line.replace(target, replacement));
    }

    public boolean fixGlobalSyntax() {
        boolean anyFixed = false;
        File mmFolder = new File(plugin.getDataFolder().getParentFile(), "MythicMobs");
        if (!mmFolder.exists()) return false;

        List<File> allFiles = new ArrayList<>();
        collectYamlFiles(mmFolder, allFiles);

        for (File file : allFiles) {
            boolean changed = searchAndReplaceGlobally(file, (f, line) -> {
                if (plugin.getConfigManager().isFixEnabled("itemflag_potion")) {
                    line = line.replace("HIDE_POTION_EFFECTS", "HIDE_ADDITIONAL_TOOLTIP");
                    line = line.replace("HIDE_FLAGS", "HIDE_ATTRIBUTES");
                }
                
                String path = f.getAbsolutePath().replace("\\", "/");
                if (path.contains("/Items/")) {
                    java.util.regex.Matcher m = MATERIAL_PATTERN.matcher(line);
                    if (m.find()) {
                        String indent = m.group(1);
                        String key = m.group(2);
                        String value = m.group(3);
                        
                        if (value != null && !value.trim().isEmpty()) {
                            try {
                                org.bukkit.Material mat = org.bukkit.Material.matchMaterial(value);
                                if (mat == null) {
                                    mat = org.bukkit.Material.matchMaterial(value, true);
                                }
                                if (mat != null) {
                                    line = indent + key + ": " + mat.name();
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
                
                return line;
            });
            if (changed) {
                anyFixed = true;
            }
        }
        return anyFixed;
    }

    private void collectYamlFiles(File dir, List<File> list) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                if (f.getName().equals("backup")) continue;
                collectYamlFiles(f, list);
            } else if (f.getName().endsWith(".yml")) {
                list.add(f);
            }
        }
    }

    private boolean searchAndReplaceGlobally(File dir, java.util.function.BiFunction<File, String, String> replacer) {
        if (!dir.exists()) return false;
        if (dir.isDirectory()) {
            boolean anyChanged = false;
            File[] files = dir.listFiles();
            if (files == null) return false;
            for (File f : files) {
                if (searchAndReplaceGlobally(f, replacer)) anyChanged = true;
            }
            return anyChanged;
        } else if (dir.getName().endsWith(".yml")) {
            Charset[] charsets = { StandardCharsets.UTF_8, Charset.forName("Shift_JIS") };
            for (Charset charset : charsets) {
                try {
                    List<String> lines = Files.readAllLines(dir.toPath(), charset);
                    boolean changed = false;
                    for (int i = 0; i < lines.size(); i++) {
                        String line = lines.get(i);
                        String newLine = replacer.apply(dir, line);
                        if (!newLine.equals(line)) {
                            lines.set(i, newLine);
                            changed = true;
                        }
                    }
                    if (changed) {
                        backupFile(dir);
                        Files.write(dir.toPath(), lines, charset);
                        return true;
                    }
                    return false;
                } catch (java.nio.charset.MalformedInputException e) {
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
            plugin.getLogger().warning("Could not read file (neither UTF-8 nor Shift-JIS): " + dir.getPath());
        }
        return false;
    }
}
