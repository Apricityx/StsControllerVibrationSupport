package stscontrollervibration.localization;

import com.megacrit.cardcrawl.core.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Properties;

public final class LocalizationManager {
    private static final Logger logger = LogManager.getLogger(LocalizationManager.class);
    private static final String ENGLISH_RESOURCE = "localization/strings_en.properties";
    private static final String CHINESE_RESOURCE = "localization/strings_zh.properties";

    private static Properties englishStrings;
    private static Properties activeStrings;
    private static Settings.GameLanguage activeLanguage;

    private LocalizationManager() {
    }

    public static void initialize() {
        if (englishStrings == null) {
            englishStrings = load(ENGLISH_RESOURCE);
        }

        Settings.GameLanguage language = Settings.language;
        if (activeStrings != null && language == activeLanguage) {
            return;
        }

        activeLanguage = language;
        activeStrings = load(resolveResource(language));
        logger.info("Loaded localization language={} resource={}", language, resolveResource(language));
    }

    public static String text(String key, Object... args) {
        initialize();

        String value = activeStrings.getProperty(key);
        if (value == null) {
            value = englishStrings.getProperty(key, key);
        }

        if (args == null || args.length == 0) {
            return value;
        }
        return MessageFormat.format(value, args);
    }

    private static String resolveResource(Settings.GameLanguage language) {
        if (language == Settings.GameLanguage.ZHS || language == Settings.GameLanguage.ZHT) {
            return CHINESE_RESOURCE;
        }
        return ENGLISH_RESOURCE;
    }

    private static Properties load(String resourcePath) {
        Properties properties = new Properties();
        ClassLoader classLoader = LocalizationManager.class.getClassLoader();

        try (InputStream stream = classLoader.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IOException("Missing resource " + resourcePath);
            }
            properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return properties;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load localization resource " + resourcePath, ex);
        }
    }
}
