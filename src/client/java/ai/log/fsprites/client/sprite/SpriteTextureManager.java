package ai.log.fsprites.client.sprite;

import ai.log.fsprites.FancySprites;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages sprite texture caching and loading.
 */
public class SpriteTextureManager {
    private static final String SPRITES_ROOT = "fsprites";
    private final Map<String, Identifier> textureCache = new HashMap<>();
    private final Map<String, Identifier> interpolatedTextureCache = new HashMap<>();
    private final Map<String, int[]> frameSizeCache = new HashMap<>();
    private final Set<Identifier> runtimeTextureIds = new HashSet<>();
    private final TextureManager textureManager;
    private static SpriteTextureManager instance;

    private SpriteTextureManager(TextureManager textureManager) {
        this.textureManager = textureManager;
    }

    public static void initialize(TextureManager textureManager) {
        instance = new SpriteTextureManager(textureManager);

    }

    public static SpriteTextureManager getInstance() {
        return instance;
    }

    /**
     * Get texture resource location for a sprite frame.
     * Format: fsprites/{sprite-id}/{frame-number}.png (filesystem root).
     */
    public Identifier getFrameTexture(String spriteId, int frameNumber) {
        String key = spriteId + ":" + frameNumber;

        Identifier cached = textureCache.get(key);
        if (cached != null) {
            return cached;
        }

        Path spritesRoot = resolveSpritesRoot();
        Path framePath = spritesRoot.resolve(spriteId).resolve(frameNumber + ".png");
        if (Files.exists(framePath)) {
            Identifier runtimeId = Identifier.fromNamespaceAndPath(
                    FancySprites.MOD_ID,
                    "runtime/fsprites/" + sanitizePath(spriteId) + "/" + frameNumber
            );

            try (InputStream stream = Files.newInputStream(framePath)) {
                NativeImage image = NativeImage.read(stream);
                frameSizeCache.put(key, new int[]{image.getWidth(), image.getHeight()});
                textureManager.register(runtimeId, new DynamicTexture(() -> "FancySprites " + spriteId + "/" + frameNumber, image));
                runtimeTextureIds.add(runtimeId);
                textureCache.put(key, runtimeId);
                return runtimeId;
            } catch (Exception e) {
                FancySprites.LOGGER.warn("Failed to load sprite frame from file {}: {}", framePath, e.getMessage());
            }
        }

        FancySprites.LOGGER.warn("Sprite frame not found on filesystem: {}", framePath.toAbsolutePath());
        return MissingTextureAtlasSprite.getLocation();
    }

    /**
     * Get a preblended intermediate texture for animation interpolation.
     */
    public Identifier getInterpolatedFrameTexture(String spriteId, int currentFrameNumber, int nextFrameNumber, float factor) {
        if (factor <= 0.0f) {
            return getFrameTexture(spriteId, currentFrameNumber);
        }

        int bucket = Math.max(0, Math.min(31, Math.round(factor * 31.0f)));
        String key = spriteId + ":" + currentFrameNumber + ":" + nextFrameNumber + ":" + bucket;

        Identifier cached = interpolatedTextureCache.get(key);
        if (cached != null) {
            return cached;
        }

        Path spritesRoot = resolveSpritesRoot();
        Path currentPath = spritesRoot.resolve(spriteId).resolve(currentFrameNumber + ".png");
        Path nextPath = spritesRoot.resolve(spriteId).resolve(nextFrameNumber + ".png");

        if (!Files.exists(currentPath) || !Files.exists(nextPath)) {
            return getFrameTexture(spriteId, currentFrameNumber);
        }

        NativeImage currentImage = null;
        NativeImage nextImage = null;
        try (InputStream currentStream = Files.newInputStream(currentPath);
             InputStream nextStream = Files.newInputStream(nextPath)) {
            currentImage = NativeImage.read(currentStream);
            nextImage = NativeImage.read(nextStream);

            int width = Math.min(currentImage.getWidth(), nextImage.getWidth());
            int height = Math.min(currentImage.getHeight(), nextImage.getHeight());
            if (width <= 0 || height <= 0) {
                return getFrameTexture(spriteId, currentFrameNumber);
            }

            float mix = bucket / 31.0f;
            NativeImage blendedImage = new NativeImage(width, height, true);

            int[] currentPixels = currentImage.getPixelsABGR();
            int[] nextPixels = nextImage.getPixelsABGR();
            int currentWidth = currentImage.getWidth();
            int nextWidth = nextImage.getWidth();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int currentPixel = currentPixels[y * currentWidth + x];
                    int nextPixel = nextPixels[y * nextWidth + x];
                    blendedImage.setPixelABGR(x, y, blendAbgr(currentPixel, nextPixel, mix));
                }
            }

