package com.example;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

public class ModEntities {

    public static final EntityType<ThrowableGodAxeEntity> THROWABLE_GOD_AXE;

    static {
        Identifier id = Identifier.of(GodMod.MOD_ID, "throwable_god_axe");
        RegistryKey<EntityType<?>> key = RegistryKey.of(Registries.ENTITY_TYPE.getKey(), id);

        THROWABLE_GOD_AXE = Registry.register(
                Registries.ENTITY_TYPE,
                id,
                FabricEntityTypeBuilder.<ThrowableGodAxeEntity>create(SpawnGroup.MISC, ThrowableGodAxeEntity::new)
                        .dimensions(EntityDimensions.fixed(0.25f, 0.25f))
                        .trackRangeBlocks(64)
                        .trackedUpdateRate(10)
                        .build(key)
        );
    }

    public static void initialize() {
        System.out.println("Registering entities for " + GodMod.MOD_ID);
        // Entity registration happens automatically when the static final fields are initialized
    }
}