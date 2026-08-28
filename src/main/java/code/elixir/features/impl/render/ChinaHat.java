package code.elixir.features.impl.render;

import code.elixir.common.repository.friend.FriendUtils;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.features.module.setting.implement.RadioSetting;
import code.elixir.utils.client.Instance;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.theme.ThemeManager;
import java.awt.Color;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.ModelWithHead;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public class ChinaHat extends Module {
   private static final float PI = 0.06981317F;
   private final BooleanSetting viewOnOthers;
   private final RadioSetting colorMode = new RadioSetting("Режим цвета", "Выбор источника цвета", new String[]{"Клиентский", "Кастом"}, "Клиентский");
   private final ColorSetting customColor = new ColorSetting("Кастомный цвет", "Кастомный цвет шляпы")
      .value(ColorAssist.getClientColor())
      .visible(() -> this.colorMode.get().equals("Кастом"));

   public ChinaHat() {
      super("ChinaHat", "ChinaHat", ModuleCategory.RENDER);
      this.viewOnOthers = new BooleanSetting("Показывать на других", "View on Others");
      this.setup(new Setting[]{this.viewOnOthers, this.colorMode, this.customColor});
   }

   public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, PlayerEntity player, ModelWithHead model) {
      boolean isLocalPlayer = player == mc.player;
      boolean isFriend = FriendUtils.isFriend(player);
      if (isLocalPlayer || isFriend || this.viewOnOthers.isValue()) {
         if (model instanceof BipedEntityModel bipedModel) {
            Box entityBoundingBox = player.getBoundingBox();
            double radius = entityBoundingBox.maxX - entityBoundingBox.minX;
            int headSlot = 39;
            ItemStack headStack = player.getInventory().getStack(headSlot);
            boolean hasHelmet = !headStack.isEmpty();
            float offset = hasHelmet ? 0.48F : 0.42F;
            matrixStack.push();
            bipedModel.head.rotate(matrixStack);
            matrixStack.translate(0.0F, -offset, 0.0F);
            matrixStack.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(180.0F));
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
            Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
            int darkenedColor;
            int baseColor;
            if (isFriend) {
               baseColor = new Color(0, 255, 0, 255).getRGB();
               darkenedColor = new Color(0, 170, 0, 255).getRGB();
            } else if (this.colorMode.get().equals("Клиентский")) {
               baseColor = ThemeManager.primaryColor.getColor();
               darkenedColor = ThemeManager.secondaryColor.getColor();
            } else {
               baseColor = this.customColor.getColor();
               darkenedColor = this.darken(baseColor, 0.5F);
            }

            float y = 0.0F;
            float outlineY = -0.01F;
            float outlineHeight = 0.01F;
            float outlineThickness = 0.01F;
            float radiusOuter = (float)(radius + outlineThickness);
            float radiusInner = (float)(radius - outlineThickness);
            VertexConsumer buffer = vertexConsumerProvider.getBuffer(RenderLayer.getDebugQuads());
            long time = System.currentTimeMillis();

            for (int i = 0; i < 360; i++) {
               float angle1 = i * 0.06981317F;
               float angle2 = (i + 1) * 0.06981317F;
               int gradientAngle = i * 8;
               float x1 = (float)(MathHelper.sin(angle1) * radius);
               float z1 = (float)(MathHelper.cos(angle1) * radius);
               float x2 = (float)(MathHelper.sin(angle2) * radius);
               float z2 = (float)(MathHelper.cos(angle2) * radius);
               int gradientColor = this.gradientAnimated(3, gradientAngle, baseColor, darkenedColor, time);
               this.putVertex(buffer, matrix4f, x1, y, z1, gradientColor);
               this.putVertex(buffer, matrix4f, x2, y, z2, gradientColor);
               this.putVertex(buffer, matrix4f, 0.0F, 0.3F, 0.0F, baseColor);
               this.putVertex(buffer, matrix4f, 0.0F, 0.3F, 0.0F, baseColor);
            }

            float prevX1Outer = MathHelper.sin(0.0F) * radiusOuter;
            float prevZ1Outer = MathHelper.cos(0.0F) * radiusOuter;
            float prevX1Inner = MathHelper.sin(0.0F) * radiusInner;
            float prevZ1Inner = MathHelper.cos(0.0F) * radiusInner;

            for (int i = 1; i <= 181; i++) {
               float angle2 = i * 0.06981317F;
               int gradientAngle = (i - 1) * 8;
               float x2Outer = MathHelper.sin(angle2) * radiusOuter;
               float z2Outer = MathHelper.cos(angle2) * radiusOuter;
               float x2Inner = MathHelper.sin(angle2) * radiusInner;
               float z2Inner = MathHelper.cos(angle2) * radiusInner;
               int gradientColor = this.gradientAnimated(3, gradientAngle, baseColor, darkenedColor, time);
               this.putVertex(buffer, matrix4f, prevX1Outer, outlineY, prevZ1Outer, gradientColor);
               this.putVertex(buffer, matrix4f, prevX1Outer, outlineY + outlineHeight, prevZ1Outer, gradientColor);
               this.putVertex(buffer, matrix4f, x2Outer, outlineY + outlineHeight, z2Outer, gradientColor);
               this.putVertex(buffer, matrix4f, x2Outer, outlineY, z2Outer, gradientColor);
               this.putVertex(buffer, matrix4f, prevX1Inner, outlineY + outlineHeight, prevZ1Inner, gradientColor);
               this.putVertex(buffer, matrix4f, x2Inner, outlineY + outlineHeight, z2Inner, gradientColor);
               this.putVertex(buffer, matrix4f, x2Outer, outlineY + outlineHeight, z2Outer, gradientColor);
               this.putVertex(buffer, matrix4f, prevX1Outer, outlineY + outlineHeight, prevZ1Outer, gradientColor);
               prevX1Outer = x2Outer;
               prevZ1Outer = z2Outer;
               prevX1Inner = x2Inner;
               prevZ1Inner = z2Inner;
            }

            matrixStack.pop();
            if (vertexConsumerProvider instanceof Immediate immediate) {
               immediate.draw();
            }
         }
      }
   }

   private void putVertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z, int rgba) {
      buffer.vertex(matrix, x, y, z).color(this.red(rgba), this.green(rgba), this.blue(rgba), this.alpha(rgba));
   }

   private int darken(int color, float factor) {
      int r = (int)((color >> 16 & 0xFF) * factor);
      int g = (int)((color >> 8 & 0xFF) * factor);
      int b = (int)((color & 0xFF) * factor);
      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   private int gradient(int speed, int gradientAngle, int color1, int color2) {
      float ratio = gradientAngle % 360 / 360.0F;
      ratio = (float)((Math.sin(ratio * Math.PI * 2.0 * speed) + 1.0) / 2.0);
      return ColorAssist.interpolate(color1, color2, ratio);
   }

   private int gradientAnimated(int speed, int gradientAngle, int color1, int color2, long time) {
      float animatedAngle = (float)((gradientAngle + time / 10L) % 360L);
      float ratio = animatedAngle / 360.0F;
      ratio = (float)((Math.sin(ratio * Math.PI * 2.0 * speed) + 1.0) / 2.0);
      return ColorAssist.interpolate(color1, color2, ratio);
   }

   private int red(int color) {
      return color >> 16 & 0xFF;
   }

   private int green(int color) {
      return color >> 8 & 0xFF;
   }

   private int blue(int color) {
      return color & 0xFF;
   }

   private int alpha(int color) {
      return color >> 24 & 0xFF;
   }

   public static ChinaHat getInstance() {
      return Instance.get(ChinaHat.class);
   }
}
