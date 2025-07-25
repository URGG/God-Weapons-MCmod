package com.example;

import java.util.function.Function;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class ModItems {

    // Fixed ToolMaterial constructor for 1.21.4
    public static final ToolMaterial GOD_MATERIAL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, // incorrectBlocksForDrops
            2000,       // durability
            12.0f,      // speed (mining speed)
            10.0f,      // attackDamageBonus (additional attack damage)
            22,         // enchantmentValue (enchantability)
            ItemTags.NETHERITE_TOOL_MATERIALS // repairItems - using ItemTags instead of null
    );

    // Declare items - they will be initialized in the initialize() method
    public static Item GOD_SWORD;
    public static Item MAGIC_CRYSTAL;

    public static void initialize() {
        System.out.println("Registering items for " + GodMod.MOD_ID);

        // Create and register items using the new 1.21.4 pattern
        GOD_SWORD = register("god_sword",
                (settings) -> new GodSwordItem(GOD_MATERIAL, settings),
                new Item.Settings().maxCount(1));

        MAGIC_CRYSTAL = register("magic_crystal",
                Item::new,
                new Item.Settings());

        // Add to creative tabs
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT)
                .register(entries -> entries.add(GOD_SWORD));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS)
                .register(entries -> entries.add(MAGIC_CRYSTAL));
    }


    public static Item register(String name, Function<Item.Settings, Item> itemFactory, Item.Settings settings) {
        // Create the item key
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(GodMod.MOD_ID, name));
        // Create the item instance with registry key set in settings
        Item item = itemFactory.apply(settings.registryKey(itemKey));
        // Register the item
        Registry.register(Registries.ITEM, itemKey, item);
        return item;
    }

    public static class GodSwordItem extends SwordItem {
        public GodSwordItem(ToolMaterial material, Item.Settings settings) {
            // Fixed constructor for 1.21.4 - using attackDamage and attackSpeed parameters
            super(material, 2000.0f, -2.4f, settings);
        }

        @Override
        public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            World world = target.getWorld();
            if (!world.isClient() && world instanceof ServerWorld serverWorld) {
                // Fixed damage method call for 1.21.4
                target.damage(serverWorld, serverWorld.getDamageSources().generic(), 2000.0f);
            }
            return super.postHit(stack, target, attacker);
        }
    }
}