package com.example;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;

public class DevastationDomeEntity extends Entity {
    private static final int DOME_RADIUS = 15;
    private static final int DOME_DURATION_TICKS = 200;
    private static final float DAMAGE_PER_TICK = 100000.0F;
    private static final int DAMAGE_INTERVAL = 1;

    private int lifeTicks = 0;
    private BlockPos domeCenter;

    public DevastationDomeEntity(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setInvulnerable(true);
    }

    public DevastationDomeEntity(EntityType<?> type, World world, BlockPos center) {
        this(type, world);
        this.domeCenter = center;
        this.setPosition(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5);
    }

    @Override
    protected void initDataTracker(net.minecraft.entity.data.DataTracker.Builder builder) {
        // No data to track
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient) {
            lifeTicks++;

            // Apply damage every tick
            if (lifeTicks % DAMAGE_INTERVAL == 0) {
                applyDomeDamage();
            }

            // Spawn particles every 5 ticks
            if (lifeTicks % 5 == 0) {
                spawnDomeParticles();
            }

            // Remove after duration
            if (lifeTicks >= DOME_DURATION_TICKS) {
                removeDome();
                this.discard();
            }
        }
    }

    private void applyDomeDamage() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        if (domeCenter == null) {
            domeCenter = this.getBlockPos();
        }

        // Create damage box
        Box damageBox = new Box(domeCenter).expand(DOME_RADIUS);

        // Find all living entities except players
        List<LivingEntity> entities = this.getWorld().getEntitiesByClass(
                LivingEntity.class,
                damageBox,
                entity -> {
                    double distance = entity.getPos().distanceTo(domeCenter.toCenterPos());
                    return distance <= DOME_RADIUS && !(entity instanceof PlayerEntity);
                }
        );

        // Create damage source using the server world's damage sources
        DamageSource lethalDamage = serverWorld.getDamageSources().outOfWorld();

        // Apply damage to all entities
        for (LivingEntity entity : entities) {
            entity.damage(serverWorld, lethalDamage, DAMAGE_PER_TICK);
        }
    }

    private void spawnDomeParticles() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        if (domeCenter == null) {
            domeCenter = this.getBlockPos();
        }

        double spin = (lifeTicks % 360) * Math.PI / 180.0;

        // Create dome shell particles
        for (int yStep = 2; yStep <= DOME_RADIUS; yStep += 3) {
            double height = yStep;
            double radius = Math.sqrt(DOME_RADIUS * DOME_RADIUS - height * height);

            for (int i = 0; i < 360; i += 24) {
                double angle = Math.toRadians(i) + spin * 0.5;
                double x = domeCenter.getX() + 0.5 + Math.cos(angle) * radius;
                double z = domeCenter.getZ() + 0.5 + Math.sin(angle) * radius;
                double y = domeCenter.getY() + height;

                serverWorld.spawnParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        x, y, z, 1, 0.0, 0.0, 0.0, 0.01
                );
                serverWorld.spawnParticles(
                        ParticleTypes.DRAGON_BREATH,
                        x, y + 0.2, z, 1, 0.0, 0.0, 0.0, 0.0
                );
            }
        }

        // Create inner spiral
        double progress = (double) lifeTicks / DOME_DURATION_TICKS;
        int spiralPoints = (int) (30.0 + 50.0 * Math.sin(Math.min(progress, 1.0) * Math.PI));
        double spiralRadius = 6.75;

        for (int i = 0; i < spiralPoints; i++) {
            double angle = spin + i * 0.35;
            double x = domeCenter.getX() + 0.5 + Math.cos(angle) * spiralRadius * (0.9 + 0.1 * Math.sin(spin));
            double z = domeCenter.getZ() + 0.5 + Math.sin(angle) * spiralRadius * (0.9 + 0.1 * Math.cos(spin));
            double y = domeCenter.getY() + 4.5 + Math.sin(angle * 0.9) * 2.0;

            serverWorld.spawnParticles(
                    ParticleTypes.ENCHANT,
                    x, y, z, 1, 0.0, 0.02, 0.0, 0.0
            );
            serverWorld.spawnParticles(
                    ParticleTypes.WARPED_SPORE,
                    x, y, z, 1, 0.0, 0.0, 0.0, 0.01
            );
        }
    }

    private void removeDome() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        if (domeCenter == null) {
            domeCenter = this.getBlockPos();
        }

        // Spawn explosion particle
        serverWorld.spawnParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                domeCenter.getX(), domeCenter.getY(), domeCenter.getZ(),
                1, 0.0, 0.0, 0.0, 0.0
        );
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        lifeTicks = nbt.getInt("LifeTicks");

        if (nbt.contains("DomeCenterX")) {
            domeCenter = new BlockPos(
                    nbt.getInt("DomeCenterX"),
                    nbt.getInt("DomeCenterY"),
                    nbt.getInt("DomeCenterZ")
            );
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("LifeTicks", lifeTicks);

        if (domeCenter != null) {
            nbt.putInt("DomeCenterX", domeCenter.getX());
            nbt.putInt("DomeCenterY", domeCenter.getY());
            nbt.putInt("DomeCenterZ", domeCenter.getZ());
        }
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        return false; // Invulnerable
    }
}