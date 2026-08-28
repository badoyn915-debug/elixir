package code.elixir.features.impl.render.custommodels.models;

import code.elixir.features.impl.render.custommodels.CustomModel;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;

public class RabbitModel extends CustomModel {
   private final ModelPart rabbitHead;
   private final ModelPart rabbitRarm;
   private final ModelPart rabbitLarm;
   private final ModelPart rabbitRleg;
   private final ModelPart rabbitLleg;

   public RabbitModel() {
      ModelData modelData = new ModelData();
      ModelPartData rootData = modelData.getRoot();
      ModelPartData boneData = rootData.addChild(
         "bone", ModelPartBuilder.create().uv(28, 45).cuboid(-5.0F, -13.0F, -5.0F, 10.0F, 11.0F, 8.0F), ModelTransform.pivot(0.0F, 24.0F, 0.0F)
      );
      boneData.addChild(
         "rabbitRleg", ModelPartBuilder.create().uv(0, 0).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 2.0F, 4.0F), ModelTransform.pivot(-3.0F, -2.0F, -1.0F)
      );
      boneData.addChild("rabbitLleg", ModelPartBuilder.create().uv(0, 0).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 2.0F, 4.0F), ModelTransform.pivot(3.0F, -2.0F, -1.0F));
      boneData.addChild(
         "rabbitRarm",
         ModelPartBuilder.create().uv(0, 0).cuboid(-2.0F, 0.0F, -2.0F, 2.0F, 8.0F, 4.0F),
         ModelTransform.of(-5.0F, -13.0F, -1.0F, 0.0F, 0.0F, 0.0873F)
      );
      boneData.addChild(
         "rabbitLarm",
         ModelPartBuilder.create().uv(0, 0).cuboid(0.0F, 0.0F, -2.0F, 2.0F, 8.0F, 4.0F),
         ModelTransform.of(5.0F, -13.0F, -1.0F, 0.0F, 0.0F, -0.0873F)
      );
      boneData.addChild(
         "rabbitHead",
         ModelPartBuilder.create()
            .uv(0, 0)
            .cuboid(-3.0F, 0.0F, -4.0F, 6.0F, 1.0F, 6.0F)
            .uv(56, 0)
            .cuboid(-5.0F, -9.0F, -5.0F, 2.0F, 3.0F, 2.0F)
            .uv(56, 0)
            .cuboid(3.0F, -9.0F, -5.0F, 2.0F, 3.0F, 2.0F)
            .uv(0, 45)
            .cuboid(-4.0F, -11.0F, -4.0F, 8.0F, 11.0F, 8.0F)
            .uv(46, 0)
            .cuboid(1.0F, -20.0F, 0.0F, 3.0F, 9.0F, 1.0F)
            .uv(46, 0)
            .cuboid(-4.0F, -20.0F, 0.0F, 3.0F, 9.0F, 1.0F),
         ModelTransform.pivot(0.0F, -14.0F, -1.0F)
      );
      this.root = rootData.createPart(64, 64);
      ModelPart bone = this.root.getChild("bone");
      this.rabbitHead = bone.getChild("rabbitHead");
      this.rabbitRarm = bone.getChild("rabbitRarm");
      this.rabbitLarm = bone.getChild("rabbitLarm");
      this.rabbitRleg = bone.getChild("rabbitRleg");
      this.rabbitLleg = bone.getChild("rabbitLleg");
   }

   @Override
   public void setAngles(PlayerEntityRenderState state, PlayerEntityModel playerModel) {
      copyRotation(this.rabbitHead, playerModel.head);
      copyRotation(this.rabbitLarm, playerModel.leftArm);
      copyRotation(this.rabbitRarm, playerModel.rightArm);
      copyRotation(this.rabbitRleg, playerModel.rightLeg);
      copyRotation(this.rabbitLleg, playerModel.leftLeg);
   }
}
