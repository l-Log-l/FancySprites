package ai.log.fsprites.client.sprite;

import ai.log.fsprites.FancySprites;

/**
 * Manages animation state for a sprite instance.
 */
public class SpriteAnimationPlayer {
    private final SpriteConfig spriteConfig;
    private int currentFrameIndex;
    private long frameElapsedMs;
    private long lastUpdateTime;
    private boolean isPlaying;
    private static final boolean DEBUG_ANIMATION = false;
    private static long lastDebugLogMs = 0L;

    public SpriteAnimationPlayer(SpriteConfig spriteConfig) {
        this.spriteConfig = spriteConfig;
        this.currentFrameIndex = 0;
        this.frameElapsedMs = 0L;
        this.lastUpdateTime = System.currentTimeMillis();
        this.isPlaying = true;
        
        if (spriteConfig.animation != null) {
            FancySprites.LOGGER.info("[FancySprites ANIMATION] Created player for sprite: {} frametime={} frames={}", 
                    spriteConfig.id, 
                    spriteConfig.animation.frametime,
                    spriteConfig.animation.frames != null ? spriteConfig.animation.frames.size() : 0);
        }
    }

    /**
     * Update animation state. Call this every frame.
     */
    public void update() {
        if (!isPlaying || spriteConfig.animation == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long deltaMs = currentTime - lastUpdateTime;
        lastUpdateTime = currentTime;

        // Track time spent inside the current frame in milliseconds for smooth interpolation.
        frameElapsedMs += Math.max(0L, deltaMs);

        SpriteAnimation animation = spriteConfig.animation;
        if (animation.frames == null || animation.frames.isEmpty()) {
            return;
        }

        // Check if we need to advance to the next frame
        AnimationFrame currentFrame = animation.getFrame(currentFrameIndex);
        if (currentFrame == null) {
            return;
        }

        float frameTimeMs = Math.max(1.0f, currentFrame.getFrametime(animation.frametime) * 50.0f);
        int prevFrameIndex = currentFrameIndex;
        
        while (frameElapsedMs >= frameTimeMs) {
            frameElapsedMs -= frameTimeMs;
            currentFrameIndex++;

            // Loop animation
            if (currentFrameIndex >= animation.getFrameCount()) {
                currentFrameIndex = 0;
            }

            currentFrame = animation.getFrame(currentFrameIndex);
            if (currentFrame == null) {
                break;
            }
            frameTimeMs = Math.max(1.0f, currentFrame.getFrametime(animation.frametime) * 50.0f);
        }
        
        // Debug log frame changes
        if (DEBUG_ANIMATION && currentFrameIndex != prevFrameIndex) {
            long now = System.currentTimeMillis();
            if (now - lastDebugLogMs > 500L) {
                FancySprites.LOGGER.info("[FancySprites ANIMATION] sprite={} frameIndex changed: {} -> {} (ticks={})", 
                        spriteConfig.id, prevFrameIndex, currentFrameIndex, frameElapsedMs);
                lastDebugLogMs = now;
            }
        }
    }

    /**
     * Get the current frame index in the animation.
     */
    public int getCurrentFrameIndex() {
        if (spriteConfig.animation == null || spriteConfig.animation.frames.isEmpty()) {
            return 0;
        }
        return currentFrameIndex % spriteConfig.animation.getFrameCount();
    }

    /**
     * Get the current animation frame.
     */
    public AnimationFrame getCurrentFrame() {
        if (spriteConfig.animation == null) {
            return null;
        }
        return spriteConfig.animation.getFrame(getCurrentFrameIndex());
    }

    /**
     * Get progress of current frame (0.0 to 1.0).
     */
    public float getCurrentFrameProgress() {
        if (spriteConfig.animation == null) {
            return 0.0f;
        }

        AnimationFrame frame = getCurrentFrame();
        if (frame == null) {
            return 0.0f;
        }

        float frameTimeMs = frame.getFrametime(spriteConfig.animation.frametime) * 50.0f;
        if (frameTimeMs <= 0.0f) {
            return 1.0f;
        }

        return Math.min(1.0f, (float) frameElapsedMs / frameTimeMs);
    }

    /**
     * Get interpolation factor for the current frame.
     */
    public float getInterpolationFactor() {
        if (spriteConfig.animation == null || !spriteConfig.animation.interpolate) {
            return 0.0f;
        }

        AnimationFrame frame = getCurrentFrame();
        if (frame == null) {
            return 0.0f;
        }

        SpriteEasing easing = frame.easing != null ? frame.easing : SpriteEasing.LINEAR;
        return easing.apply(getCurrentFrameProgress());
    }

    /**
     * Reset animation to the beginning.
     */
    public void reset() {
        currentFrameIndex = 0;
        frameElapsedMs = 0L;
        lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * Play/pause animation.
     */
    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
    }

    public boolean isPlaying() {
        return isPlaying;
    }
}
