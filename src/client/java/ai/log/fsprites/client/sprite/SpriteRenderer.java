package ai.log.fsprites.client.sprite;

import ai.log.fsprites.FancySprites;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2fStack;

/**
 * Renders sprites with support for all configured properties.
 *
 * opacity:     ARGB tint passed directly into blit() — works in MC 1.21+
 *              (RenderSystem.setShaderColor was removed in this version)
 * interpolate: two blit() calls with complementary alpha values
 * blend:       per-blend RenderPipeline — no GL11 state hacks needed
 */
public class SpriteRenderer {
    private static final boolean DEBUG_RENDER = false;
    private static long lastDebugLogMs = 0L;

    public static void renderSprite(GuiGraphics guiGraphics, SpriteConfig sprite,
                                    SpriteAnimationPlayer animationPlayer,
                                    int screenWidth, int screenHeight,
                                    int anchorX, int anchorY,
                                    int targetWidth, int targetHeight) {
        try {
            AnimationFrame currentFrame = animationPlayer.getCurrentFrame();
            AnimationFrame previousFrame = getPreviousFrame(sprite, animationPlayer);
            boolean visible = currentFrame != null && currentFrame.visible != null
                ? currentFrame.visible : sprite.visible;
            if (!visible || sprite.opacity <= 0.0f) return;

            float progress = animationPlayer.getCurrentFrameProgress();
            float movementT = applyEasing(currentFrame, progress, true, false);
            float rotationT = applyEasing(currentFrame, progress, false, true);
            float scaleT = applyEasing(currentFrame, progress, true, true);
            float sizeT = applyEasing(currentFrame, progress, true, false);

            int[] pos = interpolatePoint(previousFrame != null ? previousFrame.pos : null,
                currentFrame != null ? currentFrame.pos : null, sprite.pos, movementT);
                int rotation = interpolateInt(previousFrame != null ? previousFrame.rot : null,
                    currentFrame != null ? currentFrame.rot : null, sprite.rotation, rotationT);
            float scale = Math.max(0.0f, interpolateFloat(previousFrame != null ? previousFrame.scale : null,
                currentFrame != null ? currentFrame.scale : null, sprite.scale, scaleT));
            int[] origin = currentFrame != null && currentFrame.origin != null ? currentFrame.origin : sprite.origin;
            SpriteBlend blend = currentFrame != null && currentFrame.blend != null ? currentFrame.blend : sprite.blend;
            RenderPipeline pipeline = blend.getPipeline();

            int frameIndex = currentFrame != null ? currentFrame.index : animationPlayer.getCurrentFrameIndex();
            SpriteTextureManager textureManager = SpriteTextureManager.getInstance();

            boolean interpolate = sprite.animation != null && sprite.animation.interpolate;
            float interpFactor = interpolate ? animationPlayer.getInterpolationFactor() : 0.0f;

            int nextFrameIndex = frameIndex;
            if (interpolate && interpFactor > 0.0f && sprite.animation != null && sprite.animation.getFrameCount() > 1) {
            int rawNext = (animationPlayer.getCurrentFrameIndex() + 1) % sprite.animation.getFrameCount();
            AnimationFrame nextFrame = sprite.animation.getFrame(rawNext);
            nextFrameIndex = nextFrame != null ? nextFrame.index : rawNext;
            }

            Identifier texture = interpolate && interpFactor > 0.0f && textureManager != null
                ? textureManager.getInterpolatedFrameTexture(sprite.id, frameIndex, nextFrameIndex, interpFactor)
                : textureManager.getFrameTexture(sprite.id, frameIndex);
            int[] frameSize = textureManager.getFrameSize(sprite.id, frameIndex);
            int textureWidth  = frameSize != null ? frameSize[0] : 16;
            int textureHeight = frameSize != null ? frameSize[1] : 16;

            int sourceWidth = interpolateInt(previousFrame != null ? previousFrame.width : null,
                currentFrame != null ? currentFrame.width : null,
                sprite.fullWidth ? Math.max(1, targetWidth) : sprite.width != null ? sprite.width : textureWidth,
                sizeT);
            int sourceHeight = interpolateInt(previousFrame != null ? previousFrame.height : null,
                currentFrame != null ? currentFrame.height : null,
                sprite.fullHeight ? Math.max(1, targetHeight) : sprite.height != null ? sprite.height : textureHeight,
                sizeT);

            // --- Position ---
            int spriteX = sprite.screenSpace ? pos[0] + (screenWidth  / 2) : anchorX + pos[0];
            int spriteY = sprite.screenSpace ? pos[1] + (screenHeight / 2) : anchorY + pos[1];

            int originX = origin != null && origin.length >= 2 ? origin[0] : 0;
            int originY = origin != null && origin.length >= 2 ? origin[1] : 0;

            int offsetX = (int) (-sprite.anchor.x * sourceWidth);
            int offsetY = (int) (-sprite.anchor.y * sourceHeight);

            final int renderX = spriteX + offsetX;
            final int renderY = spriteY + offsetY;

            // --- Opacity via ARGB tint ---
            // In MC 1.21+ GuiGraphics.blit() accepts an ARGB int as the last argument.
            float baseOpacity = Math.min(1.0f, Math.max(0.0f, sprite.opacity));
            int currentAlpha = Math.round(baseOpacity * 255);
            int currentColor = ARGB.colorFromFloat(baseOpacity, 1.0f, 1.0f, 1.0f);

            if (DEBUG_RENDER) {
                long now = System.currentTimeMillis();
                if (now - lastDebugLogMs > 1000L) {
                    boolean exists = false;
                    try {
                        var client = net.minecraft.client.Minecraft.getInstance();
                        if (client != null && client.getResourceManager() != null) {
                            exists = client.getResourceManager().getResource(texture).isPresent();
                        }
                    } catch (Exception ignored) {}
                    FancySprites.LOGGER.info(
                            "[FancySprites DEBUG] sprite={} frame={} texture={} exists={} " +
                                    "draw=({},{}) src={}x{} scale={} rot={} origin=({},{}) pos=({},{}) " +
                                        "opacity={} blend={} interpFactor={} currentAlpha={}",
                            sprite.id, frameIndex, texture, exists,
                            renderX, renderY, sourceWidth, sourceHeight,
                            scale, rotation, originX, originY, pos[0], pos[1],
                                    sprite.opacity, blend.id, interpFactor, currentAlpha
                    );
                    lastDebugLogMs = now;
                }
            }

            Matrix3x2fStack pose = guiGraphics.pose();
            pose.pushMatrix();
            try {
                pose.translate(renderX, renderY);
                if (rotation != 0) {
                    pose.rotateAbout((float) Math.toRadians(rotation), originX, originY);
                }
                if (scale != 1.0f) {
                    pose.scaleAround(scale, scale, originX, originY);
                }

                // Draw current or preblended frame.
                if (currentAlpha > 0) {
                    guiGraphics.blit(
                            pipeline,
                            texture,
                            0, 0,
                            0.0f, 0.0f,
                            sourceWidth, sourceHeight,
                            sourceWidth, sourceHeight,
                            currentColor
                    );
                }
            } finally {
                pose.popMatrix();
            }
        } catch (Exception e) {
            FancySprites.LOGGER.warn("Failed to render sprite {}: {}", sprite.id, e.getMessage());
        }
    }

