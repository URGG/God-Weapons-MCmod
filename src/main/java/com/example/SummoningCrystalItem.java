package com.example;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SummoningCrystalItem extends Item {
    private static final Map<UUID, Long> PLAYER_COOLDOWNS = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 300000; // 5 minutes cooldown

    public SummoningCrystalItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            // Check cooldown
            UUID playerId = user.getUuid();
            long currentTime = System.currentTimeMillis();

            if (PLAYER_COOLDOWNS.containsKey(playerId)) {
                long lastUse = PLAYER_COOLDOWNS.get(playerId);
                long remainingCooldown = COOLDOWN_MS - (currentTime - lastUse);

                if (remainingCooldown > 0) {
                    long minutes = remainingCooldown / 60000;
                    long seconds = (remainingCooldown % 60000) / 1000;
                    user.sendMessage(Text.literal("§cCooldown remaining: " + minutes + "m " + seconds + "s"), true);
                    return ActionResult.FAIL;
                }
            }

            // Check if there's already a boss nearby (within 100 blocks)
            if (serverWorld.getEntitiesByClass(GodBossEntity.class, user.getBoundingBox().expand(100), entity -> true).size() > 0) {
                user.sendMessage(Text.literal("§cA God Boss is already active nearby!"), true);
                return ActionResult.FAIL;
            }

            // Check if player is in a valid dimension (overworld, nether, end - adjust as needed)
            if (!isValidDimension(serverWorld)) {
                user.sendMessage(Text.literal("§cCannot summon boss in this dimension!"), true);
                return ActionResult.FAIL;
            }

            // Try to spawn the God Boss
            if (spawnGodBoss(serverWorld, user)) {
                // Set cooldown
                PLAYER_COOLDOWNS.put(playerId, currentTime);

                // Consume the crystal
                if (!user.getAbilities().creativeMode) {
                    stack.decrement(1);
                }

                // Epic summoning effects
                createSummoningEffects(serverWorld, user);

                return ActionResult.SUCCESS;
            } else {
                user.sendMessage(Text.literal("§cCannot summon here - not enough space or unsafe location!"), true);
                return ActionResult.FAIL;
            }
        }

        return ActionResult.SUCCESS;
    }

    private boolean isValidDimension(ServerWorld world) {
        // Allow summoning in overworld, nether, and end - modify as needed
        String dimensionId = world.getRegistryKey().getValue().toString();
        return dimensionId.equals("minecraft:overworld") ||
                dimensionId.equals("minecraft:the_nether") ||
                dimensionId.equals("minecraft:the_end");
    }

    private boolean spawnGodBoss(ServerWorld world, PlayerEntity summoner) {
        Vec3d playerPos = summoner.getPos();

        // Try multiple spawn positions around the player
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = attempt * Math.PI / 8; // 16 directions around player
            int distance = 8 + attempt / 2; // Vary distance from 8-15 blocks

            Vec3d spawnPos = playerPos.add(
                    Math.cos(angle) * distance,
                    2,
                    Math.sin(angle) * distance
            );

            BlockPos blockPos = BlockPos.ofFloored(spawnPos);

            // Check if spawn location is valid
            if (isValidSpawnLocation(world, blockPos)) {
                // Valid spawn location found
                GodBossEntity boss = new GodBossEntity(ModEntities.GOD_BOSS, world);
                boss.setPosition(spawnPos.x, spawnPos.y, spawnPos.z);
                boss.setTarget(summoner);
                boss.setSummoner(summoner.getUuid());

                world.spawnEntity(boss);

                // Announce to server with distance info
                world.getServer().getPlayerManager().broadcast(
                        Text.literal("§4§l⚡ " + summoner.getName().getString() +
                                " has summoned the Crystal Guardian! ⚡"),
                        false
                );

                return true;
            }
        }

        return false; // No valid spawn location found
    }

    private boolean isValidSpawnLocation(ServerWorld world, BlockPos blockPos) {
        // Check if there's enough vertical clearance (4 blocks high for the boss)
        for (int y = 0; y < 4; y++) {
            if (!world.getBlockState(blockPos.up(y)).isAir()) {
                return false;
            }
        }

        // Check if ground is solid
        if (world.getBlockState(blockPos.down()).isAir()) {
            return false;
        }

        // Check for dangerous blocks below (lava, void, etc.)
        BlockPos groundPos = blockPos.down();
        if (world.getBlockState(groundPos).getBlock().toString().contains("lava") ||
                world.getBlockState(groundPos).getBlock().toString().contains("fire")) {
            return false;
        }

        // Check that spawn area isn't too close to void (y < 5 in overworld)
        if (blockPos.getY() < 5 && world.getRegistryKey().getValue().toString().equals("minecraft:overworld")) {
            return false;
        }

        // Check for nearby entities that might interfere
        int nearbyHostileCount = world.getEntitiesByClass(
                net.minecraft.entity.mob.HostileEntity.class,
                net.minecraft.util.math.Box.of(
                        Vec3d.of(blockPos), 10.0, 10.0, 10.0
                ),
                entity -> true
        ).size();

        // Don't spawn if there are too many hostile mobs nearby
        return nearbyHostileCount < 5;
    }

    private void createSummoningEffects(ServerWorld world, PlayerEntity player) {
        Vec3d playerPos = player.getPos();

        // Epic particle spiral around player
        for (int i = 0; i < 120; i++) {
            double angle = i * 0.15;
            double radius = 3.0 + (i * 0.025);
            double x = playerPos.x + Math.cos(angle) * radius;
            double z = playerPos.z + Math.sin(angle) * radius;
            double y = playerPos.y + (i * 0.04);

            // Main spiral particles
            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 2, 0.1, 0.1, 0.1, 0.05);
            world.spawnParticles(ParticleTypes.PORTAL, x, y, z, 1, 0.2, 0.2, 0.2, 0.1);

            // Special particles every 10th iteration
            if (i % 10 == 0) {
                world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.1, 0.1, 0.1, 0.02);
            }

            // Enchant particles for magical effect
            if (i % 5 == 0) {
                world.spawnParticles(ParticleTypes.ENCHANT, x, y, z, 1, 0.3, 0.3, 0.3, 0.1);
            }
        }

        // Ground circle effect (expanding rings)
        for (int ring = 1; ring <= 3; ring++) {
            double ringRadius = ring * 2.0;
            for (int angle = 0; angle < 360; angle += 8) {
                double radians = Math.toRadians(angle);
                double x = playerPos.x + Math.cos(radians) * ringRadius;
                double z = playerPos.z + Math.sin(radians) * ringRadius;

                world.spawnParticles(ParticleTypes.WITCH, x, playerPos.y, z, 2, 0.1, 0.1, 0.1, 0.1);

                if (ring == 2) {
                    world.spawnParticles(ParticleTypes.DRAGON_BREATH, x, playerPos.y + 0.5, z, 1, 0.1, 0.1, 0.1, 0.05);
                }
            }
        }

        // Vertical column effect
        for (int y = 0; y < 10; y++) {
            world.spawnParticles(ParticleTypes.END_ROD,
                    playerPos.x, playerPos.y + y * 0.5, playerPos.z,
                    3, 0.5, 0.1, 0.5, 0.05);
        }

        // Immediate epic sounds
        world.playSound(null, player.getBlockPos(),
                SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 1.5f, 0.5f);

        world.playSound(null, player.getBlockPos(),
                SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.HOSTILE, 1.0f, 0.7f);

        // Schedule delayed thunder sound using tick-based system
        scheduleDelayedThunder(world, player.getBlockPos(), 20); // 20 ticks = 1 second
    }

    private void scheduleDelayedThunder(ServerWorld world, BlockPos pos, int delayTicks) {
        // Use server scheduler for delayed sound effect
        world.getServer().execute(new DelayedSoundTask(world, pos, delayTicks));
    }

    // Inner class for handling delayed sound effects
    private static class DelayedSoundTask implements Runnable {
        private final ServerWorld world;
        private final BlockPos pos;
        private int ticksLeft;

        public DelayedSoundTask(ServerWorld world, BlockPos pos, int ticksLeft) {
            this.world = world;
            this.pos = pos;
            this.ticksLeft = ticksLeft;
        }

        @Override
        public void run() {
            if (ticksLeft > 0) {
                ticksLeft--;
                if (ticksLeft > 0) {
                    // Schedule next tick
                    world.getServer().execute(this);
                } else {
                    // Execute the delayed sound
                    world.playSound(null, pos,
                            SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                            SoundCategory.HOSTILE, 1.2f, 0.8f);

                    // Additional dramatic sound
                    world.playSound(null, pos,
                            SoundEvents.ENTITY_ENDER_DRAGON_DEATH,
                            SoundCategory.HOSTILE, 0.8f, 1.5f);
                }
            }
        }
    }

    // Static method to clean up old cooldowns (call this periodically or in mod events)
    public static void cleanupOldCooldowns() {
        long currentTime = System.currentTimeMillis();
        PLAYER_COOLDOWNS.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > COOLDOWN_MS * 2
        );
    }

    // Method to get remaining cooldown for a player (useful for other features)
    public static long getRemainingCooldown(UUID playerId) {
        if (!PLAYER_COOLDOWNS.containsKey(playerId)) {
            return 0;
        }

        long currentTime = System.currentTimeMillis();
        long lastUse = PLAYER_COOLDOWNS.get(playerId);
        long remaining = COOLDOWN_MS - (currentTime - lastUse);

        return Math.max(0, remaining);
    }

    // Method to manually reset cooldown (for admin commands, etc.)
    public static void resetCooldown(UUID playerId) {
        PLAYER_COOLDOWNS.remove(playerId);
    }
}