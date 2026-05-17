# Sprite Z-Index Layering Bugfix Design

## Overview

This bugfix addresses a critical rendering layer issue where sprites with negative z_index values incorrectly render on top of GUI elements instead of behind them. The root cause is that Minecraft 1.21+ uses render pipeline ORDER for GUI layering, not z-coordinates. The fix introduces a proper render layer system (BACKGROUND, GUI, FOREGROUND) that maps z_index values to appropriate Minecraft GUI render stages, ensuring sprites render at the correct depth relative to GUI elements.

The design replaces fragile mixin injection points with stable Minecraft render method targets and ensures proper GL state management through try/finally blocks.

## Glossary

- **Bug_Condition (C)**: The condition that triggers the bug - when sprites have z_index < 0 but render in the wrong pipeline stage
- **Property (P)**: The desired behavior - sprites should render in the correct layer based on z_index mapping
- **Preservation**: Existing sprite rendering behavior (transformations, animations, blend modes) that must remain unchanged
- **SpriteRenderLayer**: Enum defining three render layers (BACKGROUND, GUI, FOREGROUND) that map to Minecraft's GUI render pipeline stages
- **ContainerScreenMixin**: Mixin class that injects into AbstractContainerScreen render methods to trigger sprite rendering at appropriate pipeline stages
- **SpriteRenderingManager**: Manager class that filters sprites by layer and renders them in z_index sorted order
- **Render Pipeline Order**: Minecraft 1.21+ GUI rendering sequence: renderBackground() → renderBg() → render() → renderLabels() → renderTooltip()

## Bug Details

### Bug Condition

The bug manifests when sprites are assigned z_index values that should place them at different depths relative to GUI elements, but the current implementation does not properly map these values to Minecraft's render pipeline stages. The system either uses incorrect injection points (Matrix3x2fStack.popMatrix()) or fails to sort sprites within each layer.

**Formal Specification:**
```
FUNCTION isBugCondition(sprite)
  INPUT: sprite of type SpriteConfig
  OUTPUT: boolean
  
  RETURN (sprite.zIndex < 0 AND rendersInWrongStage(sprite))
         OR (sprite.zIndex >= 0 AND notSortedWithinLayer(sprite))
         OR usesFragileInjectionPoint()
         OR blendModeNotInFinallyBlock()
END FUNCTION
```

**Specific Bug Manifestations:**

1. **Wrong Render Stage**: Sprites with z_index < 0 (e.g., -999) render in foreground pass instead of background
2. **Fragile Injection Point**: ContainerScreenMixin uses `@Inject(method = "renderContents", at = @At(INVOKE, target = "Matrix3x2fStack.popMatrix()"))` which breaks with Optifine/Sodium
3. **No Within-Layer Sorting**: Sprites in the same render pass are not sorted by z_index
4. **GL State Corruption Risk**: SpriteBlend.resetBlend() called outside finally block
5. **Conflated Concerns**: z_index used for both render stage selection AND sorting priority

### Examples

- **Background Sprite Bug**: Sprite with `z_index: -999` should render behind GUI background texture, but currently renders on top of items
- **Fragile Injection**: `Matrix3x2fStack.popMatrix()` target will break when Optifine changes matrix stack implementation
- **Sorting Bug**: Two sprites with `z_index: 100` and `z_index: 200` in FOREGROUND layer render in undefined order instead of 100 before 200
- **GL State Bug**: If SpriteRenderer.renderSprite() throws exception, blend mode is not reset, corrupting subsequent rendering

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- Sprites with the same z_index value relative to each other must continue to render in the same visual order
- Sprite transformations (blend modes, rotation, scale, anchoring, animation) must continue to work correctly
- Sprite configuration parsing from .mcmeta files must remain unchanged
- Sprites must continue to render on the correct GUI screens (inventory, chest, etc.)
- Compatibility with Fabric API and other mods must be maintained

**Scope:**
All sprite rendering behavior that does NOT involve the render pipeline stage selection should be completely unaffected by this fix. This includes:
- Animation frame updates and playback
- Texture loading and caching
- Position calculations and anchor point handling
- Transformation matrix operations (rotation, scale, translation)
- Blend mode application (the modes themselves, not the cleanup)
- Screen type detection and GUI element positioning

## Hypothesized Root Cause

Based on the bug description and code analysis, the root causes are:

1. **Incorrect Render Pipeline Understanding**: The original implementation used a simple boolean "foreground" flag to split rendering into two passes, which is insufficient for Minecraft 1.21+'s pipeline-order-based GUI rendering system. Minecraft uses method call order (renderBg → render → renderLabels) to determine layering, not z-coordinates.