    private static AnimationFrame getPreviousFrame(SpriteConfig sprite, SpriteAnimationPlayer animationPlayer) {
        if (sprite.animation == null || sprite.animation.getFrameCount() <= 1) {
            return null;
        }

        int currentIndex = animationPlayer.getCurrentFrameIndex();
        int previousIndex = (currentIndex - 1 + sprite.animation.getFrameCount()) % sprite.animation.getFrameCount();
        return sprite.animation.getFrame(previousIndex);
    }

    private static float applyEasing(AnimationFrame currentFrame, float progress, boolean useMoveEasing, boolean useRotEasing) {
        SpriteEasing easing = SpriteEasing.LINEAR;
        if (currentFrame != null) {
            if (useRotEasing && currentFrame.rotEasing != null) {
                easing = currentFrame.rotEasing;
            } else if (useMoveEasing && currentFrame.moveEasing != null) {
                easing = currentFrame.moveEasing;
            } else if (currentFrame.easing != null) {
                easing = currentFrame.easing;
            }
        }
        return easing.apply(progress);
    }

    private static float interpolateFloat(Float previousValue, Float currentValue, float fallbackValue, float t) {
        float start = previousValue != null ? previousValue : fallbackValue;
        float end = currentValue != null ? currentValue : fallbackValue;
        return start + (end - start) * t;
    }

