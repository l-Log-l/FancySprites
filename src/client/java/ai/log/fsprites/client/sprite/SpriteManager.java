package ai.log.fsprites.client.sprite;

import ai.log.fsprites.FancySprites;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Manages loading and storing sprite configurations.
 */
public class SpriteManager {
    private static final String SPRITES_DIR = "fsprite";
    private final Map<String, SpriteConfig> sprites = new HashMap<>();
    private static SpriteManager instance;

    private SpriteManager() {
        loadSprites();
    }

    public static SpriteManager getInstance() {
        if (instance == null) {
            instance = new SpriteManager();
        }
        return instance;

    }

    public static void reload() {
        instance = new SpriteManager();
    }

    private void loadSprites() {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client == null || client.getResourceManager() == null) {
                FancySprites.LOGGER.debug("Resource manager not ready yet");
                return;
            }

            // Try to find all .mcmeta files in the fsprite directory
            // We'll scan through the resourcepack to find sprite definitions
            loadSpritesFromResources();
        } catch (Exception e) {
            FancySprites.LOGGER.error("Failed to load sprites: {}", e.getMessage(), e);
        }
    }

    private void loadSpritesFromResources() {
        try {
            Minecraft client = Minecraft.getInstance();
            Identifier spritesDir = Identifier.fromNamespaceAndPath(FancySprites.MOD_ID, SPRITES_DIR);
            
            // This is a simplified approach - in practice, resource scanning is limited
            // We'll need to manually define sprites or use a different approach
            FancySprites.LOGGER.info("Sprite manager initialized. Ready to load sprite metadata.");
        } catch (Exception e) {
            FancySprites.LOGGER.warn("Could not scan sprite directory: {}", e.getMessage());
        }
    }

    /**
     * Load a sprite from a metadata JSON file.
     */
    public void loadSpriteFromMetadata(String spriteId, String metadataContent) {
        try {
            JsonObject json = SpriteMetadataParser.parseJsonFile(metadataContent);
            SpriteConfig config = SpriteMetadataParser.parse(json, spriteId, SPRITES_DIR + "/textures");
            
            if (config != null && config.enabled) {
                sprites.put(spriteId, config);
                SpriteTextureManager textureManager = SpriteTextureManager.getInstance();
                if (textureManager != null) {
                    textureManager.preloadSpriteFrames(spriteId, config.animation);
                }
                FancySprites.LOGGER.info("Loaded sprite: {} (GUI: {})", spriteId, config.gui.id);
            }
        } catch (Exception e) {
            FancySprites.LOGGER.error("Failed to load sprite {}: {}", spriteId, e.getMessage());
        }
    }

    /**
     * Get a sprite by ID.
     */
    public SpriteConfig getSprite(String id) {
        return sprites.get(id);
    }

    /**
     * Get all sprites for a specific GUI screen.
     */
    public List<SpriteConfig> getSpritesForGui(String guiId) {
        List<SpriteConfig> result = new ArrayList<>();
        for (SpriteConfig sprite : sprites.values()) {
            if (sprite.matchesGui(guiId)) {
                result.add(sprite);
            }
        }
        // Sort by z_index for proper rendering order
        result.sort(Comparator.comparingInt(s -> s.zIndex));
        return result;
    }

    /**
     * Get all loaded sprites.
     */
    public Collection<SpriteConfig> getAllSprites() {
        return sprites.values();
    }

    /**
     * Register a sprite.
     */
    public void registerSprite(SpriteConfig config) {
        if (config != null && config.enabled && config.id != null) {
            sprites.put(config.id, config);
            SpriteTextureManager textureManager = SpriteTextureManager.getInstance();
            if (textureManager != null) {
                textureManager.preloadSpriteFrames(config.id, config.animation);
            }
        }
    }

    /**
     * Clear all sprites.
     */
    public void clear() {
        sprites.clear();
    }
}
