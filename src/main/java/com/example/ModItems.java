package com.example;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
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
        public ActionResult use(World world, PlayerEntity user, Hand hand) {
            ItemStack itemStack = user.getStackInHand(hand);

            if (!world.isClient()) {
                try {
                    // Create the simple throwable axe entity
                    ThrowableGodAxeEntity thrownAxe = new ThrowableGodAxeEntity(world, user);

                    // Set velocity manually
                    thrownAxe.setVelocity(user, user.getPitch(), user.getYaw(), 0.0f, 1.5f, 1.0f);

                    // Spawn the entity
                    world.spawnEntity(thrownAxe);

                    // Play throw sound
                    world.playSound(null, user.getBlockPos(),
                            SoundEvents.ENTITY_SNOWBALL_THROW,
                            SoundCategory.PLAYERS, 0.5f, 0.4f / (world.getRandom().nextFloat() * 0.4f + 0.8f));

                    // Remove the axe from inventory
                    if (!user.getAbilities().creativeMode) {
                        itemStack.decrement(1);
                    }

                } catch (Exception e) {
                    System.err.println("Error throwing axe: " + e.getMessage());
                    e.printStackTrace();
                    return ActionResult.FAIL;
                }
            }

            user.incrementStat(net.minecraft.stat.Stats.USED.getOrCreateStat(this));
            return ActionResult.SUCCESS;
        }

        @Override
        public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker){
            World world = target.getWorld();
            if(!world.isClient() && world instanceof ServerWorld serverWorld) {
                // Deal damage
                target.damage(serverWorld, serverWorld.getDamageSources().generic(), 2100.0f);

                // Purple particle effects for the axe
                serverWorld.spawnParticles(
                        ParticleTypes.WITCH,              // Purple witch particles
                        target.getX(),
                        target.getY() + target.getHeight() / 2,
                        target.getZ(),
                        12,                               // Particle count
                        0.5,                              // X spread
                        0.5,                              // Y spread
                        0.5,                              // Z spread
                        0.1                               // Speed
                );

                // Add some portal particles (also purple)
                serverWorld.spawnParticles(
                        ParticleTypes.PORTAL,
                        target.getX(),
                        target.getY() + target.getHeight(),
                        target.getZ(),
                        8,
                        0.3,
                        0.3,
                        0.3,
                        0.05
                );

                // Sound effects for the axe - mystical/magical sounds
                serverWorld.playSound(
                        null,                              // Player (null = all players can hear)
                        target.getBlockPos(),              // Position
                        SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, // Mystical enchanting sound
                        SoundCategory.PLAYERS,             // Sound category
                        0.8f,                              // Volume
                        1.2f + world.random.nextFloat() * 0.3f // Pitch (slightly higher with randomness)
                );

                // Additional ominous sound for the god axe
                serverWorld.playSound(
                        null,
                        target.getBlockPos(),
                        SoundEvents.ENTITY_WITHER_HURT,    // Deep, ominous sound
                        SoundCategory.PLAYERS,
                        0.4f,                              // Lower volume for atmosphere
                        0.7f + world.random.nextFloat() * 0.2f // Lower pitch
                );
            }
            return super.postHit(stack, target, attacker);
        }
    }

    public static class GodSwordItem extends SwordItem {
        private int lastAttackTime = 0;

        public GodSwordItem(ToolMaterial material, Item.Settings settings) {
            super(material, 2000.0f, -2.4f, settings);
        }

        @Override
        public void inventoryTick(ItemStack stack, World world, net.minecraft.entity.Entity entity, int slot, boolean selected) {
            if (selected && entity instanceof PlayerEntity player && !world.isClient()) {
                ServerWorld serverWorld = (ServerWorld) world;

                // Check if player is attacking (left-clicking)
                if (player.getAttackCooldownProgress(0.0F) > 0.9F && player.handSwinging && lastAttackTime != player.age) {
                    lastAttackTime = player.age;

                    // Swing sound effect
                    serverWorld.playSound(
                            null,
                            player.getBlockPos(),
                            SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                            SoundCategory.PLAYERS,
                            0.7f,
                            1.0f + world.random.nextFloat() * 0.3f
                    );

                    // Fire particle trail when swinging
                    serverWorld.spawnParticles(
                            ParticleTypes.FLAME,
                            player.getX(),
                            player.getY() + player.getHeight() / 2,
                            player.getZ(),
                            6,
                            1.0,
                            0.5,
                            1.0,
                            0.05
                    );

                    // Some smoke for effect
                    serverWorld.spawnParticles(
                            ParticleTypes.SMOKE,
                            player.getX(),
                            player.getY() + player.getHeight() / 2,
                            player.getZ(),
                            3,
                            0.8,
                            0.3,
                            0.8,
                            0.02
                    );
                }
            }
            super.inventoryTick(stack, world, entity, slot, selected);
        }

        @Override
        public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
            // This triggers when you finish using/swinging the item
            if (!world.isClient() && world instanceof ServerWorld serverWorld) {
                // Swing sound effect
                serverWorld.playSound(
                        null,
                        user.getBlockPos(),
                        SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,  // Swoosh sound
                        SoundCategory.PLAYERS,
                        0.7f,
                        1.0f + world.random.nextFloat() * 0.3f
                );

                // Fire particle trail around the player
                serverWorld.spawnParticles(
                        ParticleTypes.FLAME,
                        user.getX(),
                        user.getY() + user.getHeight() / 2,
                        user.getZ(),
                        6,
                        1.0,
                        0.5,
                        1.0,
                        0.05
                );

                // Some smoke for effect
                serverWorld.spawnParticles(
                        ParticleTypes.SMOKE,
                        user.getX(),
                        user.getY() + user.getHeight() / 2,
                        user.getZ(),
                        3,
                        0.8,
                        0.3,
                        0.8,
                        0.02
                );
            }
            return super.onStoppedUsing(stack, world, user, remainingUseTicks);
        }

        @Override
        public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            World world = target.getWorld();
            if (!world.isClient() && world instanceof ServerWorld serverWorld) {
                // Debug print
                System.out.println("God Sword postHit triggered!");

                // Spawn effects BEFORE damage to ensure they show
                // Fire particles for the sword (subtle amount)
                serverWorld.spawnParticles(
                        ParticleTypes.FLAME,
                        target.getX(),
                        target.getY() + target.getHeight() / 2,
                        target.getZ(),
                        6,                                // Moderate particle count
                        0.3,                              // X spread
                        0.3,                              // Y spread
                        0.3,                              // Z spread
                        0.08                              // Speed
                );

                // Add some smoke for fire effect
                serverWorld.spawnParticles(
                        ParticleTypes.SMOKE,
                        target.getX(),
                        target.getY() + target.getHeight() / 2,
                        target.getZ(),
                        4,
                        0.2,
                        0.2,
                        0.2,
                        0.05
                );

                // Sound effects for the sword - fiery/explosive sounds
                serverWorld.playSound(
                        null,                              // Player (null = all players can hear)
                        target.getBlockPos(),              // Position
                        SoundEvents.ITEM_FIRECHARGE_USE,  // Fire/ignition sound
                        SoundCategory.PLAYERS,             // Sound category
                        0.7f,                              // Volume
                        1.0f + world.random.nextFloat() * 0.4f // Pitch with randomness
                );

                // Deal damage AFTER effects
                target.damage(serverWorld, serverWorld.getDamageSources().generic(), 2000.0f);
            }
            return super.postHit(stack, target, attacker);
        }
    }
}