package ai.log.fsprites.client.sprite;

import ai.log.fsprites.FancySprites;
import ai.log.fsprites.client.SpriteRenderingManager;
import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Helper class for registering sprites from resource files.
 * Provides utility methods for loading .mcmeta files.
 */
public class SpriteRegistry {
    private static final String SPRITES_ROOT = "fsprites";

    /**
     * Load a sprite from filesystem metadata file.
     * Expects file at: fsprites/{sprite-id}.mcmeta (or run/fsprites/{sprite-id}.mcmeta in dev).
     */
    public static void registerSpriteFromResource(String spriteId) {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client == null) {
                FancySprites.LOGGER.warn("Cannot register sprite {}: Client not ready", spriteId);
                return;
            }

            Path spritesRoot = resolveSpritesRoot();
            Path metadataPath = spritesRoot.resolve(spriteId + ".mcmeta");
            if (!Files.exists(metadataPath)) {
                FancySprites.LOGGER.warn("Sprite metadata not found: {}", metadataPath.toAbsolutePath());
                return;
            }

            String content = Files.readString(metadataPath);
            SpriteManager.getInstance().loadSpriteFromMetadata(spriteId, content);
        } catch (Exception e) {
            FancySprites.LOGGER.error("Failed to register sprite {}: {}", spriteId, e.getMessage(), e);
        }
    }

    /**
     * Load all sprites from filesystem directory.
     * Looks for all .mcmeta files in fsprites/ (or run/fsprites in dev).
     */
    public static void registerSpritesFromDirectory() {
        List<String> spriteIds = discoverSpriteIds();
        if (spriteIds.isEmpty()) {
            FancySprites.LOGGER.warn("No sprite metadata files found in {}", resolveSpritesRoot().toAbsolutePath());
            return;
        }

        registerSprites(spriteIds.toArray(new String[0]));
    }

    /**
     * Register multiple sprites by ID.
     */
    public static void registerSprites(String... spriteIds) {
        for (String spriteId : spriteIds) {
            registerSpriteFromResource(spriteId);
        }
    }

    /**
     * Reload the known sprite resources and clear cached render state.
     */
    public static int reloadRegisteredSprites() {
        SpriteRenderingManager.clear();

        SpriteManager.getInstance().clear();

        SpriteTextureManager textureManager = SpriteTextureManager.getInstance();
        if (textureManager != null) {
            textureManager.clear();
        }

        List<String> spriteIds = discoverSpriteIds();
        registerSprites(spriteIds.toArray(new String[0]));
        return spriteIds.size();
    }

    private static List<String> discoverSpriteIds() {
        Path spritesRoot = resolveSpritesRoot();
        if (!Files.isDirectory(spritesRoot)) {
            return List.of();
        }

        List<String> ids = new ArrayList<>();
        try (var files = Files.list(spritesRoot)) {
            files.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".mcmeta"))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        ids.add(fileName.substring(0, fileName.length() - ".mcmeta".length()));
                    });
        } catch (Exception e) {
            FancySprites.LOGGER.error("Failed to scan sprite directory {}: {}", spritesRoot, e.getMessage(), e);
        }

        ids.sort(String::compareTo);
        return ids;
    }

    private static Path resolveSpritesRoot() {
        Path direct = Path.of(SPRITES_ROOT);
        if (Files.isDirectory(direct)) {
            return direct;
        }

        Path devRun = Path.of("run", SPRITES_ROOT);
        if (Files.isDirectory(devRun)) {
            return devRun;
        }

        return direct;
    }

    /**
     * Helper: Create a simple static sprite config.
     */
    public static SpriteConfig createStaticSprite(String id, SpriteGui gui, SpriteAnchor anchor) {
        return new SpriteConfig(
                true, id, true, 1.0f, 0,
                SpriteBlend.NORMAL, gui, List.of(gui.id), anchor,
                new int[]{0, 0}, false, null, null, false, false,
                1.0f, SpriteFit.STRETCH, 0,
                new int[]{0, 0}, null, "", "Без категории",
                "fsprite/textures/" + id
        );
    }

    /**
     * Helper: Create an animated sprite config.
     */
    public static SpriteConfig createAnimatedSprite(String id, SpriteGui gui, 
                                                     SpriteAnchor anchor,
                                                     int frameCount, int frametime) {
        List<AnimationFrame> frames = new ArrayList<>();
        for (int i = 0; i < frameCount; i++) {
            frames.add(new AnimationFrame(i, frametime));
        }

        SpriteAnimation animation = new SpriteAnimation(false, frametime, frames);

        return new SpriteConfig(
                true, id, true, 1.0f, 0,
            SpriteBlend.NORMAL, gui, List.of(gui.id), anchor,
            new int[]{0, 0}, false, null, null, false, false,
            1.0f, SpriteFit.STRETCH, 0,
            new int[]{0, 0}, animation, "", "Без категории",
                "fsprite/textures/" + id
        );
    }

    /**
     * Helper: Create a sprite with custom position and scale.
     */
    public static SpriteConfig createCustomSprite(String id, SpriteGui gui,
                                                   SpriteAnchor anchor, int[] pos,
                                                   float scale, float opacity) {
        return new SpriteConfig(
                true, id, true, opacity, 0,
            SpriteBlend.NORMAL, gui, List.of(gui.id), anchor,
            pos, false, null, null, false, false,
            scale, SpriteFit.STRETCH, 0,
            new int[]{0, 0}, null, "", "Без категории",
                "fsprite/textures/" + id
        );
    }
}
