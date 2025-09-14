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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

public class GodBossEntity extends HostileEntity {
    private final ServerBossBar bossBar = new ServerBossBar(
            Text.literal("§6⚡ Crystal Guardian ⚡"),
            BossBar.Color.YELLOW,
            BossBar.Style.PROGRESS
    );

    private int particleTimer = 0;
    private int specialAttackCooldown = 0;
    private UUID summonerId;
    private boolean isEnraged = false;

    // Attack animation fields
    private boolean attacking = false;
    private int attackTimer = 0;

    // Tick-based delayed effect timers
    private int lightningDelayTimer = -1;
    private Vec3d pendingLightningPos = null;
    private int shockwaveTimer = -1;
    private int currentShockwaveRing = 0;
    private Vec3d shockwaveCenter = null;
    private int deathEffectTimer = -1;
    private int currentDeathWave = 0;
    private Vec3d deathCenter = null;

    public GodBossEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 1000;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 300.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.ATTACK_DAMAGE, 20.0)
                .add(EntityAttributes.FOLLOW_RANGE, 64.0)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(EntityAttributes.ARMOR, 10.0)
                .add(EntityAttributes.ARMOR_TOUGHNESS, 5.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new BossAttackGoal(this, 1.2, false));
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 16.0F));
        this.goalSelector.add(5, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    public void setSummoner(UUID summonerId) {
        this.summonerId = summonerId;
    }

    public UUID getSummoner() {
        return this.summonerId;
    }

    // Attack animation methods for renderer
    public boolean isAttacking() {
        return this.attacking;
    }

    public void setAttacking(boolean attacking) {
        this.attacking = attacking;
    }

    public boolean isEnraged() {
        return this.isEnraged;
    }

    // Method to start attack animation
    public void startAttack(int duration) {
        this.setAttacking(true);
        this.attackTimer = duration;
    }

    @Override
    public void tick() {
        super.tick();

        // Update boss bar
        float healthPercent = this.getHealth() / this.getMaxHealth();
        this.bossBar.setPercent(healthPercent);

        // Enrage at low health
        if (healthPercent < 0.3f && !isEnraged) {
            enterEnrageMode();
        }

        if (!this.getWorld().isClient()) {
            ServerWorld serverWorld = (ServerWorld) this.getWorld();

            // Handle delayed effects
            handleDelayedEffects(serverWorld);

            // Particle effects
            handleParticleEffects(serverWorld);

            // Special attacks
            handleSpecialAttacks(serverWorld);

            // Cooldown timers
            if (specialAttackCooldown > 0) specialAttackCooldown--;

            // Attack animation timer
            if (attackTimer > 0) {
                attackTimer--;
                if (attackTimer == 0) {
                    this.setAttacking(false);
                }
            }
        }
    }

    private void handleDelayedEffects(ServerWorld serverWorld) {
        // Handle delayed lightning
        if (lightningDelayTimer > 0) {
            lightningDelayTimer--;
            if (lightningDelayTimer == 0 && pendingLightningPos != null) {
                executeDelayedLightning(serverWorld, pendingLightningPos);
                pendingLightningPos = null;
                lightningDelayTimer = -1;
            }
        }

        // Handle shockwave expansion
        if (shockwaveTimer > 0) {
            shockwaveTimer--;
            if (shockwaveTimer == 0) {
                executeShockwaveRing(serverWorld, shockwaveCenter, currentShockwaveRing);
                currentShockwaveRing++;

                if (currentShockwaveRing <= 8) {
                    shockwaveTimer = 5; // Next ring in 5 ticks
                } else {
                    // Reset shockwave
                    shockwaveTimer = -1;
                    currentShockwaveRing = 0;
                    shockwaveCenter = null;
                }
            }
        }

        // Handle death explosion effects
        if (deathEffectTimer > 0) {
            deathEffectTimer--;
            if (deathEffectTimer == 0) {
                executeDeathWave(serverWorld, deathCenter, currentDeathWave);
                currentDeathWave++;

                if (currentDeathWave < 5) {
                    deathEffectTimer = 10; // Next wave in 10 ticks
                } else {
                    // Reset death effects
                    deathEffectTimer = -1;
                    currentDeathWave = 0;
                    deathCenter = null;
                }
            }
        }
    }

    private void enterEnrageMode() {
        isEnraged = true;
        this.bossBar.setName(Text.literal("§c⚡ ENRAGED Crystal Guardian ⚡"));
        this.bossBar.setColor(BossBar.Color.RED);

        // Boost stats
        this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED).setBaseValue(0.4);
        this.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE).setBaseValue(30.0);

        if (!this.getWorld().isClient()) {
            ServerWorld serverWorld = (ServerWorld) this.getWorld();

            // Enrage effect
            serverWorld.spawnParticles(ParticleTypes.ANGRY_VILLAGER,
                    this.getX(), this.getY() + 2, this.getZ(),
                    20, 1.0, 1.0, 1.0, 0.1);

            this.getWorld().playSound(null, this.getBlockPos(),
                    SoundEvents.ENTITY_ENDER_DRAGON_GROWL, this.getSoundCategory(),
                    2.0F, 0.5F);
        }
    }

    private void handleParticleEffects(ServerWorld serverWorld) {
        particleTimer++;
        if (particleTimer >= 5) {
            particleTimer = 0;

            // Regular magical aura
            serverWorld.spawnParticles(
                    ParticleTypes.ENCHANT,
                    this.getX(),
                    this.getY() + this.getHeight() / 2,
                    this.getZ(),
                    3,
                    1.0, 0.5, 1.0,
                    0.1
            );

            // Special effects based on health
            if (isEnraged) {
                serverWorld.spawnParticles(
                        ParticleTypes.FLAME,
                        this.getX(),
                        this.getY() + this.getHeight(),
                        this.getZ(),
                        5,
                        0.8, 0.5, 0.8,
                        0.1
                );
            } else if (this.getHealth() < this.getMaxHealth() * 0.5f) {
                serverWorld.spawnParticles(
                        ParticleTypes.WITCH,
                        this.getX(),
                        this.getY() + this.getHeight(),
                        this.getZ(),
                        2,
                        0.5, 0.3, 0.5,
                        0.05
                );
            }
        }
    }

    private void handleSpecialAttacks(ServerWorld serverWorld) {
        if (specialAttackCooldown <= 0 && this.getTarget() != null) {
            LivingEntity target = this.getTarget();
            double distance = this.distanceTo(target);

            if (distance > 5.0 && distance < 20.0) {
                // Lightning attack for distant targets
                performLightningAttack(serverWorld, target);
                specialAttackCooldown = isEnraged ? 60 : 100; // 3-5 seconds
            } else if (distance <= 8.0 && this.random.nextFloat() < 0.3f) {
                // Shockwave attack for close targets
                performShockwaveAttack(serverWorld);
                specialAttackCooldown = isEnraged ? 80 : 120; // 4-6 seconds
            }
        }
    }

    private void performLightningAttack(ServerWorld serverWorld, LivingEntity target) {
        Vec3d targetPos = target.getPos();

        // Visual warning
        serverWorld.spawnParticles(ParticleTypes.END_ROD,
                targetPos.x, targetPos.y + 10, targetPos.z,
                10, 0.5, 0.5, 0.5, 0.1);

        // Schedule delayed lightning strike
        pendingLightningPos = targetPos;
        lightningDelayTimer = 20; // 20 ticks = 1 second
    }

    private void executeDelayedLightning(ServerWorld serverWorld, Vec3d targetPos) {
        // Lightning effect
        serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                targetPos.x, targetPos.y, targetPos.z,
                20, 1.0, 2.0, 1.0, 0.2);

        // Damage nearby players
        serverWorld.getEntitiesByClass(PlayerEntity.class,
                Box.of(targetPos, 6.0, 6.0, 6.0),
                entity -> true).forEach(player -> {
            player.damage(serverWorld, this.getDamageSources().mobAttack(this), 15.0f);
        });

        serverWorld.playSound(null, BlockPos.ofFloored(targetPos),
                SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, this.getSoundCategory(),
                1.5F, 1.0F);
    }

    private void performShockwaveAttack(ServerWorld serverWorld) {
        Vec3d bossPos = this.getPos();

        // Initialize shockwave
        shockwaveCenter = bossPos;
        currentShockwaveRing = 1;
        shockwaveTimer = 1; // Start immediately

        serverWorld.playSound(null, this.getBlockPos(),
                SoundEvents.ENTITY_GENERIC_EXPLODE.value(), this.getSoundCategory(),
                2.0F, 0.5F);
    }

    private void executeShockwaveRing(ServerWorld serverWorld, Vec3d center, int ring) {
        double radius = ring * 1.5;

        // Create ring particles
        for (int angle = 0; angle < 360; angle += 20) {
            double radians = Math.toRadians(angle);
            double x = center.x + Math.cos(radians) * radius;
            double z = center.z + Math.sin(radians) * radius;

            serverWorld.spawnParticles(ParticleTypes.EXPLOSION,
                    x, center.y, z, 1, 0.1, 0.1, 0.1, 0.01);
        }

        // Damage players in this ring
        serverWorld.getEntitiesByClass(PlayerEntity.class,
                this.getBoundingBox().expand(radius + 1.0),
                player -> {
                    double dist = player.getPos().distanceTo(center);
                    return dist >= radius - 1.0 && dist <= radius + 1.0;
                }).forEach(player -> {
            player.damage(serverWorld, this.getDamageSources().mobAttack(this), 10.0f);
            // Knockback effect
            Vec3d knockback = player.getPos().subtract(center).normalize().multiply(1.5);
            player.setVelocity(knockback.x, 0.5, knockback.z);
            player.velocityModified = true;
        });
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
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        // Reduce damage from non-player sources
        if (!(source.getAttacker() instanceof PlayerEntity)) {
            amount *= 0.3f;
        }

        // Special death effects
        if (this.getHealth() - amount <= 0 && !this.isDead()) {
            performDeathEffects(world);
        }

        return super.damage(world, source, amount);
    }

    private void performDeathEffects(ServerWorld serverWorld) {
        Vec3d bossPos = this.getPos();

        // Initialize death explosion sequence
        deathCenter = bossPos;
        currentDeathWave = 0;
        deathEffectTimer = 1; // Start immediately

        // Final explosion
        serverWorld.spawnParticles(
                ParticleTypes.END_ROD,
                bossPos.x, bossPos.y + this.getHeight() / 2, bossPos.z,
                100, 3.0, 2.0, 3.0, 0.3
        );

        // Victory announcement
        serverWorld.getServer().getPlayerManager().broadcast(
                Text.literal("§a⚡ The Crystal Guardian has been defeated! ⚡"),
                false
        );
    }

    private void executeDeathWave(ServerWorld serverWorld, Vec3d center, int wave) {
        // Create explosion at random position around boss
        serverWorld.spawnParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                center.x + (this.random.nextDouble() - 0.5) * 4,
                center.y + this.getHeight() / 2,
                center.z + (this.random.nextDouble() - 0.5) * 4,
                1, 0, 0, 0, 0
        );
    }

    @Override
    protected void dropLoot(ServerWorld world, DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(world, damageSource, causedByPlayer);

        // Guaranteed drops
        this.dropStack(world, new ItemStack(ModItems.MAGIC_CRYSTAL, 5 + this.random.nextInt(3))); // 5-7 crystals

        // Rare weapons (25% chance each)
        if (this.random.nextFloat() < 0.25f) {
            this.dropStack(world, new ItemStack(ModItems.GOD_SWORD));
        }

        if (this.random.nextFloat() < 0.25f) {
            this.dropStack(world, new ItemStack(ModItems.GOD_AXE));
        }

        // Extra summoning crystal (rare)
        if (this.random.nextFloat() < 0.1f) {
            this.dropStack(world, new ItemStack(ModItems.SUMMONING_CRYSTAL));
        }

        this.dropExperience(world, null);

        world.playSound(null, this.getBlockPos(),
                SoundEvents.ENTITY_GENERIC_EXPLODE.value(), this.getSoundCategory(),
                2.0F, 0.5F);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (summonerId != null) {
            nbt.putUuid("SummonerId", summonerId);
        }
        nbt.putBoolean("IsEnraged", isEnraged);
        nbt.putBoolean("Attacking", attacking);
        nbt.putInt("AttackTimer", attackTimer);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.containsUuid("SummonerId")) {
            this.summonerId = nbt.getUuid("SummonerId");
        }
        this.isEnraged = nbt.getBoolean("IsEnraged");
        this.attacking = nbt.getBoolean("Attacking");
        this.attackTimer = nbt.getInt("AttackTimer");

        if (isEnraged) {
            enterEnrageMode();
        }
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
        return 1.2F;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public boolean isPersistent() {
        return true;
    }

    // Custom attack goal for boss-specific behavior
    private static class BossAttackGoal extends MeleeAttackGoal {
        private final GodBossEntity boss;

        public BossAttackGoal(GodBossEntity mob, double speed, boolean pauseWhenMobIdle) {
            super(mob, speed, pauseWhenMobIdle);
            this.boss = mob;
        }

        @Override
        protected void attack(LivingEntity target) {
            if (this.canAttack(target)) {
                this.resetCooldown();
                this.mob.swingHand(this.mob.getActiveHand());

                // Start attack animation
                boss.startAttack(20); // 20 ticks = 1 second attack animation

                // Enhanced boss attack
                if (!boss.getWorld().isClient()) {
                    ServerWorld world = (ServerWorld) boss.getWorld();

                    // Damage
                    target.damage(world, boss.getDamageSources().mobAttack(boss),
                            (float) boss.getAttributeValue(EntityAttributes.ATTACK_DAMAGE));

                    // Attack particles
                    world.spawnParticles(ParticleTypes.CRIT,
                            target.getX(), target.getY() + 1, target.getZ(),
                            10, 0.5, 0.5, 0.5, 0.1);
                }
            }
        }
    }
}