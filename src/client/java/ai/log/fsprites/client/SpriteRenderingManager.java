package ai.log.fsprites.client;

import ai.log.fsprites.FancySprites;
import ai.log.fsprites.client.sprite.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.lwjgl.opengl.GL11;

import java.util.*;

/**
 * Manages sprite rendering for all screens.
 */
public class SpriteRenderingManager {
    private static final boolean DEBUG_RENDER = false;
    private static long lastDebugLogMs = 0L;
    private static final Map<String, SpriteAnimationPlayer> animationPlayers = new HashMap<>();
    private static final SpriteManager spriteManager = SpriteManager.getInstance();

    /**
     * Render sprites for a screen at a specific render layer. Called from mixins.
     */
    public static void renderSpritesForLayer(GuiGraphics guiGraphics, Screen screen, SpriteRenderLayer layer) {
        try {
            String screenName = getScreenIdentifier(screen);
            List<SpriteConfig> sprites = spriteManager.getSpritesForGui(screenName);
            SpritePersistence persistence = SpritePersistence.getInstance();

            if (DEBUG_RENDER && screen instanceof AbstractContainerScreen<?>) {
                long now = System.currentTimeMillis();
                if (now - lastDebugLogMs > 1000L) {
                    FancySprites.LOGGER.info("[FancySprites DEBUG] screen={} sprites={} layer={}", 
                            screenName, sprites.size(), layer);
                    lastDebugLogMs = now;
                }
            }

            // Filter sprites by layer and sort by z_index within the layer
            List<SpriteConfig> layerSprites = sprites.stream()
                    .filter(sprite -> {
                        SpriteRenderLayer spriteLayer = SpriteRenderLayer.fromZIndex(sprite.zIndex);
                        boolean matches = spriteLayer == layer;
                        if (DEBUG_RENDER && layer == SpriteRenderLayer.FOREGROUND) {
                            FancySprites.LOGGER.info("[FancySprites FILTER] sprite={} zIndex={} spriteLayer={} requestedLayer={} matches={}", 
                                    sprite.id, sprite.zIndex, spriteLayer, layer, matches);
                        }
                        return matches;
                    })
                    .sorted((a, b) -> Integer.compare(a.zIndex, b.zIndex))
                    .toList();

            if (DEBUG_RENDER && layer == SpriteRenderLayer.FOREGROUND) {
                FancySprites.LOGGER.info("[FancySprites FILTER] After filtering: layerSprites.size()={} for layer={}", 
                        layerSprites.size(), layer);
            }

            for (SpriteConfig sprite : layerSprites) {
                if (!persistence.isEnabled(sprite.id)) {
                    continue;
                }

                // Get or create animation player
                SpriteAnimationPlayer player = animationPlayers.computeIfAbsent(
                        sprite.id, k -> new SpriteAnimationPlayer(sprite)
                );

                // Update animation
                player.update();

                // Calculate anchor position
                int anchorX = screen.width / 2;  // Will be adjusted based on screen type
                int anchorY = screen.height / 2; // Will be adjusted based on screen type
                int targetWidth = screen.width;
                int targetHeight = screen.height;

                if (screen instanceof AbstractContainerScreen<?> containerScreen) {
                    int leftPos = 0;
                    int topPos = 0;
                    int imageWidth = 176;
                    int imageHeight = 166;
                    
                    try {
                        var field = AbstractContainerScreen.class.getDeclaredField("leftPos");
                        field.setAccessible(true);
                        leftPos = field.getInt(containerScreen);
                        
                        var topField = AbstractContainerScreen.class.getDeclaredField("topPos");
                        topField.setAccessible(true);
                        topPos = topField.getInt(containerScreen);
                        
                        var widthField = AbstractContainerScreen.class.getDeclaredField("imageWidth");
                        widthField.setAccessible(true);
                        imageWidth = widthField.getInt(containerScreen);
                        
                        var heightField = AbstractContainerScreen.class.getDeclaredField("imageHeight");
                        heightField.setAccessible(true);
                        imageHeight = heightField.getInt(containerScreen);
                    } catch (Exception e) {
                        // Use defaults if fields cannot be accessed
                    }

                    targetWidth = imageWidth;
                    targetHeight = imageHeight;
                    
                    int baseX = leftPos;  // TODO: Bug - undefined 'foreground' variable removed temporarily for testing
                    int baseY = topPos;   // TODO: Bug - undefined 'foreground' variable removed temporarily for testing

                    switch (sprite.anchor) {
                        case TOP_LEFT:
                            anchorX = baseX;
                            anchorY = baseY;
                            break;
                        case TOP:
                            anchorX = baseX + imageWidth / 2;
                            anchorY = baseY;
                            break;
                        case TOP_RIGHT:
                            anchorX = baseX + imageWidth;
                            anchorY = baseY;
                            break;
                        case LEFT:
                            anchorX = baseX;
                            anchorY = baseY + imageHeight / 2;
                            break;
                        case CENTER:
                            anchorX = baseX + imageWidth / 2;
                            anchorY = baseY + imageHeight / 2;
                            break;
                        case RIGHT:
                            anchorX = baseX + imageWidth;
                            anchorY = baseY + imageHeight / 2;
                            break;
                        case BOTTOM_LEFT:
                            anchorX = baseX;
                            anchorY = baseY + imageHeight;
                            break;
                        case BOTTOM:
                            anchorX = baseX + imageWidth / 2;
                            anchorY = baseY + imageHeight;
                            break;
                        case BOTTOM_RIGHT:
                            anchorX = baseX + imageWidth;
                            anchorY = baseY + imageHeight;
                            break;
                    }
                }

                // Render sprite
                if (sprite.fit == SpriteFit.TILE) {
                    int maxWidth = sprite.width != null ? sprite.width : sprite.getTextureWidth();
                    int maxHeight = sprite.height != null ? sprite.height : sprite.getTextureHeight();
                    SpriteRenderer.renderSpriteTiled(guiGraphics, sprite, player,
                            screen.width, screen.height, anchorX, anchorY,
                            sprite.fullWidth ? targetWidth : maxWidth,
                            sprite.fullHeight ? targetHeight : maxHeight);
                } else {
                    SpriteRenderer.renderSprite(guiGraphics, sprite, player,
                            screen.width, screen.height, anchorX, anchorY,
                            targetWidth, targetHeight);
                }
            }
        } catch (Exception e) {
            FancySprites.LOGGER.error("Error rendering sprites: {}", e.getMessage(), e);
        }
    }

