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

    // Enhanced movement properties
    private Vec3d initialDirection = Vec3d.ZERO;
    private Vec3d centerPoint = Vec3d.ZERO;
    private float circularRadius = 0.0f;
    private int flightTicks = 0;
    private static final double THROW_SPEED_MULTIPLIER = 2.5; // Increased speed
    private static final double MAX_RANGE = 80.0; // Increased range
    private static final double RETURN_SPEED_BASE = 2.0; // Faster return

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
    public void setVelocity(double x, double y, double z) {
        super.setVelocity(x * THROW_SPEED_MULTIPLIER, y * THROW_SPEED_MULTIPLIER, z * THROW_SPEED_MULTIPLIER);

        this.initialDirection = new Vec3d(x, y, z).normalize();
    }

    @Override
    public void tick() {
        super.tick();
        flightTicks++;

        // Boomerang-style spinning - fast rotation around the forward axis
        spinRotation += 45.0f; // Fast consistent spin like a real boomerang
        if (spinRotation >= 360.0f) {
            spinRotation -= 360.0f;
        }

        // Boomerang rotation: rotate around the movement direction
        Vec3d velocity = this.getVelocity();
        if (velocity.length() > 0.1) {
            // Calculate yaw and pitch based on velocity direction
            double horizontalDistance = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            float yaw = (float)(Math.atan2(velocity.z, velocity.x) * 180.0 / Math.PI) - 90.0f;
            float pitch = (float)(Math.atan2(-velocity.y, horizontalDistance) * 180.0 / Math.PI);

            // Add the spinning rotation to the base orientation
            this.setYaw(yaw + spinRotation);
            this.setPitch(pitch);
        } else {
            // Fallback when not moving
            this.setYaw(spinRotation);
            this.setPitch(0);
        }

        if (this.getWorld().isClient()) return;

        // Reduced particle effects
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            // Subtle main trail particles (reduced from 5 to 2)
            if (flightTicks % 3 == 0) { // Only every 3 ticks
                serverWorld.spawnParticles(
                        ParticleTypes.WITCH,
                        this.getX(), this.getY(), this.getZ(),
                        2, 0.2, 0.2, 0.2, 0.02
                );
            }

            // Minimal spiral effect (reduced frequency and count)
            if (flightTicks % 8 == 0) { // Less frequent
                double angle = flightTicks * 0.3;
                double spiralRadius = 0.4; // Smaller radius
                for (int i = 0; i < 2; i++) { // Only 2 particles instead of 3
                    double offsetAngle = angle + (i * 180); // 180 degrees apart
                    double offsetX = Math.cos(Math.toRadians(offsetAngle)) * spiralRadius;
                    double offsetZ = Math.sin(Math.toRadians(offsetAngle)) * spiralRadius;

                    serverWorld.spawnParticles(
                            ParticleTypes.PORTAL,
                            this.getX() + offsetX, this.getY(), this.getZ() + offsetZ,
                            1, 0.05, 0.05, 0.05, 0.01
                    );
                }
            }

            // Remove speed-based flame particles for cleaner effect
        }

        boolean returning = this.dataTracker.get(RETURNING);

        // Apply circular motion when not returning
        if (!returning && !hasHit) {
            applyCircularMotion();
        }

        // Check distance from thrower for max range
        double distanceFromThrower = originalThrower != null ?
                this.getPos().distanceTo(originalThrower.getPos()) : 0;


        if (!returning && (hasHit || this.age > 100 || distanceFromThrower > MAX_RANGE)) {
            startReturning();
            returning = true;
        }


        if (returning && originalThrower != null && !originalThrower.isRemoved()) {
            returnTimer++;

            Vec3d targetPos = originalThrower.getPos().add(0, 1, 0);
            Vec3d currentPos = this.getPos();
            Vec3d direction = targetPos.subtract(currentPos);
            double distance = direction.length();

            if (distance < 1.2) {
                returnToPlayer();
                return;
            }

            if (distance > 0.1) {
                direction = direction.normalize();

                // Much faster return speed with progressive acceleration
                double baseSpeed = RETURN_SPEED_BASE;
                double distanceMultiplier = Math.min(distance * 0.15, 3.0);
                double timeMultiplier = Math.min(returnTimer * 0.08, 3.0);
                double urgencyMultiplier = distance > 20 ? 2.0 : 1.0; // Extra speed for long distances
                double finalSpeed = baseSpeed + distanceMultiplier + timeMultiplier + urgencyMultiplier;


                Vec3d curvedDirection = applyCurvedReturn(direction, distance);
                this.setVelocity(curvedDirection.multiply(finalSpeed));
            }

            this.setNoGravity(true);


            if (returnTimer % 20 == 0) { // Every second
                this.getWorld().playSound(null, this.getBlockPos(),
                        SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.4f, 1.8f);
            }

        } else if (returning && (originalThrower == null || originalThrower.isRemoved())) {
            this.discard();
        }
    }

    private void applyCircularMotion() {
        if (initialDirection.equals(Vec3d.ZERO)) return;


        double motionStrength = 0.15; // Adjust for more/less circular motion
        double frequency = flightTicks * 0.2; // How fast it circles


        Vec3d up = new Vec3d(0, 1, 0);
        Vec3d right = initialDirection.crossProduct(up).normalize();
        Vec3d actualUp = right.crossProduct(initialDirection).normalize();


        double circularX = Math.cos(frequency) * motionStrength;
        double circularY = Math.sin(frequency) * motionStrength * 0.5; // Less vertical movement

        Vec3d circularOffset = right.multiply(circularX).add(actualUp.multiply(circularY));
        Vec3d currentVelocity = this.getVelocity();


        Vec3d newVelocity = currentVelocity.add(circularOffset);
        this.setVelocity(newVelocity.x, newVelocity.y, newVelocity.z);
    }

    private Vec3d applyCurvedReturn(Vec3d direction, double distance) {

        if (distance > 10) {
            double curveStrength = 0.3;
            double curveAngle = Math.sin(returnTimer * 0.3) * curveStrength;

            // Create perpendicular vector for curve
            Vec3d up = new Vec3d(0, 1, 0);
            Vec3d right = direction.crossProduct(up).normalize();

            return direction.add(right.multiply(curveAngle));
        }
        return direction;
    }

    private void startReturning() {
        this.dataTracker.set(RETURNING, true);
        returnTimer = 0;
        this.setVelocity(Vec3d.ZERO);


        this.getWorld().playSound(null, this.getBlockPos(),
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 0.6f);


        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                    ParticleTypes.WITCH,
                    this.getX(), this.getY(), this.getZ(),
                    12, 0.5, 0.5, 0.5, 0.15
            );
        }
    }

    private void returnToPlayer() {
        if (originalThrower != null) {
            ItemStack axeStack = new ItemStack(ModItems.GOD_AXE);
            if (!originalThrower.getInventory().insertStack(axeStack)) {
                originalThrower.dropItem(axeStack, false);
            }

            // Enhanced return effects
            this.getWorld().playSound(null, originalThrower.getBlockPos(),
                    SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1.2f, 1.5f);

            if (this.getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(
                        ParticleTypes.WITCH,
                        originalThrower.getX(), originalThrower.getY() + 1, originalThrower.getZ(),
                        15, 0.4, 0.4, 0.4, 0.12
                );

                // Explosion-like effect when returning
                serverWorld.spawnParticles(
                        ParticleTypes.PORTAL,
                        originalThrower.getX(), originalThrower.getY() + 1, originalThrower.getZ(),
                        20, 0.6, 0.6, 0.6, 0.2
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
                    SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1.2f, 1.4f);

            if (this.getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(
                        ParticleTypes.SMOKE,
                        this.getX(), this.getY(), this.getZ(),
                        5, 0.4, 0.4, 0.4, 0.08
                );


                serverWorld.spawnParticles(
                        ParticleTypes.LAVA,
                        this.getX(), this.getY(), this.getZ(),
                        4, 0.3, 0.3, 0.3, 0.1
                );
            }
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (this.dataTracker.get(RETURNING)) return;

        if (entityHitResult.getEntity() instanceof LivingEntity target && target != this.getOwner()) {
            hasHit = true;

            if (this.getWorld() instanceof ServerWorld serverWorld) {
                target.damage(serverWorld, serverWorld.getDamageSources().generic(), 1500.0f);


                serverWorld.spawnParticles(
                        ParticleTypes.WITCH,
                        target.getX(), target.getY() + target.getHeight() / 2, target.getZ(),
                        20, 0.8, 0.8, 0.8, 0.15
                );

                serverWorld.spawnParticles(
                        ParticleTypes.PORTAL,
                        target.getX(), target.getY() + target.getHeight(), target.getZ(),
                        15, 0.6, 0.6, 0.6, 0.08
                );


                serverWorld.spawnParticles(
                        ParticleTypes.CRIT,
                        target.getX(), target.getY() + target.getHeight() / 2, target.getZ(),
                        10, 0.5, 0.5, 0.5, 0.1
                );

                serverWorld.playSound(null, target.getBlockPos(),
                        SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.2f, 1.4f);

                serverWorld.playSound(null, target.getBlockPos(),
                        SoundEvents.ENTITY_WITHER_HURT, SoundCategory.PLAYERS, 0.8f, 0.8f);
            }

            this.setVelocity(Vec3d.ZERO);
        }
    }

    public float getSpinRotation() {
        return spinRotation;
    }
}