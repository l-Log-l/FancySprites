package ai.log.fsprites.client.mixin;

import ai.log.fsprites.client.gui.FancySpriteSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Consumer;

/**
 * Mixin to add FancySprites settings button to the pause screen.
 */
@Mixin(PauseScreen.class)
public class PauseScreenMixin {

    @Redirect(
        method = "createPauseMenu",
        at = @At(value = "INVOKE", target = "net/minecraft/client/gui/layouts/GridLayout.visitWidgets(Ljava/util/function/Consumer;)V")
    )
    private void redirectVisitWidgets(GridLayout gridLayout, Consumer consumer) {
        // First, do the original visitWidgets call
        gridLayout.visitWidgets(consumer);
        
        // Then add FancySprites button
        PauseScreen screen = (PauseScreen) (Object) this;
        Minecraft minecraft = Minecraft.getInstance();

        Button fancySpritesButton = Button.builder(
            Component.literal("FancySprites"),
            (button) -> {
                minecraft.setScreen(new FancySpriteSettingsScreen(screen));
            }
        ).bounds(10, screen.height - 50, 100, 20).build();

        // Call consumer to add the button (simulating what addRenderableWidget does)
        consumer.accept(fancySpritesButton);
    }
}