2. **Fragile Mixin Injection Points**: The injection targeting `Matrix3x2fStack.popMatrix()` is an internal library method that:
   - Is not part of Minecraft's stable API
   - Will break with Optifine (uses different matrix stack)
   - Will break with Sodium (optimizes rendering pipeline)
   - Will break if Mojang refactors matrix handling

3. **Missing Layer-to-Pipeline Mapping**: The system lacks a proper mapping from z_index values to Minecraft's render stages:
   - z_index < 0 should map to BACKGROUND layer (inject at renderBg HEAD)
   - z_index == 0 should map to GUI layer (inject at renderBg TAIL)
   - z_index > 0 should map to FOREGROUND layer (inject at renderLabels TAIL)

4. **No Within-Layer Sorting**: Even if sprites are in the correct layer, they are not sorted by z_index within that layer, causing undefined rendering order.

5. **GL State Management**: The blend mode reset is not in a finally block, risking GL state corruption if rendering throws an exception.

## Correctness Properties

Property 1: Bug Condition - Z-Index Layer Mapping

_For any_ sprite where z_index < 0, the fixed system SHALL render it in the BACKGROUND layer (before GUI background texture). _For any_ sprite where z_index == 0, the fixed system SHALL render it in the GUI layer (alongside GUI elements). _For any_ sprite where z_index > 0, the fixed system SHALL render it in the FOREGROUND layer (after GUI contents). Within each layer, sprites SHALL be sorted by z_index in ascending order.

**Validates: Requirements 2.1, 2.2, 2.3, 2.4**

Property 2: Preservation - Sprite Rendering Behavior

_For any_ sprite configuration that does NOT involve render pipeline stage selection (animations, transformations, blend modes, positioning, screen targeting), the fixed code SHALL produce exactly the same visual result as the original code, preserving all existing sprite rendering functionality.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5**

## Fix Implementation

### Changes Required

The fix requires changes to three main components:

**File**: `src/client/java/ai/log/fsprites/client/mixin/ContainerScreenMixin.java`

**Current Implementation Issues**:
- Uses fragile `Matrix3x2fStack.popMatrix()` injection point
- Only has two injection points (background and foreground)
- Does not align with Minecraft's actual render pipeline stages

**Specific Changes**:
1. **Replace Injection Points**: Remove fragile `Matrix3x2fStack.popMatrix()` injection and use stable Minecraft render methods:
   - BACKGROUND layer: `@Inject(method = "renderBg", at = @At("HEAD"))` - before GUI background
   - GUI layer: `@Inject(method = "renderBg", at = @At("TAIL"))` - after GUI background, before items
   - FOREGROUND layer: `@Inject(method = "renderLabels", at = @At("TAIL"))` - after labels, before tooltips

2. **Add Layer Parameter**: Each injection method should pass the appropriate SpriteRenderLayer enum value to SpriteRenderingManager

3. **State Isolation**: Each render stage should be independent with proper GL state cleanup

**File**: `src/client/java/ai/log/fsprites/client/sprite/SpriteRenderLayer.java`

**Current Implementation**: Already exists with correct structure (BACKGROUND, GUI, FOREGROUND enum and fromZIndex method)

**Verification Needed**: Ensure fromZIndex mapping is correct:
- z_index < 0 → BACKGROUND
- z_index == 0 → GUI  
- z_index > 0 → FOREGROUND

**File**: `src/client/java/ai/log/fsprites/client/SpriteRenderingManager.java`

**Current Implementation Issues**:
- Contains undefined `foreground` variable reference (compilation error)
- May not properly filter sprites by layer
- May not sort sprites within each layer

**Specific Changes**:
1. **Fix Compilation Error**: Remove or fix the undefined `foreground` variable reference in anchor calculation
2. **Layer Filtering**: Ensure sprites are filtered by SpriteRenderLayer using `SpriteRenderLayer.fromZIndex(sprite.zIndex) == layer`
3. **Within-Layer Sorting**: Ensure sprites are sorted by z_index in ascending order within each layer using `.sorted((a, b) -> Integer.compare(a.zIndex, b.zIndex))`
4. **Verify Layer Parameter**: Ensure renderSpritesForLayer accepts and uses the SpriteRenderLayer parameter correctly

**File**: `src/client/java/ai/log/fsprites/client/sprite/SpriteRenderer.java`

**Current Implementation Issue**:
- SpriteBlend.resetBlend() is in a finally block (CORRECT - no change needed here)

**Verification Needed**: Confirm the finally block structure is correct and blend mode reset happens even on exception

### Architecture Summary

The correct render pipeline order from AbstractContainerScreen is:
1. `renderBackground()` - screen background (dark overlay)
2. `renderBg()` - GUI background (inventory texture)
3. `render()` - main rendering (slots, items)
4. `renderLabels()` - text labels
5. `renderTooltip()` - tooltips on top

