package com.example;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

public class ModEntities {

    public static final EntityType<ThrowableGodAxeEntity> THROWABLE_GOD_AXE;
    public static final EntityType<GodBossEntity> GOD_BOSS;

    static {
        // Register Throwable God Axe Entity
        Identifier axeId = Identifier.of(GodMod.MOD_ID, "throwable_god_axe");
        RegistryKey<EntityType<?>> axeKey = RegistryKey.of(Registries.ENTITY_TYPE.getKey(), axeId);

        THROWABLE_GOD_AXE = Registry.register(
                Registries.ENTITY_TYPE,
                axeId,
                FabricEntityTypeBuilder.<ThrowableGodAxeEntity>create(SpawnGroup.MISC, ThrowableGodAxeEntity::new)
                        .dimensions(EntityDimensions.fixed(0.25f, 0.25f))
                        .trackRangeBlocks(64)
                        .trackedUpdateRate(10)
                        .build(axeKey)
        );

        // Register God Boss Entity
        Identifier bossId = Identifier.of(GodMod.MOD_ID, "god_boss");
        RegistryKey<EntityType<?>> bossKey = RegistryKey.of(Registries.ENTITY_TYPE.getKey(), bossId);

        GOD_BOSS = Registry.register(
                Registries.ENTITY_TYPE,
                bossId,
                FabricEntityTypeBuilder.<GodBossEntity>create(SpawnGroup.MONSTER, GodBossEntity::new)
                        .dimensions(EntityDimensions.fixed(1.5f, 3.0f)) // Width, Height
                        .trackRangeBlocks(64)
                        .trackedUpdateRate(1) // Boss entities should update frequently
                        .build(bossKey)
        );
    }

    public static void initialize() {
        System.out.println("Registering entities for " + GodMod.MOD_ID);

        // Register attributes for the God Boss
        FabricDefaultAttributeRegistry.register(GOD_BOSS, GodBossEntity.createAttributes());

        // Note: ThrowableGodAxeEntity doesn't need attributes as it's a projectile

        System.out.println("Entity registration complete!");
    }
}