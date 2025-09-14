package com.example.renderer;

import com.example.GodBossEntity;
import com.example.model.GodBossEntityModel;
import com.example.model.GodBossEntityRenderState;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class GodBossEntityRenderer extends LivingEntityRenderer<GodBossEntity, GodBossEntityRenderState, GodBossEntityModel> {
    private static final Identifier TEXTURE = Identifier.of("godmod", "textures/entity/god_boss.png");
    private static final Identifier ENRAGED_TEXTURE = Identifier.of("godmod", "textures/entity/god_boss_enraged.png");

    public static final net.minecraft.client.render.entity.model.EntityModelLayer GOD_BOSS_MODEL_LAYER =
            new net.minecraft.client.render.entity.model.EntityModelLayer(Identifier.of("godmod", "god_boss"), "main");

    public GodBossEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new GodBossEntityModel(context.getPart(GOD_BOSS_MODEL_LAYER)), 1.0f);
    }

    @Override
    public Identifier getTexture(GodBossEntityRenderState renderState) {
        // Check if the boss is enraged and return appropriate texture
        if (renderState.isEnraged) {
            return ENRAGED_TEXTURE;
        }
        return TEXTURE;
    }

    @Override
    protected void scale(GodBossEntityRenderState renderState, MatrixStack matrices) {
        // Make the boss bigger
        float scale = 2.0f;
        matrices.scale(scale, scale, scale);

        // Add a subtle breathing/pulsing effect
        float breathe = 1.0f + 0.02f * (float)Math.sin(renderState.age * 0.1f);
        matrices.scale(breathe, 1.0f, breathe);
    }

    @Override
    public GodBossEntityRenderState createRenderState() {
        return new GodBossEntityRenderState();
    }

    @Override
    public void updateRenderState(GodBossEntity entity, GodBossEntityRenderState renderState, float tickDelta) {
        super.updateRenderState(entity, renderState, tickDelta);

        // Update custom boss-specific render state
        renderState.isAttacking = entity.isAttacking();
        renderState.isEnraged = entity.isEnraged();
    }
}