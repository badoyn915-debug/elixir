package code.elixir.features.impl.render.custommodels.models;

import code.elixir.features.impl.render.custommodels.CustomModel;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;

public class JeffKillerModel extends CustomModel {
   private final ModelPart head;
   private final ModelPart leftArm;
   private final ModelPart rightLeg;
   private final ModelPart leftLeg;

   public JeffKillerModel() {
      ModelData modelData = new ModelData();
      ModelPartData rootData = modelData.getRoot();
      rootData.addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-5.0F, -9.75F, -5.0F, 10.0F, 10.0F, 8.0F), ModelTransform.pivot(0.0F, 1.0F, -3.0F));
      rootData.addChild(
         "body",
         ModelPartBuilder.create().uv(0, 18).cuboid(-4.0F, -23.1486F, 0.5266F, 8.0F, 14.0F, 4.0F),
         ModelTransform.of(0.0F, 24.0F, 0.0F, 0.2618F, 0.0F, 0.0F)
      );
      rootData.addChild(
         "rightArm",
         ModelPartBuilder.create()
            .uv(36, 0)
            .cuboid(-7.0F, -4.5F, -23.25F, 3.0F, 12.0F, 3.0F)
            .uv(16, 36)
            .cuboid(-6.0F, 5.75F, -25.25F, 1.0F, 2.0F, 5.0F)
            .uv(31, 15)
            .cuboid(-6.0F, 5.75F, -30.25F, 1.0F, 2.0F, 5.0F)
            .uv(0, 0)
            .cuboid(-6.0F, 8.75F, -28.25F, 1.0F, 1.0F, 3.0F)
            .uv(24, 18)
            .cuboid(-6.0F, 7.75F, -29.25F, 1.0F, 1.0F, 5.0F),
         ModelTransform.of(0.0F, 24.0F, 0.0F, -1.309F, 0.0F, 0.0F)
      );
      rootData.addChild("leftArm", ModelPartBuilder.create().uv(37, 37).cuboid(0.0F, -1.75F, -1.5F, 3.0F, 12.0F, 3.0F), ModelTransform.pivot(4.0F, 3.0F, -3.0F));
      rootData.addChild("rightLeg", ModelPartBuilder.create().uv(0, 36).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F), ModelTransform.pivot(-2.0F, 14.0F, 0.0F));
      rootData.addChild("leftLeg", ModelPartBuilder.create().uv(24, 24).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F), ModelTransform.pivot(2.0F, 14.0F, 0.0F));
      this.root = rootData.createPart(64, 64);
      this.head = this.root.getChild("head");
      this.leftArm = this.root.getChild("leftArm");
      this.rightLeg = this.root.getChild("rightLeg");
      this.leftLeg = this.root.getChild("leftLeg");
   }

   @Override
   public void setAngles(PlayerEntityRenderState state, PlayerEntityModel playerModel) {
      copyRotation(this.head, playerModel.head);
      copyRotation(this.leftArm, playerModel.leftArm);
      copyRotation(this.rightLeg, playerModel.rightLeg);
      copyRotation(this.leftLeg, playerModel.leftLeg);
   }
}
