package com.example;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GodMod implements ModInitializer {
    public static final String MOD_ID = "godmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("=== GODMOD STARTING TO INITIALIZE ===");
        LOGGER.info("Hello Fabric world!");

        // Initialize mod items
        LOGGER.info("About to initialize ModItems...");
        ModItems.initialize();
        LOGGER.info("ModItems.initialize() completed!");

        // Check if items were created
        LOGGER.info("GOD_SWORD is: " + (ModItems.GOD_SWORD != null ? "NOT NULL" : "NULL"));
        LOGGER.info("MAGIC_CRYSTAL is: " + (ModItems.MAGIC_CRYSTAL != null ? "NOT NULL" : "NULL"));

        LOGGER.info("=== GODMOD INITIALIZATION COMPLETE ===");
    }
}