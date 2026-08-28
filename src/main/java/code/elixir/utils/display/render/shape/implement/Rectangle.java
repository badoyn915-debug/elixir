package code.elixir.utils.display.render.shape.implement;

import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.draw.DrawEngine;
import code.elixir.utils.display.render.draw.DrawEngineImpl;
import code.elixir.utils.display.render.geometry.BufferDraw;
import code.elixir.utils.display.render.shape.Shape;
import code.elixir.utils.display.render.shape.ShapeProperties;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

public class Rectangle implements Shape, QuickImports {
   private static final ShaderProgramKey SHADER_KEY = new ShaderProgramKey(Identifier.of("minecraft", "core/round"), VertexFormats.POSITION, Defines.EMPTY);
   private static final DrawEngine BATCH_DRAW_ENGINE = new DrawEngineImpl();
   private static final ThreadLocal<Boolean> BATCHING = ThreadLocal.withInitial(() -> Boolean.FALSE);
   private static final ArrayList<Rectangle.BatchEntry> BATCH = new ArrayList<>(64);
   private static float[] rectX = new float[64];
   private static float[] rectY = new float[64];
   private static float[] rectW = new float[64];
   private static float[] rectH = new float[64];
   private static float[] rectR = new float[64];
   private static int curCount;
   private static int lastCount;

   public static void beginBatch() {
      BATCHING.set(Boolean.TRUE);
      BATCH.clear();
      curCount = 0;
   }

   private static void recordRect(ShapeProperties shape) {
      if (curCount >= rectX.length) {
         int n = rectX.length * 2;
         rectX = Arrays.copyOf(rectX, n);
         rectY = Arrays.copyOf(rectY, n);
         rectW = Arrays.copyOf(rectW, n);
         rectH = Arrays.copyOf(rectH, n);
         rectR = Arrays.copyOf(rectR, n);
      }

      rectX[curCount] = shape.getX();
      rectY[curCount] = shape.getY();
      rectW[curCount] = shape.getWidth();
      rectH[curCount] = shape.getHeight();
      rectR[curCount] = shape.getRound().x;
      curCount++;
   }

   public static int panelCount() {
      return lastCount;
   }

   public static float panelX(int i) {
      return rectX[i];
   }

   public static float panelY(int i) {
      return rectY[i];
   }

   public static float panelWidth(int i) {
      return rectW[i];
   }

   public static float panelHeight(int i) {
      return rectH[i];
   }

   public static float panelRound(int i) {
      return rectR[i];
   }

   public static void flushPending() {
      if (!BATCH.isEmpty()) {
         boolean depthTest = GL11.glIsEnabled(2929);
         boolean cull = GL11.glIsEnabled(2884);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.enableDepthTest();
         RenderSystem.enableCull();
         float alpha = RenderSystem.getShaderColor()[3];
         ShaderProgram shader = RenderSystem.setShader(SHADER_KEY);
         Tessellator tessellator = Tessellator.getInstance();
         int count = BATCH.size();

         for (int i = 0; i < count; i++) {
            drawEntry(BATCH.get(i), shader, tessellator, alpha);
         }

         RenderSystem.disableBlend();
         if (!depthTest) {
            RenderSystem.disableDepthTest();
         }

         if (!cull) {
            RenderSystem.disableCull();
         }

         BATCH.clear();
      }
   }

   public static void endBatch() {
      flushPending();
      BATCHING.set(Boolean.FALSE);
      lastCount = curCount;
   }

   public static void flushBatch() {
      endBatch();
   }

   public static boolean isBatching() {
      return BATCHING.get();
   }

   @Override
   public void render(ShapeProperties shape) {
      if (BATCHING.get()) {
         recordRect(shape);
         BATCH.add(Rectangle.BatchEntry.from(shape));
      } else {
         this.renderImmediate(shape);
      }
   }

