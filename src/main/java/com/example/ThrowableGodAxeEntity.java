package com.example;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ThrowableGodAxeEntity extends ThrownItemEntity {
    private static final TrackedData<Boolean> RETURNING = DataTracker.registerData(ThrowableGodAxeEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private boolean hasHit = false;
    private int returnTimer = 0;
    private PlayerEntity originalThrower = null;
    private float spinRotation = 0.0f;

    public ThrowableGodAxeEntity(EntityType<? extends ThrownItemEntity> entityType, World world) {
        super(entityType, world);
    }

    public ThrowableGodAxeEntity(World world, LivingEntity owner) {
        super(ModEntities.THROWABLE_GOD_AXE, owner, world, new ItemStack(ModItems.GOD_AXE));

        // Store the original thrower
        if (owner instanceof PlayerEntity player) {
            this.originalThrower = player;
        }


        if (owner == null) {
            System.err.println("Warning: ThrowableGodAxeEntity created with null owner!");
            this.discard();
        }
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(RETURNING, false);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.GOD_AXE;
    }

    @Override
    public void tick() {
        super.tick();

        // Update spinning rotation (visible on both client and server)
        spinRotation += 25.0f; // Spin speed - adjust as needed
        if (spinRotation >= 360.0f) {
            spinRotation -= 360.0f;
        }


        this.setYaw(spinRotation);
        this.setPitch(0);

        if (this.getWorld().isClient()) return;


        if (this.getWorld() instanceof ServerWorld serverWorld) {

            serverWorld.spawnParticles(
                    ParticleTypes.WITCH,
                    this.getX(), this.getY(), this.getZ(),
                    3, 0.2, 0.2, 0.2, 0.03
            );

            // Add some portal particles for extra effect
            if (this.age % 3 == 0) { // Every 3 ticks
                serverWorld.spawnParticles(
                        ParticleTypes.PORTAL,
                        this.getX(), this.getY(), this.getZ(),
                        1, 0.1, 0.1, 0.1, 0.01
                );
            }
        }

        boolean returning = this.dataTracker.get(RETURNING);

        // Check if should start returning - removed distance limit
        if (!returning && (hasHit || this.age > 60)) { // Return after 3 seconds or hitting something
            startReturning();
            returning = true;
        }

        // Handle returning behavior with enhanced homing
        if (returning && originalThrower != null && !originalThrower.isRemoved()) {
            returnTimer++;

            Vec3d targetPos = originalThrower.getPos().add(0, 1, 0);
            Vec3d currentPos = this.getPos();
            Vec3d direction = targetPos.subtract(currentPos);
            double distance = direction.length();

            if (distance < 1.0) {
                // Close enough - return to player
                returnToPlayer();
                return;
            }

            // Enhanced homing - adjusts for player movement
            if (distance > 0.1) { // Avoid division by zero
                direction = direction.normalize();

                // Increase speed based on distance and time returning
                double baseSpeed = 1.2;
                double distanceMultiplier = Math.min(distance * 0.1, 2.0); // Speed up for far distances
                double timeMultiplier = Math.min(returnTimer * 0.05, 2.0); // Speed up over time
                double finalSpeed = baseSpeed + distanceMultiplier + timeMultiplier;

                this.setVelocity(direction.multiply(finalSpeed));
            }

            // Disable gravity when returning and make it fly straight
            this.setNoGravity(true);

            // Play periodic returning sound for long distances
            if (returnTimer % 40 == 0 && distance > 5) { // Every 2 seconds if far away
                this.getWorld().playSound(null, this.getBlockPos(),
                        SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.3f, 1.5f);
            }

        } else if (returning && (originalThrower == null || originalThrower.isRemoved())) {

            this.discard();
        }
    }

    private void startReturning() {
        this.dataTracker.set(RETURNING, true);
        returnTimer = 0;
        this.setVelocity(Vec3d.ZERO); // Stop current movement


        this.getWorld().playSound(null, this.getBlockPos(),
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.8f, 0.8f);
    }

    private void returnToPlayer() {
        if (originalThrower != null) {
            // Give back the axe
            ItemStack axeStack = new ItemStack(ModItems.GOD_AXE);
            if (!originalThrower.getInventory().insertStack(axeStack)) {
                originalThrower.dropItem(axeStack, false);
            }


            this.getWorld().playSound(null, originalThrower.getBlockPos(),
                    SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.2f);


            if (this.getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(
                        ParticleTypes.WITCH,
                        originalThrower.getX(), originalThrower.getY() + 1, originalThrower.getZ(),
                        8, 0.3, 0.3, 0.3, 0.1
                );
            }
        }
        this.discard();
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        if (!this.dataTracker.get(RETURNING)) {
            if (hitResult instanceof BlockHitResult) {
                onBlockHit((BlockHitResult) hitResult);
            } else if (hitResult instanceof EntityHitResult) {
                onEntityHit((EntityHitResult) hitResult);
            }
        }

    }

    protected void onBlockHit(BlockHitResult blockHitResult) {
        if (!this.dataTracker.get(RETURNING) && !hasHit) {
            hasHit = true;
            this.setVelocity(Vec3d.ZERO);


            this.getWorld().playSound(null, this.getBlockPos(),
                    SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1.0f, 1.2f);


            if (this.getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(
                        ParticleTypes.SMOKE,
                        this.getX(), this.getY(), this.getZ(),
                        5, 0.3, 0.3, 0.3, 0.05
                );
            }
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (this.dataTracker.get(RETURNING)) return; // Don't hit anything while returning

        if (entityHitResult.getEntity() instanceof LivingEntity target && target != this.getOwner()) {
            hasHit = true;


            if (this.getWorld() instanceof ServerWorld serverWorld) {
                target.damage(serverWorld, serverWorld.getDamageSources().generic(), 1500.0f);


                serverWorld.spawnParticles(
                        ParticleTypes.WITCH,
                        target.getX(), target.getY() + target.getHeight() / 2, target.getZ(),
                        15, 0.6, 0.6, 0.6, 0.12
                );

                serverWorld.spawnParticles(
                        ParticleTypes.PORTAL,
                        target.getX(), target.getY() + target.getHeight(), target.getZ(),
                        10, 0.4, 0.4, 0.4, 0.06
                );


                serverWorld.playSound(null, target.getBlockPos(),
                        SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0f, 1.2f);

                serverWorld.playSound(null, target.getBlockPos(),
                        SoundEvents.ENTITY_WITHER_HURT, SoundCategory.PLAYERS, 0.6f, 0.7f);
            }

            this.setVelocity(Vec3d.ZERO);
        }
    }


    public float getSpinRotation() {
        return spinRotation;
    }
}