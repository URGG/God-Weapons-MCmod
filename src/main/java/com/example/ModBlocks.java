package com.example;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class ModBlocks {

    public static Block BOSS_ALTAR;
    public static Item BOSS_ALTAR_ITEM;

    public static void initialize() {
        System.out.println("Registering blocks for " + GodMod.MOD_ID);

        // Register the boss altar block
        BOSS_ALTAR = registerBlock("boss_altar",
                new BossAltarBlock(AbstractBlock.Settings.create()
                        .strength(5.0f, 6.0f) // Same as obsidian
                        .sounds(BlockSoundGroup.STONE)
                        .requiresTool()
                        .luminance(state -> 7) // Glows slightly
                ));

        // Register the block item (so it can be placed)
        BOSS_ALTAR_ITEM = registerBlockItem("boss_altar", BOSS_ALTAR);

        // Add to creative inventory
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL)
                .register(entries -> entries.add(new ItemStack(BOSS_ALTAR_ITEM)));
    }

    public static Block registerBlock(String name, Block block) {
        Identifier id = Identifier.of(GodMod.MOD_ID, name);
        return Registry.register(Registries.BLOCK, id, block);
    }

    public static Item registerBlockItem(String name, Block block) {
        Identifier id = Identifier.of(GodMod.MOD_ID, name);
        RegistryKey<Item> key = RegistryKey.of(Registries.ITEM.getKey(), id);
        return Registry.register(Registries.ITEM, id,
                new BlockItem(block, new Item.Settings().registryKey(key)));
    }
}