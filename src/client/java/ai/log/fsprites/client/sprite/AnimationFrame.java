package ai.log.fsprites.client.sprite;

/**
 * Represents a single frame in a sprite animation.
 * Can be a simple integer index or an object with detailed properties.
 */
public class AnimationFrame {
    public final int index;
    public final float frametime;
    public final Float z_index;
    public final Integer width;
    public final Integer height;
    public final Float scale;
    public final SpriteFit fit;
    public final Integer rot;
    public final int[] pos;
    public final SpriteEasing easing;
    public final SpriteEasing moveEasing;
    public final SpriteEasing rotEasing;
    public final Boolean visible;
    public final SpriteBlend blend;
    public final Boolean interpolate;
    public final int[] origin;

    public AnimationFrame(int index, float frametime) {
        this.index = index;
        this.frametime = frametime;
        this.z_index = null;
        this.width = null;
        this.height = null;
        this.scale = null;
        this.fit = null;
        this.rot = null;
        this.pos = null;
        this.easing = null;
        this.moveEasing = null;
        this.rotEasing = null;
        this.visible = null;
        this.blend = null;
        this.interpolate = null;
        this.origin = null;
    }

    public AnimationFrame(int index, float frametime, Float z_index, Integer width, Integer height, 
                         Float scale, SpriteFit fit, Integer rot, int[] pos, SpriteEasing easing,
                         SpriteEasing moveEasing, SpriteEasing rotEasing, Boolean visible, 
                         SpriteBlend blend, Boolean interpolate, int[] origin) {
        this.index = index;
        this.frametime = frametime;
        this.z_index = z_index;
        this.width = width;
        this.height = height;
        this.scale = scale;
        this.fit = fit;
        this.rot = rot;
        this.pos = pos;
        this.easing = easing;
        this.moveEasing = moveEasing;
        this.rotEasing = rotEasing;
        this.visible = visible;
        this.blend = blend;
        this.interpolate = interpolate;
        this.origin = origin;
    }

    public float getFrametime(float defaultFrametime) {
        return frametime > 0 ? frametime : defaultFrametime;
    }
}
