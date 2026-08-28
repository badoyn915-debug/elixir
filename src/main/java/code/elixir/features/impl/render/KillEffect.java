package code.elixir.features.impl.render;

import code.elixir.events.player.EntityDeathEvent;
import code.elixir.events.render.WorldRenderEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.sound.SoundManager;
import code.elixir.utils.display.render.geometry.Render3D;
import com.mojang.authlib.GameProfile;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class KillEffect extends Module {
   private final SliderSettings volume = new SliderSettings("Volume", "Volume").setValue(100.0F).range(0, 100);
   private final BooleanSetting playSound = new BooleanSetting("Play Sound", "Play Sound").setValue(true);
   private final BooleanSetting mobs = new BooleanSetting("Mobs", "Mobs").setValue(false);
   private final SelectSetting effectType = new SelectSetting("Effect Type", "Effect Type").value("Cross", "Soul").selected("Soul");
   private final SelectSetting soundType = new SelectSetting("Sound Type", "Sound Type")
      .value("Orthodox", "Kill1", "Kill2", "Kill3", "Kill4", "Kill5", "Kill6", "Random")
      .selected("Orthodox");
   private final Map<Entity, KillEffect.EntityRenderData> renderEntities = new ConcurrentHashMap<>();
   private final Random random = new Random();

   public KillEffect() {
      super("KillEffect", "Kill Effect", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.volume, this.playSound, this.mobs, this.effectType, this.soundType});
   }

   @Override
   public void deactivate() {
      this.renderEntities.clear();
      super.deactivate();
   }

   @EventHandler
   public void onEntityDeath(EntityDeathEvent event) {
      if (mc.world != null && mc.player != null) {
         Entity entity = event.getEntity();
         if (entity instanceof LivingEntity) {
            if (this.mobs.isValue() || entity instanceof PlayerEntity) {
               if (entity != mc.player && !this.renderEntities.containsKey(entity)) {
                  if (this.playSound.isValue()) {
                     SoundEvent soundToPlay = this.getSelectedSound();
                     mc.world.playSound(mc.player, entity.getBlockPos(), soundToPlay, SoundCategory.BLOCKS, this.volume.getValue() / 100.0F, 1.0F);
                  }

                  OtherClientPlayerEntity fakePlayer = null;
                  if (this.effectType.isSelected("Soul") && entity instanceof PlayerEntity) {
                     fakePlayer = new OtherClientPlayerEntity(mc.world, ((PlayerEntity)entity).getGameProfile());
                     fakePlayer.setPitch(-30.0F);
                     fakePlayer.setYaw(entity.getYaw());
                     fakePlayer.headYaw = entity.getYaw();
                     fakePlayer.bodyYaw = entity.getYaw();
                     fakePlayer.setCustomNameVisible(false);
                     fakePlayer.setCustomName(Text.literal("Ghost_" + ((PlayerEntity)entity).getGameProfile().getId()));
                     mc.world.addEntity(fakePlayer);
                  }

                  this.renderEntities
                     .put(entity, new KillEffect.EntityRenderData(System.currentTimeMillis(), entity.getYaw(), entity.getPos(), entity, fakePlayer));
               }
            }
         }
      }
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      if (mc.world != null && mc.player != null) {
         MatrixStack stack = e.getStack();
         float tickDelta = e.getPartialTicks();
         ArrayList entitiesToRemove = new ArrayList();
         this.renderEntities.forEach((entity, data) -> {
            if (System.currentTimeMillis() - data.getTimestamp() > 3000L) {
               entitiesToRemove.add(entity);
               if (data.getFakePlayer() != null) {
                  mc.world.removeEntity(data.getFakePlayer().getId(), RemovalReason.DISCARDED);
               }
            } else {
               float timeProgress = (float)(System.currentTimeMillis() - data.getTimestamp()) / 3000.0F;
               if (this.effectType.isSelected("Cross")) {
                  int color = new Color(255, 255, 255, (int)(150.0F * (1.0F - timeProgress))).getRGB();
                  float yaw = (float)Math.toRadians(data.getYaw() + 95.0F);
                  Vec3d pos = data.getStartPos();
                  Render3D.drawLine(pos.add(0.0, 0.0, 0.0), pos.add(0.0, 3.0, 0.0), color, 5.0F, true);
                  float armLength = 1.0F;
                  float yOffset = 2.3F;
                  Vec3d start = pos.add(-armLength * Math.sin(yaw), yOffset, armLength * Math.cos(yaw));
                  Vec3d end = pos.add(armLength * Math.sin(yaw), yOffset, -armLength * Math.cos(yaw));
                  Render3D.drawLine(start, end, color, 5.0F, true);
               } else if (this.effectType.isSelected("Soul")) {
                  float yOffset = timeProgress * 3.0F;
                  int alpha = (int)(255.0F * (1.0F - timeProgress));
                  Vec3d soulPos = data.getStartPos().add(0.0, yOffset, 0.0);
                  Entity renderEntity = data.getEntity();
                  if (data.getFakePlayer() != null) {
                     renderEntity = data.getFakePlayer();
                     renderEntity.setPos(soulPos.x, soulPos.y, soulPos.z);
                  }

                  Render3D.drawEntity(renderEntity, soulPos, data.getYaw(), alpha, stack, tickDelta);
               }
            }
         });
         entitiesToRemove.forEach(this.renderEntities::remove);
      }
   }

   private SoundEvent getSelectedSound() {
      String var1 = this.soundType.getSelected();
      switch (var1) {
         case "Kill1":
            return SoundManager.KILL1;
         case "Kill2":
            return SoundManager.KILL2;
         case "Kill3":
            return SoundManager.KILL3;
         case "Kill4":
            return SoundManager.KILL4;
         case "Kill5":
            return SoundManager.KILL5;
         case "Kill6":
            return SoundManager.KILL6;
         case "Random":
            SoundEvent[] killSounds = new SoundEvent[]{
               SoundManager.KILL1, SoundManager.KILL2, SoundManager.KILL3, SoundManager.KILL4, SoundManager.KILL5, SoundManager.KILL6
            };
            return killSounds[this.random.nextInt(killSounds.length)];
         default:
            return SoundManager.ORTHODOX;
      }
   }

   private static class EntityRenderData {
      private final long timestamp;
      private final float yaw;
      private final Vec3d startPos;
      private final Entity entity;
      private final GameProfile gameProfile;
      private final EntityPose pose;
      private final OtherClientPlayerEntity fakePlayer;

      public EntityRenderData(long timestamp, float yaw, Vec3d startPos, Entity entity, OtherClientPlayerEntity fakePlayer) {
         this.timestamp = timestamp;
         this.yaw = yaw;
         this.startPos = startPos;
         this.entity = entity;
         this.gameProfile = entity instanceof PlayerEntity ? ((PlayerEntity)entity).getGameProfile() : null;
         this.pose = entity.getPose();
         this.fakePlayer = fakePlayer;
      }

      public long getTimestamp() {
         return this.timestamp;
      }

      public float getYaw() {
         return this.yaw;
      }

      public Vec3d getStartPos() {
         return this.startPos;
      }

      public Entity getEntity() {
         return this.entity;
      }

      public GameProfile getGameProfile() {
         return this.gameProfile;
      }

      public EntityPose getPose() {
         return this.pose;
      }

      public OtherClientPlayerEntity getFakePlayer() {
         return this.fakePlayer;
      }
   }
}
