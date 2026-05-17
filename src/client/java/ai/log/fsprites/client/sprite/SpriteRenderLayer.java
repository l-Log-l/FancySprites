package ai.log.fsprites.client.sprite;

public enum SpriteRenderLayer {
    /**
     * Renders before screen dimming overlay (z_index <= -2).
     */
    PRE_BACKGROUND,

    /**
     * Renders after screen dimming, before GUI panel texture (z_index == -1).
     */
    BACKGROUND,

    /**
     * Renders after GUI panel texture, before slots and items (z_index == 0).
     */
    GUI,

    /**
     * Renders after all GUI contents, before carried item (z_index > 0).
     */
    FOREGROUND;

    public static SpriteRenderLayer fromZIndex(int zIndex) {
        if (zIndex <= -2) return PRE_BACKGROUND;
        if (zIndex == -1) return BACKGROUND;
        if (zIndex == 0)  return GUI;
        return FOREGROUND;
    }
}