    private static int interpolateInt(Integer previousValue, Integer currentValue, int fallbackValue, float t) {
        int start = previousValue != null ? previousValue : fallbackValue;
        int end = currentValue != null ? currentValue : fallbackValue;
        return Math.round(start + (end - start) * t);
    }

    private static int[] interpolatePoint(int[] previousValue, int[] currentValue, int[] fallbackValue, float t) {
        int startX = previousValue != null && previousValue.length >= 2 ? previousValue[0] : fallbackValue[0];
        int startY = previousValue != null && previousValue.length >= 2 ? previousValue[1] : fallbackValue[1];
        int endX = currentValue != null && currentValue.length >= 2 ? currentValue[0] : fallbackValue[0];
        int endY = currentValue != null && currentValue.length >= 2 ? currentValue[1] : fallbackValue[1];
        return new int[]{Math.round(startX + (endX - startX) * t), Math.round(startY + (endY - startY) * t)};
    }

    public static void renderSpriteTiled(GuiGraphics guiGraphics, SpriteConfig sprite,
                                         SpriteAnimationPlayer animationPlayer,
                                         int screenWidth, int screenHeight,
                                         int anchorX, int anchorY,
                         int maxWidth, int maxHeight) {
        AnimationFrame currentFrame = animationPlayer.getCurrentFrame();
        SpriteFit effectiveFit = currentFrame != null && currentFrame.fit != null
                ? currentFrame.fit : sprite.fit;

        if (effectiveFit == SpriteFit.STRETCH) {
            renderSprite(guiGraphics, sprite, animationPlayer, screenWidth, screenHeight, anchorX, anchorY, maxWidth, maxHeight);
            return;
        }

        if (effectiveFit == SpriteFit.TILE) {
            int frameIndex = currentFrame != null ? currentFrame.index : animationPlayer.getCurrentFrameIndex();
            SpriteTextureManager textureManager = SpriteTextureManager.getInstance();
            Identifier texture = textureManager.getFrameTexture(sprite.id, frameIndex);

            int[] frameSize = textureManager.getFrameSize(sprite.id, frameIndex);
            int tw = frameSize != null ? frameSize[0] : sprite.getTextureWidth();
            int th = frameSize != null ? frameSize[1] : sprite.getTextureHeight();
            int effectiveWidth = sprite.fullWidth ? Math.max(1, maxWidth) : maxWidth;
            int effectiveHeight = sprite.fullHeight ? Math.max(1, maxHeight) : maxHeight;

            RenderPipeline pipeline = sprite.blend.getPipeline();
            float baseOpacity = Math.min(1.0f, Math.max(0.0f, sprite.opacity));
            int tileColor = ARGB.colorFromFloat(baseOpacity, 1.0f, 1.0f, 1.0f);

            int tilesX = (effectiveWidth  + tw - 1) / tw;
            int tilesY = (effectiveHeight + th - 1) / th;

            for (int y = 0; y < tilesY; y++) {
                for (int x = 0; x < tilesX; x++) {
                    guiGraphics.blit(
                            pipeline,
                            texture,
                            anchorX + x * tw,
                            anchorY + y * th,
                            0.0f, 0.0f,
                            tw, th,
                            tw, th,
                            tileColor
                    );
                }
            }
        }
    }
}