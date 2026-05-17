package ai.log.fsprites.client;

import ai.log.fsprites.FancySprites;
import ai.log.fsprites.client.sprite.SpriteManager;
import ai.log.fsprites.client.sprite.SpriteRegistry;
import ai.log.fsprites.client.sprite.SpriteTextureManager;
import ai.log.fsprites.client.sprite.SpritePersistence;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class FancySpritesClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Initialize sprite systems
		FancySprites.LOGGER.info("Initializing FancySprites client");

		// Initialize texture manager when renderer is ready
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			SpriteTextureManager.initialize(client.getTextureManager());
			SpriteManager.getInstance();
			SpritePersistence.getInstance();
			SpriteRegistry.registerSpritesFromDirectory();
			FancySprites.LOGGER.info("FancySprites systems initialized");
		});

		// Update animations each frame
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// Animation updates are handled in SpriteRenderingManager during rendering
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("fancysprites")
					.then(ClientCommandManager.literal("reload")
						.executes(context -> {
							int reloaded = SpriteRegistry.reloadRegisteredSprites();
							context.getSource().sendFeedback(Component.literal("FancySprites reloaded " + reloaded + " sprite(s)."));
							return 1;
						})));
		});

		FancySprites.LOGGER.info("FancySprites client initialized successfully");
	}
}