   private void renderImmediate(ShapeProperties shape) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableDepthTest();
      RenderSystem.enableCull();
      float alpha = RenderSystem.getShaderColor()[3];
      ShaderProgram shader = RenderSystem.setShader(SHADER_KEY);
      drawEntry(Rectangle.BatchEntry.from(shape), shader, Tessellator.getInstance(), alpha);
      RenderSystem.disableBlend();
   }

   private static void drawEntry(Rectangle.BatchEntry entry, ShaderProgram shader, Tessellator tessellator, float alpha) {
      BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION);
      float softness = entry.softness;
      BATCH_DRAW_ENGINE.quad(entry.matrix, buffer, entry.x - softness / 2.0F, entry.y - softness / 2.0F, entry.width + softness, entry.height + softness);
      shader.getUniformOrDefault("size").set(entry.scaledWidth, entry.scaledHeight);
      shader.getUniformOrDefault("location").set(entry.locationX, entry.locationY);
      shader.getUniformOrDefault("radius").set(entry.round);
      shader.getUniformOrDefault("softness").set(softness);
      shader.getUniformOrDefault("thickness").set(entry.thickness);
      shader.getUniformOrDefault("color1")
         .set(
            ColorAssist.redf(entry.color1),
            ColorAssist.greenf(entry.color1),
            ColorAssist.bluef(entry.color1),
            ColorAssist.alphaf(ColorAssist.multAlpha(entry.color1, alpha))
         );
      shader.getUniformOrDefault("color2")
         .set(
            ColorAssist.redf(entry.color2),
            ColorAssist.greenf(entry.color2),
            ColorAssist.bluef(entry.color2),
            ColorAssist.alphaf(ColorAssist.multAlpha(entry.color2, alpha))
         );
      shader.getUniformOrDefault("color3")
         .set(
            ColorAssist.redf(entry.color3),
            ColorAssist.greenf(entry.color3),
            ColorAssist.bluef(entry.color3),
            ColorAssist.alphaf(ColorAssist.multAlpha(entry.color3, alpha))
         );
      shader.getUniformOrDefault("color4")
         .set(
            ColorAssist.redf(entry.color4),
            ColorAssist.greenf(entry.color4),
            ColorAssist.bluef(entry.color4),
            ColorAssist.alphaf(ColorAssist.multAlpha(entry.color4, alpha))
         );
      shader.getUniformOrDefault("outlineColor")
         .set(
            ColorAssist.redf(entry.outlineColor),
            ColorAssist.greenf(entry.outlineColor),
            ColorAssist.bluef(entry.outlineColor),
            ColorAssist.alphaf(ColorAssist.multAlpha(entry.outlineColor, alpha))
         );
      BufferDraw.draw(buffer);
   }

   private record BatchEntry(
      Matrix4f matrix,
      float x,
      float y,
      float width,
      float height,
      float softness,
      float thickness,
      float scaledWidth,
      float scaledHeight,
      float locationX,
      float locationY,
      Vector4f round,
      int color1,
      int color2,
      int color3,
      int color4,
      int outlineColor
   ) {
      private static Rectangle.BatchEntry from(ShapeProperties shape) {
         MinecraftClient mc = MinecraftClient.getInstance();
         float scale = (float)mc.getWindow().getScaleFactor();
         Matrix4f matrix4f = new Matrix4f(shape.getMatrix().peek().getPositionMatrix());
         Vector3f pos = matrix4f.transformPosition(shape.getX(), shape.getY(), 0.0F, new Vector3f()).mul(scale);
         Vector3f size = matrix4f.getScale(new Vector3f()).mul(scale);
         Vector4f round = new Vector4f(shape.getRound()).mul(size.y);
         float scaledWidth = shape.getWidth() * size.x;
         float scaledHeight = shape.getHeight() * size.y;
         return new Rectangle.BatchEntry(
            matrix4f,
            shape.getX(),
            shape.getY(),
            shape.getWidth(),
            shape.getHeight(),
            shape.getSoftness(),
            shape.getThickness(),
            scaledWidth,
            scaledHeight,
            pos.x,
            mc.getWindow().getHeight() - scaledHeight - pos.y,
            round,
            shape.getColor().x,
            shape.getColor().y,
            shape.getColor().z,
            shape.getColor().w,
            shape.getOutlineColor()
         );
      }
   }
}
