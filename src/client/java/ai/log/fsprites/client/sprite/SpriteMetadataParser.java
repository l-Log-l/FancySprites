package ai.log.fsprites.client.sprite;

import ai.log.fsprites.FancySprites;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses sprite metadata from JSON (.mcmeta) files.
 */
public class SpriteMetadataParser {

    public static SpriteConfig parse(JsonObject json, String spriteId, String basePath) {
        try {
            boolean enabled = readBoolean(json, "enabled", false);
            boolean visible = readBoolean(json, "visible", true);
            float opacity = readFloat(json, "opacity", 1.0f);
            int zIndex = readInt(json, "z_index", 0);
            String blendStr = readString(json, "blend", "normal");
            List<String> guiRules = readGuiRules(json);
            List<SpriteGui> guis = readGuiTargets(guiRules);
            SpriteGui gui = guis.isEmpty() ? SpriteGui.ALL : guis.get(0);
            String anchorStr = readString(json, "anchor", "top_left");
            boolean screenSpace = readBoolean(json, "screen_space", false);
            DimensionSpec width = readDimension(json, "width");
            DimensionSpec height = readDimension(json, "height");
            float scale = readFloat(json, "scale", 1.0f);
            String fitStr = readString(json, "fit", "stretch");
            int rotation = readInt(json, "rot", 0);
            int[] origin = readIntArray(json, "origin", new int[]{0, 0});
            int[] pos = readIntArray(json, "pos", new int[]{0, 0});
            String description = readString(json, "description", "");
            String category = readString(json, "category", "Без категории");

            SpriteAnimation animation = readAnimation(json.getAsJsonObject("animation"));

            FancySprites.LOGGER.info(
                    "[FancySprites PARSE] sprite={} z_index={} frametime={} animationFrames={}",
                    spriteId,
                    zIndex,
                    animation != null ? animation.frametime : "n/a",
                    animation != null ? animation.getFrameCount() : 0
            );

            return new SpriteConfig(
                    enabled,
                    spriteId,
                    visible,
                    opacity,
                    zIndex,
                    SpriteBlend.fromString(blendStr),
                    gui,
                    guis,
                    guiRules,
                    SpriteAnchor.fromString(anchorStr),
                    pos,
                    screenSpace,
                    width.value,
                    height.value,
                    width.full,
                    height.full,
                    scale,
                    SpriteFit.fromString(fitStr),
                    rotation,
                    origin,
                    animation,
                    description,
                    category,
                    basePath + "/" + spriteId
            );
        } catch (Exception e) {
            FancySprites.LOGGER.warn("Failed to parse sprite metadata for {}: {}", spriteId, e.getMessage());
            return null;
        }
    }

    public static SpriteConfig parse(String content, String spriteId, String basePath) {
        return parse(parseJsonFile(content), spriteId, basePath);
    }

    private static SpriteAnimation readAnimation(JsonObject json) {
        if (json == null) {
            return null;
        }

        boolean interpolate = readBoolean(json, "interpolate", false);
        float frametime = readFloat(json, "frametime", 20.0f);

        List<AnimationFrame> frames = new ArrayList<>();
        JsonArray framesArray = json.has("frames") && json.get("frames").isJsonArray()
                ? json.getAsJsonArray("frames")
                : null;
        if (framesArray != null) {
            FancySprites.LOGGER.info("[FancySprites PARSE FRAMES] count={}", framesArray.size());
            for (JsonElement element : framesArray) {
                if (element.isJsonPrimitive()) {
                    int index = element.getAsInt();
                    frames.add(new AnimationFrame(index, frametime));
                } else if (element.isJsonObject()) {
                    frames.add(readAnimationFrame(element.getAsJsonObject(), frametime));
                }
            }
        }

        if (frames.isEmpty()) {
            frames.add(new AnimationFrame(0, frametime));
        }

        return new SpriteAnimation(interpolate, frametime, frames);
    }

    private static AnimationFrame readAnimationFrame(JsonObject json, float defaultFrametime) {
        int index = readInt(json, "index", 0);
        float frametime = readFloat(json, "frametime", defaultFrametime);
        Float zIndex = json.has("z_index") && json.get("z_index").isJsonPrimitive()
                ? json.get("z_index").getAsFloat()
                : null;
        Integer width = json.has("width") ? readInt(json, "width", 16) : null;
        Integer height = json.has("height") ? readInt(json, "height", 16) : null;
        Float scale = json.has("scale") && json.get("scale").isJsonPrimitive()
                ? json.get("scale").getAsFloat()
                : null;
        SpriteFit fit = json.has("fit") && json.get("fit").isJsonPrimitive()
                ? SpriteFit.fromString(json.get("fit").getAsString())
                : null;
        Integer rot = json.has("rot") ? readInt(json, "rot", 0) : null;
        int[] pos = json.has("pos") ? readIntArray(json, "pos", new int[]{0, 0}) : null;
        SpriteEasing easing = json.has("easing") && json.get("easing").isJsonPrimitive()
                ? SpriteEasing.fromString(json.get("easing").getAsString())
                : null;
        SpriteEasing moveEasing = json.has("move_easing") && json.get("move_easing").isJsonPrimitive()
                ? SpriteEasing.fromString(json.get("move_easing").getAsString())
                : null;
        SpriteEasing rotEasing = json.has("rot_easing") && json.get("rot_easing").isJsonPrimitive()
                ? SpriteEasing.fromString(json.get("rot_easing").getAsString())
                : null;
        Boolean visible = json.has("visible") && json.get("visible").isJsonPrimitive()
                ? json.get("visible").getAsBoolean()
                : null;
        SpriteBlend blend = json.has("blend") && json.get("blend").isJsonPrimitive()
                ? SpriteBlend.fromString(json.get("blend").getAsString())
                : null;
        Boolean interpolate = json.has("interpolate") && json.get("interpolate").isJsonPrimitive()
                ? json.get("interpolate").getAsBoolean()
                : null;
        int[] origin = json.has("origin") ? readIntArray(json, "origin", new int[]{0, 0}) : null;

        return new AnimationFrame(index, frametime, zIndex, width, height, scale, fit, rot, pos,
                easing, moveEasing, rotEasing, visible, blend, interpolate, origin);
    }

