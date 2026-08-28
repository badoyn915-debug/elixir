package code.elixir.features.impl.render;

import code.elixir.common.repository.friend.FriendUtils;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.features.module.setting.implement.RadioSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.geometry.BufferDraw;
import code.elixir.utils.math.projection.Projection;
import code.elixir.utils.theme.ThemeManager;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPart.Cuboid;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

public class Chams extends Module {
   private static final Map<ModelPart, List<Cuboid>> CUBOID_CACHE = new ConcurrentHashMap<>();
   private static Field cuboidField;
   private final RadioSetting targets = new RadioSetting("Цели", "Каких игроков подсвечивать", new String[]{"Все", "Враги", "Друзья"}, "Все");
   private final BooleanSetting useThemeColor = new BooleanSetting("Цвет темы", "Использовать цвет темы").setValue(true);
   private final ColorSetting color = new ColorSetting("Цвет", "Кастомный цвет chams").value(-39836).visible(() -> !this.useThemeColor.isValue());
   private final SliderSettings alpha = new SliderSettings("Прозрачность", "Прозрачность линий").setValue(160.0F).range(10, 255).step(5.0F);
   private final SliderSettings fillAlpha = new SliderSettings("Прозрачность заливки", "Прозрачность заливки").setValue(50.0F).range(5, 200).step(5.0F);
   private final SliderSettings lineWidth = new SliderSettings("Толщина линий", "Толщина линий").setValue(1.5F).range(0.5F, 5.0F).step(0.5F);
   private final SliderSettings layers = new SliderSettings("Слои", "Количество слоев").setValue(1.0F).range(1, 3).step(1.0F);
   private final BooleanSetting throughWalls = new BooleanSetting("Сквозь стены", "Рисовать сквозь стены").setValue(true);
   private final BooleanSetting wave = new BooleanSetting("Волна яркости", "Пульсация яркости по всему телу").setValue(true);
   private final SliderSettings waveSpeed = new SliderSettings("Скорость волны", "Скорость переливания яркости")
      .setValue(1.5F)
      .range(0.5F, 4.0F)
      .step(0.1F)
      .visible(() -> this.wave.isValue());

   public Chams() {
      super("Chams", ModuleCategory.ESP);
      this.setup(
         new Setting[]{
            this.targets, this.useThemeColor, this.color, this.alpha, this.fillAlpha, this.lineWidth, this.layers, this.throughWalls, this.wave, this.waveSpeed
         }
      );
   }

   public static Chams getInstance() {
      return Instance.get(Chams.class);
   }

