package net.azisaba.mythicerrorfixer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ErrorParser {
    private final MythicErrorFixer plugin;
    private final List<ParsedError> unfixableErrors = new ArrayList<>();
    private final FileFixer fileFixer;

    public ErrorParser(MythicErrorFixer plugin) {
        this.plugin = plugin;
        this.fileFixer = new FileFixer(plugin);
    }

    public List<ParsedError> getUnfixableErrors() {
        return unfixableErrors;
    }

    public boolean parseAndFix() {
        unfixableErrors.clear();
        List<String> reloadLogs = plugin.getLogCatcher().getAndClearLogs();
        
        if (reloadLogs.isEmpty()) {
            File fallback = new File("mythicmobs_errors.txt");
            if (fallback.exists()) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(fallback))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        reloadLogs.add(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                plugin.getLogger().info("No logs captured recently and no fallback mythicmobs_errors.txt found.");
                return false;
            }
        }

        boolean anyFixed = false;
        
        if (plugin.getConfigManager().isFixEnabled("itemflag_potion")) {
            boolean changed = fileFixer.fixGlobalItemFlags();
            if (changed) anyFixed = true;
        }

        String currentFile = null;
        for (int i = 0; i < reloadLogs.size(); i++) {
            String line = reloadLogs.get(i);
            
            if (line.contains("--| File: ")) {
                currentFile = line.substring(line.indexOf("--| File: ") + 10).trim();
            } else if (line.contains("An error occurred while loading MythicConfig file ")) {
                Matcher m = Pattern.compile("file (.*?):").matcher(line);
                if (m.find()) {
                    currentFile = m.group(1).trim();
                }
            }

            if (line.contains("while scanning an anchor")) {
                for (int j = i + 1; j < Math.min(i + 10, reloadLogs.size()); j++) {
                    String lookAhead = reloadLogs.get(j);
                    if (lookAhead.contains("in 'reader', line ")) {
                        Matcher m = Pattern.compile("line (\\d+),").matcher(lookAhead);
                        if (m.find() && currentFile != null) {
                            int lineNum = Integer.parseInt(m.group(1));
                            if (plugin.getConfigManager().isFixEnabled("anchor_quotes")) {
                                boolean fixed = fileFixer.fixAnchorQuotes(currentFile, lineNum);
                                if (fixed) anyFixed = true;
                            }
                        }
                        break;
                    }
                }
            }

            if (line.contains("A delay is incorrectly configured: second argument must be an integer.")) {
                if (currentFile != null && plugin.getConfigManager().isFixEnabled("mechanic_delay")) {
                    String mechLineStr = null;
                    for (int j = i + 1; j < Math.min(i + 5, reloadLogs.size()); j++) {
                        if (reloadLogs.get(j).contains("--| Mechanic Line:")) {
                            mechLineStr = reloadLogs.get(j).substring(reloadLogs.get(j).indexOf("Mechanic Line: ") + 15).trim();
                            break;
                        }
                    }
                    if (mechLineStr != null) {
                         boolean fixed = fileFixer.fixDelayMechanic(currentFile, mechLineStr);
                         if (fixed) anyFixed = true;
                    }
                }
            }

            if (line.contains("The 'message' attribute is required.")) {
                if (currentFile != null && plugin.getConfigManager().isFixEnabled("mechanic_message")) {
                    String mechLineStr = null;
                    for (int j = i + 1; j < Math.min(i + 5, reloadLogs.size()); j++) {
                        if (reloadLogs.get(j).contains("--| Mechanic Line:")) {
                            mechLineStr = reloadLogs.get(j).substring(reloadLogs.get(j).indexOf("Mechanic Line: ") + 15).trim();
                            break;
                        }
                    }
                    if (mechLineStr != null) {
                         boolean fixed = fileFixer.fixMessageMechanic(currentFile, mechLineStr);
                         if (fixed) anyFixed = true;
                    }
                }
            }

            if (line.contains("Error loading LineConfig: Unbalanced Braces")) {
                if (plugin.getConfigManager().isFixEnabled("unbalanced_braces")) {
                    String mechLineStr = null;
                    for (int j = i + 1; j < Math.min(i + 5, reloadLogs.size()); j++) {
                        if (reloadLogs.get(j).contains("[Line]:")) {
                            mechLineStr = reloadLogs.get(j).substring(reloadLogs.get(j).indexOf("[Line]: ") + 8).trim();
                            break;
                        }
                    }
                    if (mechLineStr != null) {
                         boolean fixed = fileFixer.fixUnbalancedBraces(currentFile, mechLineStr);
                         if (fixed) anyFixed = true;
                    }
                }
            }

            if (line.contains("Invalid SkinTexture provided") ||
                (line.contains("not found") && line.contains("Material type")) ||
                line.contains("Could not find MetaSkill") ||
                line.contains("No Type specified.") ||
                line.contains("✗ Config Error") ||
                line.contains("✗ Configuration Error")) {
                
                ParsedError.ErrorType type = ParsedError.ErrorType.OTHER;
                String detail = line;
                
                int warnIdx = line.indexOf("WARN]: ");
                if (warnIdx != -1) detail = line.substring(warnIdx + 7).trim();
                
                if (detail.startsWith("[MythicMobs] --| Error Message: ")) {
                    detail = detail.substring(32);
                } else if (detail.startsWith("[MythicMobs] ")) {
                    detail = detail.substring(13);
                }
                
                String extractedFile = currentFile;
                Matcher fileMatcher = Pattern.compile("\\(File: (.*?)\\)").matcher(detail);
                if (fileMatcher.find()) {
                    extractedFile = fileMatcher.group(1).trim();
                    detail = detail.substring(0, fileMatcher.start()).trim();
                }
                
                Matcher inMatcher = Pattern.compile(" in '(.*?)':").matcher(detail);
                if (inMatcher.find()) {
                    extractedFile = inMatcher.group(1).trim();
                    detail = detail.replace(inMatcher.group(0), ":");
                }

                if (detail.contains("Material type")) {
                    type = ParsedError.ErrorType.MATERIAL;
                    Matcher mm = Pattern.compile("Material type '(.*?)'").matcher(detail);
                    if (mm.find()) detail = mm.group(1);
                } else if (detail.contains("Could not find MetaSkill")) {
                    type = ParsedError.ErrorType.METASKILL;
                    Matcher mm = Pattern.compile("Could not find MetaSkill (.*)").matcher(detail);
                    if (mm.find()) detail = mm.group(1).trim();
                } else if (detail.contains("No Type specified") || detail.contains("must be a valid MythicMob")) {
                    type = ParsedError.ErrorType.TYPE_MISSING;
                    Matcher mm = Pattern.compile("Could not load MythicMob (.*?)!").matcher(detail);
                    if (mm.find()) {
                        detail = mm.group(1);
                    } else {
                        // Extract type from summon mechanic if possible
                        Matcher m2 = Pattern.compile("summon\\{.*?(?:t|type|m|mob)=(.*?)[;\\}]").matcher(detail);
                        if (m2.find()) {
                            detail = m2.group(1);
                        } else {
                            detail = "Unknown Type";
                        }
                    }
                } else if (detail.contains("Invalid SkinTexture")) {
                    type = ParsedError.ErrorType.TEXTURE;
                    detail = "Texture parsing failed";
                }

                if (type == ParsedError.ErrorType.OTHER) {
                    if (detail.startsWith("✗ Config Error for ")) {
                        detail = detail.substring(19);
                    } else if (detail.startsWith("✗ Configuration Error ")) {
                        detail = detail.substring(22);
                    }
                }

                unfixableErrors.add(new ParsedError(type, detail, extractedFile != null ? extractedFile : "Unknown"));
            }
        }
        return anyFixed;
    }
}
