package code.elixir.utils.display.render.geometry;

import code.elixir.events.render.WorldRenderEvent;
import code.elixir.features.impl.render.TargetESP;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.math.projection.Projection;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4i;
import org.lwjgl.opengl.GL11;

public final class Render3D implements QuickImports {
   private static final Map<VoxelShape, Pair<List<Box>, List<Render3D.Line>>> SHAPE_OUTLINES = new HashMap<>();
   private static final Map<VoxelShape, List<Box>> SHAPE_BOXES = new HashMap<>();
   public static final List<Render3D.Texture> TEXTURE_DEPTH = new ArrayList<>();
   public static final List<Render3D.Texture> TEXTURE = new ArrayList<>();
   public static final List<Render3D.Line> LINE_DEPTH = new ArrayList<>();
   public static final List<Render3D.Line> LINE = new ArrayList<>();
   public static final List<Render3D.Quad> QUAD_DEPTH = new ArrayList<>();
   public static final List<Render3D.Quad> QUAD = new ArrayList<>();
   public static Matrix4f lastProjMat = new Matrix4f();
   public static Entry lastWorldSpaceMatrix = new MatrixStack().peek();
   private static final Identifier captureId = Identifier.of("textures/capture1.png");
   private static final Identifier bloom = Identifier.of("textures/bloom.png");
   private static final Identifier snow = Identifier.of("textures/show1.png");
   public static final List<Render3D.Crystal> crystalList = new ArrayList<>();
   private static float espValue = 1.0F;
   private static float espSpeed = 1.0F;
   private static float prevEspValue;
   private static float circleStep;
   private static boolean flipSpeed;

