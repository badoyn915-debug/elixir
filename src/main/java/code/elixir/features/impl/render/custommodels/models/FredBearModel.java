package code.elixir.features.impl.render.custommodels.models;

import code.elixir.features.impl.render.custommodels.CustomModel;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;

public class FredBearModel extends CustomModel {
   private final ModelPart fredHead;
   private final ModelPart armRight;
   private final ModelPart armLeft;
   private final ModelPart legRight;
   private final ModelPart legLeft;

   public FredBearModel() {
      ModelData modelData = new ModelData();
      ModelPartData rootData = modelData.getRoot();
      ModelPartData fredBodyData = rootData.addChild(
         "fredBody", ModelPartBuilder.create().uv(0, 0).cuboid(-1.0F, -14.0F, -1.0F, 2.0F, 24.0F, 2.0F), ModelTransform.pivot(0.0F, -9.0F, 0.0F)
      );
      fredBodyData.addChild(
         "torso", ModelPartBuilder.create().uv(8, 0).cuboid(-6.0F, -9.0F, -4.0F, 12.0F, 18.0F, 8.0F), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0175F, 0.0F, 0.0F)
      );
      ModelPartData armRightData = fredBodyData.addChild(
         "armRight",
         ModelPartBuilder.create().uv(48, 0).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F),
         ModelTransform.of(-6.5F, -8.0F, 0.0F, 0.0F, 0.0F, 0.2618F)
      );
      armRightData.addChild(
         "armRightpad", ModelPartBuilder.create().uv(70, 10).cuboid(-2.5F, 0.0F, -2.5F, 5.0F, 9.0F, 5.0F), ModelTransform.pivot(0.0F, 0.5F, 0.0F)
      );
      ModelPartData armRight2 = armRightData.addChild(
         "armRight2",
         ModelPartBuilder.create().uv(90, 20).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
         ModelTransform.of(0.0F, 9.6F, 0.0F, -0.1745F, 0.0F, 0.0F)
      );
      armRight2.addChild(
         "armRightpad2", ModelPartBuilder.create().uv(0, 26).cuboid(-2.5F, 0.0F, -2.5F, 5.0F, 7.0F, 5.0F), ModelTransform.pivot(0.0F, 0.5F, 0.0F)
      );
      armRight2.addChild(
         "handRight",
         ModelPartBuilder.create().uv(20, 26).cuboid(-2.0F, 0.0F, -2.5F, 4.0F, 4.0F, 5.0F),
         ModelTransform.of(0.0F, 8.0F, 0.0F, 0.0F, 0.0F, -0.0524F)
      );
      ModelPartData armLeftData = fredBodyData.addChild(
         "armLeft",
         ModelPartBuilder.create().uv(62, 10).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F),
         ModelTransform.of(6.5F, -8.0F, 0.0F, 0.0F, 0.0F, -0.2618F)
      );
      armLeftData.addChild(
         "armLeftpad", ModelPartBuilder.create().uv(38, 54).cuboid(-2.5F, 0.0F, -2.5F, 5.0F, 9.0F, 5.0F), ModelTransform.pivot(0.0F, 0.5F, 0.0F)
      );
      ModelPartData armLeft2 = armLeftData.addChild(
         "armLeft2",
         ModelPartBuilder.create().uv(90, 48).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
         ModelTransform.of(0.0F, 9.6F, 0.0F, -0.1745F, 0.0F, 0.0F)
      );
      armLeft2.addChild("armLeftpad2", ModelPartBuilder.create().uv(0, 58).cuboid(-2.5F, 0.0F, -2.5F, 5.0F, 7.0F, 5.0F), ModelTransform.pivot(0.0F, 0.5F, 0.0F));
      armLeft2.addChild(
         "handLeft",
         ModelPartBuilder.create().uv(58, 56).cuboid(-1.0F, 0.0F, -2.5F, 4.0F, 4.0F, 5.0F),
         ModelTransform.of(0.0F, 8.0F, 0.0F, 0.0F, 0.0F, 0.0524F)
      );
      fredBodyData.addChild("crotch", ModelPartBuilder.create().uv(56, 0).cuboid(-5.5F, 0.0F, -3.5F, 11.0F, 3.0F, 7.0F), ModelTransform.pivot(0.0F, 9.5F, 0.0F));
      ModelPartData legRightData = fredBodyData.addChild(
         "legRight", ModelPartBuilder.create().uv(90, 8).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F), ModelTransform.pivot(-3.3F, 12.5F, 0.0F)
      );
      legRightData.addChild(
         "legRightpad", ModelPartBuilder.create().uv(73, 33).cuboid(-3.0F, 0.0F, -3.0F, 6.0F, 9.0F, 6.0F), ModelTransform.pivot(0.0F, 0.5F, 0.0F)
      );
      ModelPartData legRight2 = legRightData.addChild(
         "legRight2",
         ModelPartBuilder.create().uv(20, 35).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
         ModelTransform.of(0.0F, 9.6F, 0.0F, 0.0349F, 0.0F, 0.0F)
      );
      legRight2.addChild(
         "legRightpad2", ModelPartBuilder.create().uv(0, 39).cuboid(-2.5F, 0.0F, -3.0F, 5.0F, 7.0F, 6.0F), ModelTransform.pivot(0.0F, 0.5F, 0.0F)
      );
      legRight2.addChild(
         "footRight",
         ModelPartBuilder.create().uv(22, 39).cuboid(-2.5F, 0.0F, -6.0F, 5.0F, 3.0F, 8.0F),
         ModelTransform.of(0.0F, 8.0F, 0.0F, -0.0349F, 0.0F, 0.0F)
      );
      ModelPartData legLeftData = fredBodyData.addChild(
         "legLeft", ModelPartBuilder.create().uv(54, 10).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F), ModelTransform.pivot(3.3F, 12.5F, 0.0F)
      );
      legLeftData.addChild(
         "legLeftpad", ModelPartBuilder.create().uv(48, 39).cuboid(-3.0F, 0.0F, -3.0F, 6.0F, 9.0F, 6.0F), ModelTransform.pivot(0.0F, 0.5F, 0.0F)
      );
      ModelPartData legLeft2 = legLeftData.addChild(
         "legLeft2",
         ModelPartBuilder.create().uv(72, 48).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
         ModelTransform.of(0.0F, 9.6F, 0.0F, 0.0349F, 0.0F, 0.0F)
      );
      legLeft2.addChild(
         "legLeftpad2", ModelPartBuilder.create().uv(16, 50).cuboid(-2.5F, 0.0F, -3.0F, 5.0F, 7.0F, 6.0F), ModelTransform.pivot(0.0F, 0.5F, 0.0F)
      );
      legLeft2.addChild(
         "footLeft",
         ModelPartBuilder.create().uv(72, 50).cuboid(-2.5F, 0.0F, -6.0F, 5.0F, 3.0F, 8.0F),
         ModelTransform.of(0.0F, 8.0F, 0.0F, -0.0349F, 0.0F, 0.0F)
      );
      ModelPartData fredHeadData = fredBodyData.addChild(
         "fredHead", ModelPartBuilder.create().uv(39, 22).cuboid(-5.5F, -8.0F, -4.5F, 11.0F, 8.0F, 9.0F), ModelTransform.pivot(0.0F, -13.0F, -0.5F)
      );
      fredHeadData.addChild(
         "jaw", ModelPartBuilder.create().uv(49, 65).cuboid(-5.0F, 0.0F, -4.5F, 10.0F, 3.0F, 9.0F), ModelTransform.of(0.0F, 0.5F, 0.0F, 0.0873F, 0.0F, 0.0F)
      );
      fredHeadData.addChild(
         "frednose", ModelPartBuilder.create().uv(17, 67).cuboid(-4.0F, -2.0F, -3.0F, 8.0F, 4.0F, 3.0F), ModelTransform.pivot(0.0F, -2.0F, -4.5F)
      );
      ModelPartData earRight = fredHeadData.addChild(
         "earRight",
         ModelPartBuilder.create().uv(8, 0).cuboid(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F),
         ModelTransform.of(-4.5F, -5.5F, 0.0F, 0.0524F, 0.0F, -1.0472F)
      );
      earRight.addChild(
         "earRightpad", ModelPartBuilder.create().uv(85, 0).cuboid(-2.0F, -5.0F, -1.0F, 4.0F, 4.0F, 2.0F), ModelTransform.pivot(0.0F, -1.0F, 0.0F)
      );
      ModelPartData earLeft = fredHeadData.addChild(
         "earLeft",
         ModelPartBuilder.create().uv(40, 0).cuboid(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F),
         ModelTransform.of(4.5F, -5.5F, 0.0F, 0.0524F, 0.0F, 1.0472F)
      );
      earLeft.addChild(
         "earRightpad_1", ModelPartBuilder.create().uv(40, 39).cuboid(-2.0F, -5.0F, -1.0F, 4.0F, 4.0F, 2.0F), ModelTransform.pivot(0.0F, -1.0F, 0.0F)
      );
      ModelPartData hat = fredHeadData.addChild(
         "hat", ModelPartBuilder.create().uv(70, 24).cuboid(-3.0F, -0.5F, -3.0F, 6.0F, 1.0F, 6.0F), ModelTransform.of(0.0F, -8.4F, 0.0F, -0.0175F, 0.0F, 0.0F)
      );
      hat.addChild(
         "hat2", ModelPartBuilder.create().uv(78, 61).cuboid(-2.0F, -4.0F, -2.0F, 4.0F, 4.0F, 4.0F), ModelTransform.of(0.0F, 0.1F, 0.0F, -0.0175F, 0.0F, 0.0F)
      );
      this.root = rootData.createPart(100, 80);
      ModelPart fredBody = this.root.getChild("fredBody");
      this.armRight = fredBody.getChild("armRight");
      this.armLeft = fredBody.getChild("armLeft");
      this.legRight = fredBody.getChild("legRight");
      this.legLeft = fredBody.getChild("legLeft");
      this.fredHead = fredBody.getChild("fredHead");
   }

   @Override
   public void setAngles(PlayerEntityRenderState state, PlayerEntityModel playerModel) {
      copyRotation(this.fredHead, playerModel.head);
      copyRotation(this.armLeft, playerModel.leftArm, -0.2618F);
      copyRotation(this.armRight, playerModel.rightArm, 0.2618F);
      copyRotation(this.legRight, playerModel.rightLeg);
      copyRotation(this.legLeft, playerModel.leftLeg);
   }
}