    /**
     * Render sprites for the in-game HUD.
     */
    public static void renderSpritesForGame(GuiGraphics guiGraphics) {
        renderSpritesForGamePhase(guiGraphics, true);
    }

    /**
     * Render sprites for the in-game HUD either before or after the hotbar.
     */
    public static void renderSpritesForGamePhase(GuiGraphics guiGraphics, boolean afterHotbar) {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client == null || client.getWindow() == null) {
                return;
            }

            int screenWidth = client.getWindow().getGuiScaledWidth();
            int screenHeight = client.getWindow().getGuiScaledHeight();
            List<SpriteConfig> sprites = spriteManager.getSpritesForGui("minecraft:game");
            SpritePersistence persistence = SpritePersistence.getInstance();
            int targetWidth = screenWidth;
            int targetHeight = screenHeight;

            int hotbarWidth = 182;
            int hotbarHeight = 22;
            int hotbarX = screenWidth / 2 - hotbarWidth / 2;
            int hotbarY = screenHeight - hotbarHeight;

            for (SpriteConfig sprite : sprites) {
                if (!persistence.isEnabled(sprite.id)) {
                    continue;
                }

                if (!sprite.visible || sprite.opacity <= 0.0f) {
                    continue;
                }

                boolean spriteAfterHotbar = sprite.zIndex >= 0;
                if (afterHotbar != spriteAfterHotbar) {
                    continue;
                }

                SpriteAnimationPlayer player = animationPlayers.computeIfAbsent(
                        sprite.id, k -> new SpriteAnimationPlayer(sprite)
                );
                player.update();

                int anchorX = 0;
                int anchorY = 0;

                switch (sprite.anchor) {
                    case TOP_LEFT:
                        anchorX = hotbarX;
                        anchorY = hotbarY;
                        break;
                    case TOP:
                        anchorX = hotbarX + hotbarWidth / 2;
                        anchorY = hotbarY;
                        break;
                    case TOP_RIGHT:
                        anchorX = hotbarX + hotbarWidth;
                        anchorY = hotbarY;
                        break;
                    case LEFT:
                        anchorX = hotbarX;
                        anchorY = hotbarY + hotbarHeight / 2;
                        break;
                    case CENTER:
                        anchorX = hotbarX + hotbarWidth / 2;
                        anchorY = hotbarY + hotbarHeight / 2;
                        break;
                    case RIGHT:
                        anchorX = hotbarX + hotbarWidth;
                        anchorY = hotbarY + hotbarHeight / 2;
                        break;
                    case BOTTOM_LEFT:
                        anchorX = hotbarX;
                        anchorY = hotbarY + hotbarHeight;
                        break;
                    case BOTTOM:
                        anchorX = hotbarX + hotbarWidth / 2;
                        anchorY = hotbarY + hotbarHeight;
                        break;
                    case BOTTOM_RIGHT:
                        anchorX = hotbarX + hotbarWidth;
                        anchorY = hotbarY + hotbarHeight;
                        break;
                }

                if (sprite.fit == SpriteFit.TILE) {
                    int maxWidth = sprite.width != null ? sprite.width : sprite.getTextureWidth();
                    int maxHeight = sprite.height != null ? sprite.height : sprite.getTextureHeight();
                    SpriteRenderer.renderSpriteTiled(guiGraphics, sprite, player,
                            screenWidth, screenHeight, anchorX, anchorY,
                            sprite.fullWidth ? targetWidth : maxWidth,
                            sprite.fullHeight ? targetHeight : maxHeight);
                } else {
                    SpriteRenderer.renderSprite(guiGraphics, sprite, player,
                            screenWidth, screenHeight, anchorX, anchorY,
                            targetWidth, targetHeight);
                }
            }
        } catch (Exception e) {
            FancySprites.LOGGER.error("Error rendering game sprites: {}", e.getMessage(), e);
        }
    }

    /**
     * Get a string identifier for the screen type.
     */
    private static String getScreenIdentifier(Screen screen) {
        if (screen == null) {
            return "minecraft:*";
        }

        // Map screen classes to their identifiers
        String screenClass = screen.getClass().getSimpleName();

        return switch (screenClass) {
            case "InventoryScreen", "PlayerInventoryScreen" -> "minecraft:inventory";
            case "CreativeInventoryScreen" -> "minecraft:creative_inventory";
            case "ChestScreen", "Generic3x3ContainerScreen" -> "minecraft:chest";
            case "ShulkerBoxScreen" -> "minecraft:shulker_box";
            case "FurnaceScreen" -> "minecraft:furnace";
            case "BlastFurnaceScreen" -> "minecraft:blast_furnace";
            case "SmokerScreen" -> "minecraft:smoker";
            case "CraftingTableScreen" -> "minecraft:crafting_table";
            case "SmithingTableScreen" -> "minecraft:smithing_table";
            case "CartographyTableScreen" -> "minecraft:cartography_table";
            case "LoomScreen" -> "minecraft:loom";
            case "StonecutterScreen" -> "minecraft:stonecutter";
            case "AnvilScreen" -> "minecraft:anvil";
            case "GrindstoneScreen" -> "minecraft:grindstone";
            case "EnchantmentScreen" -> "minecraft:enchanting_table";
            case "BrewingStandScreen" -> "minecraft:brewing_stand";
            case "BeaconScreen" -> "minecraft:beacon";
            case "MerchantScreen" -> "minecraft:villager";
            case "HorseInventoryScreen" -> "minecraft:horse";
            case "HopperScreen" -> "minecraft:hopper";
            case "DispenserScreen" -> "minecraft:dispenser";
            case "DropperScreen" -> "minecraft:dropper";
            case "CommandBlockScreen" -> "minecraft:command_block";
            case "BarrelScreen" -> "minecraft:barrel";
            case "EnderChestScreen" -> "minecraft:ender_chest";
            default -> "minecraft:*";
        };
    }

    /**
     * Render all sprites with scissor masking to prevent them from appearing over GUI.
     * Sprites are clipped to not render pixels that would overlap the container GUI.
     */
    public static void renderSpritesWithScissorMask(GuiGraphics guiGraphics, Screen screen) {
        try {
            String screenName = getScreenIdentifier(screen);
            List<SpriteConfig> sprites = spriteManager.getSpritesForGui(screenName);
            SpritePersistence persistence = SpritePersistence.getInstance();

            if (sprites.isEmpty()) {
                return;
            }

            // Get GUI bounds if this is a container screen
            int guiX = 0, guiY = 0, guiWidth = 0, guiHeight = 0;
            if (screen instanceof AbstractContainerScreen<?> containerScreen) {
                try {
                    var field = AbstractContainerScreen.class.getDeclaredField("leftPos");
                    field.setAccessible(true);
                    guiX = field.getInt(containerScreen);
                    
                    var topField = AbstractContainerScreen.class.getDeclaredField("topPos");
                    topField.setAccessible(true);
                    guiY = topField.getInt(containerScreen);
                    
                    var widthField = AbstractContainerScreen.class.getDeclaredField("imageWidth");
                    widthField.setAccessible(true);
                    guiWidth = widthField.getInt(containerScreen);
                    
                    var heightField = AbstractContainerScreen.class.getDeclaredField("imageHeight");
                    heightField.setAccessible(true);
                    guiHeight = heightField.getInt(containerScreen);
                    
                    FancySprites.LOGGER.info("[FancySprites DEBUG SCISSOR] GUI bounds: x={} y={} w={} h={}", 
                            guiX, guiY, guiWidth, guiHeight);
                } catch (Exception e) {
                    FancySprites.LOGGER.warn("Failed to get GUI bounds: {}", e.getMessage());
                }
            }
            int targetWidth = guiWidth > 0 ? guiWidth : screen.width;
            int targetHeight = guiHeight > 0 ? guiHeight : screen.height;

            // Sort sprites by z_index
            List<SpriteConfig> sortedSprites = sprites.stream()
                    .sorted((a, b) -> Integer.compare(a.zIndex, b.zIndex))
                    .toList();

            for (SpriteConfig sprite : sortedSprites) {
                if (!persistence.isEnabled(sprite.id)) {
                    continue;
                }

                if (!sprite.visible || sprite.opacity <= 0.0f) {
                    continue;
                }

                // Get or create animation player
                SpriteAnimationPlayer player = animationPlayers.computeIfAbsent(
                        sprite.id, k -> new SpriteAnimationPlayer(sprite)
                );
                player.update();

                // Calculate anchor position
                int anchorX = screen.width / 2;
                int anchorY = screen.height / 2;

                if (screen instanceof AbstractContainerScreen<?> containerScreen) {
                    switch (sprite.anchor) {
                        case TOP_LEFT:
                            anchorX = guiX;
                            anchorY = guiY;
                            break;
                        case TOP:
                            anchorX = guiX + guiWidth / 2;
                            anchorY = guiY;
                            break;
                        case TOP_RIGHT:
                            anchorX = guiX + guiWidth;
                            anchorY = guiY;
                            break;
                        case LEFT:
                            anchorX = guiX;
                            anchorY = guiY + guiHeight / 2;
                            break;
                        case CENTER:
                            anchorX = guiX + guiWidth / 2;
                            anchorY = guiY + guiHeight / 2;
                            break;
                        case RIGHT:
                            anchorX = guiX + guiWidth;
                            anchorY = guiY + guiHeight / 2;
                            break;
                        case BOTTOM_LEFT:
                            anchorX = guiX;
                            anchorY = guiY + guiHeight;
                            break;
                        case BOTTOM:
                            anchorX = guiX + guiWidth / 2;
                            anchorY = guiY + guiHeight;
                            break;
                        case BOTTOM_RIGHT:
                            anchorX = guiX + guiWidth;
                            anchorY = guiY + guiHeight;
                            break;
                    }
                }

                FancySprites.LOGGER.info("[FancySprites DEBUG SCISSOR] Rendering sprite: id={} z_index={} needsScissor={}", 
                        sprite.id, sprite.zIndex, sprite.zIndex < 0);

                // Apply scissor test to clip sprite pixels outside GUI for z_index < 0
                if (sprite.zIndex < 0 && guiWidth > 0 && guiHeight > 0) {
                    FancySprites.LOGGER.info("[FancySprites DEBUG SCISSOR] ENABLING scissor for sprite {}", sprite.id);
                    enableScissorForGui(guiX, guiY, guiWidth, guiHeight, screen.height);
                }

                // Render sprite
                SpriteRenderer.renderSprite(guiGraphics, sprite, player,
                    screen.width, screen.height, anchorX, anchorY,
                    targetWidth, targetHeight);

                // Disable scissor after rendering background layer sprite
                if (sprite.zIndex < 0) {
                    FancySprites.LOGGER.info("[FancySprites DEBUG SCISSOR] DISABLING scissor for sprite {}", sprite.id);
                    GL11.glDisable(GL11.GL_SCISSOR_TEST);
                }
            }
        } catch (Exception e) {
            FancySprites.LOGGER.error("Error rendering sprites with scissor mask: {}", e.getMessage(), e);
        }
    }

    /**
     * Enable scissor test to clip rendering to the GUI bounds.
     * Inverse the Y coordinate because OpenGL uses bottom-left origin.
     */
    private static void enableScissorForGui(int guiX, int guiY, int guiWidth, int guiHeight, int screenHeight) {
        // Flush any pending rendering operations first
        // GuiGraphics might have buffered operations that need to be rendered before we change scissor
        
        // OpenGL scissor: Y is measured from bottom, not top
        int scissorY = screenHeight - guiY - guiHeight;
        
        FancySprites.LOGGER.info("[FancySprites DEBUG SCISSOR] Setting scissor: x={} y={} w={} h={} (screenH={})", 
                guiX, scissorY, guiWidth, guiHeight, screenHeight);
        
        // Make sure GL state is set
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(guiX, scissorY, guiWidth, guiHeight);
        
        // Verify scissor was set
        int[] scissorBox = new int[4];
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox);
        FancySprites.LOGGER.info("[FancySprites DEBUG SCISSOR] Scissor box after set: x={} y={} w={} h={}", 
                scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
    }

    /**
     * Clear animation players (call when reloading textures).
     */
    public static void clear() {
        animationPlayers.clear();
    }
}
