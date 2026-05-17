package ai.log.fsprites.client.mixin;

import ai.log.fsprites.client.SpriteRenderingManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to render sprites on the in-game HUD.
 */
@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "renderHotbarAndDecorations", at = @At("HEAD"))
    private void onRenderHotbarBefore(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        SpriteRenderingManager.renderSpritesForGamePhase(guiGraphics, false);
    }

    @Inject(method = "renderHotbarAndDecorations", at = @At("TAIL"))
    private void onRenderHotbarAfter(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        SpriteRenderingManager.renderSpritesForGamePhase(guiGraphics, true);
    }
}