   public static void drawEntity(Entity entity, Vec3d pos, float yaw, int alpha, MatrixStack matrices, float tickDelta) {
      if (entity instanceof LivingEntity livingEntity) {
         matrices.push();
         matrices.translate(pos.x, pos.y, pos.z);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
         matrices.scale(1.0F, 1.0F, 1.0F);
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
         RenderSystem.enableDepthTest();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha / 255.0F);
         EntityRenderer renderer = mc.getEntityRenderDispatcher().getRenderer(entity);
         if (renderer != null) {
            int light = renderer.getLight(livingEntity, tickDelta);
            Immediate vertexConsumers = mc.getBufferBuilders().getEntityVertexConsumers();
            EntityRenderState renderState = renderer.getAndUpdateRenderState(livingEntity, tickDelta);
            if (renderState != null) {
               renderer.render(renderState, matrices, vertexConsumers, light);
            }

            vertexConsumers.draw();
         }

         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.disableBlend();
         matrices.pop();
      }
   }

   public static void onWorldRender(WorldRenderEvent e) {
      if (!TEXTURE.isEmpty()) {
         RenderSystem.enableBlend();
         RenderSystem.disableDepthTest();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_CONSTANT_ALPHA);
         int size = TEXTURE.size();

         for (int i = 0; i < size; i++) {
            Render3D.Texture tex = TEXTURE.get(i);
            if (tex != null) {
               Identifier id = tex.id;
               RenderSystem.setShaderTexture(0, id);
               RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
               BufferBuilder textureBuffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

               for (int j = i; j < size; j++) {
                  Render3D.Texture other = TEXTURE.get(j);
                  if (other != null && Objects.equals(other.id, id)) {
                     quadTexture(other.entry, textureBuffer, other.x, other.y, other.width, other.height, other.color);
                     if (j > i) {
                        TEXTURE.set(j, null);
                     }
                  }
               }

               BufferDraw.draw(textureBuffer);
            }
         }

         RenderSystem.enableDepthTest();
         RenderSystem.disableBlend();
         TEXTURE.clear();
      }

      if (!TEXTURE_DEPTH.isEmpty()) {
         RenderSystem.enableBlend();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_CONSTANT_ALPHA);
         int size = TEXTURE_DEPTH.size();

         for (int ix = 0; ix < size; ix++) {
            Render3D.Texture tex = TEXTURE_DEPTH.get(ix);
            if (tex != null) {
               Identifier id = tex.id;
               RenderSystem.setShaderTexture(0, id);
               RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
               BufferBuilder textureBuffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

               for (int jx = ix; jx < size; jx++) {
                  Render3D.Texture other = TEXTURE_DEPTH.get(jx);
                  if (other != null && Objects.equals(other.id, id)) {
                     quadTexture(other.entry, textureBuffer, other.x, other.y, other.width, other.height, other.color);
                     if (jx > ix) {
                        TEXTURE_DEPTH.set(jx, null);
                     }
                  }
               }

               BufferDraw.draw(textureBuffer);
            }
         }

         RenderSystem.depthMask(true);
         RenderSystem.disableBlend();
         TEXTURE_DEPTH.clear();
      }

      if (!LINE.isEmpty()) {
         GL11.glEnable(2881);
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.disableDepthTest();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_CONSTANT_ALPHA);
         RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
         int size = LINE.size();

         for (int ixx = 0; ixx < size; ixx++) {
            Render3D.Line line = LINE.get(ixx);
            if (line != null) {
               float width = line.width;
               RenderSystem.lineWidth(width);
               BufferBuilder lineBuffer = Tessellator.getInstance().begin(DrawMode.LINES, VertexFormats.LINES);

               for (int jxx = ixx; jxx < size; jxx++) {
                  Render3D.Line other = LINE.get(jxx);
                  if (other != null && other.width == width) {
                     vertexLine(other.entry, lineBuffer, other.start.toVector3f(), other.end.toVector3f(), other.colorStart, other.colorEnd);
                     if (jxx > ixx) {
                        LINE.set(jxx, null);
                     }
                  }
               }

               BufferDraw.draw(lineBuffer);
            }
         }

         RenderSystem.enableDepthTest();
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
         LINE.clear();
         GL11.glDisable(2881);
      }

      if (!QUAD.isEmpty()) {
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.disableDepthTest();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_CONSTANT_ALPHA);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         BufferBuilder quadBuffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         int size = QUAD.size();

         for (int ixxx = 0; ixxx < size; ixxx++) {
            Render3D.Quad quad = QUAD.get(ixxx);
            vertexQuad(quad.entry, quadBuffer, quad.x, quad.y, quad.w, quad.z, quad.color);
         }

         BufferDraw.draw(quadBuffer);
         RenderSystem.enableDepthTest();
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
         QUAD.clear();
      }

      if (!LINE_DEPTH.isEmpty()) {
         GL11.glEnable(2881);
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_CONSTANT_ALPHA);
         RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
         int size = LINE_DEPTH.size();

         for (int ixxx = 0; ixxx < size; ixxx++) {
            Render3D.Line line = LINE_DEPTH.get(ixxx);
            if (line != null) {
               float width = line.width;
               RenderSystem.lineWidth(width);
               BufferBuilder lineBuffer = Tessellator.getInstance().begin(DrawMode.LINES, VertexFormats.LINES);

               for (int jxxx = ixxx; jxxx < size; jxxx++) {
                  Render3D.Line other = LINE_DEPTH.get(jxxx);
                  if (other != null && other.width == width) {
                     vertexLine(other.entry, lineBuffer, other.start.toVector3f(), other.end.toVector3f(), other.colorStart, other.colorEnd);
                     if (jxxx > ixxx) {
                        LINE_DEPTH.set(jxxx, null);
                     }
                  }
               }

               BufferDraw.draw(lineBuffer);
            }
         }

         RenderSystem.depthMask(true);
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
         LINE_DEPTH.clear();
         GL11.glDisable(2881);
      }

      if (!QUAD_DEPTH.isEmpty()) {
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_CONSTANT_ALPHA);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         BufferBuilder quadBuffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         int size = QUAD_DEPTH.size();

         for (int ixxxx = 0; ixxxx < size; ixxxx++) {
            Render3D.Quad quad = QUAD_DEPTH.get(ixxxx);
            vertexQuad(quad.entry, quadBuffer, quad.x, quad.y, quad.w, quad.z, quad.color);
         }

         BufferDraw.draw(quadBuffer);
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
         QUAD_DEPTH.clear();
      }
   }

   public static void drawShape(BlockPos blockPos, VoxelShape voxelShape, int color, float width) {
      drawShape(blockPos, voxelShape, color, width, true, false);
   }

   public static void drawShape(BlockPos blockPos, VoxelShape voxelShape, int color, float width, boolean fill, boolean depth) {
      if (!SHAPE_BOXES.containsKey(voxelShape)) {
         SHAPE_BOXES.put(voxelShape, voxelShape.getBoundingBoxes());
      }

      for (Box box : SHAPE_BOXES.get(voxelShape)) {
         Box offsetBox = box.offset(blockPos);
         if (Projection.canSee(offsetBox)) {
            drawBox(offsetBox, color, width, true, fill, depth);
         }
      }
   }

   public static void drawShapeAlternative(BlockPos blockPos, VoxelShape voxelShape, int color, float width, boolean fill, boolean depth) {
      drawShapeAlternative(blockPos, voxelShape, color, width, fill, true, depth);
   }

   public static void drawShapeAlternative(BlockPos blockPos, VoxelShape voxelShape, int color, float width, boolean fill, boolean lines, boolean depth) {
      if (Projection.canSee(new Box(blockPos))) {
         if (!SHAPE_OUTLINES.containsKey(voxelShape)) {
            ArrayList edgeLines = new ArrayList();
            voxelShape.forEachEdge(
               (minX, minY, minZ, maxX, maxY, maxZ) -> edgeLines.add(
                  new Render3D.Line(null, new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ), 0, 0, 0.0F)
               )
            );
            SHAPE_OUTLINES.put(voxelShape, new Pair(voxelShape.getBoundingBoxes(), edgeLines));
         }

         Vec3d vec3d = Vec3d.of(blockPos);
         Pair<List<Box>, List<Render3D.Line>> pair = SHAPE_OUTLINES.get(voxelShape);
         if (fill) {
            pair.getLeft().forEach(box -> drawBox(box.offset(vec3d), color, width, false, true, depth));
         }

         if (lines) {
            pair.getRight().forEach(line -> drawLine(line.start().add(vec3d), line.end().add(vec3d), color, width, depth));
         }
      }
   }

   public static void drawBox(Box box, int color, float width) {
      drawBox(box, color, width, true, true, false);
   }

   public static void drawBox(Box box, int color, float width, boolean line, boolean fill, boolean depth) {
      drawBox(null, box, color, width, line, fill, depth);
   }

   public static void drawBox(Entry entry, Box box, int color, float width, boolean line, boolean fill, boolean depth) {
      box = box.expand(0.001);
      double x1 = box.minX;
      double y1 = box.minY;
      double z1 = box.minZ;
      double x2 = box.maxX;
      double y2 = box.maxY;
      double z2 = box.maxZ;
      if (fill) {
         int fillColor = ColorAssist.multAlpha(color, 0.1F);
         drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), fillColor, depth);
         drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y1, z1), fillColor, depth);
         drawQuad(entry, new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), new Vec3d(x2, y1, z2), fillColor, depth);
         drawQuad(entry, new Vec3d(x1, y1, z2), new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), fillColor, depth);
         drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), fillColor, depth);
         drawQuad(entry, new Vec3d(x1, y2, z1), new Vec3d(x1, y2, z2), new Vec3d(x2, y2, z2), new Vec3d(x2, y2, z1), fillColor, depth);
      }

      if (line) {
         drawLine(entry, x1, y1, z1, x2, y1, z1, color, width, depth);
         drawLine(entry, x2, y1, z1, x2, y1, z2, color, width, depth);
         drawLine(entry, x2, y1, z2, x1, y1, z2, color, width, depth);
         drawLine(entry, x1, y1, z2, x1, y1, z1, color, width, depth);
         drawLine(entry, x1, y1, z2, x1, y2, z2, color, width, depth);
         drawLine(entry, x1, y1, z1, x1, y2, z1, color, width, depth);
         drawLine(entry, x2, y1, z2, x2, y2, z2, color, width, depth);
         drawLine(entry, x2, y1, z1, x2, y2, z1, color, width, depth);
         drawLine(entry, x1, y2, z1, x2, y2, z1, color, width, depth);
         drawLine(entry, x2, y2, z1, x2, y2, z2, color, width, depth);
         drawLine(entry, x2, y2, z2, x1, y2, z2, color, width, depth);
         drawLine(entry, x1, y2, z2, x1, y2, z1, color, width, depth);
      }
   }

   public static void vertexLine(MatrixStack matrices, VertexConsumer buffer, Vec3d start, Vec3d end, int startColor, int endColor) {
      vertexLine(matrices.peek(), buffer, start.toVector3f(), end.toVector3f(), startColor, endColor);
   }

   public static void vertexLine(Entry entry, VertexConsumer buffer, Vector3f start, Vector3f end, int startColor, int endColor) {
      if (entry == null) {
         entry = lastWorldSpaceMatrix;
      }

      Vector3f vec = getNormal(start, end);
      buffer.vertex(entry, start).color(startColor).normal(entry, vec);
      buffer.vertex(entry, end).color(endColor).normal(entry, vec);
   }

   public static void vertexQuad(Entry entry, VertexConsumer buffer, Vec3d vec1, Vec3d vec2, Vec3d vec3, Vec3d vec4, int color) {
      vertexQuad(entry, buffer, vec1.toVector3f(), vec2.toVector3f(), vec3.toVector3f(), vec4.toVector3f(), color);
   }

   public static void vertexQuad(Entry entry, VertexConsumer buffer, Vector3f vec1, Vector3f vec2, Vector3f vec3, Vector3f vec4, int color) {
      if (entry == null) {
         entry = lastWorldSpaceMatrix;
      }

      buffer.vertex(entry, vec1).color(color);
      buffer.vertex(entry, vec2).color(color);
      buffer.vertex(entry, vec3).color(color);
      buffer.vertex(entry, vec4).color(color);
   }

   public static void quadTexture(Entry entry, BufferBuilder buffer, float x, float y, float width, float height, Vector4i color) {
      buffer.vertex(entry, x, y + height, 0.0F).texture(0.0F, 0.0F).color(color.x);
      buffer.vertex(entry, x + width, y + height, 0.0F).texture(0.0F, 1.0F).color(color.y);
      buffer.vertex(entry, x + width, y, 0.0F).texture(1.0F, 1.0F).color(color.w);
      buffer.vertex(entry, x, y, 0.0F).texture(1.0F, 0.0F).color(color.z);
   }

   public static Vector3f getNormal(Vector3f start, Vector3f end) {
      Vector3f normal = new Vector3f(start).sub(end);
      float sqrt = MathHelper.sqrt(normal.lengthSquared());
      return normal.div(sqrt);
   }

   public static void drawCube(LivingEntity lastTarget, float anim, float red) {
      float size = red - anim - 0.17F;
      Camera camera = mc.getEntityRenderDispatcher().camera;
      Vec3d vec = Calculate.interpolate(lastTarget).subtract(camera.getPos());
      MatrixStack matrix = new MatrixStack();
      matrix.push();
      matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
      matrix.translate(vec.x, vec.y + lastTarget.getBoundingBox().getLengthY() / 2.0, vec.z);
      matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
      matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(Calculate.interpolate(prevEspValue, espValue)));
      Entry entry = matrix.peek().copy();
      int baseColor = TargetESP.getInstance().redOnHit.isValue() && red > 0.0F ? -49088 : TargetESP.getInstance().getColor();
      int finalColor = ColorAssist.setAlpha(baseColor, (int)(ColorAssist.getAlpha(baseColor) * anim));
      drawTexture(
         entry,
         Identifier.of("textures/capture2.png"),
         -size / 2.0F,
         -size / 2.0F,
         size,
         size,
         new Vector4i(finalColor, finalColor, finalColor, finalColor),
         false
      );
      matrix.pop();
   }

   public static void drawTextureCube(LivingEntity lastTarget, float anim, float red, Identifier id, boolean glow) {
      float size = red - anim - 0.17F;
      Camera camera = mc.getEntityRenderDispatcher().camera;
      Vec3d vec = Calculate.interpolate(lastTarget).subtract(camera.getPos());
      MatrixStack matrix = new MatrixStack();
      matrix.push();
      matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
      matrix.translate(vec.x, vec.y + lastTarget.getBoundingBox().getLengthY() / 2.0, vec.z);
      matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
      matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(Calculate.interpolate(prevEspValue, espValue)));
      Entry entry = matrix.peek().copy();
      int baseColor = TargetESP.getInstance().redOnHit.isValue() && red > 0.0F ? -49088 : TargetESP.getInstance().getColor();
      int finalColor = ColorAssist.setAlpha(baseColor, (int)(ColorAssist.getAlpha(baseColor) * anim));
      if (glow) {
         float glowSize = size * 1.7F;
         int glowColor = ColorAssist.setAlpha(baseColor, (int)(ColorAssist.getAlpha(baseColor) * anim * 0.45F));
         drawTexture(entry, bloom, -glowSize / 2.0F, -glowSize / 2.0F, glowSize, glowSize, new Vector4i(glowColor, glowColor, glowColor, glowColor), false);
         int solidColor = ColorAssist.setAlpha(16777215, (int)(255.0F * anim));
         drawTexture(entry, id, -size / 2.0F, -size / 2.0F, size, size, new Vector4i(solidColor, solidColor, solidColor, solidColor), false);
         drawTexture(entry, id, -size / 2.0F, -size / 2.0F, size, size, new Vector4i(solidColor, solidColor, solidColor, solidColor), false);
      } else {
         drawTexture(entry, id, -size / 2.0F, -size / 2.0F, size, size, new Vector4i(finalColor, finalColor, finalColor, finalColor), false);
      }

      matrix.pop();
   }

   public static void drawTargetEspCubes(LivingEntity lastTarget, float anim, float red) {
      drawOrbitingCubes(lastTarget, anim, red);
   }

   private static void drawOrbitingCubes(LivingEntity lastTarget, float anim, float red) {
      if (lastTarget != null) {
         Camera camera = mc.gameRenderer.getCamera();
         Vec3d targetPos = Calculate.interpolate(lastTarget);
         long time = System.currentTimeMillis();
         int baseColor = TargetESP.getInstance().redOnHit.isValue() && red > 0.0F ? -49088 : TargetESP.getInstance().getColor();
         int fillColor = ColorAssist.setAlpha(baseColor, (int)(58.0F * anim));
         int lineColor = ColorAssist.setAlpha(baseColor, (int)(205.0F * anim));
         int glowColor = ColorAssist.setAlpha(baseColor, (int)(135.0F * anim));
         int count = 24;
         double radius = 0.4 + lastTarget.getWidth() / 2.0F + 0.35F - 0.35F * anim;
         double heightRange = lastTarget.getHeight();

         for (int i = 0; i < count; i++) {
            double r1 = Math.sin(i * 132.12 + 4.12);
            double r3 = Math.sin(i * 789.34 + 9.87);
            double angleOffset = (Math.PI * 2) / count * i;
            double angle = time / 6000.0 * (Math.PI * 2) + angleOffset;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double ySpeed = 1.0 + r1 * 0.2;
            double yPhase = angleOffset + r3 * 2.0;
            double yOffset = Math.sin(time / 9000.0 * (Math.PI * 2) * ySpeed + yPhase) * 0.45 + 0.55;
            double y = yOffset * heightRange;
            double renderX = targetPos.x + x;
            double renderY = targetPos.y + y;
            double renderZ = targetPos.z + z;
            MatrixStack cubeStack = new MatrixStack();
            cubeStack.multiplyPositionMatrix(lastWorldSpaceMatrix.getPositionMatrix());
            cubeStack.translate(renderX, renderY, renderZ);
            float pulse = 1.0F + 0.15F * (float)Math.sin(time / 400.0 + i * 1.5);
            float cubeSize = 0.15F * pulse * (0.85F + 0.15F * anim);
            double hurtFactor = red * (0.5 + 0.5 * Math.sin(i * 123.45));
            if (hurtFactor > 0.05) {
               cubeSize *= (float)(1.0 - hurtFactor * 0.2);
               double pushOut = hurtFactor * 0.4;
               cubeStack.translate(Math.cos(angle) * pushOut, 0.0, Math.sin(angle) * pushOut);
            }

            float selfRotSpeed = 12000.0F + (float)r3 * 2000.0F;
            float selfRot = (float)(time % (long)Math.abs(selfRotSpeed)) / Math.abs(selfRotSpeed) * 360.0F;
            if (i % 3 == 0) {
               cubeStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(selfRot));
               cubeStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(selfRot));
            } else if (i % 3 == 1) {
               cubeStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(selfRot));
               cubeStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(selfRot));
            } else {
               cubeStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(selfRot));
               cubeStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(selfRot));
            }

            Entry cubeEntry = cubeStack.peek().copy();
            drawCubeBox(cubeEntry, cubeSize, fillColor, lineColor, false);
            drawCubeBox(cubeEntry, cubeSize * 0.92F, ColorAssist.multAlpha(fillColor, 0.28F), ColorAssist.multAlpha(lineColor, 0.7F), false);
            MatrixStack glowStack = new MatrixStack();
            glowStack.multiplyPositionMatrix(lastWorldSpaceMatrix.getPositionMatrix());
            glowStack.translate(renderX, renderY, renderZ);
            glowStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            glowStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-camera.getPitch()));
            Entry glowEntry = glowStack.peek().copy();
            float glowSize = cubeSize * 3.9F;
            drawTexture(
               glowEntry, bloom, -glowSize / 2.0F, -glowSize / 2.0F, glowSize, glowSize, new Vector4i(glowColor, glowColor, glowColor, glowColor), false
            );
         }
      }
   }

   private static void drawCubeBox(Entry entry, float size, int fillColor, int lineColor, boolean depth) {
      float halfSize = size / 2.0F;
      Vec3d p000 = new Vec3d(-halfSize, -halfSize, -halfSize);
      Vec3d p001 = new Vec3d(-halfSize, -halfSize, halfSize);
      Vec3d p010 = new Vec3d(-halfSize, halfSize, -halfSize);
      Vec3d p011 = new Vec3d(-halfSize, halfSize, halfSize);
      Vec3d p100 = new Vec3d(halfSize, -halfSize, -halfSize);
      Vec3d p101 = new Vec3d(halfSize, -halfSize, halfSize);
      Vec3d p110 = new Vec3d(halfSize, halfSize, -halfSize);
      Vec3d p111 = new Vec3d(halfSize, halfSize, halfSize);
      drawCubeFace(entry, p010, p011, p111, p110, fillColor, depth);
      drawCubeFace(entry, p000, p100, p101, p001, fillColor, depth);
      drawCubeFace(entry, p001, p101, p111, p011, fillColor, depth);
      drawCubeFace(entry, p000, p010, p110, p100, fillColor, depth);
      drawCubeFace(entry, p000, p001, p011, p010, fillColor, depth);
      drawCubeFace(entry, p100, p110, p111, p101, fillColor, depth);
      float lineWidth = depth ? 1.2F : 1.9F;
      drawLine(entry, p000, p001, lineColor, lineColor, lineWidth, depth);
      drawLine(entry, p001, p011, lineColor, lineColor, lineWidth, depth);
      drawLine(entry, p011, p010, lineColor, lineColor, lineWidth, depth);
      drawLine(entry, p010, p000, lineColor, lineColor, lineWidth, depth);
      drawLine(entry, p100, p101, lineColor, lineColor, lineWidth, depth);
      drawLine(entry, p101, p111, lineColor, lineColor, lineWidth, depth);
      drawLine(entry, p111, p110, lineColor, lineColor, lineWidth, depth);
      drawLine(entry, p110, p100, lineColor, lineColor, lineWidth, depth);
      drawLine(entry, p000, p100, lineColor, lineColor, lineWidth, depth);
      drawLine(entry, p001, p101, lineColor, lineColor, lineWidth, depth);
      drawLine(entry, p010, p110, lineColor, lineColor, lineWidth, depth);
      drawLine(entry, p011, p111, lineColor, lineColor, lineWidth, depth);
   }

   private static void drawCubeFace(Entry entry, Vec3d a, Vec3d b, Vec3d c, Vec3d d, int color, boolean depth) {
      drawQuad(entry, a, b, c, d, color, depth);
      drawQuad(entry, d, c, b, a, color, depth);
   }

   public static void drawCircle(MatrixStack matrix, LivingEntity lastTarget, float anim, float red) {
      double cs = Calculate.interpolate(circleStep - 0.17, (double)circleStep);
      Vec3d target = Calculate.interpolate(lastTarget);
      boolean canSee = Objects.requireNonNull(mc.player).canSee(lastTarget);
      GL11.glEnable(2881);
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

      for (int i = 0; i <= size; i++) {
         float width = lastTarget.getWidth() * 0.82F;
         float height = lastTarget.getHeight();
         double yAnim = Calculate.absSinAnimation(cs) * height;
         double yAnim2 = Calculate.absSinAnimation(cs - 0.45) * height;
         Vec3d cosSin = Calculate.cosSin(i, size, width);
         Vec3d nextCosSin = Calculate.cosSin(i + 1, size, width);
         int baseColor = TargetESP.getInstance().redOnHit.isValue() && red > 0.0F ? -49088 : TargetESP.getInstance().getColor();
         int color = ColorAssist.multRed(baseColor, 1.0F + red * 10.0F);
         Vec3d start = target.add(cosSin.x, cosSin.y + yAnim, cosSin.z);
         Vec3d end = target.add(cosSin.x, cosSin.y + yAnim2, cosSin.z);
         vertexLine(matrix, buffer, start, end, ColorAssist.multAlpha(color, 0.76F * anim), ColorAssist.multAlpha(color, 0.0F));
         drawLine(
            target.add(cosSin.x, cosSin.y + yAnim, cosSin.z),
            target.add(nextCosSin.x, nextCosSin.y + yAnim, nextCosSin.z),
            ColorAssist.multAlpha(color, anim),
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

      GL11.glDisable(2881);
   }

   public static void updateTargetEsp() {
      prevEspValue = espValue;
      espValue = espValue + espSpeed;
      if (espSpeed > 25.0F) {
         flipSpeed = true;
      }

      if (espSpeed < -25.0F) {
         flipSpeed = false;
      }

      espSpeed = flipSpeed ? espSpeed - 0.5F : espSpeed + 0.5F;
      circleStep += 0.22F;
   }

   public static void drawLine(Entry entry, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color, float width, boolean depth) {
      drawLine(entry, new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ), color, color, width, depth);
   }

   public static void drawLine(Vec3d start, Vec3d end, int color, float width, boolean depth) {
      drawLine(null, start, end, color, color, width, depth);
   }

   public static void drawLine(Entry entry, Vec3d start, Vec3d end, int colorStart, int colorEnd, float width, boolean depth) {
      Render3D.Line line = new Render3D.Line(entry, start, end, colorStart, colorEnd, width);
      if (depth) {
         LINE_DEPTH.add(line);
      } else {
         LINE.add(line);
      }
   }

   public static void drawCircleQuad(BufferBuilder buffer, Entry entry, Vec3d center, double radius, int color, int segments) {
      buffer.vertex(entry, (float)center.x, (float)center.y, (float)center.z).color(color);

      for (int i = 0; i <= segments; i++) {
         double angle = (Math.PI * 2) * i / segments;
         double dx = Math.cos(angle) * radius;
         double dz = Math.sin(angle) * radius;
         buffer.vertex(entry, (float)(center.x + dx), (float)center.y, (float)(center.z + dz)).color(color);
      }
   }

   public static void drawQuad(Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color, boolean depth) {
      drawQuad(null, x, y, w, z, color, depth);
   }

   public static void drawQuad(Entry entry, Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color, boolean depth) {
      Render3D.Quad quad = new Render3D.Quad(entry, x, y, w, z, color);
      if (depth) {
         QUAD_DEPTH.add(quad);
      } else {
         QUAD.add(quad);
      }
   }

   public static void drawTexture(Entry entry, Identifier id, float x, float y, float width, float height, Vector4i color, boolean depth) {
      Render3D.Texture texture = new Render3D.Texture(entry, id, x, y, width, height, color);
      if (depth) {
         TEXTURE_DEPTH.add(texture);
      } else {
         TEXTURE.add(texture);
      }
   }

   private Render3D() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static void setLastProjMat(Matrix4f lastProjMat) {
      Render3D.lastProjMat = lastProjMat;
   }

   public static void setLastWorldSpaceMatrix(Entry lastWorldSpaceMatrix) {
      Render3D.lastWorldSpaceMatrix = lastWorldSpaceMatrix;
   }

   private static class Crystal {
      private final Entity entity;
      private final Vec3d position;
      private final Vec3d rotation;
      private final float size;
      private final float rotationSpeed;

      public Crystal(Entity entity, Vec3d position, Vec3d rotation) {
         this.entity = entity;
         this.position = position;
         this.rotation = rotation;
         this.size = 0.09F;
         this.rotationSpeed = 0.5F + (float)(Math.random() * 1.5);
      }

      public void render(MatrixStack ms) {
         ms.push();
         ms.translate(this.position.x, this.position.y, this.position.z);
         float pulsation = 1.0F + (float)(Math.sin(System.currentTimeMillis() / 500.0) * 0.1F);
         ms.scale(pulsation, pulsation, pulsation);
         float selfRotation = (float)(System.currentTimeMillis() % 36000L) / 100.0F * this.rotationSpeed;
         ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float)this.rotation.x));
         ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)this.rotation.y + selfRotation));
         ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)this.rotation.z));
         RenderSystem.disableCull();
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         int baseColor = ColorAssist.fade(90);
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
         this.drawCrystal(ms, baseColor, 0.2F, true);
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
         this.drawCrystal(ms, baseColor, 0.3F, true);
         this.drawCrystal(ms, baseColor, 0.8F, false);
         RenderSystem.disableBlend();
         RenderSystem.enableCull();
         ms.pop();
      }

      private void drawCrystal(MatrixStack ms, int baseColor, float alpha, boolean filled) {
         BufferBuilder bufferBuilder = Tessellator.getInstance().begin(filled ? DrawMode.TRIANGLES : DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
         float s = this.size;
         float h_prism = this.size * 1.0F;
         float h_pyramid = this.size * 1.5F;
         int numSides = 8;
         ArrayList<Vec3d> topVertices = new ArrayList<>();
         ArrayList<Vec3d> bottomVertices = new ArrayList<>();

         for (int i2 = 0; i2 < numSides; i2++) {
            float angle = (float)((Math.PI * 2) * i2 / numSides);
            float x = (float)(s * Math.cos(angle));
            float z = (float)(s * Math.sin(angle));
            topVertices.add(new Vec3d(x, h_prism / 2.0F, z));
            bottomVertices.add(new Vec3d(x, -h_prism / 2.0F, z));
         }

         Vec3d vTop = new Vec3d(0.0, h_prism / 2.0F + h_pyramid, 0.0);
         Vec3d vBottom = new Vec3d(0.0, -h_prism / 2.0F - h_pyramid, 0.0);
         int finalColor = ColorAssist.setAlpha(baseColor, (int)(alpha * 255.0F));

         for (int i = 0; i < numSides; i++) {
            Vec3d v1 = bottomVertices.get(i);
            Vec3d v2 = bottomVertices.get((i + 1) % numSides);
            Vec3d v3 = topVertices.get((i + 1) % numSides);
            Vec3d v4 = topVertices.get(i);
            this.drawQuad(ms, bufferBuilder, v1, v2, v3, v4, finalColor, filled);
         }

         for (int var24 = 0; var24 < numSides; var24++) {
            Vec3d v1 = topVertices.get(var24);
            Vec3d v2 = topVertices.get((var24 + 1) % numSides);
            this.drawTriangle(ms, bufferBuilder, vTop, v1, v2, finalColor, filled);
         }

         for (int var25 = 0; var25 < numSides; var25++) {
            Vec3d v1 = bottomVertices.get(var25);
            Vec3d v2 = bottomVertices.get((var25 + 1) % numSides);
            this.drawTriangle(ms, bufferBuilder, vBottom, v2, v1, finalColor, filled);
         }

         BufferDraw.draw(bufferBuilder);
      }

      private void drawTriangle(MatrixStack ms, BufferBuilder bb, Vec3d v1, Vec3d v2, Vec3d v3, int color, boolean filled) {
         if (filled) {
            bb.vertex(ms.peek().getPositionMatrix(), (float)v1.x, (float)v1.y, (float)v1.z).color(color);
            bb.vertex(ms.peek().getPositionMatrix(), (float)v2.x, (float)v2.y, (float)v2.z).color(color);
            bb.vertex(ms.peek().getPositionMatrix(), (float)v3.x, (float)v3.y, (float)v3.z).color(color);
         } else {
            bb.vertex(ms.peek().getPositionMatrix(), (float)v1.x, (float)v1.y, (float)v1.z).color(color);
            bb.vertex(ms.peek().getPositionMatrix(), (float)v2.x, (float)v2.y, (float)v2.z).color(color);
            bb.vertex(ms.peek().getPositionMatrix(), (float)v2.x, (float)v2.y, (float)v2.z).color(color);
            bb.vertex(ms.peek().getPositionMatrix(), (float)v3.x, (float)v3.y, (float)v3.z).color(color);
            bb.vertex(ms.peek().getPositionMatrix(), (float)v3.x, (float)v3.y, (float)v3.z).color(color);
            bb.vertex(ms.peek().getPositionMatrix(), (float)v1.x, (float)v1.y, (float)v1.z).color(color);
         }
      }

      private void drawQuad(MatrixStack ms, BufferBuilder bb, Vec3d v1, Vec3d v2, Vec3d v3, Vec3d v4, int color, boolean filled) {
         if (filled) {
            this.drawTriangle(ms, bb, v1, v2, v3, color, true);
            this.drawTriangle(ms, bb, v1, v3, v4, color, true);
         } else {
            bb.vertex(ms.peek().getPositionMatrix(), (float)v1.x, (float)v1.y, (float)v1.z).color(color);
            bb.vertex(ms.peek().getPositionMatrix(), (float)v2.x, (float)v2.y, (float)v2.z).color(color);
            bb.vertex(ms.peek().getPositionMatrix(), (float)v2.x, (float)v2.y, (float)v2.z).color(color);
            bb.vertex(ms.peek().getPositionMatrix(), (float)v3.x, (float)v3.y, (float)v3.z).color(color);
            bb.vertex(ms.peek().getPositionMatrix(), (float)v3.x, (float)v3.y, (float)v3.z).color(color);
            bb.vertex(ms.peek().getPositionMatrix(), (float)v4.x, (float)v4.y, (float)v4.z).color(color);
            bb.vertex(ms.peek().getPositionMatrix(), (float)v4.x, (float)v4.y, (float)v4.z).color(color);
            bb.vertex(ms.peek().getPositionMatrix(), (float)v1.x, (float)v1.y, (float)v1.z).color(color);
         }
      }
   }

   public record Line(Entry entry, Vec3d start, Vec3d end, int colorStart, int colorEnd, float width) {
   }

   public record Quad(Entry entry, Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color) {
   }

   public record Texture(Entry entry, Identifier id, float x, float y, float width, float height, Vector4i color) {
   }
}
