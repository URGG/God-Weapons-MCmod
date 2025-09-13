package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;

public class ExampleModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Register entity renderer for the throwable axe
		EntityRendererRegistry.register(ModEntities.THROWABLE_GOD_AXE, FlyingItemEntityRenderer::new);

		System.out.println("GodMod client initialized with entity renderers");
	}
}