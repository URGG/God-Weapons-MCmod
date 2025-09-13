package com.example;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class GodBossEntity extends HostileEntity {
    private final ServerBossBar bossBar = new ServerBossBar(
            Text.literal("§6Crystal Guardian"),
            BossBar.Color.YELLOW,
            BossBar.Style.PROGRESS
    );
    private int particleTimer = 0;

    public GodBossEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 500; // Lots of XP when killed
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 200.0) // High health
                .add(EntityAttributes.MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.ATTACK_DAMAGE, 15.0)
                .add(EntityAttributes.FOLLOW_RANGE, 50.0)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.8); // Hard to knockback
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(5, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        // Update boss bar
        this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());

        // Spawn particles around the boss
        if (!this.getWorld().isClient()) {
            particleTimer++;
            if (particleTimer >= 10) { // Every 10 ticks (0.5 seconds)
                particleTimer = 0;
                ServerWorld serverWorld = (ServerWorld) this.getWorld();

                // Spawn magical particles around the boss
                serverWorld.spawnParticles(
                        ParticleTypes.ENCHANT,
                        this.getX(),
                        this.getY() + this.getHeight() / 2,
                        this.getZ(),
                        5,
                        1.0, 0.5, 1.0,
                        0.1
                );

                // Occasional special effect when low health
                if (this.getHealth() < this.getMaxHealth() * 0.3f) {
                    serverWorld.spawnParticles(
                            ParticleTypes.WITCH,
                            this.getX(),
                            this.getY() + this.getHeight(),
                            this.getZ(),
                            3,
                            0.5, 0.3, 0.5,
                            0.05
                    );
                }
            }
        }
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    protected void dropLoot(ServerWorld world, DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(world, damageSource, causedByPlayer);

        // Always drop 2-4 magic crystals
        int crystalCount = 2 + this.random.nextInt(3); // 2-4 crystals
        for (int i = 0; i < crystalCount; i++) {
            this.dropItem(world, ModItems.MAGIC_CRYSTAL);
        }

        // 50% chance to drop a fireball scroll
        if (this.random.nextBoolean()) {
            this.dropItem(world, ModScrolls.FIREBALL_SCROLL);
        }

        // Rare chance (10%) to drop an already crafted god weapon
        if (this.random.nextFloat() < 0.1f) {
            if (this.random.nextBoolean()) {
                this.dropStack(world, new ItemStack(ModItems.GOD_SWORD));
            } else {
                this.dropStack(world, new ItemStack(ModItems.GOD_AXE));
            }
        }
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        // Special death effects
        if (this.getHealth() - amount <= 0 && !this.getWorld().isClient()) {
            ServerWorld serverWorld = (ServerWorld) this.getWorld();

            // Big explosion of particles when dying
            serverWorld.spawnParticles(
                    ParticleTypes.EXPLOSION_EMITTER,
                    this.getX(),
                    this.getY() + this.getHeight() / 2,
                    this.getZ(),
                    1,
                    0, 0, 0, 0
            );

            serverWorld.spawnParticles(
                    ParticleTypes.ENCHANT,
                    this.getX(),
                    this.getY() + this.getHeight() / 2,
                    this.getZ(),
                    50,
                    2.0, 1.0, 2.0,
                    0.2
            );

            // Play dramatic death sound
            this.getWorld().playSound(null, this.getBlockPos(),
                    SoundEvents.ENTITY_WITHER_DEATH,
                    this.getSoundCategory(),
                    1.0F, 0.8F);
        }

        return super.damage(world, source, amount);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_WITHER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.ENTITY_WITHER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_WITHER_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.8F;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false; // Boss never despawns naturally
    }

    @Override
    public boolean isPersistent() {
        return true;
    }
}