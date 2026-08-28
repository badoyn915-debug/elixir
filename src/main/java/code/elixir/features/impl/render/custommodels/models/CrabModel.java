package code.elixir.features.impl.render.custommodels.models;

import code.elixir.features.impl.render.custommodels.CustomModel;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;

public class CrabModel extends CustomModel {
   public CrabModel() {
      ModelData modelData = new ModelData();
      ModelPartData rootData = modelData.getRoot();
      ModelPartData boneData = rootData.addChild(
         "bone",
         ModelPartBuilder.create()
            .uv(0, 0)
            .cuboid(-8.0F, -12.0F, -6.0F, 16.0F, 7.0F, 12.0F)
            .uv(44, 0)
            .cuboid(7.0F, -8.0F, -13.0F, 2.0F, 3.0F, 7.0F)
            .uv(40, 40)
            .cuboid(-9.0F, -8.0F, -13.0F, 2.0F, 3.0F, 7.0F)
            .uv(44, 27)
            .cuboid(-10.0F, -9.0F, -16.0F, 6.0F, 5.0F, 3.0F)
            .uv(44, 19)
            .cuboid(4.0F, -9.0F, -16.0F, 6.0F, 5.0F, 3.0F)
            .uv(32, 55)
            .cuboid(2.0F, -5.0F, -16.0F, 2.0F, 1.0F, 3.0F)
            .uv(31, 19)
            .cuboid(-4.0F, -5.0F, -16.0F, 2.0F, 1.0F, 3.0F)
            .uv(0, 41)
            .cuboid(-4.0F, -9.0F, -16.0F, 3.0F, 2.0F, 3.0F)
            .uv(20, 39)
            .cuboid(1.0F, -9.0F, -16.0F, 3.0F, 2.0F, 3.0F)
            .uv(0, 35)
            .cuboid(-6.0F, -8.0F, -9.0F, 3.0F, 3.0F, 3.0F)
            .uv(22, 22)
            .cuboid(3.0F, -8.0F, -9.0F, 3.0F, 3.0F, 3.0F),
         ModelTransform.pivot(0.0F, 24.0F, 0.0F)
      );
      boneData.addChild(
         "cube_r1",
         ModelPartBuilder.create()
            .uv(51, 39)
            .cuboid(-11.5F, -1.8F, 4.0F, 2.0F, 6.0F, 2.0F)
            .uv(8, 51)
            .cuboid(-11.5F, -1.8F, 0.0F, 2.0F, 6.0F, 2.0F)
            .uv(24, 55)
            .cuboid(-11.5F, -1.8F, -6.0F, 2.0F, 6.0F, 2.0F)
            .uv(0, 35)
            .cuboid(-12.5F, -5.8F, -6.0F, 4.0F, 4.0F, 12.0F)
            .uv(22, 23)
            .cuboid(3.3F, -13.6F, -6.0F, 5.0F, 4.0F, 12.0F),
         ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.3491F)
      );
      boneData.addChild(
         "cube_r2",
         ModelPartBuilder.create()
            .uv(52, 52)
            .cuboid(9.5F, -1.8F, 4.0F, 2.0F, 6.0F, 2.0F)
            .uv(16, 55)
            .cuboid(9.5F, -1.8F, 0.0F, 2.0F, 6.0F, 2.0F)
            .uv(0, 51)
            .cuboid(9.5F, -1.8F, -6.0F, 2.0F, 6.0F, 2.0F)
            .uv(20, 39)
            .cuboid(8.5F, -5.8F, -6.0F, 4.0F, 4.0F, 12.0F)
            .uv(0, 19)
            .cuboid(-8.3F, -13.6F, -6.0F, 5.0F, 4.0F, 12.0F),
         ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3491F)
      );
      boneData.addChild(
         "cube_r3", ModelPartBuilder.create().uv(0, 0).cuboid(1.3F, -18.0F, -6.7F, 3.0F, 6.0F, 3.0F), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1309F)
      );
      boneData.addChild(
         "cube_r4",
         ModelPartBuilder.create().uv(0, 19).cuboid(-4.3F, -18.0F, -6.7F, 3.0F, 6.0F, 3.0F),
         ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1309F)
      );
      this.root = rootData.createPart(64, 64);
   }

   @Override
   public void setAngles(PlayerEntityRenderState state, PlayerEntityModel playerModel) {
   }
}
