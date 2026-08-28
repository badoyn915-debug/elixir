package code.elixir.features.impl.render.custommodels.models;

import code.elixir.features.impl.render.custommodels.CustomModel;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;

public class SonicModel extends CustomModel {
   private final ModelPart sonicHead;
   private final ModelPart rleg;
   private final ModelPart lleg;
   private final ModelPart larm;
   private final ModelPart rarm;

   public SonicModel() {
      ModelData modelData = new ModelData();
      ModelPartData rootData = modelData.getRoot();
      rootData.addChild(
         "bbMain",
         ModelPartBuilder.create()
            .uv(0, 0)
            .cuboid(-3.0F, -17.0F, -2.0F, 5.0F, 7.0F, 5.0F)
            .uv(0, 22)
            .cuboid(-3.0F, -16.0F, -3.0F, 5.0F, 5.0F, 1.0F)
            .uv(0, 0)
            .cuboid(-3.0F, -16.0F, 3.0F, 5.0F, 5.0F, 1.0F)
            .uv(0, 0)
            .cuboid(0.0F, -15.0F, 3.0F, 1.0F, 1.0F, 3.0F)
            .uv(0, 0)
            .cuboid(-3.0F, -14.0F, 4.0F, 1.0F, 1.0F, 2.0F)
            .uv(0, 0)
            .cuboid(-1.0F, -11.0F, 3.0F, 1.0F, 1.0F, 2.0F)
            .uv(0, 0)
            .cuboid(0.0F, -14.0F, 6.0F, 1.0F, 1.0F, 1.0F)
            .uv(0, 0)
            .cuboid(-3.0F, -13.0F, 6.0F, 1.0F, 1.0F, 1.0F)
            .uv(0, 0)
            .cuboid(-1.0F, -12.0F, 5.0F, 1.0F, 1.0F, 1.0F)
            .uv(0, 0)
            .cuboid(-4.0F, -16.0F, -2.0F, 1.0F, 5.0F, 5.0F)
            .uv(0, 0)
            .cuboid(2.0F, -16.0F, -2.0F, 1.0F, 5.0F, 5.0F),
         ModelTransform.pivot(0.0F, 24.0F, 0.0F)
      );
      ModelPartData sonicHeadData = rootData.addChild(
         "sonicHead",
         ModelPartBuilder.create().uv(36, 3).cuboid(-3.5F, -7.0F, -3.0F, 7.0F, 7.0F, 7.0F).uv(60, 0).cuboid(-0.5F, -3.0F, -4.0F, 1.0F, 1.0F, 1.0F),
         ModelTransform.pivot(-0.5F, 7.0F, 0.0F)
      );
      sonicHeadData.addChild(
         "spike1",
         ModelPartBuilder.create().uv(0, 0).cuboid(-3.5F, -4.0F, 6.0F, 3.0F, 3.0F, 3.0F).uv(0, 0).cuboid(-3.5F, -5.0F, 4.0F, 3.0F, 3.0F, 2.0F),
         ModelTransform.pivot(0.0F, 0.0F, 0.0F)
      );
      sonicHeadData.addChild(
         "spike2",
         ModelPartBuilder.create().uv(0, 0).cuboid(0.5F, -6.0F, 6.0F, 3.0F, 3.0F, 3.0F).uv(0, 0).cuboid(0.5F, -7.0F, 4.0F, 3.0F, 3.0F, 2.0F),
         ModelTransform.pivot(0.0F, 0.0F, 0.0F)
      );
      sonicHeadData.addChild(
         "spike3",
         ModelPartBuilder.create().uv(0, 0).cuboid(-2.5F, -9.0F, 3.0F, 3.0F, 3.0F, 2.0F).uv(0, 0).cuboid(-2.5F, -10.0F, 5.0F, 3.0F, 3.0F, 3.0F),
         ModelTransform.pivot(0.0F, 0.0F, 0.0F)
      );
      rootData.addChild(
         "rleg",
         ModelPartBuilder.create()
            .uv(53, 54)
            .cuboid(-1.5F, 7.0F, -1.0F, 2.0F, 1.0F, 2.0F)
            .uv(0, 0)
            .cuboid(-1.0F, -1.0F, -0.5F, 1.0F, 8.0F, 1.0F)
            .uv(52, 58)
            .cuboid(-1.5F, 8.0F, -3.0F, 2.0F, 2.0F, 4.0F)
            .uv(52, 58)
            .cuboid(-2.0F, 9.0F, -2.0F, 0.0F, 1.0F, 1.0F),
         ModelTransform.pivot(-3.0F, 14.0F, 0.5F)
      );
      rootData.addChild(
         "lleg",
         ModelPartBuilder.create()
            .uv(53, 54)
            .cuboid(-0.5F, 7.0F, -1.0F, 2.0F, 1.0F, 2.0F)
            .uv(0, 0)
            .cuboid(0.0F, -1.0F, -0.5F, 1.0F, 8.0F, 1.0F)
            .uv(52, 58)
            .cuboid(-0.5F, 8.0F, -3.0F, 2.0F, 2.0F, 4.0F)
            .uv(52, 58)
            .cuboid(1.5F, 9.0F, -2.0F, 0.0F, 1.0F, 1.0F),
         ModelTransform.pivot(2.0F, 14.0F, 0.5F)
      );
      rootData.addChild(
         "larm",
         ModelPartBuilder.create().uv(0, 60).cuboid(-0.5F, 8.0F, -1.0F, 2.0F, 2.0F, 2.0F).uv(60, 17).cuboid(0.0F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F),
         ModelTransform.of(3.0F, 9.0F, 0.5F, 0.0F, 0.0F, -0.0873F)
      );
      rootData.addChild(
         "rarm",
         ModelPartBuilder.create().uv(60, 17).cuboid(-1.0F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F).uv(0, 60).cuboid(-1.5F, 8.0F, -1.0F, 2.0F, 2.0F, 2.0F),
         ModelTransform.of(-4.0F, 9.0F, 0.5F, 0.0F, 0.0F, 0.0873F)
      );
      this.root = rootData.createPart(64, 64);
      this.sonicHead = this.root.getChild("sonicHead");
      this.rleg = this.root.getChild("rleg");
      this.lleg = this.root.getChild("lleg");
      this.larm = this.root.getChild("larm");
      this.rarm = this.root.getChild("rarm");
   }

   @Override
   public void setAngles(PlayerEntityRenderState state, PlayerEntityModel playerModel) {
      copyRotation(this.sonicHead, playerModel.head);
      copyRotation(this.larm, playerModel.leftArm, -0.0873F);
      copyRotation(this.rarm, playerModel.rightArm, 0.0873F);
      copyRotation(this.rleg, playerModel.rightLeg);
      copyRotation(this.lleg, playerModel.leftLeg);
   }
}
