package com.example;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.List;

public class DevastationDomeScrollItem extends Item {
    private static final int DOME_RADIUS = 15;
    public static final SoundEvent DEVASTATION_SOUND = registerSound("destructive_dome_burst");

    public DevastationDomeScrollItem(Settings settings) {
        super(settings);
    }

    private static SoundEvent registerSound(String name) {
        Identifier id = Identifier.of("godmod", name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient) {
            ServerWorld serverWorld = (ServerWorld) world;
            BlockPos centerPos = user.getBlockPos();

            // Play sound (will be silent for now without sound file)
            world.playSound(null, centerPos, DEVASTATION_SOUND, SoundCategory.PLAYERS, 2.0F, 1.0F);

            // Spawn particle burst
            spawnParticleBurst(serverWorld, centerPos);

            // Spawn dome entity
            DevastationDomeEntity domeEntity = new DevastationDomeEntity(
                    ModEntities.DEVASTATION_DOME_ENTITY,
                    serverWorld,
                    centerPos
            );
            serverWorld.spawnEntity(domeEntity);

            // Send message to player
            user.sendMessage(
                    Text.literal("Devastation Dome Activated!")
                            .formatted(Formatting.RED, Formatting.BOLD),
                    true
            );

            // Consume item if not in creative
            if (!user.getAbilities().creativeMode) {
                stack.decrement(1);
            }
        }

        return ActionResult.SUCCESS;
    }

    private void spawnParticleBurst(ServerWorld world, BlockPos center) {
        // Main explosion burst
        for (int i = 0; i < 600; i++) {
            double angle = Math.random() * Math.PI * 2.0;
            double pitch = Math.random() * Math.PI;
            double speed = 0.6 + Math.random();

            double velX = Math.sin(pitch) * Math.cos(angle) * speed;
            double velY = Math.cos(pitch) * speed;
            double velZ = Math.sin(pitch) * Math.sin(angle) * speed;

            world.spawnParticles(
                    ParticleTypes.FLAME,
                    center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                    1, velX, velY, velZ, 0.08
            );
            world.spawnParticles(
                    ParticleTypes.SMOKE,
                    center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                    1, velX, velY, velZ, 0.08
            );
            world.spawnParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                    1, velX * 0.6, velY * 0.6, velZ * 0.6, 0.02
            );
            world.spawnParticles(
                    ParticleTypes.WARPED_SPORE,
                    center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                    1, velX * 0.4, velY * 0.4, velZ * 0.4, 0.01
            );
        }

        // Ring particles
        double innerRadius = 8.25;
        double outerRadius = 12.75;

        for (int i = 0; i < 360; i += 8) {
            double radians = Math.toRadians(i);
            double innerX = center.getX() + 0.5 + Math.cos(radians) * innerRadius;
            double innerZ = center.getZ() + 0.5 + Math.sin(radians) * innerRadius;
            double outerX = center.getX() + 0.5 + Math.cos(radians) * outerRadius;
            double outerZ = center.getZ() + 0.5 + Math.sin(radians) * outerRadius;

            world.spawnParticles(
                    ParticleTypes.ENCHANT,
                    innerX, center.getY() + 0.5, innerZ,
                    2, 0.0, 0.02, 0.0, 0.0
            );
            world.spawnParticles(
                    ParticleTypes.DRAGON_BREATH,
                    outerX, center.getY() + 1.0, outerZ,
                    1, 0.0, 0.02, 0.0, 0.0
            );
        }

        // Vertical pillars
        for (int i = 0; i < 15; i += 2) {
            world.spawnParticles(
                    ParticleTypes.END_ROD,
                    center.getX() + 0.5 + innerRadius, center.getY() + i, center.getZ() + 0.5,
                    8, 0.02, 0.02, 0.02, 0.01
            );
            world.spawnParticles(
                    ParticleTypes.END_ROD,
                    center.getX() + 0.5 - innerRadius, center.getY() + i, center.getZ() + 0.5,
                    8, 0.02, 0.02, 0.02, 0.01
            );
            world.spawnParticles(
                    ParticleTypes.END_ROD,
                    center.getX() + 0.5, center.getY() + i, center.getZ() + 0.5 + innerRadius,
                    8, 0.02, 0.02, 0.02, 0.01
            );
            world.spawnParticles(
                    ParticleTypes.END_ROD,
                    center.getX() + 0.5, center.getY() + i, center.getZ() + 0.5 - innerRadius,
                    8, 0.02, 0.02, 0.02, 0.01
            );
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.godmod.devastation_dome_scroll.tooltip").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.godmod.devastation_dome_scroll.tooltip2").formatted(Formatting.DARK_RED));
        tooltip.add(Text.translatable("item.godmod.devastation_dome_scroll.tooltip3").formatted(Formatting.DARK_GRAY));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}