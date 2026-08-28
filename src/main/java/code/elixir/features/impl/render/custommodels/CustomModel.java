package code.elixir.features.impl.render.custommodels;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

public abstract class CustomModel {
   protected ModelPart root;

   public abstract void setAngles(PlayerEntityRenderState var1, PlayerEntityModel var2);

   public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
      this.root.render(matrices, vertices, light, overlay);
   }

   protected static void copyRotation(ModelPart target, ModelPart source) {
      copyRotation(target, source, 0.0F);
   }

   protected static void copyRotation(ModelPart target, ModelPart source, float rollOffset) {
      target.pitch = source.pitch;
      target.yaw = source.yaw;
      target.roll = source.roll + rollOffset;
   }
}