            Identifier runtimeId = Identifier.fromNamespaceAndPath(
                    FancySprites.MOD_ID,
                    "runtime/fsprites/interpolated/" + sanitizePath(spriteId) + "/" + currentFrameNumber + "_" + nextFrameNumber + "_" + bucket
            );

            textureManager.register(runtimeId, new DynamicTexture(() -> "FancySprites " + spriteId + " interpolated", blendedImage));
            runtimeTextureIds.add(runtimeId);
            interpolatedTextureCache.put(key, runtimeId);
            frameSizeCache.put(spriteId + ":" + currentFrameNumber, new int[]{width, height});
            return runtimeId;
        } catch (Exception e) {
            FancySprites.LOGGER.warn("Failed to build interpolated texture for {}: {}", spriteId, e.getMessage());
            return getFrameTexture(spriteId, currentFrameNumber);
        } finally {
            if (currentImage != null) {
                currentImage.close();
            }
            if (nextImage != null) {
                nextImage.close();
            }
        }
    }

    /**
     * Preload a frame texture into the runtime cache.
     */
    public void preloadFrameTexture(String spriteId, int frameNumber) {
        getFrameTexture(spriteId, frameNumber);
    }

    /**
     * Preload all frame textures referenced by a sprite animation.
     */
    public void preloadSpriteFrames(String spriteId, SpriteAnimation animation) {
        if (animation == null || animation.frames == null || animation.frames.isEmpty()) {
            preloadFrameTexture(spriteId, 0);
            return;
        }

        for (AnimationFrame frame : animation.frames) {
            if (frame != null) {
                preloadFrameTexture(spriteId, frame.index);
            }
        }
    }

    public int[] getFrameSize(String spriteId, int frameNumber) {
        String key = spriteId + ":" + frameNumber;
        int[] cached = frameSizeCache.get(key);
        if (cached != null) {
            return new int[]{cached[0], cached[1]};
        }

        Path framePath = resolveSpritesRoot().resolve(spriteId).resolve(frameNumber + ".png");
        if (!Files.exists(framePath)) {
            return null;
        }

        try (InputStream stream = Files.newInputStream(framePath)) {
            NativeImage image = NativeImage.read(stream);
            int[] size = new int[]{image.getWidth(), image.getHeight()};
            frameSizeCache.put(key, size);
            image.close();
            return new int[]{size[0], size[1]};
        } catch (Exception e) {
            FancySprites.LOGGER.warn("Failed to read sprite frame size {}: {}", framePath, e.getMessage());
            return null;
        }
    }

    /**
     * Clear texture cache.
     */
    public void clear() {
        for (Identifier textureId : runtimeTextureIds) {
            textureManager.release(textureId);
        }
        runtimeTextureIds.clear();
        textureCache.clear();
        interpolatedTextureCache.clear();
        frameSizeCache.clear();
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

    private static String sanitizePath(String value) {
        return value.toLowerCase().replaceAll("[^a-z0-9/_\\.-]", "_");
    }

    private static int blendChannel(int current, int next, float factor) {
        return Math.round(current * (1.0f - factor) + next * factor);
    }

    private static int blendAbgr(int currentPixel, int nextPixel, float factor) {
        int currentA = (currentPixel >>> 24) & 0xFF;
        int currentB = (currentPixel >>> 16) & 0xFF;
        int currentG = (currentPixel >>> 8) & 0xFF;
        int currentR = currentPixel & 0xFF;

        int nextA = (nextPixel >>> 24) & 0xFF;
        int nextB = (nextPixel >>> 16) & 0xFF;
        int nextG = (nextPixel >>> 8) & 0xFF;
        int nextR = nextPixel & 0xFF;

        int a = blendChannel(currentA, nextA, factor);
        int b = blendChannel(currentB, nextB, factor);
        int g = blendChannel(currentG, nextG, factor);
        int r = blendChannel(currentR, nextR, factor);

        return (a << 24) | (b << 16) | (g << 8) | r;
    }
}
