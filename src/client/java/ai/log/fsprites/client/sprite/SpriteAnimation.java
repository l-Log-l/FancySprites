package ai.log.fsprites.client.sprite;

import java.util.List;

/**
 * Contains animation configuration for a sprite.
 */
public class SpriteAnimation {
    public final boolean interpolate;
    public final float frametime;
    public final List<AnimationFrame> frames;

    public SpriteAnimation(boolean interpolate, float frametime, List<AnimationFrame> frames) {
        this.interpolate = interpolate;
        this.frametime = frametime;
        this.frames = frames;
    }

    public AnimationFrame getFrame(int frameIndex) {
        if (frames == null || frames.isEmpty()) {
            return null;
        }
        return frames.get(frameIndex % frames.size());
    }

    public int getFrameCount() {
        return frames != null ? frames.size() : 1;
    }

    public float getTotalFrametime() {
        if (frames == null || frames.isEmpty()) {
            return frametime;
        }
        float total = 0.0f;
        for (AnimationFrame frame : frames) {
            total += frame.getFrametime(frametime);
        }
        return total;
    }
}
