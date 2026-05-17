package ai.log.fsprites.client.mixin;

import ai.log.fsprites.client.SpriteRenderingManager;
import ai.log.fsprites.client.sprite.SpriteRenderLayer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to render sprites on container/inventory screens.
 *
 * Render order inside AbstractContainerScreen:
 *
 *   Screen.render()
 *     └─ renderBackground()
 *           ├─ super.renderBackground()   ← затемнение экрана
 *           └─ renderBg()                 ← текстура GUI-панели
 *     └─ renderContents()
 *           ├─ super.render()
 *           ├─ renderLabels()
 *           ├─ renderSlotHighlightBack()
 *           ├─ renderSlots()
 *           └─ renderSlotHighlightFront()
 *     └─ renderCarriedItem()
 *     └─ renderSnapbackItem()
 *
 * Слои спрайтов:
 *   PRE_BACKGROUND  (z_index <= -2) — до затемнения
 *   BACKGROUND      (z_index == -1) — после затемнения, до GUI-панели
 *   GUI             (z_index ==  0) — после GUI-панели, до слотов
 *   FOREGROUND      (z_index  >  0) — после всего содержимого, до переносимого предмета
 */
@Mixin(AbstractContainerScreen.class)
public class ContainerScreenMixin {

    /**
     * PRE_BACKGROUND: рендерится до затемнения экрана (z_index <= -2).
     * HEAD метода renderBackground — самая ранняя точка.
     */
    @Inject(
            method = "renderBackground",
            at = @At("HEAD")
    )
    private void onPreBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        SpriteRenderingManager.renderSpritesForLayer(guiGraphics, screen, SpriteRenderLayer.PRE_BACKGROUND);
    }

    /**
     * BACKGROUND: рендерится после затемнения, но до текстуры GUI-панели (z_index == -1).
     *
     * Встраиваемся сразу после вызова super.renderBackground() внутри
     * AbstractContainerScreen.renderBackground(), то есть после того как
     * затемнение уже нарисовано, но до renderBg().
     *
     * Если маппинги отличаются от Mojang, замени владельца:
     *   Yarn:        net/minecraft/client/gui/screen/Screen
     *   Intermediary: net/minecraft/class_437
     */
    @Inject(
            method = "renderBackground",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;renderBackground(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onAfterDimming(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        SpriteRenderingManager.renderSpritesForLayer(guiGraphics, screen, SpriteRenderLayer.BACKGROUND);
    }

    /**
     * GUI: рендерится после текстуры GUI-панели, до слотов и предметов (z_index == 0).
     * TAIL метода renderBackground — после renderBg().
     */
    @Inject(
            method = "renderBackground",
            at = @At("TAIL")
    )
    private void onAfterBg(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        SpriteRenderingManager.renderSpritesForLayer(guiGraphics, screen, SpriteRenderLayer.GUI);
    }

    /**
     * FOREGROUND: рендерится после всего содержимого контейнера, до переносимого предмета (z_index > 0).
     * RETURN метода renderContents — после слотов, подсветки, лейблов.
     */
    @Inject(
            method = "renderContents",
            at = @At("RETURN")
    )
    private void onRenderContentsReturn(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        SpriteRenderingManager.renderSpritesForLayer(guiGraphics, screen, SpriteRenderLayer.FOREGROUND);
    }
}