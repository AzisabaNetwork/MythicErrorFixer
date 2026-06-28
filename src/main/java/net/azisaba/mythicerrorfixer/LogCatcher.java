package net.azisaba.mythicerrorfixer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class LogCatcher extends AbstractAppender {

    private final List<String> capturedLogs = new CopyOnWriteArrayList<>();
    private boolean capturing = false;

    public LogCatcher() {
        super("MythicErrorFixer-LogCatcher", null, null, false, null);
    }

    public void attach() {
        start();
        Logger rootLogger = (Logger) LogManager.getRootLogger();
        rootLogger.addAppender(this);
    }

    public void detach() {
        Logger rootLogger = (Logger) LogManager.getRootLogger();
        rootLogger.removeAppender(this);
        stop();
    }

    @Override
    public void append(LogEvent event) {
        String message = event.getMessage().getFormattedMessage();
        if (message == null) return;

        String loggerName = event.getLoggerName();

        if (message.contains("Loading Packs...") || message.contains("issued server command: /mm r") || message.contains("Loading Items...")) {
            capturedLogs.clear();
            capturing = true;
        }

        if (capturing) {
            String cleanMessage = stripAnsi(message);
            if ((loggerName != null && loggerName.contains("MythicMobs")) || 
                cleanMessage.contains("org.bukkit.configuration") || 
                cleanMessage.contains("while scanning an anchor") || 
                cleanMessage.contains("in 'reader', line ") ||
                cleanMessage.contains("[Line]:") ||
                cleanMessage.contains("Configuration Error in")) {
                
                capturedLogs.add("[MythicMobs] " + cleanMessage);
            }
        }
    }

    private String stripAnsi(String msg) {
        if (msg == null) return null;
        return msg.replaceAll("\u001B\\[[;\\d]*m", "");
    }

    public List<String> getAndClearLogs() {
        List<String> logs = new ArrayList<>(capturedLogs);
        capturedLogs.clear();
        capturing = false;
        return logs;
    }
}