   public boolean shouldRender(PlayerEntity player) {
      if (this.isState() && player != null) {
         if (mc.player != null && (player != mc.player || !mc.options.getPerspective().isFirstPerson())) {
            if (!Projection.isWithinRenderDistance(player)) {
               return false;
            } else {
               boolean friend = FriendUtils.isFriend(player);
               String var3 = this.targets.get();

               return switch (var3) {
                  case "Враги" -> !friend;
                  case "Друзья" -> friend;
                  default -> true;
               };
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public void renderPlayer(MatrixStack matrices, PlayerEntity player, PlayerEntityModel model) {
      if (this.shouldRender(player) && model != null) {
         boolean friend = FriendUtils.isFriend(player);
         int rgb = friend
            ? ColorAssist.getColor(0, 220, 80, 255)
            : (this.useThemeColor.isValue() ? ThemeManager.primaryColor.getColor() : this.color.getColor());
         float waveBrightness = this.resolveWaveBrightness();
         this.renderModel(
            matrices,
            model,
            rgb,
            Math.max(5, (int)this.fillAlpha.getValue()),
            Math.max(10, (int)this.alpha.getValue()),
            this.lineWidth.getValue(),
            this.throughWalls.isValue(),
            Math.max(1, this.layers.getInt()),
            waveBrightness
         );
      }
   }

   public void renderCustom(MatrixStack matrices, PlayerEntityModel model, int rgb, float opacity) {
      if (model != null) {
         int lineAlpha = Math.max(10, (int)(opacity * 200.0F));
         int fill = Math.max(5, (int)(opacity * 55.0F));
         this.renderModel(matrices, model, rgb, fill, lineAlpha, 1.5F, true, 1, 1.0F);
      }
   }

   private void renderModel(
      MatrixStack matrices,
      PlayerEntityModel model,
      int rgb,
      int fillAlphaValue,
      int lineAlphaValue,
      float width,
      boolean throughWalls,
      int layersCount,
      float waveBrightness
   ) {
      ModelPart[] parts = new ModelPart[]{model.head, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg};
      Matrix4f[] partMatrix = new Matrix4f[parts.length];
      Matrix3f[] partNormal = new Matrix3f[parts.length];
      boolean hasGeometry = false;

      for (int p = 0; p < parts.length; p++) {
         List<Cuboid> cuboids = this.getCuboids(parts[p]);
         if (cuboids != null && !cuboids.isEmpty()) {
            matrices.push();
            parts[p].rotate(matrices);
            Entry entry = matrices.peek();
            partMatrix[p] = new Matrix4f(entry.getPositionMatrix());
            partNormal[p] = new Matrix3f(entry.getNormalMatrix());
            matrices.pop();
            hasGeometry = true;
         }
      }

      if (hasGeometry) {
         int baseR = ColorAssist.getRed(rgb);
         int baseG = ColorAssist.getGreen(rgb);
         int baseB = ColorAssist.getBlue(rgb);
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
         RenderSystem.disableCull();
         if (throughWalls) {
            RenderSystem.disableDepthTest();
         } else {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(515);
         }

         GL11.glEnable(32823);
         GL11.glPolygonOffset(-1.0F, -1.0F);
         RenderSystem.depthMask(false);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         BufferBuilder fill = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

         for (int i = 0; i < layersCount; i++) {
            float expand = i * 0.012F;
            int fillColor = this.scaleBrightness(
               ColorAssist.getColor(baseR, baseG, baseB, Math.max(5, (int)(fillAlphaValue / (1.0F + i * 1.5F)))), waveBrightness
            );

            for (int px = 0; px < parts.length; px++) {
               if (partMatrix[px] != null) {
                  this.appendPartFill(partMatrix[px], this.getCuboids(parts[px]), fillColor, expand, fill);
               }
            }
         }

         BufferDraw.draw(fill);
         RenderSystem.depthMask(true);
         GL11.glPolygonOffset(-2.0F, -2.0F);
         RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
         RenderSystem.lineWidth(width);
         BufferBuilder line = Tessellator.getInstance().begin(DrawMode.LINES, VertexFormats.LINES);

         for (int i = 0; i < layersCount; i++) {
            float expand = i * 0.012F;
            int lineColor = this.scaleBrightness(
               ColorAssist.getColor(baseR, baseG, baseB, Math.max(10, (int)(lineAlphaValue / (1.0F + i * 1.5F)))), waveBrightness
            );

            for (int pxx = 0; pxx < parts.length; pxx++) {
               if (partMatrix[pxx] != null) {
                  this.appendPartLines(partMatrix[pxx], partNormal[pxx], this.getCuboids(parts[pxx]), lineColor, expand, line);
               }
            }
         }

         BufferDraw.draw(line);
         RenderSystem.lineWidth(1.0F);
         GL11.glDisable(32823);
         GL11.glPolygonOffset(0.0F, 0.0F);
         RenderSystem.depthFunc(515);
         if (throughWalls) {
            RenderSystem.enableDepthTest();
         }

         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   }

   private List<Cuboid> getCuboids(ModelPart part) {
      if (part == null) {
         return null;
      } else {
         List cuboids = CUBOID_CACHE.get(part);
         if (cuboids == null && cuboidField != null) {
            try {
               cuboids = (List)cuboidField.get(part);
               if (cuboids != null) {
                  CUBOID_CACHE.put(part, cuboids);
               }
            } catch (Exception var4) {
            }
         }

         return cuboids;
      }
   }

   private float resolveWaveBrightness() {
      if (!this.wave.isValue()) {
         return 1.0F;
      } else {
         float pulse = 0.5F + 0.5F * MathHelper.sin((float)System.currentTimeMillis() / 1000.0F * this.waveSpeed.getValue() * (float) (Math.PI * 2));
         return 0.45F + 0.55F * pulse;
      }
   }

   private void appendPartFill(Matrix4f matrix, List<Cuboid> cuboids, int fillColor, float expand, BufferBuilder buffer) {
      if (cuboids != null && !cuboids.isEmpty()) {
         int fa = ColorAssist.getAlpha(fillColor);
         int fr = ColorAssist.getRed(fillColor);
         int fg = ColorAssist.getGreen(fillColor);
         int fb = ColorAssist.getBlue(fillColor);

         for (Cuboid cuboid : cuboids) {
            float x1 = cuboid.minX / 16.0F - expand;
            float y1 = cuboid.minY / 16.0F - expand;
            float z1 = cuboid.minZ / 16.0F - expand;
            float x2 = cuboid.maxX / 16.0F + expand;
            float y2 = cuboid.maxY / 16.0F + expand;
            float z2 = cuboid.maxZ / 16.0F + expand;
            this.vertex(buffer, matrix, x1, y1, z1, fr, fg, fb, fa);
            this.vertex(buffer, matrix, x2, y1, z1, fr, fg, fb, fa);
            this.vertex(buffer, matrix, x2, y1, z2, fr, fg, fb, fa);
            this.vertex(buffer, matrix, x1, y1, z2, fr, fg, fb, fa);
            this.vertex(buffer, matrix, x1, y2, z1, fr, fg, fb, fa);
            this.vertex(buffer, matrix, x1, y2, z2, fr, fg, fb, fa);
            this.vertex(buffer, matrix, x2, y2, z2, fr, fg, fb, fa);
            this.vertex(buffer, matrix, x2, y2, z1, fr, fg, fb, fa);
            this.vertex(buffer, matrix, x1, y1, z1, fr, fg, fb, fa);
            this.vertex(buffer, matrix, x1, y2, z1, fr, fg, fb, fa);
            this.vertex(buffer, matrix, x2, y2, z1, fr, fg, fb, fa);
            this.vertex(buffer, matrix, x2, y1, z1, fr, fg, fb, fa);
            this.vertex(buffer, matrix, x1, y1, z2, fr, fg, fb, fa);
            this.vertex(buffer, matrix, x2, y1, z2, fr, fg, fb, fa);
            this.vertex(buffer, matrix, x2, y2, z2, fr, fg, fb, fa);
            this.vertex(buffer, matrix, x1, y2, z2, fr, fg, fb, fa);
            this.vertex(buffer, matrix, x1, y1, z1, fr, fg, fb, fa);
            this.vertex(buffer, matrix, x1, y1, z2, fr, fg, fb, fa);
            this.vertex(buffer, matrix, x1, y2, z2, fr, fg, fb, fa);
            this.vertex(buffer, matrix, x1, y2, z1, fr, fg, fb, fa);
            this.vertex(buffer, matrix, x2, y1, z1, fr, fg, fb, fa);
            this.vertex(buffer, matrix, x2, y2, z1, fr, fg, fb, fa);
            this.vertex(buffer, matrix, x2, y2, z2, fr, fg, fb, fa);
            this.vertex(buffer, matrix, x2, y1, z2, fr, fg, fb, fa);
         }
      }
   }

   private void appendPartLines(Matrix4f matrix, Matrix3f normalMatrix, List<Cuboid> cuboids, int lineColor, float expand, BufferBuilder buffer) {
      if (cuboids != null && !cuboids.isEmpty()) {
         int la = ColorAssist.getAlpha(lineColor);
         int lr = ColorAssist.getRed(lineColor);
         int lg = ColorAssist.getGreen(lineColor);
         int lb = ColorAssist.getBlue(lineColor);

         for (Cuboid cuboid : cuboids) {
            float x1 = cuboid.minX / 16.0F - expand;
            float y1 = cuboid.minY / 16.0F - expand;
            float z1 = cuboid.minZ / 16.0F - expand;
            float x2 = cuboid.maxX / 16.0F + expand;
            float y2 = cuboid.maxY / 16.0F + expand;
            float z2 = cuboid.maxZ / 16.0F + expand;
            this.edge(buffer, matrix, normalMatrix, x1, y1, z1, x2, y1, z1, lr, lg, lb, la);
            this.edge(buffer, matrix, normalMatrix, x2, y1, z1, x2, y1, z2, lr, lg, lb, la);
            this.edge(buffer, matrix, normalMatrix, x2, y1, z2, x1, y1, z2, lr, lg, lb, la);
            this.edge(buffer, matrix, normalMatrix, x1, y1, z2, x1, y1, z1, lr, lg, lb, la);
            this.edge(buffer, matrix, normalMatrix, x1, y2, z1, x2, y2, z1, lr, lg, lb, la);
            this.edge(buffer, matrix, normalMatrix, x2, y2, z1, x2, y2, z2, lr, lg, lb, la);
            this.edge(buffer, matrix, normalMatrix, x2, y2, z2, x1, y2, z2, lr, lg, lb, la);
            this.edge(buffer, matrix, normalMatrix, x1, y2, z2, x1, y2, z1, lr, lg, lb, la);
            this.edge(buffer, matrix, normalMatrix, x1, y1, z1, x1, y2, z1, lr, lg, lb, la);
            this.edge(buffer, matrix, normalMatrix, x2, y1, z1, x2, y2, z1, lr, lg, lb, la);
            this.edge(buffer, matrix, normalMatrix, x2, y1, z2, x2, y2, z2, lr, lg, lb, la);
            this.edge(buffer, matrix, normalMatrix, x1, y1, z2, x1, y2, z2, lr, lg, lb, la);
         }
      }
   }

   private void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, int r, int g, int b, int a) {
      buffer.vertex(matrix, x, y, z).color(r, g, b, a);
   }

   private void edge(
      BufferBuilder buffer, Matrix4f matrix, Matrix3f normalMatrix, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int b, int a
   ) {
      float dx = x2 - x1;
      float dy = y2 - y1;
      float dz = z2 - z1;
      float length = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (length != 0.0F) {
         Vector3f normal = normalMatrix.transform(dx / length, dy / length, dz / length, new Vector3f()).normalize();
         buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(normal.x, normal.y, normal.z);
         buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).normal(normal.x, normal.y, normal.z);
      }
   }

   private int scaleBrightness(int argb, float factor) {
      int a = ColorAssist.getAlpha(argb);
      int r = Math.min(255, (int)(ColorAssist.getRed(argb) * factor));
      int g = Math.min(255, (int)(ColorAssist.getGreen(argb) * factor));
      int b = Math.min(255, (int)(ColorAssist.getBlue(argb) * factor));
      return ColorAssist.getColor(r, g, b, a);
   }

   static {
      try {
         for (Field field : ModelPart.class.getDeclaredFields()) {
            if (List.class.isAssignableFrom(field.getType())) {
               field.setAccessible(true);
               cuboidField = field;
               break;
            }
         }
      } catch (Exception var4) {
      }
   }
}