Our injection points map to this pipeline:
- **BACKGROUND layer**: Inject at `renderBg` HEAD - sprites render before GUI background texture
- **GUI layer**: Inject at `renderBg` TAIL - sprites render after GUI background, before items
- **FOREGROUND layer**: Inject at `renderLabels` TAIL - sprites render after everything, before tooltips

This ensures sprites respect Minecraft's pipeline-order-based layering system.

## Testing Strategy

### Validation Approach

The testing strategy follows a two-phase approach: first, surface counterexamples that demonstrate the bug on unfixed code, then verify the fix works correctly and preserves existing behavior.

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples that demonstrate the bug BEFORE implementing the fix. Confirm or refute the root cause analysis. If we refute, we will need to re-hypothesize.

**Test Plan**: Write tests that create sprites with various z_index values and verify they render in the correct pipeline stage. Run these tests on the UNFIXED code to observe failures and understand the root cause.

**Test Cases**:
1. **Background Sprite Test**: Create sprite with z_index = -999, verify it renders BEFORE GUI background texture (will fail on unfixed code - sprite renders on top)
2. **GUI Sprite Test**: Create sprite with z_index = 0, verify it renders AFTER GUI background but BEFORE items (may pass or fail depending on current implementation)
3. **Foreground Sprite Test**: Create sprite with z_index = 100, verify it renders AFTER items and labels (may pass or fail)
4. **Within-Layer Sorting Test**: Create two sprites with z_index = 100 and z_index = 200 in same layer, verify 100 renders before 200 (will fail on unfixed code - undefined order)
5. **Fragile Injection Test**: Verify current injection point uses Matrix3x2fStack.popMatrix() (will confirm fragility)

**Expected Counterexamples**:
- Sprite with z_index = -999 renders on top of GUI elements instead of behind them
- Sprites in same layer render in undefined order instead of z_index sorted order
- Possible causes: wrong injection points, no layer-to-pipeline mapping, no within-layer sorting

### Fix Checking

**Goal**: Verify that for all inputs where the bug condition holds, the fixed function produces the expected behavior.

**Pseudocode:**
```
FOR ALL sprite WHERE isBugCondition(sprite) DO
  result := renderSprite_fixed(sprite)
  ASSERT expectedLayerBehavior(result, sprite.zIndex)
  ASSERT sortedWithinLayer(result)
  ASSERT usesStableInjectionPoints()
  ASSERT blendModeInFinallyBlock()
END FOR
```

### Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold, the fixed function produces the same result as the original function.

**Pseudocode:**
```
FOR ALL sprite WHERE NOT isBugCondition(sprite) DO
  ASSERT renderSprite_original(sprite) = renderSprite_fixed(sprite)
END FOR
```

**Testing Approach**: Property-based testing is recommended for preservation checking because:
- It generates many test cases automatically across the input domain
- It catches edge cases that manual unit tests might miss
- It provides strong guarantees that behavior is unchanged for all non-buggy inputs

**Test Plan**: Observe behavior on UNFIXED code first for sprite transformations, animations, and positioning, then write property-based tests capturing that behavior.

**Test Cases**:
1. **Animation Preservation**: Observe that animated sprites cycle through frames correctly on unfixed code, then write test to verify this continues after fix
2. **Transformation Preservation**: Observe that rotation, scale, and anchor transformations work correctly on unfixed code, then write test to verify this continues after fix
3. **Blend Mode Preservation**: Observe that blend modes (NORMAL, ADD, MULTIPLY) apply correctly on unfixed code, then write test to verify this continues after fix
4. **Screen Targeting Preservation**: Observe that sprites render on correct GUI screens on unfixed code, then write test to verify this continues after fix

### Unit Tests

- Test SpriteRenderLayer.fromZIndex() mapping for various z_index values (-999, -1, 0, 1, 100)
- Test ContainerScreenMixin injection points are at correct methods (renderBg HEAD/TAIL, renderLabels TAIL)
- Test SpriteRenderingManager filters sprites by layer correctly
- Test SpriteRenderingManager sorts sprites within layer by z_index
- Test SpriteRenderer blend mode reset happens in finally block

### Property-Based Tests

- Generate random z_index values and verify correct layer mapping (BACKGROUND for z < 0, GUI for z == 0, FOREGROUND for z > 0)
- Generate random sprite configurations and verify preservation of transformations (rotation, scale, position)
- Generate random animation configurations and verify frame playback continues correctly
- Test that all blend modes continue to work across many sprite configurations

### Integration Tests

- Test full rendering pipeline with sprites at all three layers (background, GUI, foreground)
- Test switching between different GUI screens (inventory, chest, furnace) with sprites configured for each
- Test that sprites render correctly with Fabric API loaded
- Test visual layering by capturing rendered frames and verifying pixel depth order
