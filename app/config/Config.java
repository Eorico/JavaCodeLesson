package app.config;

import app.constants.Constants;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Properties;

/** Reads config/app.properties, falling back to sensible development values. */
public final class Config {

    private final Properties values = new Properties();

    private Config(Properties values) {
        this.values.putAll(values);
    }

    public static Config load() {
        Properties loaded = new Properties();
        if (Files.exists(Constants.CONFIG_FILE)) {
            try (var reader =
                         Files.newBufferedReader(Constants.CONFIG_FILE, StandardCharsets.UTF_8)) {
                loaded.load(reader);
            } catch (IOException unreadable) {
                throw new UncheckedIOException("Could not read " + Constants.CONFIG_FILE,
                        unreadable);
            }
        }
        return new Config(loaded);
    }

    public static Config of(Map<String, String> entries) {
        Properties properties = new Properties();
        properties.putAll(entries);
        return new Config(properties);
    }

    public String get(String key, String fallback) {
        String value = values.getProperty(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(get(key, String.valueOf(fallback)));
        } catch (NumberFormatException notANumber) {
            return fallback;
        }
    }

    public boolean getBoolean(String key, boolean fallback) {
        return Boolean.parseBoolean(get(key, String.valueOf(fallback)));
    }

    /** True when the file exists and a real mail account has been filled in. */
    public boolean isMailConfigured() {
        return getBoolean("mail.enabled", false)
                && !get("mail.username", "").isBlank()
                && !get("mail.password", "").isBlank();
    }
}
