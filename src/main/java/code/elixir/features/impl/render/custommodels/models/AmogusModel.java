package code.elixir.features.impl.render.custommodels.models;

import code.elixir.features.impl.render.custommodels.CustomModel;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;

public class AmogusModel extends CustomModel {
   private final ModelPart leftLeg;
   private final ModelPart rightLeg;

   public AmogusModel() {
      ModelData modelData = new ModelData();
      ModelPartData rootData = modelData.getRoot();
      rootData.addChild(
         "body",
         ModelPartBuilder.create()
            .uv(34, 8)
            .cuboid(-4.0F, 6.0F, -3.0F, 8.0F, 12.0F, 6.0F)
            .uv(15, 10)
            .cuboid(-3.0F, 9.0F, 3.0F, 6.0F, 8.0F, 3.0F)
            .uv(26, 0)
            .cuboid(-3.0F, 5.0F, -3.0F, 6.0F, 1.0F, 6.0F),
         ModelTransform.pivot(0.0F, 0.0F, 0.0F)
      );
      rootData.addChild("eye", ModelPartBuilder.create().uv(0, 10).cuboid(-3.0F, 7.0F, -4.0F, 6.0F, 4.0F, 1.0F), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
      rootData.addChild("leftLeg", ModelPartBuilder.create().uv(0, 0).cuboid(2.9F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F), ModelTransform.pivot(-2.0F, 18.0F, 0.0F));
      rootData.addChild("rightLeg", ModelPartBuilder.create().uv(13, 0).cuboid(-5.9F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F), ModelTransform.pivot(2.0F, 18.0F, 0.0F));
      this.root = rootData.createPart(64, 64);
      this.leftLeg = this.root.getChild("leftLeg");
      this.rightLeg = this.root.getChild("rightLeg");
   }

   @Override
   public void setAngles(PlayerEntityRenderState state, PlayerEntityModel playerModel) {
      copyRotation(this.leftLeg, playerModel.leftLeg);
      copyRotation(this.rightLeg, playerModel.rightLeg);
   }
}
