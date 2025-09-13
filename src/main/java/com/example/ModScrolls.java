package com.example;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.function.Function;

public class ModScrolls {

    // Only fireball scroll
    public static Item FIREBALL_SCROLL;

    public static void initialize() {
        System.out.println("Registering scrolls for " + GodMod.MOD_ID);

        FIREBALL_SCROLL = registerItem("fireball_scroll", key -> new FireballScrollItem(key));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT)
                .register(entries -> entries.add(new ItemStack(FIREBALL_SCROLL)));
    }

    public static Item registerItem(String name, Function<RegistryKey<Item>, Item> function) {
        Identifier id = Identifier.of(GodMod.MOD_ID, name);
        RegistryKey<Item> key = RegistryKey.of(Registries.ITEM.getKey(), id);
        return Registry.register(Registries.ITEM, id, function.apply(key));
    }

    public abstract static class ScrollItem extends Item {
        public ScrollItem(RegistryKey<Item> key) {
            super(new Item.Settings().registryKey(key).maxCount(16));
        }

        @Override
        public ActionResult use(World world, PlayerEntity user, Hand hand) {
            ItemStack itemStack = user.getStackInHand(hand);

            if (!world.isClient) {
                boolean success = useScroll(world, user, itemStack);
                if (success) {
                    if (!user.getAbilities().creativeMode) {
                        itemStack.decrement(1);
                    }
                    return ActionResult.SUCCESS;
                }
                return ActionResult.FAIL;
            }

            return ActionResult.SUCCESS;
        }

        protected abstract boolean useScroll(World world, PlayerEntity user, ItemStack stack);

        protected void spawnMagicParticles(World world, PlayerEntity player) {
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.ENCHANT,
                        player.getX(), player.getY() + 1, player.getZ(),
                        20, 0.5, 1.0, 0.5, 0.1);
            }
        }

        protected void playMagicSound(World world, PlayerEntity player) {
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                    SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
    }

    public static class FireballScrollItem extends ScrollItem {
        public FireballScrollItem(RegistryKey<Item> key) {
            super(key);
        }

        @Override
        protected boolean useScroll(World world, PlayerEntity user, ItemStack stack) {
            if (world instanceof ServerWorld serverWorld) {
                // Create fireball entity using 1.21.4 constructor
                Vec3d lookDirection = user.getRotationVec(1.0F);
                FireballEntity fireball = new FireballEntity(world, user, lookDirection, 1);

                // Position fireball slightly in front of player
                Vec3d playerPos = user.getEyePos();
                Vec3d fireballPos = playerPos.add(lookDirection.multiply(1.5));
                fireball.setPosition(fireballPos.x, fireballPos.y, fireballPos.z);

                serverWorld.spawnEntity(fireball);

                spawnMagicParticles(world, user);
                playMagicSound(world, user);
                return true;
            }
            return false;
        }
    }
}