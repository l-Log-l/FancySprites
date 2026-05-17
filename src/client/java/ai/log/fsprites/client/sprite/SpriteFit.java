package ai.log.fsprites.client.sprite;

/**
 * Scaling mode for sprites.
 */
public enum SpriteFit {
    STRETCH("stretch"),
    TILE("tile");

    public final String id;

    SpriteFit(String id) {
        this.id = id;
    }

    public static SpriteFit fromString(String name) {
        if (name == null) {
            return STRETCH;
        }
        try {
            return SpriteFit.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return STRETCH;
        }
    }
}
