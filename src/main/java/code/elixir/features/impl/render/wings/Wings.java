package code.elixir.features.impl.render.wings;

import code.elixir.common.repository.friend.FriendUtils;
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
import code.elixir.utils.display.render.geometry.Render3D;
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
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class Wings extends Module {
   private static final Wings.WingPoint[] SHAPE = new Wings.WingPoint[]{
      new Wings.WingPoint(0.08F, 0.1F, 0.88F),
      new Wings.WingPoint(0.28F, 0.34F, 0.78F),
      new Wings.WingPoint(0.56F, 0.82F, 0.62F),
      new Wings.WingPoint(0.86F, 0.3F, 0.52F),
      new Wings.WingPoint(1.14F, 0.46F, 0.4F),
      new Wings.WingPoint(1.24F, 0.04F, 0.3F),
      new Wings.WingPoint(1.02F, -0.18F, 0.28F),
      new Wings.WingPoint(1.18F, -0.64F, 0.22F),
      new Wings.WingPoint(0.86F, -0.46F, 0.2F),
      new Wings.WingPoint(0.8F, -0.98F, 0.14F),
      new Wings.WingPoint(0.54F, -0.74F, 0.16F),
      new Wings.WingPoint(0.3F, -1.16F, 0.12F),
      new Wings.WingPoint(0.1F, -0.54F, 0.18F)
   };
   private final BooleanSetting self = new BooleanSetting("На себя", "Отображать крылья на себе").setValue(true);
   private final BooleanSetting friends = new BooleanSetting("На друзей", "Отображать крылья на друзьях").setValue(false);
   private final BooleanSetting players = new BooleanSetting("На игроков", "Отображать крылья на других игроках").setValue(false);
   private final SliderSettings size = new SliderSettings("Размер", "Размер крыльев").setValue(1.0F).range(0.75F, 1.35F).step(0.05F);
   private final BooleanSetting useThemeColor = new BooleanSetting("Цвет темы", "Использовать цвет темы клиента").setValue(true);
   private final ColorSetting color = new ColorSetting("Цвет", "Цвет крыльев")
      .value(new Color(150, 130, 255).getRGB())
      .presets(new Color(150, 130, 255).getRGB(), new Color(120, 180, 255).getRGB(), new Color(235, 120, 220).getRGB(), new Color(255, 255, 255).getRGB())
      .visible(() -> !this.useThemeColor.isValue());
   private float selfBodyYaw;
   private boolean selfBodyYawInitialized;
   private float glideProgress;

   public Wings() {
      super("Wings", "Wings", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.self, this.friends, this.players, this.size, this.useThemeColor, this.color});
   }

   @EventHandler
   public void onRender3D(WorldRenderEvent event) {
      if (mc.player != null && mc.world != null && mc.gameRenderer != null) {
         float tickDelta = event.getPartialTicks();
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         if (this.self.isValue() && !mc.options.getPerspective().isFirstPerson() && mc.player.isAlive() && !this.hasElytra(mc.player)) {
            try {
               this.renderWings(mc.player, tickDelta);
            } catch (Exception var9) {
            }
         }

         if (this.players.isValue()) {
            for (Entity entity : mc.world.getEntities()) {
               PlayerEntity player;
               if (entity instanceof PlayerEntity && (player = (PlayerEntity)entity) != mc.player && player.isAlive() && !this.hasElytra(player)) {
                  try {
                     this.renderWings(player, tickDelta);
                  } catch (Exception var8) {
                  }
               }
            }
         } else if (this.friends.isValue()) {
            for (Entity entityx : mc.world.getEntities()) {
               PlayerEntity player;
               if (entityx instanceof PlayerEntity
                  && (player = (PlayerEntity)entityx) != mc.player
                  && player.isAlive()
                  && !this.hasElytra(player)
                  && FriendUtils.isFriend(player)) {
                  try {
                     this.renderWings(player, tickDelta);
                  } catch (Exception var7) {
                  }
               }
            }
         }

         RenderSystem.depthMask(true);
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
         RenderSystem.blendFuncSeparate(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA, SrcFactor.ONE, DstFactor.ZERO);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private boolean hasElytra(PlayerEntity player) {
      return player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
   }

   private void renderWings(PlayerEntity player, float tickDelta) {
      double x = MathHelper.lerp(tickDelta, player.prevX, player.getX());
      double y = MathHelper.lerp(tickDelta, player.prevY, player.getY());
      double z = MathHelper.lerp(tickDelta, player.prevZ, player.getZ());
      float bodyYaw = this.resolveBodyYaw(player, tickDelta);
      float move = MathHelper.clamp(player.limbAnimator.getPos(tickDelta), 0.0F, 1.0F);
      Wings.WingPose pose = this.resolvePose(player, tickDelta);
      if (pose != null) {
         float flap = (float)Math.sin((player.age + tickDelta) * pose.flapSpeed) * pose.flapAmplitude;
         float open = (8.0F + flap + move * pose.motionSpreadBoost) * pose.openMultiplier;
         float wingScale = this.size.getValue() * pose.scaleMultiplier;
         int baseColor = this.resolveBaseColor();
         int glowColor = this.resolveGlowColor(baseColor);
         int coreColor = this.resolveCoreColor(baseColor);
         MatrixStack stack = new MatrixStack();
         stack.multiplyPositionMatrix(Render3D.lastWorldSpaceMatrix.getPositionMatrix());
         stack.push();
         stack.translate(x, y, z);
         stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - bodyYaw));
         if (pose.preTranslateY != 0.0F || pose.preTranslateZ != 0.0F) {
            stack.translate(0.0F, pose.preTranslateY, pose.preTranslateZ);
         }

         if (pose.pitchRotation != 0.0F) {
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pose.pitchRotation));
         }

         if (pose.rollRotation != 0.0F) {
            stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(pose.rollRotation));
         }

         stack.translate(0.0F, pose.anchorY, pose.anchorZ);
         stack.scale(wingScale, wingScale, wingScale);
         this.renderWingSide(stack, -1.0F, open, baseColor, glowColor, coreColor, baseColor, pose);
         this.renderWingSide(stack, 1.0F, open, baseColor, glowColor, coreColor, baseColor, pose);
         stack.pop();
      }
   }

   private void renderWingSide(MatrixStack stack, float side, float open, int baseColor, int glowColor, int coreColor, int outlineColor, Wings.WingPose pose) {
      stack.push();
      stack.translate(side * pose.sideOffset, pose.sideYOffset, pose.sideZOffset);
      stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * open));
      stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * pose.sideRoll));
      stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pose.sidePitch));
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      this.drawWingLayer(stack, side, 1.22F, setAlpha(glowColor, 48), setAlpha(glowColor, 0));
      this.drawWingLayer(stack, side, 0.84F, setAlpha(coreColor, 57), setAlpha(coreColor, 0));
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
      this.drawWingLayer(stack, side, 1.0F, setAlpha(baseColor, 220), setAlpha(baseColor, 10));
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      this.drawWingOutline(stack, side, 1.0F, setAlpha(outlineColor, 136));
      this.drawWingRibs(stack, side, 0.96F, setAlpha(glowColor, 44));
      stack.pop();
   }

   private void drawWingLayer(MatrixStack stack, float side, float scale, int rootColor, int edgeColor) {
      Matrix4f matrix = stack.peek().getPositionMatrix();
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

      for (int i = 0; i < SHAPE.length; i++) {
         Wings.WingPoint cur = SHAPE[i];
         Wings.WingPoint next = SHAPE[(i + 1) % SHAPE.length];
         this.vertex(buffer, matrix, 0.0F, 0.0F, 0.0F, rootColor);
         this.vertex(buffer, matrix, side * cur.x * scale, cur.y * scale, 0.0F, this.applyPointAlpha(edgeColor, cur.alphaMul));
         this.vertex(buffer, matrix, side * next.x * scale, next.y * scale, 0.0F, this.applyPointAlpha(edgeColor, next.alphaMul));
      }

      BufferDraw.draw(buffer);
   }

   private void drawWingOutline(MatrixStack stack, float side, float scale, int color) {
      Matrix4f matrix = stack.peek().getPositionMatrix();
      RenderSystem.lineWidth(1.35F);
      GL11.glEnable(2848);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

      for (Wings.WingPoint point : SHAPE) {
         this.vertex(buffer, matrix, side * point.x * scale, point.y * scale, 0.0F, color);
      }

      this.vertex(buffer, matrix, side * SHAPE[0].x * scale, SHAPE[0].y * scale, 0.0F, color);
      BufferDraw.draw(buffer);
      GL11.glDisable(2848);
   }

   private void drawWingRibs(MatrixStack stack, float side, float scale, int color) {
      Matrix4f matrix = stack.peek().getPositionMatrix();
      int[] ribIndices = new int[]{2, 4, 7, 9, 11};
      RenderSystem.lineWidth(0.9F);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

      for (int idx : ribIndices) {
         Wings.WingPoint point = SHAPE[idx];
         this.vertex(buffer, matrix, 0.0F, 0.0F, 0.0F, setAlpha(color, Math.max(8, (int)(alpha(color) * 0.75F))));
         this.vertex(buffer, matrix, side * point.x * scale, point.y * scale, 0.0F, this.applyPointAlpha(color, point.alphaMul));
      }

      BufferDraw.draw(buffer);
   }

   private int applyPointAlpha(int color, float multiplier) {
      return setAlpha(color, Math.max(0, Math.min(255, (int)(alpha(color) * multiplier))));
   }

   private static int setAlpha(int color, int a) {
      return MathHelper.clamp(a, 0, 255) << 24 | color & 16777215;
   }

   private static int alpha(int color) {
      return color >> 24 & 0xFF;
   }

   private static int red(int color) {
      return color >> 16 & 0xFF;
   }

   private static int green(int color) {
      return color >> 8 & 0xFF;
   }

   private static int blue(int color) {
      return color & 0xFF;
   }

   private static int getColor(int r, int g, int b, int a) {
      return a << 24 | r << 16 | g << 8 | b;
   }

   private void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, int color) {
      buffer.vertex(matrix, x, y, z).color(color);
   }

   private int resolveBaseColor() {
      return this.useThemeColor.isValue() ? ColorAssist.getClientColor() : this.color.getColor();
   }

   private int resolveGlowColor(int base) {
      return ColorAssist.interpolateColor(base, getColor(255, 255, 255, 255), 0.28F);
   }

   private int resolveCoreColor(int base) {
      return ColorAssist.interpolateColor(base, getColor(255, 255, 255, 255), 0.55F);
   }

   private float resolveBodyYaw(PlayerEntity player, float tickDelta) {
      float target = MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw);
      if (player != mc.player) {
         return target;
      } else if (this.selfBodyYawInitialized && player.age >= 2) {
         this.selfBodyYaw = approachDegrees(this.selfBodyYaw, target, 14.0F);
         return this.selfBodyYaw;
      } else {
         this.selfBodyYaw = target;
         this.selfBodyYawInitialized = true;
         return this.selfBodyYaw;
      }
   }

   private static float approachDegrees(float current, float target, float maxDelta) {
      float delta = MathHelper.wrapDegrees(target - current);
      delta = MathHelper.clamp(delta, -maxDelta, maxDelta);
      return current + delta;
   }

   private Wings.WingPose resolvePose(PlayerEntity player, float tickDelta) {
      float pitch = MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());
      if (player == mc.player) {
         this.glideProgress = MathHelper.clamp(this.glideProgress + (player.isGliding() ? 0.12F : -0.2F), 0.0F, 1.0F);
      }

      if (player.isGliding()) {
         float flightProgress = player == mc.player ? this.glideProgress : 1.0F;
         float pitchRotation = flightProgress * (-90.0F - pitch);
         return new Wings.WingPose(0.34F, 0.46F, 0.0F, 0.0F, pitchRotation, 0.0F, 0.76F, 0.92F, 0.1F, 0.58F, 0.05F, 0.06F, -5.0F, -2.0F, 0.13F);
      } else if (player.isTouchingWater()) {
         return null;
      } else {
         return player.isSneaking()
            ? new Wings.WingPose(0.0F, 0.0F, 0.96F, 0.1F, 18.0F, 0.0F, 1.0F, 1.0F, 0.18F, 4.5F, 0.06F, 0.02F, -11.0F, -4.0F, 0.12F)
            : new Wings.WingPose(0.0F, 0.0F, 1.38F, 0.1F, 0.0F, 0.0F, 1.0F, 1.0F, 0.18F, 4.5F, 0.06F, 0.02F, -11.0F, -4.0F, 0.12F);
      }
   }

   @Override
   public void deactivate() {
      super.deactivate();
      this.selfBodyYawInitialized = false;
      this.glideProgress = 0.0F;
   }

   private static final class WingPoint {
      final float x;
      final float y;
      final float alphaMul;

      WingPoint(float x, float y, float alphaMul) {
         this.x = x;
         this.y = y;
         this.alphaMul = alphaMul;
      }
   }

   private static final class WingPose {
      final float preTranslateY;
      final float preTranslateZ;
      final float anchorY;
      final float anchorZ;
      final float pitchRotation;
      final float rollRotation;
      final float openMultiplier;
      final float scaleMultiplier;
      final float motionSpreadBoost;
      final float flapAmplitude;
      final float sideOffset;
      final float sideYOffset;
      final float sideZOffset;
      final float sideRoll;
      final float sidePitch;
      final float flapSpeed;

      WingPose(
         float preTranslateY,
         float preTranslateZ,
         float anchorY,
         float anchorZ,
         float pitchRotation,
         float rollRotation,
         float openMultiplier,
         float scaleMultiplier,
         float motionSpreadBoost,
         float flapAmplitude,
         float sideOffset,
         float sideZOffset,
         float sideRoll,
         float sidePitch,
         float flapSpeed
      ) {
         this(
            preTranslateY,
            preTranslateZ,
            anchorY,
            anchorZ,
            pitchRotation,
            rollRotation,
            openMultiplier,
            scaleMultiplier,
            motionSpreadBoost,
            flapAmplitude,
            sideOffset,
            0.0F,
            sideZOffset,
            sideRoll,
            sidePitch,
            flapSpeed
         );
      }

      WingPose(
         float preTranslateY,
         float preTranslateZ,
         float anchorY,
         float anchorZ,
         float pitchRotation,
         float rollRotation,
         float openMultiplier,
         float scaleMultiplier,
         float motionSpreadBoost,
         float flapAmplitude,
         float sideOffset,
         float sideYOffset,
         float sideZOffset,
         float sideRoll,
         float sidePitch,
         float flapSpeed
      ) {
         this.preTranslateY = preTranslateY;
         this.preTranslateZ = preTranslateZ;
         this.anchorY = anchorY;
         this.anchorZ = anchorZ;
         this.pitchRotation = pitchRotation;
         this.rollRotation = rollRotation;
         this.openMultiplier = openMultiplier;
         this.scaleMultiplier = scaleMultiplier;
         this.motionSpreadBoost = motionSpreadBoost;
         this.flapAmplitude = flapAmplitude;
         this.sideOffset = sideOffset;
         this.sideYOffset = sideYOffset;
         this.sideZOffset = sideZOffset;
         this.sideRoll = sideRoll;
         this.sidePitch = sidePitch;
         this.flapSpeed = flapSpeed;
      }
   }
}
