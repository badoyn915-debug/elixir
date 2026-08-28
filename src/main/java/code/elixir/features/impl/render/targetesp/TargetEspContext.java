package code.elixir.features.impl.render.targetesp;

import code.elixir.features.impl.render.TargetESP;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

public class TargetEspContext {
   public final MatrixStack matrices;
   public final Camera camera;
   public final float tickDelta;
   public final LivingEntity target;
   public final Vec3d targetPos;
   public final float anim;
   public final float red;
   public final int primaryColor;
   public final int secondaryColor;
   public final long timeMs;
   public final TargetESP esp;

   public TargetEspContext(
      MatrixStack matrices,
      Camera camera,
      float tickDelta,
      LivingEntity target,
      Vec3d targetPos,
      float anim,
      float red,
      int primaryColor,
      int secondaryColor,
      long timeMs,
      TargetESP esp
   ) {
      this.matrices = matrices;
      this.camera = camera;
      this.tickDelta = tickDelta;
      this.target = target;
      this.targetPos = targetPos;
      this.anim = anim;
      this.red = red;
      this.primaryColor = primaryColor;
      this.secondaryColor = secondaryColor;
      this.timeMs = timeMs;
      this.esp = esp;
   }

   public float getRotationDirMultiplier() {
      return this.esp.getRotationDir().get().equals("Против часовой") ? -1.0F : 1.0F;
   }

   public float getEffectiveSpeed() {
      return this.esp.getAnimSpeed().getValue();
   }

   public float getEffectiveSize() {
      return this.esp.getAnimSize().getValue();
   }

   public float getEffectiveScale() {
      return this.esp.getAnimScale().getValue();
   }

   public float getPulseFactor() {
      if (!this.esp.getPulse().isValue()) {
         return 1.0F;
      }
      double speed = this.timeMs * 0.003 * this.esp.getPulseSpeed().getValue() * this.getEffectiveSpeed();
      float strength = this.esp.getPulseStrength().getValue();
      return (float)(1.0 + Math.sin(speed) * strength);
   }
}
