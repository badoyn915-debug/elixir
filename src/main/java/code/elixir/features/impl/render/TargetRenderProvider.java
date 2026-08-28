package code.elixir.features.impl.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

public interface TargetRenderProvider {
   void render(MatrixStack var1, LivingEntity var2, Vec3d var3, float var4);
}
