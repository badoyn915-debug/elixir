package code.elixir.features.impl.render.targetesp.modes;

import code.elixir.features.impl.render.targetesp.TargetEspContext;
import code.elixir.features.impl.render.targetesp.TargetEspMode;
import code.elixir.features.impl.render.targetesp.TargetEspRenderUtil;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.geometry.Render3D;
import code.elixir.utils.math.calc.Calculate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4i;

public class CubeTargetEsp implements TargetEspMode {
   private static final Identifier CAPTURE_TEXTURE = Identifier.of("textures/capture2.png");
   private float espValue = 1.0F;
   private float espSpeed = 1.0F;
   private float prevEspValue;
   private boolean flipSpeed;

   @Override
   public String getName() {
      return "Квадрат";
   }

   @Override
   public void reset() {
      this.espValue = 1.0F;
      this.espSpeed = 1.0F;
   }

   @Override
   public void render(TargetEspContext ctx) {
      if (ctx.anim <= 0.005F || ctx.target == null) {
         return;
      }

      this.updateRotation(ctx);

      float size = (ctx.red - ctx.anim - 0.17F) * ctx.getEffectiveSize() * ctx.getPulseFactor();
      Vec3d vec = Calculate.interpolate(ctx.target).subtract(ctx.camera.getPos());

      MatrixStack matrix = new MatrixStack();
      matrix.push();
      matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(ctx.camera.getPitch()));
      matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(ctx.camera.getYaw() + 180.0F));
      matrix.translate(vec.x, vec.y + ctx.target.getBoundingBox().getLengthY() / 2.0, vec.z);
      matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-ctx.camera.getYaw()));
      matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(ctx.camera.getPitch()));
      matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(Calculate.interpolate(this.prevEspValue, this.espValue)));

      Entry entry = matrix.peek().copy();
      int baseColor = TargetEspRenderUtil.getBlendedColor(ctx, 0.0F);
      int finalColor = ColorAssist.setAlpha(baseColor, (int)(ColorAssist.getAlpha(baseColor) * ctx.anim));

      if (ctx.esp.getGlow().isValue()) {
         float glowSize = size * 1.7F * ctx.esp.getGlowSize().getValue();
         int glowColor = ColorAssist.setAlpha(baseColor, (int)(ColorAssist.getAlpha(baseColor) * ctx.anim * 0.45F));
         Render3D.drawTexture(
            entry,
            TargetEspRenderUtil.BLOOM_TEXTURE,
            -glowSize / 2.0F,
            -glowSize / 2.0F,
            glowSize,
            glowSize,
            new Vector4i(glowColor, glowColor, glowColor, glowColor),
            false
         );
      }

      Render3D.drawTexture(
         entry,
         CAPTURE_TEXTURE,
         -size / 2.0F,
         -size / 2.0F,
         size,
         size,
         new Vector4i(finalColor, finalColor, finalColor, finalColor),
         false
      );

      matrix.pop();
   }

   private void updateRotation(TargetEspContext ctx) {
      this.prevEspValue = this.espValue;
      float speedFactor = ctx.getEffectiveSpeed() * ctx.esp.getRotationSpeed().getValue() * ctx.getRotationDirMultiplier();
      this.espValue = this.espValue + this.espSpeed * speedFactor;
      if (this.espSpeed > 25.0F) {
         this.flipSpeed = true;
      }
      if (this.espSpeed < -25.0F) {
         this.flipSpeed = false;
      }
      this.espSpeed = this.flipSpeed ? this.espSpeed - 0.5F : this.espSpeed + 0.5F;
   }
}
