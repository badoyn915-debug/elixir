package code.elixir.features.impl.render.custommodels.models;

import code.elixir.features.impl.render.custommodels.CustomModel;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;

public class ChinchillaModel extends CustomModel {
   private final ModelPart nose;
   private final ModelPart rightEar;
   private final ModelPart rightWhiskers;
   private final ModelPart leftEar;
   private final ModelPart leftRearFoot;
   private final ModelPart rightRearFoot;
   private final ModelPart rightFrontLeg;
   private final ModelPart rightFrontFoot;
   private final ModelPart leftFrontLeg;
   private final ModelPart leftFrontFoot;
   private final ModelPart chin;
   private final ModelPart leftWhiskers;
   private final ModelPart chinHead;

   public ChinchillaModel() {
      ModelData modelData = new ModelData();
      ModelPartData rootData = modelData.getRoot();
      rootData.addChild(
         "back", ModelPartBuilder.create().uv(0, 14).cuboid(-3.0F, 0.3F, 3.3F, 6.0F, 6.0F, 3.0F), ModelTransform.of(0.0F, 18.0F, -2.0F, 0.2443461F, 0.0F, 0.0F)
      );
      rootData.addChild(
         "nose",
         ModelPartBuilder.create().uv(46, 24).cuboid(-2.0F, -3.0F, -3.0F, 4.0F, 4.0F, 2.0F),
         ModelTransform.of(0.0F, 18.0F, -3.0F, 0.3687912F, 0.0F, 0.0F)
      );
      rootData.addChild(
         "rightEar",
         ModelPartBuilder.create().uv(48, 8).cuboid(0.5F, -6.0F, -0.6F, 1.0F, 4.0F, 3.0F),
         ModelTransform.of(0.0F, 18.0F, -3.0F, 0.8726646F, 2.094395F, 0.0F)
      );
      rootData.addChild(
         "rightWhiskers",
         ModelPartBuilder.create().uv(18, 19).cuboid(-5.0F, -1.4F, -4.04F, 4.0F, 3.0F, 1.0F),
         ModelTransform.of(0.0F, 18.0F, -3.0F, -0.0872665F, 0.1745329F, 0.0F)
      );
      rootData.addChild(
         "leftEar",
         ModelPartBuilder.create().uv(48, 0).cuboid(0.5F, -6.0F, -2.4F, 1.0F, 4.0F, 3.0F),
         ModelTransform.of(0.0F, 18.0F, -3.0F, -0.8726646F, 1.047198F, 0.0F)
      );
      rootData.addChild(
         "upperTail",
         ModelPartBuilder.create().uv(14, 23).cuboid(-1.5F, 1.5F, 2.6F, 3.0F, 3.0F, 4.0F),
         ModelTransform.of(0.0F, 22.2F, 4.066667F, 0.7330383F, 0.0F, 0.0F)
      );
      rootData.addChild(
         "chinBody",
         ModelPartBuilder.create().uv(0, 0).cuboid(-3.5F, -2.0F, -1.0F, 7.0F, 7.0F, 7.0F),
         ModelTransform.of(0.0F, 18.0F, -2.0F, -0.0872665F, 0.0F, 0.0F)
      );
      rootData.addChild(
         "lowerTail",
         ModelPartBuilder.create().uv(0, 23).cuboid(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 5.0F),
         ModelTransform.of(0.0F, 22.2F, 4.066667F, -0.1047198F, 0.0F, 0.0F)
      );
      rootData.addChild(
         "rightRearFoot",
         ModelPartBuilder.create().uv(28, 9).cuboid(-1.5F, 0.0F, -3.0F, 2.0F, 1.0F, 4.0F),
         ModelTransform.of(-3.0F, 23.0F, 3.0F, 0.0F, 0.0698132F, 0.0F)
      );
      rootData.addChild(
         "leftRearFoot",
         ModelPartBuilder.create().uv(28, 0).cuboid(-0.5F, 0.0F, -3.0F, 2.0F, 1.0F, 4.0F),
         ModelTransform.of(3.0F, 23.0F, 3.0F, 0.0F, -0.0698132F, 0.0F)
      );
      rootData.addChild(
         "rightFrontLeg",
         ModelPartBuilder.create().uv(40, 9).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F),
         ModelTransform.of(-3.0F, 20.0F, -3.0F, -0.1858931F, 0.0F, 0.0F)
      );
      rootData.addChild(
         "rightFrontFoot",
         ModelPartBuilder.create().uv(28, 14).cuboid(-1.0F, 3.0F, -2.8F, 2.0F, 1.0F, 3.0F),
         ModelTransform.of(-3.0F, 20.0F, -3.0F, 0.0F, -0.0698132F, 0.0F)
      );
      rootData.addChild(
         "leftFrontLeg",
         ModelPartBuilder.create().uv(40, 0).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F),
         ModelTransform.of(3.0F, 20.0F, -3.0F, -0.1858931F, 0.0F, 0.0F)
      );
      rootData.addChild(
         "leftFrontFoot",
         ModelPartBuilder.create().uv(28, 5).cuboid(-1.0F, 3.0F, -2.8F, 2.0F, 1.0F, 3.0F),
         ModelTransform.of(3.0F, 20.0F, -3.0F, 0.0F, 0.0698132F, 0.0F)
      );
      rootData.addChild(
         "chin",
         ModelPartBuilder.create().uv(46, 18).cuboid(-2.0F, -1.3F, -4.04F, 4.0F, 3.0F, 2.0F),
         ModelTransform.of(0.0F, 18.0F, -3.0F, -0.1115358F, 0.0F, 0.0F)
      );
      rootData.addChild(
         "leftWhiskers",
         ModelPartBuilder.create().uv(18, 14).cuboid(1.0F, -1.4F, -4.04F, 4.0F, 3.0F, 1.0F),
         ModelTransform.of(0.0F, 18.0F, -3.0F, -0.0872665F, -0.1745329F, 0.0F)
      );
      rootData.addChild(
         "chinHead", ModelPartBuilder.create().uv(28, 21).cuboid(-2.5F, -3.0F, -2.4F, 5.0F, 5.0F, 4.0F), ModelTransform.pivot(0.0F, 18.0F, -3.0F)
      );
      this.root = rootData.createPart(64, 32);
      this.nose = this.root.getChild("nose");
      this.rightEar = this.root.getChild("rightEar");
      this.rightWhiskers = this.root.getChild("rightWhiskers");
      this.leftEar = this.root.getChild("leftEar");
      this.leftRearFoot = this.root.getChild("leftRearFoot");
      this.rightRearFoot = this.root.getChild("rightRearFoot");
      this.rightFrontLeg = this.root.getChild("rightFrontLeg");
      this.rightFrontFoot = this.root.getChild("rightFrontFoot");
      this.leftFrontLeg = this.root.getChild("leftFrontLeg");
      this.leftFrontFoot = this.root.getChild("leftFrontFoot");
      this.chin = this.root.getChild("chin");
      this.leftWhiskers = this.root.getChild("leftWhiskers");
      this.chinHead = this.root.getChild("chinHead");
   }

   @Override
   public void setAngles(PlayerEntityRenderState state, PlayerEntityModel playerModel) {
      this.chinHead.pitch = playerModel.head.pitch;
      this.chinHead.yaw = playerModel.head.yaw;
      this.chin.pitch = this.chinHead.pitch;
      this.chin.yaw = this.chinHead.yaw;
      this.nose.pitch = this.chinHead.pitch;
      this.nose.yaw = this.chinHead.yaw;
      this.rightEar.yaw = this.chinHead.yaw + 2.094395F;
      this.leftEar.yaw = this.chinHead.yaw + 1.047198F;
      this.leftFrontLeg.pitch = playerModel.leftLeg.pitch;
      this.rightFrontLeg.pitch = playerModel.rightLeg.pitch;
      this.leftFrontFoot.pitch = playerModel.leftLeg.pitch;
      this.rightFrontFoot.pitch = playerModel.rightLeg.pitch;
      this.leftRearFoot.pitch = -playerModel.leftLeg.pitch;
      this.rightRearFoot.pitch = -playerModel.rightLeg.pitch;
      this.leftWhiskers.yaw = this.chinHead.yaw - 0.1745329F;
      this.leftWhiskers.pitch = this.chinHead.pitch - 0.1745329F;
      this.rightWhiskers.yaw = this.chinHead.yaw + 0.1745329F;
      this.rightWhiskers.pitch = this.chinHead.pitch + 0.1745329F;
   }
}
