package com.example.model;

import com.example.GodBossEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.render.RenderLayer;

import java.util.function.Function;

public class GodBossEntityModel extends EntityModel<GodBossEntityRenderState> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart crown;
    private final ModelPart cape;

    public GodBossEntityModel(ModelPart root) {
        super(root, RenderLayer::getEntityCutoutNoCull);
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.crown = head.getChild("crown");
        this.cape = body.getChild("cape");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();

        // Scale factor for making the boss bigger (2x larger than normal)
        float scale = 2.0f;

        // Head (bigger than normal)
        ModelPartData head = modelPartData.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 0)
                        .cuboid(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, new Dilation(scale - 1.0f)),
                ModelTransform.pivot(0.0f, 0.0f, 0.0f));

        // Crown on top of head (unique to boss)
        head.addChild("crown",
                ModelPartBuilder.create()
                        .uv(32, 0)
                        .cuboid(-6.0f, -12.0f, -6.0f, 12.0f, 4.0f, 12.0f),
                ModelTransform.pivot(0.0f, 0.0f, 0.0f));

        // Body (taller and wider)
        ModelPartData body = modelPartData.addChild("body",
                ModelPartBuilder.create()
                        .uv(16, 16)
                        .cuboid(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f, new Dilation(scale - 1.0f)),
                ModelTransform.pivot(0.0f, 0.0f, 0.0f));

        // Cape behind body
        body.addChild("cape",
                ModelPartBuilder.create()
                        .uv(0, 32)
                        .cuboid(-6.0f, 0.0f, 2.1f, 12.0f, 18.0f, 1.0f),
                ModelTransform.pivot(0.0f, 0.0f, 0.0f));

        // Right Arm (longer and thicker)
        modelPartData.addChild("right_arm",
                ModelPartBuilder.create()
                        .uv(40, 16)
                        .cuboid(-3.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, new Dilation(scale - 1.0f)),
                ModelTransform.pivot(-5.0f * scale, 2.0f, 0.0f));

        // Left Arm
        modelPartData.addChild("left_arm",
                ModelPartBuilder.create()
                        .uv(40, 16)
                        .mirrored()
                        .cuboid(-1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, new Dilation(scale - 1.0f)),
                ModelTransform.pivot(5.0f * scale, 2.0f, 0.0f));

        // Right Leg (longer)
        modelPartData.addChild("right_leg",
                ModelPartBuilder.create()
                        .uv(0, 16)
                        .cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new Dilation(scale - 1.0f)),
                ModelTransform.pivot(-1.9f, 12.0f, 0.0f));

        // Left Leg
        modelPartData.addChild("left_leg",
                ModelPartBuilder.create()
                        .uv(0, 16)
                        .mirrored()
                        .cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new Dilation(scale - 1.0f)),
                ModelTransform.pivot(1.9f, 12.0f, 0.0f));

        return TexturedModelData.of(modelData, 128, 64); // Larger texture size
    }

    @Override
    public void setAngles(GodBossEntityRenderState renderState) {
        super.setAngles(renderState);

        // Head movement
        this.head.yaw = renderState.yawDegrees * 0.017453292F;
        this.head.pitch = renderState.pitch * 0.017453292F;

        // Walking animation
        float limbSwing = renderState.limbFrequency;
        float limbSwingAmount = renderState.limbAmplitudeMultiplier;
        this.rightArm.pitch = MathHelper.cos(limbSwing * 0.6662F + (float)Math.PI) * 2.0F * limbSwingAmount * 0.5F;
        this.leftArm.pitch = MathHelper.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F;
        this.rightLeg.pitch = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.leftLeg.pitch = MathHelper.cos(limbSwing * 0.6662F + (float)Math.PI) * 1.4F * limbSwingAmount;

        // Floating/hovering animation
        float hoverOffset = MathHelper.sin(renderState.age * 0.1F) * 0.05F;
        this.root.pivotY = hoverOffset;

        // Crown glow animation
        this.crown.yaw = MathHelper.sin(renderState.age * 0.05F) * 0.1F;

        // Cape sway animation
        this.cape.pitch = MathHelper.sin(renderState.age * 0.08F) * 0.2F + 0.2F;
        this.cape.yaw = MathHelper.sin(renderState.age * 0.06F) * 0.1F;

        // Attack animation
        if (renderState.isAttacking) {
            this.rightArm.pitch -= 0.5F;
            this.leftArm.pitch -= 0.3F;
        }

        // Enraged animation
        if (renderState.isEnraged) {
            this.head.pitch += MathHelper.sin(renderState.age * 0.3F) * 0.1F;
            this.crown.yaw += MathHelper.sin(renderState.age * 0.2F) * 0.2F;
        }
    }
}