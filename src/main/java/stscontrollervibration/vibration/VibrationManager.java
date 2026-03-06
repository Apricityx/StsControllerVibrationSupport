package stscontrollervibration.vibration;

import basemod.BaseMod;
import basemod.ModLabel;
import basemod.ModLabeledButton;
import basemod.ModPanel;
import basemod.ModSlider;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.megacrit.cardcrawl.helpers.FontHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import stscontrollervibration.ControllerVibrationMod;
import stscontrollervibration.localization.LocalizationManager;
import stscontrollervibration.rumble.RumbleManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class VibrationManager {
    public static final String PLAYER_DAMAGED_ID = "player_damaged";
    public static final String ENEMY_HIT_ID = "enemy_hit";
    public static final String CARD_EXHAUSTED_ID = "card_exhausted";
    public static final String CARD_DISCARDED_ID = "card_discarded";
    public static final String CHARACTER_SELECTED_ID = "character_selected";
    public static final String EVENT_OPTION_NAVIGATED_ID = "event_option_navigated";
    public static final String EVENT_OPTION_SELECTED_ID = "event_option_selected";
    public static final String PLAYER_BLOCK_GAINED_ID = "player_block_gained";
    public static final String DAMAGE_BLOCKED_ID = "damage_blocked";
    public static final String CARD_UPGRADED_ID = "card_upgraded";
    public static final String CHEST_OPENED_ID = "chest_opened";

    private static final Logger logger = LogManager.getLogger(VibrationManager.class);
    private static final Color ID_COLOR = new Color(0.78f, 0.82f, 0.87f, 1.0f);
    private static final int MIN_STRENGTH_PERCENT = 0;
    private static final int MAX_STRENGTH_PERCENT = 100;
    private static final int LEGACY_MAX_STRENGTH_PERCENT = 200;
    private static final int CURRENT_CONFIG_VERSION = 5;
    private static final float SLIDER_MULTIPLIER = MAX_STRENGTH_PERCENT;
    private static final float LEGACY_ENEMY_HIT_SCALE = 0.58f;
    private static final float TITLE_X = 360.0f;
    private static final float TITLE_Y = 710.0f;
    private static final float NOTE_X = 360.0f;
    private static final float NOTE_Y = 660.0f;
    private static final float LABEL_X = 420.0f;
    private static final float SLIDER_X = 1080.0f;
    private static final float START_Y = 560.0f;
    private static final float ROW_SPACING = 96.0f;
    private static final float PAGE_LABEL_X = 780.0f;
    private static final float PAGE_LABEL_Y = 105.0f;
    private static final float PREV_BUTTON_X = 500.0f;
    private static final float RESET_BUTTON_X = 735.0f;
    private static final float NEXT_BUTTON_X = 1000.0f;
    private static final float PAGE_BUTTON_Y = 72.0f;
    private static final float HIDDEN_X = -4000.0f;
    private static final float HIDDEN_Y = -4000.0f;
    private static final int ROWS_PER_PAGE = 5;
    private static final String CONFIG_NAME = "vibration";
    private static final String CONFIG_VERSION_KEY = "config.version";
    private static final Map<String, Integer> LEGACY_DEFAULTS_V2 = createLegacyDefaultMap();
    private static final Map<String, Integer> LEGACY_DEFAULTS_V3 = createComfortDefaultMapV3();

    private static final Map<String, RegisteredVibration> registry = new LinkedHashMap<>();
    private static final List<ConfigRow> configRows = new ArrayList<>();

    private static SpireConfig config;
    private static Texture badgeTexture;
    private static ModLabel pageLabel;
    private static boolean builtInsRegistered = false;
    private static boolean panelRegistered = false;
    private static int currentPage = 0;

    private VibrationManager() {
    }

    public static void initialize() {
        LocalizationManager.initialize();
        ensureBuiltInsRegistered();
        ensureConfigLoaded();
        ensureConfigPanelRegistered();
    }

    public static void register(String id, String name, int defaultStrengthPercent, VibrationFactory factory) {
        register(id, name, defaultStrengthPercent, 0L, factory);
    }

    public static void register(String id, String name, int defaultStrengthPercent, long minIntervalMs, VibrationFactory factory) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Vibration id must not be empty.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Vibration name must not be empty.");
        }
        if (factory == null) {
            throw new IllegalArgumentException("Vibration factory must not be null.");
        }
        if (registry.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate vibration id: " + id);
        }

        RegisteredVibration vibration = new RegisteredVibration(
            id,
            name,
            clampPercent(defaultStrengthPercent),
            Math.max(0L, minIntervalMs),
            factory
        );
        registry.put(id, vibration);

        if (config != null) {
            loadConfiguredStrength(vibration);
        }
    }

    public static void trigger(String id) {
        trigger(id, 1);
    }

    public static void trigger(String id, int magnitude) {
        ensureBuiltInsRegistered();
        if (magnitude <= 0) {
            return;
        }

        RegisteredVibration vibration = registry.get(id);
        if (vibration == null) {
            logger.warn("Tried to trigger unknown vibration id {}", id);
            return;
        }

        float strengthScale = vibration.getStrengthScale();
        if (strengthScale <= 0.0f) {
            return;
        }

        long now = System.currentTimeMillis();
        if (!vibration.canTrigger(now)) {
            return;
        }

        RumbleSpec spec = vibration.factory.create(magnitude);
        if (spec == null) {
            return;
        }
        vibration.markTriggered(now);

        for (RumbleSpec.RumblePulse pulse : spec.pulses) {
            RumbleManager.queue(
                pulse.left * strengthScale,
                pulse.right * strengthScale,
                pulse.durationMs,
                pulse.delayMs
            );
        }
    }

    public static String describeRegisteredVibrations() {
        ensureBuiltInsRegistered();

        StringBuilder builder = new StringBuilder();
        for (RegisteredVibration vibration : registry.values()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder
                .append(vibration.id)
                .append("=")
                .append(vibration.name)
                .append("(")
                .append(vibration.strengthPercent)
                .append("%)");
        }
        return builder.toString();
    }

    private static void ensureBuiltInsRegistered() {
        if (builtInsRegistered) {
            return;
        }
        builtInsRegistered = true;

        register(
            PLAYER_DAMAGED_ID,
            LocalizationManager.text("vibration.player_damaged.name"),
            82,
            180L,
            magnitude -> {
                int clampedMagnitude = Math.min(magnitude, 30);
                float impact = clamp(0.42f + clampedMagnitude * 0.02f, 0.42f, 1.0f);
                float tail = clamp(0.26f + clampedMagnitude * 0.012f, 0.26f, 0.62f);
                long impactDurationMs = 60L + clampedMagnitude * 3L;
                long tailDurationMs = 55L + clampedMagnitude * 2L;
                return RumbleSpec.pattern(
                    RumbleSpec.pulse(impact * 0.55f, impact, impactDurationMs, 0L),
                    RumbleSpec.pulse(tail * 0.6f, tail, tailDurationMs, Math.max(40L, impactDurationMs - 10L))
                );
            }
        );
        register(
            ENEMY_HIT_ID,
            LocalizationManager.text("vibration.enemy_hit.name"),
            64,
            90L,
            magnitude -> {
                float cappedDamage = clamp(magnitude, 0.0f, 30.0f);
                float amplitude = 0.58f + 0.42f * (cappedDamage / 30.0f);
                float overflowRatio = clamp((magnitude - 30.0f) / 120.0f, 0.0f, 1.0f);
                long impactDurationMs = 82L;
                long reboundDurationMs = 58L;
                long overflowDurationMs = Math.round(overflowRatio * 1360.0f);
                if (overflowRatio <= 0.0f) {
                    return RumbleSpec.pattern(
                        RumbleSpec.pulse(amplitude * 0.62f, amplitude, impactDurationMs, 0L),
                        RumbleSpec.pulse(amplitude * 0.24f, amplitude * 0.44f, reboundDurationMs, 88L)
                    );
                }

                return RumbleSpec.pattern(
                    RumbleSpec.pulse(amplitude * 0.62f, amplitude, impactDurationMs, 0L),
                    RumbleSpec.pulse(amplitude * 0.3f, amplitude * 0.52f, reboundDurationMs, 88L),
                    RumbleSpec.pulse(amplitude * 0.24f, amplitude * 0.44f, overflowDurationMs, 150L)
                );
            }
        );
        register(
            CARD_EXHAUSTED_ID,
            LocalizationManager.text("vibration.card_exhausted.name"),
            48,
            140L,
            magnitude -> RumbleSpec.pattern(
                RumbleSpec.pulse(0.52f, 1.0f, 70L, 0L),
                RumbleSpec.pulse(0.24f, 0.46f, 70L, 80L)
            )
        );
        register(
            CARD_DISCARDED_ID,
            LocalizationManager.text("vibration.card_discarded.name"),
            32,
            80L,
            magnitude -> RumbleSpec.normalized(0.22f, 1.0f, 28L)
        );
        register(
            CHARACTER_SELECTED_ID,
            LocalizationManager.text("vibration.character_selected.name"),
            50,
            180L,
            magnitude -> RumbleSpec.normalized(0.5f, 1.0f, 60L)
        );
        register(
            EVENT_OPTION_NAVIGATED_ID,
            LocalizationManager.text("vibration.event_option_navigated.name"),
            24,
            70L,
            magnitude -> RumbleSpec.normalized(0.18f, 1.0f, 20L)
        );
        register(
            EVENT_OPTION_SELECTED_ID,
            LocalizationManager.text("vibration.event_option_selected.name"),
            46,
            160L,
            magnitude -> RumbleSpec.pattern(
                RumbleSpec.pulse(0.34f, 1.0f, 45L, 0L),
                RumbleSpec.pulse(0.14f, 0.42f, 40L, 55L)
            )
        );
        register(
            PLAYER_BLOCK_GAINED_ID,
            LocalizationManager.text("vibration.player_block_gained.name"),
            48,
            170L,
            magnitude -> {
                int clampedMagnitude = Math.min(magnitude, 30);
                float amplitude = clamp(0.48f + clampedMagnitude * 0.018f, 0.48f, 1.0f);
                return RumbleSpec.pattern(
                    RumbleSpec.pulse(amplitude, amplitude * 0.68f, 90L + clampedMagnitude * 3L, 0L),
                    RumbleSpec.pulse(amplitude * 0.42f, amplitude * 0.26f, 55L, 96L)
                );
            }
        );
        register(
            DAMAGE_BLOCKED_ID,
            LocalizationManager.text("vibration.damage_blocked.name"),
            54,
            120L,
            magnitude -> {
                int clampedMagnitude = Math.min(magnitude, 30);
                float amplitude = clamp(0.52f + clampedMagnitude * 0.016f, 0.52f, 1.0f);
                return RumbleSpec.pattern(
                    RumbleSpec.pulse(amplitude, amplitude * 0.74f, 72L + clampedMagnitude, 0L),
                    RumbleSpec.pulse(amplitude * 0.46f, amplitude * 0.24f, 52L, 82L)
                );
            }
        );
        register(
            CARD_UPGRADED_ID,
            LocalizationManager.text("vibration.card_upgraded.name"),
            78,
            1200L,
            magnitude -> RumbleSpec.pattern(
                RumbleSpec.pulse(0.54f, 0.86f, 58L, 150L),
                RumbleSpec.pulse(0.68f, 0.96f, 64L, 455L),
                RumbleSpec.pulse(0.86f, 1.0f, 92L, 745L)
            )
        );
        register(
            CHEST_OPENED_ID,
            LocalizationManager.text("vibration.chest_opened.name"),
            68,
            1000L,
            magnitude -> RumbleSpec.pattern(
                RumbleSpec.pulse(0.42f, 0.82f, 70L, 0L),
                RumbleSpec.pulse(0.62f, 1.0f, 95L, 95L),
                RumbleSpec.pulse(0.22f, 0.4f, 80L, 205L)
            )
        );
    }

    private static void ensureConfigLoaded() {
        if (config != null) {
            return;
        }

        Properties defaults = new Properties();
        for (RegisteredVibration vibration : registry.values()) {
            defaults.setProperty(vibration.getConfigKey(), Integer.toString(vibration.defaultStrengthPercent));
        }

        try {
            config = new SpireConfig(ControllerVibrationMod.MOD_ID, CONFIG_NAME, defaults);

            boolean dirty = migrateConfigIfNeeded();
            for (RegisteredVibration vibration : registry.values()) {
                dirty |= loadConfiguredStrength(vibration);
            }

            if (dirty) {
                config.save();
            }
        } catch (IOException ex) {
            logger.error("Failed to load vibration config", ex);
            config = null;
        }
    }

    private static boolean migrateConfigIfNeeded() {
        int originalVersion = readConfigVersion();
        int version = originalVersion;
        boolean dirty = false;
        if (version < 2) {
            dirty |= migrateStrengthKey(PLAYER_DAMAGED_ID, 1.0f);
            dirty |= migrateStrengthKey(ENEMY_HIT_ID, LEGACY_ENEMY_HIT_SCALE);
            version = 2;
        }
        if (version < 3) {
            dirty |= migrateComfortDefaults();
            version = 3;
        }
        if (version < 4) {
            dirty |= migrateTuningDefaultsV4();
            version = 4;
        }
        if (version < 5) {
            dirty |= migrateUpgradeDefaultV5();
            version = 5;
        }
        if (version != originalVersion) {
            config.setInt(CONFIG_VERSION_KEY, version);
            dirty = true;
        }
        return dirty;
    }

    private static int readConfigVersion() {
        if (config == null || !config.has(CONFIG_VERSION_KEY)) {
            return 0;
        }

        try {
            return config.getInt(CONFIG_VERSION_KEY);
        } catch (Exception ex) {
            logger.warn("Invalid vibration config version, treating as legacy", ex);
            return 0;
        }
    }

    private static boolean migrateStrengthKey(String vibrationId, float legacyScale) {
        RegisteredVibration vibration = registry.get(vibrationId);
        if (vibration == null || config == null) {
            return false;
        }

        String key = vibration.getConfigKey();
        if (!config.has(key)) {
            return false;
        }

        int oldPercent = config.getInt(key);
        int migratedPercent = clampPercent(Math.round(clamp(oldPercent, MIN_STRENGTH_PERCENT, LEGACY_MAX_STRENGTH_PERCENT) * legacyScale));
        if (migratedPercent == oldPercent) {
            return false;
        }

        config.setInt(key, migratedPercent);
        return true;
    }

    private static boolean migrateComfortDefaults() {
        boolean dirty = false;
        for (Map.Entry<String, Integer> entry : LEGACY_DEFAULTS_V2.entrySet()) {
            dirty |= migrateDefaultStrength(entry.getKey(), entry.getValue());
        }
        return dirty;
    }

    private static boolean migrateTuningDefaultsV4() {
        boolean dirty = false;
        for (Map.Entry<String, Integer> entry : LEGACY_DEFAULTS_V3.entrySet()) {
            dirty |= migrateDefaultStrength(entry.getKey(), entry.getValue());
        }
        return dirty;
    }

    private static boolean migrateUpgradeDefaultV5() {
        return migrateDefaultStrength(CARD_UPGRADED_ID, 62);
    }

    private static boolean migrateDefaultStrength(String vibrationId, int legacyDefaultStrength) {
        RegisteredVibration vibration = registry.get(vibrationId);
        if (vibration == null || config == null) {
            return false;
        }

        String key = vibration.getConfigKey();
        if (!config.has(key) || config.getInt(key) != legacyDefaultStrength) {
            return false;
        }

        if (legacyDefaultStrength == vibration.defaultStrengthPercent) {
            return false;
        }

        config.setInt(key, vibration.defaultStrengthPercent);
        return true;
    }

    private static boolean loadConfiguredStrength(RegisteredVibration vibration) {
        if (config == null) {
            vibration.strengthPercent = vibration.defaultStrengthPercent;
            return false;
        }

        String key = vibration.getConfigKey();
        if (!config.has(key)) {
            vibration.strengthPercent = vibration.defaultStrengthPercent;
            config.setInt(key, vibration.defaultStrengthPercent);
            return true;
        }

        int configured = clampPercent(config.getInt(key));
        vibration.strengthPercent = configured;
        if (configured != config.getInt(key)) {
            config.setInt(key, configured);
            return true;
        }
        return false;
    }

    private static void ensureConfigPanelRegistered() {
        if (panelRegistered) {
            return;
        }
        panelRegistered = true;

        BaseMod.registerModBadge(
            createBadgeTexture(),
            LocalizationManager.text("mod.badge.name"),
            "Codex",
            LocalizationManager.text("mod.badge.description"),
            new ModPanel(VibrationManager::populateConfigPanel)
        );
    }

    private static void populateConfigPanel(ModPanel panel) {
        configRows.clear();
        currentPage = 0;

        panel.addUIElement(new ModLabel(
            LocalizationManager.text("config.panel.title"),
            TITLE_X,
            TITLE_Y,
            Color.WHITE,
            FontHelper.buttonLabelFont,
            panel,
            label -> { }
        ));
        panel.addUIElement(new ModLabel(
            LocalizationManager.text("config.panel.note"),
            NOTE_X,
            NOTE_Y,
            ID_COLOR,
            FontHelper.charDescFont,
            panel,
            label -> { }
        ));

        for (RegisteredVibration vibration : registry.values()) {
            ModLabel nameLabel = new ModLabel(
                vibration.name,
                HIDDEN_X,
                HIDDEN_Y,
                Color.WHITE,
                FontHelper.buttonLabelFont,
                panel,
                label -> { }
            );
            panel.addUIElement(nameLabel);

            ModLabel idLabel = new ModLabel(
                LocalizationManager.text("config.panel.id", vibration.id),
                HIDDEN_X,
                HIDDEN_Y,
                ID_COLOR,
                FontHelper.charDescFont,
                panel,
                label -> { }
            );
            panel.addUIElement(idLabel);

            ModSlider slider = new ModSlider(
                "",
                HIDDEN_X,
                HIDDEN_Y,
                SLIDER_MULTIPLIER,
                "%",
                panel,
                modSlider -> updateStrength(vibration, Math.round(modSlider.value * modSlider.multiplier))
            );

            float normalized = vibration.strengthPercent / SLIDER_MULTIPLIER;
            slider.value = normalized;
            slider.setValue(normalized);
            panel.addUIElement(slider);
            configRows.add(new ConfigRow(vibration, nameLabel, idLabel, slider));
        }

        pageLabel = new ModLabel(
            "",
            PAGE_LABEL_X,
            PAGE_LABEL_Y,
            ID_COLOR,
            FontHelper.buttonLabelFont,
            panel,
            label -> { }
        );
        panel.addUIElement(pageLabel);
        panel.addUIElement(new ModLabeledButton(
            LocalizationManager.text("config.panel.prev"),
            PREV_BUTTON_X,
            PAGE_BUTTON_Y,
            panel,
            button -> {
                currentPage = Math.max(0, currentPage - 1);
                updateConfigPageLayout();
            }
        ));
        panel.addUIElement(new ModLabeledButton(
            LocalizationManager.text("config.panel.reset"),
            RESET_BUTTON_X,
            PAGE_BUTTON_Y,
            panel,
            button -> resetAllToDefaults()
        ));
        panel.addUIElement(new ModLabeledButton(
            LocalizationManager.text("config.panel.next"),
            NEXT_BUTTON_X,
            PAGE_BUTTON_Y,
            panel,
            button -> {
                currentPage = Math.min(getTotalPages() - 1, currentPage + 1);
                updateConfigPageLayout();
            }
        ));

        updateConfigPageLayout();
    }

    private static void updateStrength(RegisteredVibration vibration, int strengthPercent) {
        int clamped = clampPercent(strengthPercent);
        if (vibration.strengthPercent == clamped) {
            return;
        }

        vibration.strengthPercent = clamped;
        if (config == null) {
            return;
        }

        config.setInt(vibration.getConfigKey(), clamped);
        saveConfig();
    }

    private static Texture createBadgeTexture() {
        if (badgeTexture != null) {
            return badgeTexture;
        }

        Pixmap pixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.09f, 0.11f, 0.14f, 1.0f);
        pixmap.fill();
        pixmap.setColor(0.71f, 0.16f, 0.12f, 1.0f);
        pixmap.fillRectangle(6, 6, 52, 52);
        pixmap.setColor(0.95f, 0.89f, 0.67f, 1.0f);
        pixmap.fillRectangle(17, 29, 30, 6);
        pixmap.fillRectangle(29, 17, 6, 30);
        pixmap.setColor(0.09f, 0.11f, 0.14f, 1.0f);
        pixmap.drawRectangle(6, 6, 52, 52);

        badgeTexture = new Texture(pixmap);
        pixmap.dispose();
        return badgeTexture;
    }

    private static int clampPercent(int strengthPercent) {
        return Math.max(MIN_STRENGTH_PERCENT, Math.min(MAX_STRENGTH_PERCENT, strengthPercent));
    }

    private static void updateConfigPageLayout() {
        int totalPages = getTotalPages();
        currentPage = Math.max(0, Math.min(currentPage, totalPages - 1));

        for (int index = 0; index < configRows.size(); index++) {
            ConfigRow row = configRows.get(index);
            int page = index / ROWS_PER_PAGE;
            if (page != currentPage) {
                row.hide();
                continue;
            }

            int rowIndex = index % ROWS_PER_PAGE;
            float y = START_Y - rowIndex * ROW_SPACING;
            row.setY(y);
        }

        if (pageLabel != null) {
            pageLabel.text = LocalizationManager.text("config.panel.page", currentPage + 1, totalPages);
        }
    }

    private static int getTotalPages() {
        return Math.max(1, (configRows.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
    }

    private static void resetAllToDefaults() {
        boolean dirty = false;
        for (ConfigRow row : configRows) {
            dirty |= row.resetToDefault();
        }
        if (!dirty || config == null) {
            return;
        }

        for (RegisteredVibration vibration : registry.values()) {
            config.setInt(vibration.getConfigKey(), vibration.strengthPercent);
        }
        config.setInt(CONFIG_VERSION_KEY, CURRENT_CONFIG_VERSION);
        saveConfig();
    }

    private static void saveConfig() {
        if (config == null) {
            return;
        }

        try {
            config.save();
        } catch (IOException ex) {
            logger.error("Failed to save vibration config", ex);
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Map<String, Integer> createLegacyDefaultMap() {
        Map<String, Integer> defaults = new LinkedHashMap<>();
        defaults.put(PLAYER_DAMAGED_ID, 100);
        defaults.put(ENEMY_HIT_ID, 58);
        defaults.put(CARD_EXHAUSTED_ID, 72);
        defaults.put(CARD_DISCARDED_ID, 46);
        defaults.put(CHARACTER_SELECTED_ID, 70);
        defaults.put(EVENT_OPTION_SELECTED_ID, 68);
        defaults.put(PLAYER_BLOCK_GAINED_ID, 60);
        defaults.put(DAMAGE_BLOCKED_ID, 70);
        defaults.put(CARD_UPGRADED_ID, 76);
        defaults.put(CHEST_OPENED_ID, 82);
        return defaults;
    }

    private static Map<String, Integer> createComfortDefaultMapV3() {
        Map<String, Integer> defaults = new LinkedHashMap<>();
        defaults.put(PLAYER_DAMAGED_ID, 82);
        defaults.put(ENEMY_HIT_ID, 52);
        defaults.put(CARD_EXHAUSTED_ID, 48);
        defaults.put(CARD_DISCARDED_ID, 32);
        defaults.put(CHARACTER_SELECTED_ID, 50);
        defaults.put(EVENT_OPTION_NAVIGATED_ID, 24);
        defaults.put(EVENT_OPTION_SELECTED_ID, 46);
        defaults.put(PLAYER_BLOCK_GAINED_ID, 34);
        defaults.put(DAMAGE_BLOCKED_ID, 40);
        defaults.put(CARD_UPGRADED_ID, 62);
        defaults.put(CHEST_OPENED_ID, 68);
        return defaults;
    }

    @FunctionalInterface
    public interface VibrationFactory {
        RumbleSpec create(int magnitude);
    }

    private static final class RegisteredVibration {
        private final String id;
        private final String name;
        private final int defaultStrengthPercent;
        private final long minIntervalMs;
        private final VibrationFactory factory;
        private int strengthPercent;
        private long lastTriggeredAtMs;

        private RegisteredVibration(String id, String name, int defaultStrengthPercent, long minIntervalMs, VibrationFactory factory) {
            this.id = id;
            this.name = name;
            this.defaultStrengthPercent = defaultStrengthPercent;
            this.minIntervalMs = minIntervalMs;
            this.factory = factory;
            this.strengthPercent = defaultStrengthPercent;
        }

        private String getConfigKey() {
            return "strength." + id;
        }

        private float getStrengthScale() {
            return strengthPercent / 100.0f;
        }

        private boolean canTrigger(long nowMs) {
            return minIntervalMs <= 0L || nowMs - lastTriggeredAtMs >= minIntervalMs;
        }

        private void markTriggered(long nowMs) {
            lastTriggeredAtMs = nowMs;
        }
    }

    private static final class ConfigRow {
        private final RegisteredVibration vibration;
        private final ModLabel nameLabel;
        private final ModLabel idLabel;
        private final ModSlider slider;

        private ConfigRow(RegisteredVibration vibration, ModLabel nameLabel, ModLabel idLabel, ModSlider slider) {
            this.vibration = vibration;
            this.nameLabel = nameLabel;
            this.idLabel = idLabel;
            this.slider = slider;
        }

        private void setY(float y) {
            nameLabel.set(LABEL_X, y + 16.0f);
            idLabel.set(LABEL_X, y - 18.0f);
            slider.set(SLIDER_X, y);
        }

        private void hide() {
            nameLabel.set(HIDDEN_X, HIDDEN_Y);
            idLabel.set(HIDDEN_X, HIDDEN_Y);
            slider.set(HIDDEN_X, HIDDEN_Y);
        }

        private boolean resetToDefault() {
            if (vibration.strengthPercent == vibration.defaultStrengthPercent) {
                return false;
            }

            vibration.strengthPercent = vibration.defaultStrengthPercent;
            float normalized = vibration.strengthPercent / SLIDER_MULTIPLIER;
            slider.value = normalized;
            slider.setValue(normalized);
            return true;
        }
    }

    public static final class RumbleSpec {
        private final RumblePulse[] pulses;

        private RumbleSpec(RumblePulse... pulses) {
            this.pulses = pulses;
        }

        public static RumbleSpec of(float left, float right, long durationMs) {
            return pattern(pulse(left, right, durationMs, 0L));
        }

        public static RumbleSpec normalized(float left, float right, long durationMs) {
            float peak = Math.max(left, right);
            if (peak <= 0.0f) {
                return of(0.0f, 0.0f, durationMs);
            }

            return of(left / peak, right / peak, durationMs);
        }

        public static RumbleSpec pattern(RumblePulse... pulses) {
            return new RumbleSpec(pulses);
        }

        public static RumblePulse pulse(float left, float right, long durationMs, long delayMs) {
            return new RumblePulse(left, right, durationMs, delayMs);
        }

        public static final class RumblePulse {
            private final float left;
            private final float right;
            private final long durationMs;
            private final long delayMs;

            private RumblePulse(float left, float right, long durationMs, long delayMs) {
                this.left = left;
                this.right = right;
                this.durationMs = durationMs;
                this.delayMs = delayMs;
            }
        }
    }
}
