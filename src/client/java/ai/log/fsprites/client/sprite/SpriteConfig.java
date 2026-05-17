package ai.log.fsprites.client.sprite;

import java.util.List;

/**
 * Complete configuration for a sprite as loaded from .mcmeta file.
 */
public class SpriteConfig {
    // Required
    public final boolean enabled;
    public final String id;

    // Display properties
    public final boolean visible;
    public final float opacity;
    public final int zIndex;
    public final SpriteBlend blend;

    // Positioning and sizing
    public final SpriteGui gui;
    public final List<SpriteGui> guis;
    public final List<String> guiRules;
    public final SpriteAnchor anchor;
    public final int[] pos;
    public final boolean screenSpace;
    public final Integer width;
    public final Integer height;
    public final boolean fullWidth;
    public final boolean fullHeight;

    // Transformation
    public final float scale;
    public final SpriteFit fit;
    public final int rotation;
    public final int[] origin;

    // Animation
    public final SpriteAnimation animation;

    // Metadata
    public final String description;
    public final String category;

    // Resource path
    public final String texturePath;

    public SpriteConfig(boolean enabled, String id, boolean visible, float opacity, int zIndex,
                        SpriteBlend blend, SpriteGui gui, List<String> guiRules, SpriteAnchor anchor, int[] pos,
                        boolean screenSpace, Integer width, Integer height, boolean fullWidth, boolean fullHeight, float scale,
                        SpriteFit fit, int rotation, int[] origin, SpriteAnimation animation,
                        String description, String category, String texturePath) {
        this(enabled, id, visible, opacity, zIndex, blend, gui, List.of(gui), guiRules, anchor, pos,
            screenSpace, width, height, fullWidth, fullHeight, scale, fit, rotation, origin, animation, description, category, texturePath);
        }

        public SpriteConfig(boolean enabled, String id, boolean visible, float opacity, int zIndex,
                SpriteBlend blend, SpriteGui gui, List<SpriteGui> guis, List<String> guiRules, SpriteAnchor anchor, int[] pos,
                boolean screenSpace, Integer width, Integer height, boolean fullWidth, boolean fullHeight, float scale,
                SpriteFit fit, int rotation, int[] origin, SpriteAnimation animation,
                String description, String category, String texturePath) {
        this.enabled = enabled;
        this.id = id;
        this.visible = visible;
        this.opacity = opacity;
        this.zIndex = zIndex;
        this.blend = blend;
        this.gui = gui;
        this.guis = guis != null && !guis.isEmpty() ? List.copyOf(guis) : List.of(gui);
        this.guiRules = guiRules != null && !guiRules.isEmpty() ? List.copyOf(guiRules) : List.of("minecraft:*");
        this.anchor = anchor;
        this.pos = pos;
        this.screenSpace = screenSpace;
        this.width = width;
        this.height = height;
        this.fullWidth = fullWidth;
        this.fullHeight = fullHeight;
        this.scale = scale;
        this.fit = fit;
        this.rotation = rotation;
        this.origin = origin;
        this.animation = animation;
        this.description = description != null ? description : "";
        this.category = category != null && !category.isEmpty() ? category : "Без категории";
        this.texturePath = texturePath;
    }

    public boolean isAnimated() {
        return animation != null && animation.frames != null && animation.frames.size() > 1;
    }

    public int getTextureWidth() {
        return width != null ? width : 16;
    }

    public int getTextureHeight() {
        return height != null ? height : 16;
    }

    public boolean matchesGui(String guiId) {
        String targetGui = guiId != null ? guiId : "minecraft:*";
        boolean matchedPositiveRule = false;
        boolean sawPositiveRule = false;

        for (String rule : guiRules) {
            if (rule == null || rule.isBlank()) {
                continue;
            }

            boolean negated = rule.startsWith("!");
            String normalizedRule = negated ? rule.substring(1) : rule;
            boolean ruleMatches = matchesRule(normalizedRule, targetGui);

            if (negated && ruleMatches) {
                return false;
            }

            if (!negated && ruleMatches) {
                matchedPositiveRule = true;
                sawPositiveRule = true;
            } else if (!negated) {
                sawPositiveRule = true;
            }
        }

        if (sawPositiveRule) {
            return matchedPositiveRule;
        }

        return true;
    }

    private static boolean matchesRule(String rule, String guiId) {
        if (rule == null || rule.isBlank()) {
            return false;
        }
        if ("minecraft:*".equals(rule)) {
            return true;
        }
        return rule.equals(guiId);
    }
}
