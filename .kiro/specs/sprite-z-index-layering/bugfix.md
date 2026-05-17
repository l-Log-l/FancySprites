# Bugfix Requirements Document

## Introduction

This bugfix addresses a critical rendering layer issue where sprites with negative z_index values (e.g., -999) incorrectly render on top of GUI elements instead of behind them. The root cause is that the current implementation uses a simple boolean "foreground" flag to split rendering into two passes, which is insufficient for Minecraft 1.21+'s pipeline-order-based GUI rendering system. Additionally, the current mixin injection point targeting `Matrix3x2fStack.popMatrix()` is fragile and will break with Optifine, Sodium, or Mojang refactors.

The fix will introduce a proper render layer system that maps z_index values to appropriate Minecraft GUI render stages (BACKGROUND, GUI, FOREGROUND), ensuring sprites render at the correct depth relative to GUI elements while maintaining stable mixin injection points.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN a sprite has z_index < 0 (e.g., -999) THEN the system renders it in the foreground pass instead of behind GUI backgrounds

1.2 WHEN the ContainerScreenMixin uses the `Matrix3x2fStack.popMatrix()` injection target THEN the system is vulnerable to breakage from Optifine, Sodium, or Mojang refactors

1.3 WHEN z_index is used to determine render stage (via `isForeground = sprite.zIndex >= 0`) THEN the system conflates three separate concerns: render stage selection, sprite sorting priority, and GUI depth

1.4 WHEN SpriteRenderer.renderSprite() completes or throws an exception THEN the system calls SpriteBlend.resetBlend() outside a finally block, risking GL state corruption

1.5 WHEN sprites with different z_index values are in the same render pass THEN the system does not sort them by z_index within that pass

### Expected Behavior (Correct)

2.1 WHEN a sprite has z_index < 0 THEN the system SHALL render it in a BACKGROUND layer that executes before GUI background rendering

2.2 WHEN a sprite has z_index == 0 THEN the system SHALL render it in a GUI layer that executes alongside normal GUI element rendering

2.3 WHEN a sprite has z_index > 0 THEN the system SHALL render it in a FOREGROUND layer that executes after GUI contents (above items/tooltips)

2.4 WHEN multiple sprites exist in the same render layer THEN the system SHALL sort them by z_index in ascending order before rendering

2.5 WHEN ContainerScreenMixin injects into render methods THEN the system SHALL use stable Minecraft render method targets instead of internal library methods

2.6 WHEN SpriteRenderer.renderSprite() executes THEN the system SHALL reset blend modes in a finally block to prevent GL state corruption

### Unchanged Behavior (Regression Prevention)

3.1 WHEN sprites have the same z_index value as before the fix THEN the system SHALL CONTINUE TO render them at the same visual position relative to each other

3.2 WHEN sprites use blend modes, rotation, scale, anchoring, or animation THEN the system SHALL CONTINUE TO apply these transformations correctly

3.3 WHEN sprites are configured for different GUI screens (inventory, chest, etc.) THEN the system SHALL CONTINUE TO render them on the correct screens

3.4 WHEN sprite configuration files (.mcmeta) are loaded THEN the system SHALL CONTINUE TO parse all existing properties without errors

3.5 WHEN the mod runs alongside Fabric API and other mods THEN the system SHALL CONTINUE TO function without conflicts
