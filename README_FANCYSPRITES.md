# FancySprites - Custom GUI Sprite Mod

A powerful client-side Fabric mod for Minecraft 1.21.11 that allows adding custom animated sprites to any GUI screen. Perfect for creating overlays, decorations, and visual effects on inventory, chests, crafting tables, and all other GUI screens.

## Features

✨ **Flexible Sprite Rendering**
- Place sprites on any Minecraft GUI screen
- Support for static images and animations
- Customizable positioning, scaling, and rotation
- Multiple blend modes (normal, additive, multiply, screen)

🎬 **Advanced Animation System**
- Frame-by-frame animation control
- Per-frame timing configuration
- Multiple easing functions for smooth transitions
- Interpolation support for frame blending

🎨 **Rich Customization**
- 9-point anchor positioning system
- Z-index layering (render above/below GUI)
- Opacity and blend mode control
- Screen-space and GUI-relative positioning
- Automatic and manual texture sizing

🔧 **Easy Integration**
- JSON-based sprite configuration
- Simple asset structure
- Programmatic API for mod developers
- Comprehensive documentation and examples

## Installation

1. Install [Fabric Loader](https://fabricmc.net/) for Minecraft 1.21.11
2. Install [Fabric API](https://modrinth.com/mod/fabric-api)
3. Download FancySprites mod and place in `mods` folder
4. Launch Minecraft with Fabric

## Quick Start

### Creating Your First Sprite

#### Step 1: Add Textures

Create texture files in your resource pack:
```
assets/fsprite/textures/my-sprite/
├── 0.png          (frame 0)
└── 1.png          (frame 1)
```

#### Step 2: Create Metadata File

Create `assets/fsprite/my-sprite.mcmeta`:
```json
{
  "enabled": true,
  "id": "my-sprite",
  "gui": "minecraft:inventory",
  "anchor": "center",
  "visible": true,
  "animation": {
    "frametime": 20,
    "frames": [0, 1]
  }
}
```

#### Step 3: Load the Sprite

In your mod's client initializer:
```java
import ai.log.fsprites.client.sprite.SpriteRegistry;

public class MyModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SpriteRegistry.registerSpriteFromResource("my-sprite");
    }
}
```

## Configuration

### .mcmeta File Format

Each sprite is configured via a JSON file at `assets/fsprite/{sprite-id}.mcmeta`

#### Short Format
```json
{
  "enabled": true,
  "id": "sword",
  "gui": ["minecraft:inventory", "!minecraft:game"],
  "category": "Weapons",
  "width": "full",
  "height": "full"
}
```

### GUI Rules
- `gui` accepts a string or array of strings.
- Use exact ids like `minecraft:inventory` or `minecraft:game`.
- Use `"minecraft:*"` for all screens.
- Use `"!minecraft:game"` to exclude a screen.

### Categories
- `category` groups sprites by exact text match.
- Same category text means the same group in the menu.

### Full Size
- `width: "full"` stretches to the bound GUI/screen width.
- `height: "full"` stretches to the bound GUI/screen height.

#### Basic Example
```json
{
  "enabled": true,
  "id": "my-sprite",
  "gui": "minecraft:inventory",
  "anchor": "center",
  "visible": true,
  "opacity": 1.0,
  "scale": 1.0,
  "blend": "normal"
}
```

#### Animated Example
```json
{
  "enabled": true,
  "id": "animated-sprite",
  "gui": "minecraft:*",
  "anchor": "top_right",
  "animation": {
    "interpolate": false,
    "frametime": 5,
    "frames": [0, 1, 2, 3, 0, 1, 2]
  }
}
```

#### Advanced Example with Per-Frame Control
```json
{
  "enabled": true,
  "id": "advanced",
  "gui": "minecraft:chest",
  "anchor": "center",
  "blend": "additive",
  "opacity": 0.8,
  "animation": {
    "interpolate": true,
    "frametime": 10,
    "frames": [
      0,
      1,
      {
        "index": 2,
        "frametime": 20,
        "scale": 1.2,
        "easing": "ease_in_out",
        "rot": 45
      },
      3
    ]
  }
}
```

### Property Reference

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | bool | - | **Required.** Must be `true` to load sprite |
| `id` | string | - | **Required.** Unique sprite identifier |
| `visible` | bool | `true` | Sprite visibility |
| `gui` | string or array of strings | `minecraft:*` | Target GUI screen(s); supports exact ids, wildcards, and negation like `"!minecraft:game"` |
| `anchor` | string | `top_left` | Positioning anchor point |
| `pos` | [x, y] | [0, 0] | Position offset from anchor |
| `rot` | int | `0` | Rotation in degrees |
| `scale` | float | `1.0` | Scaling factor |
| `opacity` | float | `1.0` | Transparency (0.0-1.0) |
| `z_index` | int | `0` | Layer (positive=above, negative=below) |
| `blend` | string | `normal` | Blend mode |
| `fit` | string | `stretch` | Texture scaling mode |
| `width` | int/auto | `auto` | Sprite width |
| `height` | int/auto | `auto` | Sprite height |
| `origin` | [x, y] | [0, 0] | Rotation/scale center |
| `screen_space` | bool | `false` | Use screen coords instead of GUI-relative |
| `animation` | object | - | Animation configuration |

### Anchor Points

Available anchor positions:
```
top_left     top     top_right
  left      center     right
bottom_left  bottom  bottom_right
```

### Supported GUIs

```
minecraft:inventory              minecraft:chest
minecraft:creative_inventory     minecraft:large_chest
minecraft:furnace                minecraft:crafting_table
minecraft:anvil                  minecraft:enchanting_table
minecraft:beacon                 minecraft:brewing_stand
minecraft:dispenser              minecraft:hopper
minecraft:shulker_box            minecraft:barrel
minecraft:villager               minecraft:horse
minecraft:game                   minecraft:* (all screens)
```

### Blend Modes

- **normal** - Standard Minecraft GUI alpha blending
- **additive** - Custom additive blending, not a vanilla Minecraft GUI mode
- **multiply** - Custom multiplicative blending, not a vanilla Minecraft GUI mode
- **screen** - Custom screen blending, not a vanilla Minecraft GUI mode

### Animation Easing

- `none`, `linear`, `ease_in`, `ease_out`, `ease_in_out`
- `bounce`, `elastic`, `step`, `smooth`

## Programmatic API

### Loading Sprites

```java
import ai.log.fsprites.client.sprite.*;

// Load from .mcmeta resource
SpriteRegistry.registerSpriteFromResource("my-sprite");

// Create programmatically
SpriteConfig sprite = SpriteRegistry.createStaticSprite(
    "my-sprite",
    SpriteGui.INVENTORY,
    SpriteAnchor.CENTER
);
SpriteManager.getInstance().registerSprite(sprite);
```

### Advanced Usage

```java
// Create animated sprite
SpriteConfig animated = SpriteRegistry.createAnimatedSprite(
    "animated",
    SpriteGui.ALL,
    SpriteAnchor.BOTTOM_RIGHT,
    4,    // frameCount
    20    // frametime
);

SpriteManager.getInstance().registerSprite(animated);
```

### Custom Rendering

```java
SpriteConfig sprite = /* ... */;
SpriteAnimationPlayer player = new SpriteAnimationPlayer(sprite);

// In render method:
player.update();
SpriteRenderer.renderSprite(
    guiGraphics, sprite, player,
    screenWidth, screenHeight,
    posX, posY
);
```


### Static Overlay Badge

```json
{
  "enabled": true,
  "id": "badge",
  "gui": "minecraft:inventory",
  "anchor": "top_right",
  "pos": [-20, 20],
  "scale": 0.8,
  "width": 16,
  "height": 16
}
```

### Animated Background

```json
{
  "enabled": true,
  "id": "background",
  "gui": "minecraft:chest",
  "z_index": -1,
  "blend": "multiply",
  "opacity": 0.5,
  "animation": {
    "frametime": 10,
    "frames": [0, 1, 2, 3]
  }
}
```

### Glowing Frame Effect

```json
{
  "enabled": true,
  "id": "glow-frame",
  "gui": "minecraft:*",
  "anchor": "center",
  "blend": "additive",
  "opacity": 0.7,
  "scale": 1.1,
  "animation": {
    "interpolate": true,
    "frametime": 20,
    "frames": [
      { "index": 0, "opacity": 0.3 },
      { "index": 1, "opacity": 0.7 },
      { "index": 0, "opacity": 0.3 }
    ]
  }
}
```

## Troubleshooting

**Sprite not showing:**
- ✓ Check `enabled: true` in .mcmeta
- ✓ Verify texture files exist at correct paths
- ✓ Check GUI identifier matches your screen
- ✓ Ensure sprite is registered in client initializer

**Animation not playing:**
- ✓ Verify texture files for all frame indices exist
- ✓ Check `frames` array is not empty
- ✓ Ensure `frametime` > 0

**Performance issues:**
- ✓ Reduce number of animated sprites
- ✓ Increase `frametime` for slower animations
- ✓ Use static sprites when animation not needed

## Version Compatibility

- **Minecraft**: 1.21.11
- **Fabric Loader**: Latest
- **Java**: 21+

## Performance

- Efficient sprite batching
- Optimized animation updates
- Minimal performance impact on FPS
- Supports dozens of concurrent sprites

## Contributing

Found a bug or have a feature request? Feel free to open an issue or contribute!

---

**Need Help?**
- See [SPRITE_ASSETS_GUIDE.md](SPRITE_ASSETS_GUIDE.md) for asset creation guide
- See [SPRITE_API.md](SPRITE_API.md) for detailed API documentation
