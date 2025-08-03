package com.example;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.function.Function;

public class ModScrolls {

    // Declare scroll items
    public static Item FIREBALL_SCROLL;
    public static Item TELEPORT_SCROLL;
    public static Item HEAL_SCROLL;
    public static Item SPEED_SCROLL;
    public static Item INVISIBLE_SCROLL;
    public static Item SHARPNESS_SCROLL;
    public static Item UNBREAKING_SCROLL;
    public static Item FIRE_ASPECT_SCROLL;
    public static Item KNOCKBACK_SCROLL;
    public static Item PROTECTION_SCROLL;

    public static void initialize() {
        System.out.println("Registering scrolls for " + GodMod.MOD_ID);

        FIREBALL_SCROLL = registerItem("fireball_scroll", key -> new FireballScrollItem(key));
        TELEPORT_SCROLL = registerItem("teleport_scroll", key -> new TeleportScrollItem(key));
        HEAL_SCROLL = registerItem("heal_scroll", key -> new HealScrollItem(key));
        SPEED_SCROLL = registerItem("speed_scroll", key -> new SpeedScrollItem(key));
        INVISIBLE_SCROLL = registerItem("invisible_scroll", key -> new InvisibleScrollItem(key));

        SHARPNESS_SCROLL = registerItem("sharpness_scroll", key -> new SharpnessScrollItem(key));
        UNBREAKING_SCROLL = registerItem("unbreaking_scroll", key -> new UnbreakingScrollItem(key));
        FIRE_ASPECT_SCROLL = registerItem("fire_aspect_scroll", key -> new FireAspectScrollItem(key));
        KNOCKBACK_SCROLL = registerItem("knockback_scroll", key -> new KnockbackScrollItem(key));
        PROTECTION_SCROLL = registerItem("protection_scroll", key -> new ProtectionScrollItem(key));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT)
                .register(entries -> {
                    entries.add(new ItemStack(FIREBALL_SCROLL));
                    entries.add(new ItemStack(TELEPORT_SCROLL));
                    entries.add(new ItemStack(HEAL_SCROLL));
                    entries.add(new ItemStack(SPEED_SCROLL));
                    entries.add(new ItemStack(INVISIBLE_SCROLL));
                    entries.add(new ItemStack(SHARPNESS_SCROLL));
                    entries.add(new ItemStack(UNBREAKING_SCROLL));
                    entries.add(new ItemStack(FIRE_ASPECT_SCROLL));
                    entries.add(new ItemStack(KNOCKBACK_SCROLL));
                    entries.add(new ItemStack(PROTECTION_SCROLL));
                });
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

    // Combat effect scrolls
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

    public static class TeleportScrollItem extends ScrollItem {
        public TeleportScrollItem(RegistryKey<Item> key) {
            super(key);
        }

