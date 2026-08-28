package code.elixir.utils.features.aura.striking;

import code.elixir.events.item.UsingItemEvent;
import code.elixir.events.packet.PacketEvent;
import code.elixir.features.impl.combat.AttackAura;
import code.elixir.features.impl.combat.PacketCriticals;
import code.elixir.features.impl.combat.TriggerBot;
import code.elixir.mixins.ClientPlayerEntityAccessor;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.aura.utils.MathAngle;
import code.elixir.utils.features.aura.utils.Pressing;
import code.elixir.utils.features.aura.utils.RaycastAngle;
import code.elixir.utils.features.aura.warp.Turns;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import code.elixir.utils.features.combat.LegitSprintReset;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.interactions.inv.InventoryFlowManager;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.interactions.item.ItemUseHelper;
import code.elixir.utils.interactions.simulate.PlayerSimulation;
import code.elixir.utils.math.time.StopWatch;
import java.util.function.Predicate;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;

public class StrikeManager implements QuickImports {
   private final StopWatch attackTimer = new StopWatch();
   private final StopWatch shieldWatch = new StopWatch();
   private final Pressing clickScheduler = new Pressing();
   private int count = 0;
   private Mode lastSprintCommand = null;

   void onPacket(PacketEvent event) {
      Packet<?> packet = event.getPacket();
      if (packet instanceof HandSwingC2SPacket || packet instanceof UpdateSelectedSlotC2SPacket) {
         this.clickScheduler.recalculate();
      }
   }

   void onUsingItem(UsingItemEvent event) {
      if (event.getType() == -1 && mc.player != null) {
         if (this.shieldWatch.finished(50.0)) {
            ItemStack useStack = event.getHand() != null ? ItemUseHelper.getStackForHand(mc.player, event.getHand()) : mc.player.getMainHandStack();
            if (!ItemUseHelper.isPriorityUseItem(useStack)) {
               if (!ItemUseHelper.isPriorityUseItem(mc.player.getMainHandStack()) && !ItemUseHelper.isPriorityUseItem(mc.player.getOffHandStack())) {
                  if (!ItemUseHelper.isUsingPriorityItem(mc.player)) {
                     if (this.shouldBlockItemUseStart()) {
                        event.cancel();
                     }
                  }
               }
            }
         }
      }
   }

   private boolean shouldBlockItemUseStart() {
      AttackAura aura = AttackAura.getInstance();
      if (aura != null && aura.isState() && aura.getTarget() != null && aura.getTarget().isAlive()) {
         return true;
      } else {
         TriggerBot triggerBot = TriggerBot.getInstance();
         return triggerBot != null && triggerBot.isState() && triggerBot.getTarget() != null && triggerBot.getTarget().isAlive();
      }
   }

