package code.elixir.features.impl.combat;

import code.elixir.events.player.AttackEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.ArmadilloEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

public class PacketCriticals extends Module {
   private static final double OLD_HOLYWORLD_OFFSET = 1.0E-10;
   private static final double KRYSTAL_OFFSET = 0.0625;
   private static final float GRIM_FALL_MIN = 0.08F;
   private static final float GRIM_FALL_RANGE = 0.25F;
   private final SelectSetting mode = new SelectSetting("Режим", "Режим пакетных критов").value("Post", "Old Holyworld", "KrystalMC", "Grim").selected("Post");

   public PacketCriticals() {
      super("PacketCriticals", "Packet Criticals", ModuleCategory.COMBAT);
      this.setup(new Setting[]{this.mode});
   }

   public static PacketCriticals getInstance() {
      return Instance.get(PacketCriticals.class);
   }

   public boolean isPostMode() {
      return this.mode.isSelected("Post");
   }

   public boolean isOldHolyworldMode() {
      return this.mode.isSelected("Old Holyworld");
   }

   public boolean isKrystalMode() {
      return this.mode.isSelected("KrystalMC");
   }

   public boolean isGrimMode() {
      return this.mode.isSelected("Grim");
   }

   public boolean allowsPacketCritState() {
      return this.isState() && mc.player != null && (mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING) || this.isInCobweb());
   }

   public boolean shouldDeferMainTickAttack() {
      return this.isState()
         && this.isPostMode()
         && mc.player != null
         && mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)
         && !mc.player.isSubmergedInWater();
   }

   public boolean shouldAttackOnPostTick() {
      return this.isState()
         && this.isGrimMode()
         && mc.player != null
         && mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)
         && !mc.player.isSubmergedInWater()
         && mc.player.fallDistance > 0.0F
         && mc.player.fallDistance < 1.0F;
   }

   @EventHandler
   public void onAttack(AttackEvent event) {
      if (this.isState() && mc.player != null && mc.getNetworkHandler() != null) {
         if (this.isOldHolyworldMode()) {
            this.sendOffsetPacket(1.0E-10);
         } else if (this.isKrystalMode()) {
            if (this.isInCobweb() || mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
               this.sendOffsetPacket(0.0625);
            }
         } else {
            if (this.isGrimMode()) {
               Entity target = event.getEntity();
               if (target == null || target instanceof ArmadilloEntity) {
                  return;
               }

               this.sendGrimCrit();
            }
         }
      }
   }

   private void sendOffsetPacket(double offset) {
      mc.getNetworkHandler().sendPacket(new PositionAndOnGround(mc.player.getX(), mc.player.getY() - offset, mc.player.getZ(), false, false));
   }

   private void sendGrimCrit() {
      LivingEntity auraTarget = AttackAura.getInstance() != null && AttackAura.getInstance().isState() ? AttackAura.getInstance().getTarget() : null;
      LivingEntity triggerTarget = TriggerBot.getInstance() != null && TriggerBot.getInstance().isState() ? TriggerBot.getInstance().getTarget() : null;
      if (auraTarget != null || triggerTarget != null) {
         if (!mc.player.isOnGround()) {
            double y = mc.player.getY();
            if (y != (int)y) {
               if (this.isInCobweb() || mc.player.isTouchingWater()) {
                  float fall;
                  mc.player.fallDistance = fall = ThreadLocalRandom.current().nextFloat() * 0.25F + 0.08F;
                  mc.getNetworkHandler()
                     .sendPacket(new Full(mc.player.getX(), mc.player.getY() - fall, mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), false, false));
               }
            }
         }
      }
   }

   private boolean isInCobweb() {
      if (mc.player != null && mc.world != null) {
         Box box = mc.player.getBoundingBox();

         for (BlockPos pos : BlockPos.iterate(
            MathHelper.floor(box.minX),
            MathHelper.floor(box.minY),
            MathHelper.floor(box.minZ),
            MathHelper.floor(box.maxX),
            MathHelper.floor(box.maxY),
            MathHelper.floor(box.maxZ)
         )) {
            if (mc.world.getBlockState(pos).isOf(Blocks.COBWEB)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }
}
