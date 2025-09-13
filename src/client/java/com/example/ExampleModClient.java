package com.example;

import net.fabricmc.api.ClientModInitializer;

public class ExampleModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Skip custom renderer for now - will use default entity rendering
		System.out.println("GodMod client initialized - using default rendering");
	}
}