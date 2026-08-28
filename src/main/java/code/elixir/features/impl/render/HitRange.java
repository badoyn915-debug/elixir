package code.elixir.features.impl.render;

import code.elixir.events.render.WorldRenderEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.geometry.BufferDraw;
import code.elixir.utils.math.calc.CalcVector;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class HitRange extends Module {
   static final int SIDES = 8;
   static final float Y_OFFSET = 0.02F;
   private final ColorSetting color = new ColorSetting("Цвет", "Цвет").setColor(new Color(80, 180, 255, 200).getRGB());
   private final SliderSettings thickness = new SliderSettings("Толщина", "Толщина").setValue(0.06F).range(0.01F, 0.5F);
   private final SliderSettings opacity = new SliderSettings("Прозрачность", "Прозрачность кольца").setValue(0.85F).range(0.1F, 1.0F).step(0.05F);
   private final SliderSettings radius = new SliderSettings("Радиус", "Радиус кольца в блоках").setValue(3.32F).range(1.0F, 6.0F).step(0.1F);
   private final SliderSettings rotSpeed = new SliderSettings("Скорость вращения", "Скорость вращения").setValue(0.4F).range(0.05F, 2.0F);
   private final BooleanSetting depthTest = new BooleanSetting("Скрывать за блоками", "Скрывать за блоками").setValue(true);
   private float rotationAngle = 0.0F;

   public HitRange() {
      super("HitRange", "Hit Range", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.color, this.thickness, this.opacity, this.radius, this.rotSpeed, this.depthTest});
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      if (mc.player != null && mc.world != null) {
         float hitRadius = this.radius.getValue();
         float hitRadiusSq = hitRadius * hitRadius;
         boolean enemyNear = false;

         for (PlayerEntity p : mc.world.getPlayers()) {
            if (p != mc.player && p.squaredDistanceTo(mc.player) <= hitRadiusSq) {
               enemyNear = true;
               break;
            }
         }

         this.rotationAngle = (this.rotationAngle + this.rotSpeed.getValue()) % 360.0F;
         Vec3d origin = CalcVector.lerpPosition(mc.player);
         double y = origin.y + 0.02F;
         float alphaMul = this.opacity.getValue();
         int ringColor = enemyNear
            ? ColorAssist.multAlpha(new Color(255, 45, 45, 220).getRGB(), alphaMul)
            : ColorAssist.multAlpha(this.color.getColor(), alphaMul);
         int glowColor = ColorAssist.multAlpha(ringColor, 0.25F);
         double rotRad = Math.toRadians(this.rotationAngle);
         float half = this.thickness.getValue() / 2.0F;
         boolean depth = this.depthTest.isValue();
         MatrixStack matrix = e.getStack();
         Matrix4f matrix4f = matrix.peek().getPositionMatrix();
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

         for (int i = 0; i < 8; i++) {
            double a1 = rotRad + (Math.PI * 2) * i / 8.0;
            double a2 = rotRad + (Math.PI * 2) * (i + 1) / 8.0;
            double outerR = hitRadius + half;
            double innerR = hitRadius - half;
            appendRingQuad(buffer, matrix4f, origin.x, y, origin.z, outerR, innerR, a1, a2, ringColor);
            double gOuter = hitRadius + half * 3.0F;
            double gInner = hitRadius - half * 3.0F;
            appendRingQuad(buffer, matrix4f, origin.x, y, origin.z, gOuter, gInner, a1, a2, glowColor);
         }

         RenderSystem.enableBlend();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
         RenderSystem.disableCull();
         if (depth) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
         } else {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
         }

         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         BufferDraw.draw(buffer);
         RenderSystem.depthMask(true);
         if (!depth) {
            RenderSystem.enableDepthTest();
         }

         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   }

   private static void appendRingQuad(
      BufferBuilder buffer, Matrix4f matrix, double cx, double y, double cz, double outerR, double innerR, double a1, double a2, int color
   ) {
      float ox1 = (float)(cx + outerR * Math.cos(a1));
      float oz1 = (float)(cz + outerR * Math.sin(a1));
      float ox2 = (float)(cx + outerR * Math.cos(a2));
      float oz2 = (float)(cz + outerR * Math.sin(a2));
      float ix1 = (float)(cx + innerR * Math.cos(a1));
      float iz1 = (float)(cz + innerR * Math.sin(a1));
      float ix2 = (float)(cx + innerR * Math.cos(a2));
      float iz2 = (float)(cz + innerR * Math.sin(a2));
      float fy = (float)y;
      buffer.vertex(matrix, ox1, fy, oz1).color(color);
      buffer.vertex(matrix, ox2, fy, oz2).color(color);
      buffer.vertex(matrix, ix2, fy, iz2).color(color);
      buffer.vertex(matrix, ix1, fy, iz1).color(color);
   }
}
