package code.elixir.features.impl.render.custommodels.models;

import code.elixir.features.impl.render.custommodels.CustomModel;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;

public class DemonModel extends CustomModel {
   private final ModelPart head;
   private final ModelPart leftArm;
   private final ModelPart rightArm;
   private final ModelPart leftLeg;
   private final ModelPart rightLeg;

   public DemonModel() {
      ModelData modelData = new ModelData();
      ModelPartData rootData = modelData.getRoot();
      ModelPartData headData = rootData.addChild(
         "head", ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -4.0F, -3.0F, 8.0F, 8.0F, 8.0F, new Dilation(0.3F)), ModelTransform.pivot(0.0F, -6.0F, -1.0F)
      );
      headData.addChild(
         "leftHorn",
         ModelPartBuilder.create()
            .uv(32, 8)
            .cuboid(13.4346F, -5.2071F, 2.7071F, 6.0F, 2.0F, 2.0F, new Dilation(0.1F))
            .uv(0, 0)
            .cuboid(17.4346F, -10.4071F, 2.7071F, 2.0F, 5.0F, 2.0F, new Dilation(0.1F)),
         ModelTransform.of(-8.0F, 8.0F, 0.0F, -0.3927F, 0.3927F, -0.5236F)
      );
      headData.addChild(
         "rightHorn",
         ModelPartBuilder.create()
            .uv(32, 8)
            .cuboid(-19.4346F, -5.2071F, 2.7071F, 6.0F, 2.0F, 2.0F, new Dilation(0.1F))
            .uv(0, 0)
            .cuboid(-19.4346F, -10.4071F, 2.7071F, 2.0F, 5.0F, 2.0F, new Dilation(0.1F)),
         ModelTransform.of(8.0F, 8.0F, 0.0F, -0.3927F, -0.3927F, 0.5236F)
      );
      ModelPartData bodyData = rootData.addChild(
         "body",
         ModelPartBuilder.create().uv(0, 16).cuboid(-4.5F, -1.7028F, 1.4696F, 8.0F, 12.0F, 4.0F),
         ModelTransform.of(0.5F, -0.1F, -3.5F, 0.1745F, 0.0F, 0.0F)
      );
      bodyData.addChild(
         "leftWing",
         ModelPartBuilder.create().uv(40, 12).cuboid(-7.0072F, -0.5972F, 0.7515F, 12.0F, 13.0F, 0.0F),
         ModelTransform.of(8.25F, -2.0F, 10.0F, 0.0873F, -0.829F, 0.1745F)
      );
      bodyData.addChild(
         "rightWing",
         ModelPartBuilder.create().uv(40, 12).cuboid(-4.9928F, -0.5972F, 0.7515F, 12.0F, 13.0F, 0.0F),
         ModelTransform.of(-9.25F, -2.0F, 10.0F, 0.0873F, 0.829F, -0.1745F)
      );
      rootData.addChild(
         "leftArm",
         ModelPartBuilder.create().uv(24, 16).cuboid(-1.1F, -1.05F, 0.0F, 4.0F, 14.0F, 4.0F),
         ModelTransform.of(5.4F, -1.25F, -2.0F, 0.0F, 0.0F, -0.2182F)
      );
      rootData.addChild(
         "rightArm",
         ModelPartBuilder.create().uv(24, 16).cuboid(-2.9F, -1.05F, 0.0F, 4.0F, 14.0F, 4.0F),
         ModelTransform.of(-5.4F, -1.25F, -2.0F, 0.0F, 0.0F, 0.2182F)
      );
      ModelPartData leftLegData = rootData.addChild(
         "leftLeg", ModelPartBuilder.create().uv(48, 22).cuboid(-3.25F, -2.25F, -1.0F, 4.0F, 9.0F, 4.0F), ModelTransform.pivot(3.0F, 10.0F, 0.0F)
      );
      ModelPartData leftLeg1 = leftLegData.addChild(
         "leftLeg1",
         ModelPartBuilder.create().uv(34, 34).cuboid(0.95F, 4.6F, 8.0511F, 3.0F, 5.0F, 3.0F),
         ModelTransform.of(-1.7F, -0.1F, -3.55F, -0.5236F, 0.0F, 0.0F)
      );
      leftLeg1.addChild(
         "bone2",
         ModelPartBuilder.create().uv(26, 0).cuboid(-0.7F, -1.15F, 9.3F, 4.0F, 2.0F, 4.0F).uv(40, 0).cuboid(-0.7F, -1.15F, 7.3F, 4.0F, 2.0F, 2.0F),
         ModelTransform.of(1.4F, 15.0F, 0.25F, 0.5236F, 0.0F, 0.0F)
      );
      ModelPartData bone3 = leftLeg1.addChild("bone3", ModelPartBuilder.create(), ModelTransform.of(-1.0F, 0.0F, -2.0F, 0.0F, -0.0873F, -0.2618F));
      bone3.addChild(
         "bone7",
         ModelPartBuilder.create()
            .uv(16, 34)
            .cuboid(-0.7911F, -10.1159F, 8.0029F, 4.0F, 4.0F, 5.0F)
            .uv(0, 32)
            .cuboid(-0.7911F, -15.1159F, 4.0029F, 4.0F, 9.0F, 4.0F),
         ModelTransform.pivot(1.9F, 12.0F, 0.25F)
      );
      ModelPartData rightLegData = rootData.addChild(
         "rightLeg", ModelPartBuilder.create().uv(48, 22).cuboid(-0.75F, -2.25F, -1.0F, 4.0F, 9.0F, 4.0F), ModelTransform.pivot(-3.0F, 10.0F, 0.0F)
      );
      ModelPartData rightLeg3 = rightLegData.addChild(
         "rightLeg3",
         ModelPartBuilder.create().uv(34, 34).cuboid(-3.95F, 4.6F, 8.0511F, 3.0F, 5.0F, 3.0F),
         ModelTransform.of(1.7F, -0.1F, -3.55F, -0.5236F, 0.0F, 0.0F)
      );
      rightLeg3.addChild(
         "bone4",
         ModelPartBuilder.create().uv(26, 0).cuboid(-3.3F, -1.15F, 9.3F, 4.0F, 2.0F, 4.0F).uv(40, 0).cuboid(-3.3F, -1.15F, 7.3F, 4.0F, 2.0F, 2.0F),
         ModelTransform.of(-1.4F, 15.0F, 0.25F, 0.5236F, 0.0F, 0.0F)
      );
      ModelPartData bone5 = rightLeg3.addChild("bone5", ModelPartBuilder.create(), ModelTransform.of(1.0F, 0.0F, -2.0F, 0.0F, 0.0873F, 0.2618F));
      bone5.addChild(
         "bone6",
         ModelPartBuilder.create()
            .uv(16, 34)
            .cuboid(-3.2089F, -10.1159F, 8.0029F, 4.0F, 4.0F, 5.0F)
            .uv(0, 32)
            .cuboid(-3.2089F, -15.1159F, 4.0029F, 4.0F, 9.0F, 4.0F),
         ModelTransform.pivot(-1.9F, 12.0F, 0.25F)
      );
      this.root = rootData.createPart(64, 64);
      this.head = this.root.getChild("head");
      this.leftArm = this.root.getChild("leftArm");
      this.rightArm = this.root.getChild("rightArm");
      this.leftLeg = this.root.getChild("leftLeg");
      this.rightLeg = this.root.getChild("rightLeg");
   }

   @Override
   public void setAngles(PlayerEntityRenderState state, PlayerEntityModel playerModel) {
      copyRotation(this.head, playerModel.head);
      copyRotation(this.leftArm, playerModel.leftArm, -0.2182F);
      copyRotation(this.rightArm, playerModel.rightArm, 0.2182F);
      copyRotation(this.leftLeg, playerModel.leftLeg);
      copyRotation(this.rightLeg, playerModel.rightLeg);
   }
}
