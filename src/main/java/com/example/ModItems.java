package com.example;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
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


    public static Item GOD_SWORD;
    public static Item MAGIC_CRYSTAL;
    public static Item GOD_AXE;

    public static void initialize() {
        System.out.println("Registering items for " + GodMod.MOD_ID);

        // Register only non-scroll items
        GOD_SWORD = registerItem("god_sword", key -> new GodSwordItem(GOD_MATERIAL, new Item.Settings().registryKey(key).maxCount(1)));
        MAGIC_CRYSTAL = registerItem("magic_crystal", key -> new Item(new Item.Settings().registryKey(key)));
        GOD_AXE = registerItem("god_axe", key ->new GodAxe(GOD_MATERIAL, new Item.Settings().registryKey(key).maxCount(1)));



        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT)
                .register(entries -> entries.add(new ItemStack(GOD_SWORD)));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT)
                        .register(entries -> entries.add(new ItemStack(GOD_AXE)));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS)
                .register(entries -> entries.add(new ItemStack(MAGIC_CRYSTAL)));
    }


    public static Item registerItem(String name, Function<RegistryKey<Item>, Item> function) {
        Identifier id = Identifier.of(GodMod.MOD_ID, name);
        RegistryKey<Item> key = RegistryKey.of(Registries.ITEM.getKey(), id);
        return Registry.register(Registries.ITEM, id, function.apply(key));
    }

    public static class GodAxe extends AxeItem {
        public GodAxe(ToolMaterial material, Item.Settings settings) {

            super(material, 21000.0f, -2.5f, settings);
        }

        @Override
        public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker){
            World world = target.getWorld();
            if(!world.isClient() && world instanceof ServerWorld serverWorld) {

                target.damage(serverWorld, serverWorld.getDamageSources().generic(), 2100.0f);
            }
            return super.postHit(stack, target, attacker);
        }


    }

    public static class GodSwordItem extends SwordItem {
        public GodSwordItem(ToolMaterial material, Item.Settings settings) {

            super(material, 2000.0f, -2.4f, settings);
        }

        @Override
        public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            World world = target.getWorld();
            if (!world.isClient() && world instanceof ServerWorld serverWorld) {
                // Damage method for 1.21.4
                target.damage(serverWorld, serverWorld.getDamageSources().generic(), 2000.0f);
            }
            return super.postHit(stack, target, attacker);
        }
    }
}