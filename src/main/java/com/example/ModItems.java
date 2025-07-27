package com.example;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.function.Function;

public class ModItems {

    // Fixed ToolMaterial constructor for 1.21.4
    public static final ToolMaterial GOD_MATERIAL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL, // incorrectBlocksForDrops
            2000,       // durability
            12.0f,      // speed (mining speed)
            10.0f,      // attackDamageBonus (additional attack damage)
            22,         // enchantmentValue (enchantability)
            ItemTags.NETHERITE_TOOL_MATERIALS // repairItems
    );

    // Declare items
    public static Item GOD_SWORD;
    public static Item MAGIC_CRYSTAL;

    public static void initialize() {
        System.out.println("Registering items for " + GodMod.MOD_ID);

        // Register items using simple method
        GOD_SWORD = registerItem("god_sword", key -> new GodSwordItem(GOD_MATERIAL, new Item.Settings().registryKey(key).maxCount(1)));
        MAGIC_CRYSTAL = registerItem("magic_crystal", key -> new Item(new Item.Settings().registryKey(key)));

        // Add to creative tabs using ItemStack instead of ItemConvertible
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT)
                .register(entries -> entries.add(new ItemStack(GOD_SWORD)));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS)
                .register(entries -> entries.add(new ItemStack(MAGIC_CRYSTAL)));
    }

    // Simplified registration method for 1.21.4
    public static Item registerItem(String name, Function<RegistryKey<Item>, Item> function) {
        Identifier id = Identifier.of(GodMod.MOD_ID, name);
        RegistryKey<Item> key = RegistryKey.of(Registries.ITEM.getKey(), id);
        return Registry.register(Registries.ITEM, id, function.apply(key));
    }

    public static class GodSwordItem extends SwordItem {
        public GodSwordItem(ToolMaterial material, Item.Settings settings) {
            // 1.21.4 constuctor
            super(material, 2000.0f, -2.4f, settings);
        }

        @Override
        public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            World world = target.getWorld();
            if (!world.isClient() && world instanceof ServerWorld serverWorld) {
                // Damage method for 1.21.4
                target.damage(serverWorld, serverWorld.getDamageSources().generic(), 2000.0f);
            }
            return super.postHit(stack, target, target);
        }
    }
}