    private static List<String> readGuiRules(JsonObject json) {
        List<String> result = new ArrayList<>();
        if (json == null || !json.has("gui") || json.get("gui").isJsonNull()) {
            result.add("minecraft:*");
            return result;
        }

        JsonElement guiElement = json.get("gui");
        if (guiElement.isJsonArray()) {
            for (JsonElement element : guiElement.getAsJsonArray()) {
                if (!element.isJsonPrimitive()) {
                    continue;
                }

                String rule = element.getAsString();
                if (rule != null && !rule.isBlank()) {
                    result.add(rule);
                }
            }
        } else if (guiElement.isJsonPrimitive()) {
            String rule = guiElement.getAsString();
            if (rule != null && !rule.isBlank()) {
                result.add(rule);
            }
        }

        if (result.isEmpty()) {
            result.add("minecraft:*");
        }

        return result;
    }

    private static List<SpriteGui> readGuiTargets(List<String> guiRules) {
        List<SpriteGui> result = new ArrayList<>();
        for (String rule : guiRules) {
            if (rule == null || rule.isBlank() || rule.startsWith("!")) {
                continue;
            }
            SpriteGui gui = parseGuiTarget(rule);
            if (gui != null && !result.contains(gui)) {
                result.add(gui);
            }
        }
        if (result.isEmpty()) {
            result.add(SpriteGui.ALL);
        }
        return result;
    }

    private static SpriteGui parseGuiTarget(String id) {
        if (id == null) {
            return null;
        }

        for (SpriteGui gui : SpriteGui.values()) {
            if (gui.id.equals(id)) {
                return gui;
            }
        }

        return "minecraft:*".equals(id) ? SpriteGui.ALL : null;
    }

    private static boolean readBoolean(JsonObject json, String key, boolean defaultValue) {
        if (json == null || !json.has(key) || !json.get(key).isJsonPrimitive()) {
            return defaultValue;
        }

        try {
            return json.get(key).getAsBoolean();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static int readInt(JsonObject json, String key, int defaultValue) {
        if (json == null || !json.has(key) || !json.get(key).isJsonPrimitive()) {
            return defaultValue;
        }

        try {
            JsonElement element = json.get(key);
            try {
                return element.getAsInt();
            } catch (NumberFormatException ignored) {
                return Integer.parseInt(element.getAsString());
            }
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static float readFloat(JsonObject json, String key, float defaultValue) {
        if (json == null || !json.has(key) || !json.get(key).isJsonPrimitive()) {
            return defaultValue;
        }

        try {
            return json.get(key).getAsFloat();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static String readString(JsonObject json, String key, String defaultValue) {
        if (json == null || !json.has(key) || !json.get(key).isJsonPrimitive()) {
            return defaultValue;
        }

        try {
            return json.get(key).getAsString();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static DimensionSpec readDimension(JsonObject json, String key) {
        if (json == null || !json.has(key) || json.get(key).isJsonNull()) {
            return new DimensionSpec(null, false);
        }

        JsonElement element = json.get(key);
        if (element.isJsonPrimitive()) {
            String value = element.getAsString();
            if ("auto".equalsIgnoreCase(value)) {
                return new DimensionSpec(null, false);
            }
            if ("full".equalsIgnoreCase(value)) {
                return new DimensionSpec(null, true);
            }
        }

        return new DimensionSpec(readInt(json, key, 16), false);
    }

    private static int[] readIntArray(JsonObject json, String key, int[] defaultValue) {
        if (json == null || !json.has(key) || !json.get(key).isJsonArray()) {
            return defaultValue;
        }

        JsonArray array = json.getAsJsonArray(key);
        if (array.size() != 2) {
            return defaultValue;
        }

        try {
            return new int[]{array.get(0).getAsInt(), array.get(1).getAsInt()};
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static JsonObject parseJsonFile(String content) {
        try {
            return JsonParser.parseString(content).getAsJsonObject();
        } catch (Exception e) {
            FancySprites.LOGGER.error("Failed to parse JSON: {}", e.getMessage());
            return new JsonObject();
        }
    }

    private static final class DimensionSpec {
        final Integer value;
        final boolean full;

        DimensionSpec(Integer value, boolean full) {
            this.value = value;
            this.full = full;
        }
    }
}
