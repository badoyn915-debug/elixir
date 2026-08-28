package code.elixir.features.impl.render;

import code.elixir.events.player.AttackEvent;
import code.elixir.events.render.WorldRenderEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.mixins.LivingEntityRendererAccessor;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.geometry.Render2D;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public class HitMarker extends Module {
   private static final long DURATION = 1000L;
   private static final long CHAMS_SOUL_DURATION = 3000L;
   private static final int MAX_MARKERS = 24;
   private static final float WORLD_SCALE = 0.012F;
   private static final float SOUL_PITCH = 0.0F;
   private final SliderSettings sizeSetting = new SliderSettings("Размер", "Размер хит-маркера").setValue(12.0F).range(6, 30);
   private final BooleanSetting chams = new BooleanSetting("Chams", "Призрак в стиле Chams при ударе").setValue(false);
   private final ColorSetting chamsColor = new ColorSetting("Цвет Chams", "Цвет призрака Chams")
      .value(ColorAssist.getClientColor())
      .visible(() -> this.chams.isValue());
   private final SliderSettings chamsOpacity = new SliderSettings("Прозрачность Chams", "Прозрачность призрака Chams")
      .setValue(0.85F)
      .range(0.1F, 1.0F)
      .step(0.05F)
      .visible(() -> this.chams.isValue());
   private final SliderSettings chamsSpeed = new SliderSettings("Скорость Chams", "Скорость полёта призрака")
      .setValue(1.0F)
      .range(0.2F, 4.0F)
      .step(0.1F)
      .visible(() -> this.chams.isValue());
   private final MultiSelectSetting chamsDirection = new MultiSelectSetting("Направление Chams", "Куда летит призрак")
      .value("Вверх", "Вниз", "Влево", "Вправо")
      .selected("Вверх")
      .visible(() -> this.chams.isValue());
   private static final int MAX_CHAMS_SOULS = 32;
   private final List<HitMarker.Marker> markers = new ArrayList<>();
   private final List<HitMarker.ChamsSoul> chamsSouls = new ArrayList<>();
   private int directionCycleIndex;

   public static HitMarker getInstance() {
      return Instance.get(HitMarker.class);
   }

   public HitMarker() {
      super("Hit Marker", "Hit Marker", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.sizeSetting, this.chams, this.chamsColor, this.chamsOpacity, this.chamsSpeed, this.chamsDirection});
   }

   @Override
   public void deactivate() {
      this.markers.clear();
      this.chamsSouls.clear();
   }

   @EventHandler
   public void onAttack(AttackEvent e) {
      Entity entity = e.getEntity();
      LivingEntity living;
      if (entity instanceof LivingEntity && (living = (LivingEntity)entity) != mc.player && living.isAlive()) {
         HitResult class_2392 = mc.crosshairTarget;
         EntityHitResult result;
         Vec3d hitPos = class_2392 instanceof EntityHitResult && (result = (EntityHitResult)class_2392).getEntity() == entity
            ? result.getPos()
            : living.getBoundingBox().getCenter();
         this.markers.add(new HitMarker.Marker(hitPos, System.currentTimeMillis()));
         if (this.markers.size() > 24) {
            this.markers.remove(0);
         }

         if (this.chams.isValue() && entity instanceof PlayerEntity player) {
            this.spawnChamsSoul(player, living.getPos());
         }
      }
   }

   private void spawnChamsSoul(PlayerEntity player, Vec3d startPos) {
      ClientWorld class_6382 = mc.world;
      if (class_6382 instanceof ClientWorld && mc.player != null) {
         GameProfile profile = player.getGameProfile();
         OtherClientPlayerEntity fakePlayer = new OtherClientPlayerEntity(class_6382, profile);
         fakePlayer.copyFrom(player);
         fakePlayer.setPose(EntityPose.STANDING);
         fakePlayer.setPos(startPos.x, startPos.y, startPos.z);
         fakePlayer.setVelocity(0.0, 0.0, 0.0);
         fakePlayer.setInvisible(true);
         fakePlayer.noClip = true;
         float faceYaw = this.computeYawTowardsPlayer(startPos);
         freezeEntityPose(fakePlayer, faceYaw, faceYaw, 0.0F);
         Vec3d flyDirection = this.computeFlyDirection(startPos);
         this.chamsSouls.add(new HitMarker.ChamsSoul(fakePlayer, startPos, flyDirection, System.currentTimeMillis()));
         if (this.chamsSouls.size() > 32) {
            this.chamsSouls.remove(0);
         }
      }
   }

   private float computeYawTowardsPlayer(Vec3d from) {
      if (mc.player == null) {
         return 0.0F;
      } else {
         double dx = mc.player.getX() - from.x;
         double dz = mc.player.getZ() - from.z;
         return MathHelper.wrapDegrees((float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
      }
   }

   private Vec3d computeFlyDirection(Vec3d soulPos) {
      if (mc.player == null) {
         return new Vec3d(0.0, 1.0, 0.0);
      } else {
         Vec3d toVictim = soulPos.add(0.0, 0.9, 0.0).subtract(mc.player.getEyePos());
         Vec3d forward = new Vec3d(toVictim.x, 0.0, toVictim.z);
         if (forward.lengthSquared() < 1.0E-6) {
            double yawRad = Math.toRadians(mc.player.getYaw());
            forward = new Vec3d(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
         }

         forward = forward.normalize();
         Vec3d right = forward.crossProduct(new Vec3d(0.0, 1.0, 0.0)).normalize();
         Vec3d left = right.negate();
         ArrayList<Vec3d> options = new ArrayList<>();
         if (this.chamsDirection.isSelected("Вверх")) {
            options.add(new Vec3d(0.0, 1.0, 0.0));
         }

         if (this.chamsDirection.isSelected("Вниз")) {
            options.add(new Vec3d(0.0, -1.0, 0.0));
         }

         if (this.chamsDirection.isSelected("Влево")) {
            options.add(left);
         }

         if (this.chamsDirection.isSelected("Вправо")) {
            options.add(right);
         }

         if (options.isEmpty()) {
            return new Vec3d(0.0, 1.0, 0.0);
         } else {
            Vec3d direction = options.get(this.directionCycleIndex % options.size());
            this.directionCycleIndex++;
            return direction;
         }
      }
   }

   private static void freezeEntityPose(OtherClientPlayerEntity entity, float yaw, float bodyYaw, float pitch) {
      entity.setYaw(yaw);
      entity.prevYaw = yaw;
      entity.setPitch(pitch);
      entity.prevPitch = pitch;
      entity.headYaw = yaw;
      entity.prevHeadYaw = yaw;
      entity.bodyYaw = bodyYaw;
      entity.prevBodyYaw = bodyYaw;
      entity.lastRenderX = entity.getX();
      entity.lastRenderY = entity.getY();
      entity.lastRenderZ = entity.getZ();
      entity.prevX = entity.getX();
      entity.prevY = entity.getY();
      entity.prevZ = entity.getZ();
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      if (mc.player != null && mc.world != null && mc.gameRenderer != null) {
         long now = System.currentTimeMillis();
         Camera camera = mc.gameRenderer.getCamera();
         MatrixStack matrices = e.getStack();
         if (!this.markers.isEmpty()) {
            this.renderMarkers(now, camera, matrices);
         }

         if (!this.chamsSouls.isEmpty()) {
            this.renderChamsSouls(now, matrices);
         }
      }
   }

   private void renderMarkers(long now, Camera camera, MatrixStack matrices) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableDepthTest();
      RenderSystem.disableCull();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      Iterator<HitMarker.Marker> iterator = this.markers.iterator();

      while (iterator.hasNext()) {
         HitMarker.Marker marker = iterator.next();
         long elapsed = now - marker.time;
         if (elapsed > 1000L) {
            iterator.remove();
         } else {
            float progress = (float)elapsed / 1000.0F;
            float fade = 1.0F - progress;
            float scale = 1.0F + progress * 0.35F;
            float size = this.sizeSetting.getValue() * scale;
            float half = size / 2.0F;
            float gap = size * 0.18F;
            float thickness = Math.max(1.5F, size / 7.0F);
            int color = ColorAssist.multAlpha(ColorAssist.getHudColor(), fade);
            matrices.push();
            matrices.translate(marker.pos.x, marker.pos.y, marker.pos.z);
            matrices.multiply(camera.getRotation());
            matrices.scale(-0.012F, -0.012F, 0.012F);
            drawCross(matrices, 0.0F, 0.0F, half, gap, thickness, color);
            matrices.pop();
         }
      }

      RenderSystem.enableCull();
      RenderSystem.enableDepthTest();
      RenderSystem.disableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void renderChamsSouls(long now, MatrixStack matrices) {
      Chams chamsModule = Chams.getInstance();
      if (chamsModule != null) {
         Iterator<HitMarker.ChamsSoul> iterator = this.chamsSouls.iterator();

         while (iterator.hasNext()) {
            HitMarker.ChamsSoul soul = iterator.next();
            long elapsed = now - soul.startTime;
            if (elapsed > 3000L) {
               iterator.remove();
            } else {
               float progress = (float)elapsed / 3000.0F;
               float fade = 1.0F - progress;
               float travel = (float)elapsed / 1000.0F * this.chamsSpeed.getValue();
               Vec3d soulPos = soul.startPos.add(soul.flyDirection.multiply(travel));
               OtherClientPlayerEntity fakePlayer = soul.fakePlayer;
               fakePlayer.setPos(soulPos.x, soulPos.y, soulPos.z);
               float faceYaw = this.computeYawTowardsPlayer(soulPos);
               freezeEntityPose(fakePlayer, faceYaw, faceYaw, 0.0F);
               if (mc.getEntityRenderDispatcher().getRenderer(fakePlayer) instanceof PlayerEntityRenderer renderer) {
                  PlayerEntityModel model = (PlayerEntityModel)((LivingEntityRendererAccessor)renderer).elixir$getModel();
                  PlayerEntityRenderState state = (PlayerEntityRenderState)renderer.getAndUpdateRenderState(fakePlayer, 0.0F);
                  model.setAngles(state);
                  matrices.push();
                  matrices.translate(soulPos.x, soulPos.y, soulPos.z);
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - faceYaw));
                  matrices.scale(1.0F, -1.0F, 1.0F);
                  int rgb = this.chamsColor.getColor();
                  chamsModule.renderCustom(matrices, model, rgb, this.chamsOpacity.getValue() * fade);
                  matrices.pop();
               }
            }
         }
      }
   }

   private static void drawCross(MatrixStack matrix, float cx, float cy, float half, float gap, float thickness, int color) {
      drawArm(matrix, cx, cy, -1, -1, half, gap, thickness, color);
      drawArm(matrix, cx, cy, 1, -1, half, gap, thickness, color);
      drawArm(matrix, cx, cy, 1, 1, half, gap, thickness, color);
      drawArm(matrix, cx, cy, -1, 1, half, gap, thickness, color);
   }

   private static void drawArm(MatrixStack matrix, float cx, float cy, int sx, int sy, float half, float gap, float thickness, int color) {
      float nx = sx / (float)Math.sqrt(2.0);
      float ny = sy / (float)Math.sqrt(2.0);
      float x1 = cx + nx * gap;
      float y1 = cy + ny * gap;
      float x2 = cx + nx * half;
      float y2 = cy + ny * half;
      Render2D.drawLine(matrix, x1, y1, x2, y2, thickness, color);
   }

   private static final class ChamsSoul {
      final OtherClientPlayerEntity fakePlayer;
      final Vec3d startPos;
      final Vec3d flyDirection;
      final long startTime;

      ChamsSoul(OtherClientPlayerEntity fakePlayer, Vec3d startPos, Vec3d flyDirection, long startTime) {
         this.fakePlayer = fakePlayer;
         this.startPos = startPos;
         this.flyDirection = flyDirection;
         this.startTime = startTime;
      }
   }

   private record Marker(Vec3d pos, long time) {
   }
}