   void handleAttack(StrikerConstructor.AttackPerpetratorConfigurable config) {
      if (RaycastAngle.rayTrace(config) && this.canAttack(config, 1)) {
         String sprintMode = this.resolveSprintMode(config);
         if (sprintMode.equals("Легитно")) {
            if (LegitSprintReset.prepareForAttack()) {
               this.preAttackEntity(config);
               this.attackEntity(config);
            }
         } else {
            if (this.canAttack(config, 1)) {
               this.preAttackEntity(config);
            }

            if (sprintMode.equals("Пакетно")) {
               ClientPlayerEntityAccessor accessor = (ClientPlayerEntityAccessor)mc.player;
               boolean wasSprinting = accessor.getLastSprinting();
               if (wasSprinting && !mc.player.isSubmergedInWater() && !mc.player.isTouchingWater() && this.lastSprintCommand != Mode.STOP_SPRINTING) {
                  mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
                  this.lastSprintCommand = Mode.STOP_SPRINTING;
                  mc.player.setSprinting(false);
               }

               this.attackEntity(config);
               AttackAura aura;
               if (wasSprinting
                  && !mc.player.isSubmergedInWater()
                  && !mc.player.isTouchingWater()
                  && this.lastSprintCommand != Mode.START_SPRINTING
                  && (aura = AttackAura.getInstance()) != null
                  && aura.canRestartSprintAfterAttack()) {
                  mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_SPRINTING));
                  this.lastSprintCommand = Mode.START_SPRINTING;
                  mc.player.setSprinting(true);
               }
            }
         }
      }
   }

   private String resolveSprintMode(StrikerConstructor.AttackPerpetratorConfigurable config) {
      if (config.getSprintResetOverride() != null) {
         return config.getSprintResetOverride();
      } else {
         AttackAura aura = AttackAura.getInstance();
         if (aura != null && aura.isState()) {
            String var3 = aura.getSprintReset().get();

            return switch (var3) {
               case "Пакетно" -> "Пакетно";
               case "Легитно" -> "Легитно";
               default -> "Пакетно";
            };
         } else {
            return "Легитно";
         }
      }
   }

   void preAttackEntity(StrikerConstructor.AttackPerpetratorConfigurable config) {
      if (config.isShouldUnPressShield() && mc.player.isUsingItem() && mc.player.getActiveItem().isOf(Items.SHIELD)) {
         mc.interactionManager.stopUsingItem(mc.player);
         this.shieldWatch.reset();
      }
   }

   void attackEntity(StrikerConstructor.AttackPerpetratorConfigurable config) {
      this.attack(config);
      this.breakShield(config);
      this.attackTimer.reset();
      this.count++;
   }

   private void breakShield(StrikerConstructor.AttackPerpetratorConfigurable config) {
      LivingEntity target = config.getTarget();
      Turns angleToPlayer = MathAngle.fromVec3d(mc.player.getBoundingBox().getCenter().subtract(target.getEyePos()));
      boolean targetOnShield = target.isUsingItem() && target.getActiveItem().isOf(Items.SHIELD);
      boolean angle = Math.abs(TurnsConnection.computeAngleDifference(target.getYaw(), angleToPlayer.getYaw())) < 90.0F;
      Slot axe = InventoryTask.getSlot((Predicate<Slot>)(slot -> slot.getStack().getItem() instanceof AxeItem));
      if (config.isShouldBreakShield() && targetOnShield && axe != null && angle && InventoryFlowManager.script.isFinished()) {
         InventoryTask.swapHand(axe, Hand.MAIN_HAND, false);
         InventoryTask.closeScreen(true);
         this.attack(config);
         InventoryTask.swapHand(axe, Hand.MAIN_HAND, false, true);
         InventoryTask.closeScreen(true);
      }
   }

   private void attack(StrikerConstructor.AttackPerpetratorConfigurable config) {
      mc.interactionManager.attackEntity(mc.player, config.getTarget());
      mc.player.swingHand(Hand.MAIN_HAND);
   }

   public boolean canAttack(StrikerConstructor.AttackPerpetratorConfigurable config, int ticks) {
      for (int i = 0; i <= ticks; i++) {
         if (this.canCrit(config, i)) {
            return true;
         }
      }

      return false;
   }

   public boolean canCrit(StrikerConstructor.AttackPerpetratorConfigurable config, int ticks) {
      if (ItemUseHelper.isUsingPriorityItem(mc.player) && config.isEatAndAttack()) {
         return false;
      } else {
         AttackAura aura = AttackAura.getInstance();
         boolean useTpsSync = aura != null && aura.isState() && aura.isSyncTpsEnabled();
         if (!this.clickScheduler.isCooldownComplete(useTpsSync, 1)) {
            return false;
         } else {
            PlayerSimulation simulated = PlayerSimulation.simulateLocalPlayer(ticks);
            boolean noRestrict = !this.hasMovementRestrictions(simulated);
            boolean critState = this.isPlayerInCriticalState(config, simulated, ticks);
            if (config.isOnlyCritical() && noRestrict) {
               return critState;
            } else {
               return config.isOnlyCritical() && !noRestrict ? true : true;
            }
         }
      }
   }

   private boolean hasMovementRestrictions(PlayerSimulation simulated) {
      return simulated.hasStatusEffect(StatusEffects.BLINDNESS)
         || simulated.hasStatusEffect(StatusEffects.LEVITATION)
         || PlayerInteractionHelper.isBoxInBlock(simulated.boundingBox.expand(-0.001), Blocks.COBWEB)
         || simulated.isSubmergedInWater()
         || simulated.isInLava()
         || simulated.isClimbing()
         || !PlayerInteractionHelper.canChangeIntoPose(EntityPose.STANDING, simulated.pos)
         || simulated.player.getAbilities().flying;
   }

   private boolean isPlayerInCriticalState(StrikerConstructor.AttackPerpetratorConfigurable config, PlayerSimulation simulated, int ticks) {
      PacketCriticals packetCriticals = PacketCriticals.getInstance();
      if (packetCriticals != null && packetCriticals.isState() && PlayerInteractionHelper.isBoxInBlock(simulated.boundingBox.expand(-0.001), Blocks.COBWEB)) {
         return !simulated.onGround || simulated.fallDistance > 0.001F;
      } else {
         boolean fall = simulated.fallDistance > 0.0F;
         boolean fallingDown = simulated.velocity.y < 0.0;
         return !simulated.onGround && fall && fallingDown;
      }
   }

   public void setCount(int count) {
      this.count = count;
   }

   public void setLastSprintCommand(Mode lastSprintCommand) {
      this.lastSprintCommand = lastSprintCommand;
   }

   public StopWatch getAttackTimer() {
      return this.attackTimer;
   }

   public StopWatch getShieldWatch() {
      return this.shieldWatch;
   }

   public Pressing getClickScheduler() {
      return this.clickScheduler;
   }

   public int getCount() {
      return this.count;
   }

   public Mode getLastSprintCommand() {
      return this.lastSprintCommand;
   }
}
