package code.elixir.features.impl.render.targetesp.modes;

import code.elixir.features.impl.render.targetesp.TargetEspContext;
import code.elixir.features.impl.render.targetesp.TargetEspMode;
import code.elixir.features.impl.render.targetesp.TargetEspRenderUtil;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.geometry.BufferDraw;
import code.elixir.utils.display.render.geometry.Render3D;
import code.elixir.utils.math.calc.Calculate;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Objects;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

public class CircleTargetEsp implements TargetEspMode, QuickImports {
   private float circleStep = 0.0F;

   @Override
   public String getName() {
      return "Круг";
   }

   @Override
   public void reset() {
      this.circleStep = 0.0F;
   }

   @Override
   public void render(TargetEspContext ctx) {
      if (ctx.anim <= 0.005F || ctx.target == null) {
         return;
      }

      this.circleStep += 0.22F * ctx.getEffectiveSpeed() * ctx.esp.getWaveSpeed().getValue();

      double cs = Calculate.interpolate(this.circleStep - 0.17, (double)this.circleStep);
      Vec3d target = Calculate.interpolate(ctx.target);
      boolean canSee = mc.player != null && mc.player.canSee(ctx.target);

      GL11.glEnable(GL11.GL_LINE_SMOOTH);
      if (canSee) {
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(false);
      } else {
         RenderSystem.disableDepthTest();
      }

      RenderSystem.enableBlend();
      RenderSystem.disableCull();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_CONSTANT_ALPHA);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
      int size = 64;

      float sizeMultiplier = ctx.getEffectiveSize() * ctx.getPulseFactor();
      float width = ctx.target.getWidth() * 0.82F * sizeMultiplier;
      float height = ctx.target.getHeight();

      int baseColor = TargetEspRenderUtil.getBlendedColor(ctx, 0.0F);
      int color = ColorAssist.multRed(baseColor, 1.0F + ctx.red * 10.0F);

      for (int i = 0; i <= size; i++) {
         double yAnim = Calculate.absSinAnimation(cs) * height;
         double yAnim2 = Calculate.absSinAnimation(cs - 0.45) * height;
         Vec3d cosSin = Calculate.cosSin(i, size, width);
         Vec3d nextCosSin = Calculate.cosSin(i + 1, size, width);

         Vec3d start = target.add(cosSin.x, cosSin.y + yAnim, cosSin.z);
         Vec3d end = target.add(cosSin.x, cosSin.y + yAnim2, cosSin.z);

         Render3D.vertexLine(ctx.matrices, buffer, start, end, ColorAssist.multAlpha(color, 0.76F * ctx.anim), ColorAssist.multAlpha(color, 0.0F));
         Render3D.drawLine(
            target.add(cosSin.x, cosSin.y + yAnim, cosSin.z),
            target.add(nextCosSin.x, nextCosSin.y + yAnim, nextCosSin.z),
            ColorAssist.multAlpha(color, ctx.anim),
            2.0F,
            canSee
         );
      }

      BufferDraw.draw(buffer);
      if (canSee) {
         RenderSystem.depthMask(true);
         RenderSystem.disableDepthTest();
      } else {
         RenderSystem.enableDepthTest();
      }

      GL11.glDisable(GL11.GL_LINE_SMOOTH);
   }
}
