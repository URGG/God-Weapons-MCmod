package com.example.model;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;

public class GodBossEntityRenderState extends LivingEntityRenderState {
    public boolean isAttacking;
    public boolean isEnraged;
    public float attackProgress; // 0.0 to 1.0 for attack animations
    public int attackType; // Different attack types if your boss has multiple
}