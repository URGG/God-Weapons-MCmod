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

        LOGGER.info("About to initialize ModItems...");
        ModItems.initialize();
        LOGGER.info("ModItems.initialize() completed!");

        LOGGER.info("About to initialize ModScrolls...");
        ModScrolls.initialize();  // This was missing!
        LOGGER.info("ModScrolls.initialize() completed!");
        ModEntities.initialize();
        LOGGER.info("ModEntities.initialize() completed!");
        LOGGER.info("DEVASTATION_DOME_SCROLL is: " + (ModItems.Devastation_DOME_SCROLL != null ? "NOT NULL" : "NULL"));


        //ModBlocks.initialize();




        LOGGER.info("GOD_SWORD is: " + (ModItems.GOD_SWORD != null ? "NOT NULL" : "NULL"));
        LOGGER.info("MAGIC_CRYSTAL is: " + (ModItems.MAGIC_CRYSTAL != null ? "NOT NULL" : "NULL"));
        LOGGER.info("FIREBALL_SCROLL is: " + (ModScrolls.FIREBALL_SCROLL != null ? "NOT NULL" : "NULL"));

        LOGGER.info("=== GODMOD INITIALIZATION COMPLETE ===");
    }
}