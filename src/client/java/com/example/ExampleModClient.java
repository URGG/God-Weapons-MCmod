package com.example;

import com.example.ModEntities;
import com.example.model.GodBossEntityModel;
import com.example.renderer.GodBossEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.EmptyEntityRenderer;
public class ExampleModClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		// Register entity renderer for the throwable axe
		EntityRendererRegistry.register(ModEntities.THROWABLE_GOD_AXE, FlyingItemEntityRenderer::new);

		// Register the boss entity renderer
		EntityRendererRegistry.register(ModEntities.GOD_BOSS, GodBossEntityRenderer::new);


		EntityRendererRegistry.register(ModEntities.GOD_BOSS, GodBossEntityRenderer::new);


		EntityRendererRegistry.register(ModEntities.DEVASTATION_DOME_ENTITY, EmptyEntityRenderer::new);




		System.out.println("GodMod client initialized with entity renderers and custom boss model");
	}
}