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

    // Create the registry key first
    public static final RegistryKey<EntityType<?>> GOD_BOSS_KEY = RegistryKey.of(
            Registries.ENTITY_TYPE.getKey(),
            Identifier.of(GodMod.MOD_ID, "god_boss")
    );

    public static final EntityType<GodBossEntity> GOD_BOSS = Registry.register(
            Registries.ENTITY_TYPE,
            GOD_BOSS_KEY.getValue(),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, GodBossEntity::new)
                    .dimensions(EntityDimensions.fixed(1.4F, 2.8F))
                    .build(GOD_BOSS_KEY)
    );

    public static void initialize() {
        System.out.println("Registering entities for " + GodMod.MOD_ID);

        // Register entity attributes
        FabricDefaultAttributeRegistry.register(GOD_BOSS, GodBossEntity.createAttributes());

        // Skip spawn egg for now - just register the entity
        System.out.println("Entity registered successfully!");
    }
}