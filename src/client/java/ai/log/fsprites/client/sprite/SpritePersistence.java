package ai.log.fsprites.client.sprite;

import ai.log.fsprites.FancySprites;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles persistence of sprite enable/disable state and settings.
 */
public class SpritePersistence {
    private static final String SETTINGS_FILE = "fancysprites_settings.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static SpritePersistence instance;
    private Map<String, Boolean> spriteStates = new HashMap<>();
    private Path settingsPath;

    private SpritePersistence() {
        initialize();
    }

    public static SpritePersistence getInstance() {
        if (instance == null) {
            instance = new SpritePersistence();
        }
        return instance;
    }

    private void initialize() {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.gameDirectory != null) {
                settingsPath = client.gameDirectory.toPath().resolve(SETTINGS_FILE);
                loadSettings();
            }
        } catch (Exception e) {
            FancySprites.LOGGER.warn("Failed to initialize sprite persistence: {}", e.getMessage());
        }
    }

    private void loadSettings() {
        try {
            if (settingsPath != null && Files.exists(settingsPath)) {
                try (FileReader reader = new FileReader(settingsPath.toFile())) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    if (json != null && json.has("sprites")) {
                        JsonObject spritesJson = json.getAsJsonObject("sprites");
                        for (String key : spritesJson.keySet()) {
                            spriteStates.put(key, spritesJson.get(key).getAsBoolean());
                        }
                    }
                }
                FancySprites.LOGGER.debug("Loaded sprite settings from {}", settingsPath);
            }
        } catch (Exception e) {
            FancySprites.LOGGER.warn("Failed to load sprite settings: {}", e.getMessage());
        }
    }

    public void saveSettings() {
        try {
            if (settingsPath == null) {
                return;
            }

            JsonObject json = new JsonObject();
            JsonObject spritesJson = new JsonObject();
            
            for (Map.Entry<String, Boolean> entry : spriteStates.entrySet()) {
                spritesJson.addProperty(entry.getKey(), entry.getValue());
            }
            
            json.add("sprites", spritesJson);

            Files.createDirectories(settingsPath.getParent());
            try (FileWriter writer = new FileWriter(settingsPath.toFile())) {
                GSON.toJson(json, writer);
            }
            
            FancySprites.LOGGER.debug("Saved sprite settings to {}", settingsPath);
        } catch (Exception e) {
            FancySprites.LOGGER.warn("Failed to save sprite settings: {}", e.getMessage());
        }
    }

    /**
     * Check if a sprite is enabled. Default is true if not explicitly disabled.
     */
    public boolean isEnabled(String spriteId) {
        return spriteStates.getOrDefault(spriteId, true);
    }

    /**
     * Set sprite enabled state.
     */
    public void setEnabled(String spriteId, boolean enabled) {
        spriteStates.put(spriteId, enabled);
        saveSettings();
    }

    /**
     * Toggle sprite enabled state.
     */
    public void toggleEnabled(String spriteId) {
        setEnabled(spriteId, !isEnabled(spriteId));
    }

    /**
     * Get all sprite states.
     */
    public Map<String, Boolean> getAllStates() {
        return new HashMap<>(spriteStates);
    }
}
