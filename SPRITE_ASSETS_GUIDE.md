# FancySprites Mod - Asset Structure Guide

## Directory Structure

```
assets/
├── fancysprites/
│   ├── icon.png
│   └── textures/
│       ├── {sprite-id}/
│       │   ├── 0.png
│       │   ├── 1.png
│       │   ├── 2.png
│       │   └── ... (more frames)
│       └── ...
├── fsprite/
│   ├── textures/
│   │   ├── {sprite-id}/
│   │   │   ├── 0.png
│   │   │   ├── 1.png
│   │   │   └── ...
│   │   └── ...
│   └── {sprite-id}.mcmeta (JSON configuration)
```

## .mcmeta File Format

Each sprite is defined by a `.mcmeta` JSON file located in `assets/fsprite/`.

### Example: `g-flag.mcmeta`

```json
{
  "enabled": true,
  "visible": true,
  "id": "g-flag",
  "gui": "minecraft:inventory",
  "anchor": "center",
  "z_index": 0,
  "pos": [0, 0],
  "rot": 0,
  "origin": [0, 0],
  "width": "auto",
  "height": "auto",
  "scale": 1.0,
  "fit": "stretch",
  "blend": "normal",
  "screen_space": false,
  "opacity": 1.0,
  "animation": {
    "interpolate": false,
    "frametime": 1,
    "frames": [0, 1, 2, 3]
  }
}
```

## Property Reference

### Required Properties
- **id**: String - Unique identifier for the sprite
- **enabled**: Boolean - Must be true for sprite to load

### Display Properties
- **visible**: Boolean (default: true) - Visibility of sprite
- **opacity**: Float (default: 1.0) - Transparency (0.0 to 1.0)
- **z_index**: Integer (default: 0) - Layer (positive = above UI, negative = below)
- **blend**: String (default: "normal")
  - "normal" - Standard alpha blending
  - "additive" - Additive blending (glowing effect)
  - "multiply" - Multiplicative blending (darkening)
  - "screen" - Screen blending (lightening)

### Positioning Properties
- **gui**: String (default: "minecraft:*") - Target GUI screen
- **anchor**: String (default: "top_left") - Anchor point
  - "top_left", "top", "top_right"
  - "left", "center", "right"
  - "bottom_left", "bottom", "bottom_right"
- **pos**: [x, y] (default: [0, 0]) - Position offset from anchor
- **screen_space**: Boolean (default: false) - Use screen coordinates instead of GUI-relative

### Sizing Properties
- **width**: Integer or "auto" (default: "auto") - Sprite width in pixels
- **height**: Integer or "auto" (default: "auto") - Sprite height in pixels
- **scale**: Float (default: 1.0) - Scaling factor

### Transformation Properties
- **fit**: String (default: "stretch")
  - "stretch" - Stretch texture to fill
  - "tile" - Repeat texture
- **rot**: Integer (default: 0) - Rotation in degrees
- **origin**: [x, y] (default: [0, 0]) - Rotation/scale center point

### Animation Properties
- **animation**: Object (optional)
  - **interpolate**: Boolean (default: false) - Frame interpolation
  - **frametime**: Integer (default: 20) - Ticks per frame
  - **frames**: Array - Frame indices or objects
    ```json
    "frames": [0, 1, 2, 3]
    // or
    "frames": [
      0,
      1,
      {
        "index": 2,
        "frametime": 2,
        "easing": "ease_in_out",
        "visible": true,
        "scale": 1.1,
        "rot": 45,
        "pos": [10, 10]
      },
      3
    ]
    ```

### Frame-Specific Properties
- **index**: Integer - Frame index
- **frametime**: Integer - Override frametime for this frame
- **easing**: String - Interpolation type
  - "none", "linear", "ease_in", "ease_out", "ease_in_out"
  - "bounce", "elastic", "step", "smooth"
- **move_easing**: String - Movement interpolation
- **rot_easing**: String - Rotation interpolation
- **visible**: Boolean - Override visibility
- **blend**: String - Override blend mode
- **scale**: Float - Frame-specific scale
- **rot**: Integer - Frame-specific rotation
- **pos**: [x, y] - Frame-specific position
- **z_index**: Float - Frame-specific z-index
- **interpolate**: Boolean - Frame-specific interpolation

## Supported GUIs

```
minecraft:inventory              - Player inventory
minecraft:creative_inventory     - Creative mode inventory
minecraft:chest                  - Chest
minecraft:large_chest           - Large chest
minecraft:ender_chest           - Ender chest
minecraft:barrel                - Barrel
minecraft:shulker_box           - Shulker box
minecraft:crafting_table        - Crafting table
minecraft:smithing_table        - Smithing table
minecraft:cartography_table     - Cartography table
minecraft:loom                  - Loom
minecraft:stonecutter           - Stonecutter
minecraft:furnace               - Furnace
minecraft:blast_furnace         - Blast furnace
minecraft:smoker                - Smoker
minecraft:anvil                 - Anvil
minecraft:grindstone            - Grindstone
minecraft:enchanting_table      - Enchanting table
minecraft:brewing_stand         - Brewing stand
minecraft:beacon                - Beacon
minecraft:villager              - Villager trading
minecraft:horse                 - Horse inventory
minecraft:hopper                - Hopper
minecraft:dispenser             - Dispenser
minecraft:dropper               - Dropper
minecraft:command_block         - Command block
minecraft:*                     - All screens (default)
```

## Frametime Reference

Frametime is measured in game ticks (50ms per tick):
- frametime: 1 → 50ms per frame
- frametime: 2 → 100ms per frame
- frametime: 20 → 1 second per frame

## Example Assets

### Simple Static Sprite

Directory: `assets/fsprite/textures/my-sprite/`
Files: `0.png`

Config: `assets/fsprite/my-sprite.mcmeta`
```json
{
  "enabled": true,
  "id": "my-sprite",
  "gui": "minecraft:inventory",
  "anchor": "center",
  "visible": true
}
```

### Animated Sprite with Custom Timing

Directory: `assets/fsprite/textures/flame/`
Files: `0.png`, `1.png`, `2.png`, `3.png`

Config: `assets/fsprite/flame.mcmeta`
```json
{
  "enabled": true,
  "id": "flame",
  "gui": "minecraft:*",
  "anchor": "bottom_right",
  "opacity": 0.8,
  "animation": {
    "frametime": 5,
    "frames": [0, 1, 2, 3]
  }
}
```

### Advanced Frame Animation

```json
{
  "enabled": true,
  "id": "advanced",
  "gui": "minecraft:inventory",
  "anchor": "center",
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
        "easing": "ease_in_out"
      },
      {
        "index": 3,
        "frametime": 15,
        "rot": 360,
        "rot_easing": "linear"
      }
    ]
  }
}
```
