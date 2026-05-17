package ai.log.fsprites.client.sprite;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;


import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
/**
 * Blend modes for sprite rendering.
 *
 * Each mode has its own RenderPipeline built from GUI_TEXTURED_SNIPPET.
 * Use getPipeline() and pass it directly to GuiGraphics.blit().
 */
public enum SpriteBlend {

    NORMAL("normal") {
        @Override
        public RenderPipeline getPipeline() {
            return RenderPipelines.GUI_TEXTURED;
        }
    },

    ADDITIVE("additive") {
        @Override
        public RenderPipeline getPipeline() {
            return Pipelines.ADDITIVE;
        }
    },

    MULTIPLY("multiply") {
        @Override
        public RenderPipeline getPipeline() {
            return Pipelines.MULTIPLY;
        }
    },

    SCREEN("screen") {
        @Override
        public RenderPipeline getPipeline() {
            return Pipelines.SCREEN;
        }
    };

    public final String id;

    SpriteBlend(String id) {
        this.id = id;
    }

    public abstract RenderPipeline getPipeline();

    public static SpriteBlend fromString(String name) {
        if (name == null) return NORMAL;
        for (SpriteBlend blend : values()) {
            if (blend.id.equalsIgnoreCase(name)) return blend;
        }
        return NORMAL;
    }

    /**
     * Custom pipelines for non-standard blend modes.
     *
     * IMPORTANT: withLocation() must receive an Identifier, not a plain string.
     * withLocation(String) calls Identifier.withDefaultNamespace() which prepends
     * "minecraft:" — resulting in "minecraft:fsprites:..." which is invalid.
     * Use Identifier.fromNamespaceAndPath("fsprites", "pipeline/...") instead.
     */
    private static final class Pipelines {

        static final RenderPipeline ADDITIVE = RenderPipelines.register(
                RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                        .withLocation(Identifier.fromNamespaceAndPath("fsprites", "pipeline/gui_textured_additive"))
                        .withBlend(BlendFunction.ADDITIVE)
                        .build()
        );

        static final RenderPipeline MULTIPLY = RenderPipelines.register(
                RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                        .withLocation(Identifier.fromNamespaceAndPath("fsprites", "pipeline/gui_textured_multiply"))
                        .withBlend(new BlendFunction(SourceFactor.DST_COLOR, DestFactor.ZERO, SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA))
                        .build()
        );

        static final RenderPipeline SCREEN = RenderPipelines.register(
                RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                        .withLocation(Identifier.fromNamespaceAndPath("fsprites", "pipeline/gui_textured_screen"))
                        .withBlend(new BlendFunction(SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_COLOR, SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA))
                        .build()
        );
    }
}