        @Override
        protected boolean useScroll(World world, PlayerEntity user, ItemStack stack) {
            if (world instanceof ServerWorld serverWorld) {
                double newX = user.getX() + (user.getRandom().nextDouble() - 0.5) * 16.0;
                double newZ = user.getZ() + (user.getRandom().nextDouble() - 0.5) * 16.0;
                double newY = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, (int)newX, (int)newZ);

                // Simple teleport method for 1.21.4
                user.teleport(newX, newY, newZ, true);

                world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                        SoundCategory.PLAYERS, 1.0F, 1.0F);
                spawnMagicParticles(world, user);
                return true;
            }
            return false;
        }
    }

    public static class HealScrollItem extends ScrollItem {
        public HealScrollItem(RegistryKey<Item> key) {
            super(key);
        }

        @Override
        protected boolean useScroll(World world, PlayerEntity user, ItemStack stack) {
            if (user.getHealth() < user.getMaxHealth()) {
                user.heal(10.0f);
                spawnMagicParticles(world, user);
                playMagicSound(world, user);
                return true;
            }
            return false;
        }
    }

    public static class SpeedScrollItem extends ScrollItem {
        public SpeedScrollItem(RegistryKey<Item> key) {
            super(key);
        }

        @Override
        protected boolean useScroll(World world, PlayerEntity user, ItemStack stack) {
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 1200, 2)); // 1 minute, level 3
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 1200, 1)); // 1 minute, level 2
            spawnMagicParticles(world, user);
            playMagicSound(world, user);
            return true;
        }
    }

    public static class InvisibleScrollItem extends ScrollItem {
        public InvisibleScrollItem(RegistryKey<Item> key) {
            super(key);
        }

        @Override
        protected boolean useScroll(World world, PlayerEntity user, ItemStack stack) {
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 600, 0)); // 30 seconds
            spawnMagicParticles(world, user);
            playMagicSound(world, user);
            return true;
        }
    }

    // Enchantment scrolls base class
    public abstract static class EnchantmentScrollItem extends ScrollItem {
        public EnchantmentScrollItem(RegistryKey<Item> key) {
            super(key);
        }

        @Override
        public ActionResult useOnBlock(ItemUsageContext context) {
            World world = context.getWorld();
            PlayerEntity player = context.getPlayer();
            ItemStack scrollStack = context.getStack();

            if (player != null && !world.isClient) {
                // Check off-hand first, then main hand (but not the scroll itself)
                ItemStack targetItem = player.getOffHandStack();
                if (targetItem.isEmpty() || targetItem == scrollStack) {
                    ItemStack mainHand = player.getMainHandStack();
                    if (mainHand != scrollStack) {
                        targetItem = mainHand;
                    } else {
                        targetItem = ItemStack.EMPTY;
                    }
                }

                if (!targetItem.isEmpty() && canEnchant(targetItem)) {
                    if (applyEnchantment(targetItem, world)) {
                        if (!player.getAbilities().creativeMode) {
                            scrollStack.decrement(1);
                        }

                        spawnMagicParticles(world, player);
                        playMagicSound(world, player);
                        player.sendMessage(Text.literal("§aEnchantment applied!"), true);
                        return ActionResult.SUCCESS;
                    } else {
                        player.sendMessage(Text.literal("§cEnchantment already at maximum level!"), true);
                        return ActionResult.FAIL;
                    }
                }

                player.sendMessage(Text.literal("§cHold an item to enchant in your other hand!"), true);
                return ActionResult.FAIL;
            }

            return world.isClient ? ActionResult.SUCCESS : ActionResult.FAIL;
        }

        @Override
        protected boolean useScroll(World world, PlayerEntity user, ItemStack stack) {
            return false; // Enchantment scrolls use right-click on block instead
        }

        protected abstract boolean canEnchant(ItemStack stack);
        protected abstract boolean applyEnchantment(ItemStack stack, World world);
    }

    public static class SharpnessScrollItem extends EnchantmentScrollItem {
        public SharpnessScrollItem(RegistryKey<Item> key) {
            super(key);
        }

        @Override
        protected boolean canEnchant(ItemStack stack) {
            return stack.getItem() instanceof net.minecraft.item.SwordItem ||
                    stack.getItem() instanceof net.minecraft.item.AxeItem;
        }

        @Override
        protected boolean applyEnchantment(ItemStack stack, World world) {
            RegistryEntry<Enchantment> sharpness = world.getRegistryManager()
                    .getOrThrow(RegistryKeys.ENCHANTMENT)
                    .getOrThrow(Enchantments.SHARPNESS);



            if (sharpness != null) {
                ItemEnchantmentsComponent enchantments = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
                int currentLevel = enchantments.getLevel(sharpness);


                if (currentLevel < 5) {
                    ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(enchantments);
                    builder.set(sharpness, currentLevel + 1);
                    stack.set(DataComponentTypes.ENCHANTMENTS, builder.build());
                    return true;
                }
            }
            return false;
        }
    }

    public static class UnbreakingScrollItem extends EnchantmentScrollItem {
        public UnbreakingScrollItem(RegistryKey<Item> key) {
            super(key);
        }

        @Override
        protected boolean canEnchant(ItemStack stack) {
            return stack.isDamageable();
        }

        @Override
        protected boolean applyEnchantment(ItemStack stack, World world) {
            RegistryEntry<Enchantment> unbreaking = world.getRegistryManager()
                    .getOrThrow(RegistryKeys.ENCHANTMENT)
                    .getOrThrow(Enchantments.UNBREAKING);

            if (unbreaking != null) {
                ItemEnchantmentsComponent enchantments = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
                int currentLevel = enchantments.getLevel(unbreaking);

                if (currentLevel < 3) {
                    ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(enchantments);
                    builder.set(unbreaking, currentLevel + 1);
                    stack.set(DataComponentTypes.ENCHANTMENTS, builder.build());
                    return true;
                }
            }
            return false;
        }
    }

    public static class FireAspectScrollItem extends EnchantmentScrollItem {
        public FireAspectScrollItem(RegistryKey<Item> key) {
            super(key);
        }

        @Override
        protected boolean canEnchant(ItemStack stack) {
            return stack.getItem() instanceof net.minecraft.item.SwordItem;
        }

        @Override
        protected boolean applyEnchantment(ItemStack stack, World world) {
            RegistryEntry<Enchantment> fireAspect = world.getRegistryManager()
                    .getOrThrow(RegistryKeys.ENCHANTMENT)
                    .getOrThrow(Enchantments.FIRE_ASPECT);

            if (fireAspect != null) {
                ItemEnchantmentsComponent enchantments = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
                int currentLevel = enchantments.getLevel(fireAspect);

                if (currentLevel < 2) {
                    ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(enchantments);
                    builder.set(fireAspect, currentLevel + 1);
                    stack.set(DataComponentTypes.ENCHANTMENTS, builder.build());
                    return true;
                }
            }
            return false;
        }
    }

    public static class KnockbackScrollItem extends EnchantmentScrollItem {
        public KnockbackScrollItem(RegistryKey<Item> key) {
            super(key);
        }

        @Override
        protected boolean canEnchant(ItemStack stack) {
            return stack.getItem() instanceof net.minecraft.item.SwordItem;
        }

        @Override
        protected boolean applyEnchantment(ItemStack stack, World world) {
            RegistryEntry<Enchantment> knockback = world.getRegistryManager()
                    .getOrThrow(RegistryKeys.ENCHANTMENT)
                    .getOrThrow(Enchantments.KNOCKBACK);

            if (knockback != null) {
                ItemEnchantmentsComponent enchantments = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
                int currentLevel = enchantments.getLevel(knockback);

                if (currentLevel < 2) {
                    ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(enchantments);
                    builder.set(knockback, currentLevel + 1);
                    stack.set(DataComponentTypes.ENCHANTMENTS, builder.build());
                    return true;
                }
            }
            return false;
        }
    }

    public static class ProtectionScrollItem extends EnchantmentScrollItem {
        public ProtectionScrollItem(RegistryKey<Item> key) {
            super(key);
            
        }

        @Override
        protected boolean canEnchant(ItemStack stack) {
            return stack.getItem() instanceof net.minecraft.item.ArmorItem;
        }

        @Override
        protected boolean applyEnchantment(ItemStack stack, World world) {
            RegistryEntry<Enchantment> protection = world.getRegistryManager()
                    .getOrThrow(RegistryKeys.ENCHANTMENT)
                    .getOrThrow(Enchantments.PROTECTION);

            if (protection != null) {
                ItemEnchantmentsComponent enchantments = stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
                int currentLevel = enchantments.getLevel(protection);

                if (currentLevel < 4) {
                    ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(enchantments);
                    builder.set(protection, currentLevel + 1);
                    stack.set(DataComponentTypes.ENCHANTMENTS, builder.build());
                    return true;
                }
            }
            return false;
        }
    }
}