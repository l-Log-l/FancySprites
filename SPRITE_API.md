# FancySprites API Documentation

## Overview

FancySprites provides a programmatic API for registering and managing custom GUI sprites with animation support.

## Basic Usage

### Loading Sprites Programmatically

```java
import ai.log.fsprites.client.sprite.*;

// Get the sprite manager
SpriteManager manager = SpriteManager.getInstance();

// Create a sprite configuration
SpriteConfig sprite = new SpriteConfig(
    true,                           // enabled
    "my-sprite",                    // id
    true,                           // visible
    1.0f,                           // opacity
    0,                              // z_index
    SpriteBlend.NORMAL,             // blend
    SpriteGui.INVENTORY,            // gui
    SpriteAnchor.CENTER,            // anchor
    new int[]{0, 0},               // pos
    false,                          // screen_space
    null,                           // width (auto)
    null,                           // height (auto)
    1.0f,                           // scale
    SpriteFit.STRETCH,              // fit
    0,                              // rotation
    new int[]{0, 0},               // origin
    null,                           // animation
    "fsprite/textures/my-sprite"   // texturePath
);

// Register the sprite
manager.registerSprite(sprite);
```

### Loading from JSON Metadata

```java
import com.google.gson.JsonObject;

String jsonContent = """
{
  "enabled": true,
  "id": "my-sprite",
  "gui": "minecraft:inventory",
  "anchor": "center",
  "visible": true,
  "opacity": 1.0
}
""";

SpriteManager manager = SpriteManager.getInstance();
manager.loadSpriteFromMetadata("my-sprite", jsonContent);
```

### Creating Animated Sprites

```java
// Create animation frames
List<AnimationFrame> frames = Arrays.asList(
    new AnimationFrame(0, 20),  // Frame 0, 20 ticks duration
    new AnimationFrame(1, 20),
    new AnimationFrame(2, 20),
    new AnimationFrame(3, 20)
);

SpriteAnimation animation = new SpriteAnimation(
    false,    // interpolate
    20,       // default frametime
    frames
);

// Use in sprite config
SpriteConfig animatedSprite = new SpriteConfig(
    true, "animated-sprite", true, 1.0f, 0,
    SpriteBlend.NORMAL, SpriteGui.INVENTORY,
    SpriteAnchor.CENTER, new int[]{0, 0}, false,
    null, null, 1.0f, SpriteFit.STRETCH, 0,
    new int[]{0, 0}, animation,
    "fsprite/textures/animated"
);

SpriteManager.getInstance().registerSprite(animatedSprite);
```

### Advanced Frame Configuration

```java
// Create a frame with custom properties
AnimationFrame advancedFrame = new AnimationFrame(
    2,                                  // index
    20,                                 // frametime
    1.5f,                              // z_index
    null,                              // width
    null,                              // height
    1.1f,                              // scale
    SpriteFit.STRETCH,                 // fit
    45,                                // rotation
    new int[]{10, 5},                  // pos
    SpriteEasing.EASE_IN_OUT,          // easing
    SpriteEasing.LINEAR,               // moveEasing
    SpriteEasing.LINEAR,               // rotEasing
    true,                              // visible
    SpriteBlend.ADDITIVE,              // blend
    true,                              // interpolate
    new int[]{8, 8}                    // origin
);
```

## Working with Animation Players

```java
SpriteConfig sprite = /* ... */;
SpriteAnimationPlayer player = new SpriteAnimationPlayer(sprite);

// Update each frame
player.update();

// Get current frame info
int currentFrame = player.getCurrentFrameIndex();
AnimationFrame frame = player.getCurrentFrame();
float progress = player.getCurrentFrameProgress();

// Control playback
player.setPlaying(false);  // Pause
player.reset();            // Reset to beginning
```

## Rendering Sprites

```java
import net.minecraft.client.gui.GuiGraphics;

SpriteConfig sprite = /* ... */;
SpriteAnimationPlayer player = new SpriteAnimationPlayer(sprite);

// In your render method:
player.update();
SpriteRenderer.renderSprite(
    guiGraphics,
    sprite,
    player,
    screenWidth,
    screenHeight,
    anchorX,
    anchorY
);
```

