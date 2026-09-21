package ru.homework.printer;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupLog {
    private final ObjectProvider<BuildProperties> build;

    public StartupLog(ObjectProvider<BuildProperties> build) {
        this.build = build;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logVersion() {
        BuildProperties info = build.getIfAvailable();
        LoggerFactory.getLogger(StartupLog.class).info("Application=rate-printer version={} java={}",
                info == null ? "dev" : info.getVersion(), System.getProperty("java.version"));
    }
}