## Blend Modes

```java
// Apply custom blend mode
SpriteBlend.ADDITIVE.apply();
// ... render something ...
SpriteBlend.resetBlend();

// Available modes:
// - SpriteBlend.NORMAL (default alpha blending)
// - SpriteBlend.ADDITIVE (additive blending)
// - SpriteBlend.MULTIPLY (multiplicative blending)
// - SpriteBlend.SCREEN (screen blending)
```

## Easing Functions

```java
// Use easing in animations
SpriteEasing easing = SpriteEasing.EASE_IN_OUT;
float progress = 0.5f;  // 0.0 to 1.0
float eased = easing.apply(progress);

// Available easings:
// - NONE         (instant)
// - LINEAR       (linear interpolation)
// - EASE_IN      (acceleration)
// - EASE_OUT     (deceleration)
// - EASE_IN_OUT  (smooth start and end)
// - BOUNCE       (bouncing effect)
// - ELASTIC      (elastic effect)
// - STEP         (step animation)
// - SMOOTH       (smooth curve)
```

## GUI Screens

```java
// Target specific GUIs
SpriteGui gui = SpriteGui.INVENTORY;
boolean matches = gui.matches("minecraft:inventory");  // true

// Match any GUI
SpriteGui allGuis = SpriteGui.ALL;

// Available GUIs:
// INVENTORY, CREATIVE_INVENTORY, CHEST, LARGE_CHEST,
// ENDER_CHEST, BARREL, SHULKER_BOX, CRAFTING_TABLE,
// SMITHING_TABLE, CARTOGRAPHY_TABLE, LOOM, STONECUTTER,
// FURNACE, BLAST_FURNACE, SMOKER, ANVIL, GRINDSTONE,
// ENCHANTING_TABLE, BREWING_STAND, BEACON, VILLAGER,
// HORSE, HOPPER, DISPENSER, DROPPER, COMMAND_BLOCK, ALL
```

## Complete Example: Custom Overlay Mod

```java
import ai.log.fsprites.client.sprite.*;
import net.fabricmc.api.ClientModInitializer;

public class MyModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Get manager
        SpriteManager manager = SpriteManager.getInstance();
        
        // Create animated overlay
        List<AnimationFrame> frames = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            frames.add(new AnimationFrame(i, 10));
        }
        
        SpriteAnimation animation = new SpriteAnimation(true, 10, frames);
        
        SpriteConfig overlay = new SpriteConfig(
            true, "my-overlay", true, 0.8f, 1,
            SpriteBlend.ADDITIVE,
            SpriteGui.ALL,  // On all GUIs
            SpriteAnchor.TOP_RIGHT,
            new int[]{-10, 10},  // 10px from top-right
            false,
            null, null, 1.0f, SpriteFit.STRETCH, 0,
            new int[]{0, 0}, animation,
            "fsprite/textures/overlay"
        );
        
        manager.registerSprite(overlay);
    }
}
```

## Troubleshooting

### Sprite Not Showing
1. Ensure `enabled: true` in .mcmeta
2. Check sprite ID matches texture directory name
3. Verify texture files exist at correct paths
4. Check GUI identifier is correct
5. Verify anchor and position calculations

### Animation Not Playing
1. Check `animation` object exists in .mcmeta
2. Verify `frames` array is not empty
3. Ensure texture files for all frame indices exist
4. Check `frametime` is > 0

### Performance Issues
1. Reduce animation frame count
2. Use static sprites when animation not needed
3. Limit z_index layers used
4. Consider texture atlas for many sprites

## Events and Hooks

### Reloading Sprites

```java
// Reload sprite manager
SpriteManager.reload();

// Clear animation players
SpriteRenderingManager.clear();
```

### Texture Reloads

The sprite system automatically handles texture reloads when resources are reloaded by Minecraft.
