package code.elixir.features.impl.movement;

import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.impl.combat.AttackAura;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.RadioSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.DecompilerSupport;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.chat.ChatMessage;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.interactions.item.ItemTask;
import code.elixir.utils.math.time.StopWatch;
import java.util.Objects;
import java.util.Random;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class ElytraMotion extends Module {
   private StopWatch timer = new StopWatch();
   private Vec3d targetPosition = null;
   private Random random = new Random();
   private double rotationAngle = 0.0;
   private final BooleanSetting auto = new BooleanSetting("Авто-фейр", "Автоматически использует фейрверк");
   private final RadioSetting mode = new RadioSetting("Режим", "Режим использования фейерверка", new String[]{"Легитно", "Пакетно"}, "Легитно")
      .visible(() -> this.auto.isValue());
   private final SliderSettings timer2 = new SliderSettings("Скорость исп.", "")
      .setValue(500.0F)
      .range(0.0F, 10000.0F)
      .step(10.0F)
      .visible(() -> this.auto.isValue());

   public static ElytraMotion getInstance() {
      return Instance.get(ElytraMotion.class);
   }

   public ElytraMotion() {
      super("ElytraMotion", "ElytraMotion", ModuleCategory.MOVEMENT);
      this.setup(new Setting[]{this.auto, this.mode, this.timer2});
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (this.state && mc.player != null && mc.world != null && mc.player.isGliding()) {
         AttackAura aura = Instance.get(AttackAura.class);
         if (this.auto.isValue() && this.timer.every(this.timer2.getValue())) {
            this.useFirework();
            this.timer.reset();
         }

         if (aura.isState() && aura.isState() && aura.getTarget() != null && mc.player.distanceTo(aura.getTarget()) < aura.attackRange.getValue() - 0.425F) {
            mc.player.setVelocity(0.0, 0.02, 0.0);
         }
      }
   }

   private void useFirework() {
      if ("Пакетно".equals(this.mode.getValue())) {
         this.useFireworkPacket();
      } else {
         this.useFireworkLegit();
      }
   }

   private void useFireworkLegit() {
      if (!mc.player.isUsingItem() || mc.player.getActiveHand() != Hand.MAIN_HAND) {
         if (mc.player.getMainHandStack().isOf(Items.FIREWORK_ROCKET)) {
            PlayerInteractionHelper.interactItem(Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
         } else if (mc.player.getOffHandStack().isOf(Items.FIREWORK_ROCKET)) {
            PlayerInteractionHelper.interactItem(Hand.OFF_HAND);
            mc.player.swingHand(Hand.OFF_HAND);
         } else {
            int hotbarSlot = InventoryTask.findHotbarSlot(Items.FIREWORK_ROCKET);
            if (hotbarSlot != -1) {
               InventoryTask.switchTo(hotbarSlot);
               PlayerInteractionHelper.interactItem(Hand.MAIN_HAND);
               mc.player.swingHand(Hand.MAIN_HAND);
            } else {
               Slot slot = InventoryTask.getSlot(Items.FIREWORK_ROCKET);
               if (slot != null) {
                  InventoryTask.swapHand(slot, Hand.OFF_HAND, false);
                  PlayerInteractionHelper.interactItem(Hand.OFF_HAND);
                  mc.player.swingHand(Hand.OFF_HAND);
               }
            }
         }
      }
   }

   private void useFireworkPacket() {
      if (mc.getNetworkHandler() != null) {
         if (!(ItemTask.getCooldownProgress(Items.FIREWORK_ROCKET) > 0.0F)) {
            if (mc.player.getMainHandStack().isOf(Items.FIREWORK_ROCKET)) {
               PlayerInteractionHelper.sendSequencedPacket(
                  seq -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, seq, mc.player.getYaw(), mc.player.getPitch())
               );
            } else if (mc.player.getOffHandStack().isOf(Items.FIREWORK_ROCKET)) {
               PlayerInteractionHelper.sendSequencedPacket(seq -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, seq, mc.player.getYaw(), mc.player.getPitch()));
            } else {
               int rocketHotbarSlot = InventoryTask.findHotbarSlot(Items.FIREWORK_ROCKET);
               if (rocketHotbarSlot != -1) {
                  int previousSlot = mc.player.getInventory().selectedSlot;
                  if (rocketHotbarSlot != previousSlot) {
                     mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(rocketHotbarSlot));
                  }

                  PlayerInteractionHelper.sendSequencedPacket(
                     seq -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, seq, mc.player.getYaw(), mc.player.getPitch())
                  );
                  if (rocketHotbarSlot != previousSlot) {
                     mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
                  }
               } else {
                  Slot slot = InventoryTask.getSlot(Items.FIREWORK_ROCKET);
                  if (slot == null) {
                     ChatMessage.brandmessage("§cНет фейерверка в инвентаре");
                  } else {
                     InventoryTask.swapHand(slot, Hand.OFF_HAND, false);
                     PlayerInteractionHelper.sendSequencedPacket(
                        seq -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, seq, mc.player.getYaw(), mc.player.getPitch())
                     );
                     InventoryTask.swapHand(slot, Hand.OFF_HAND, false, true);
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
   }

   @Override
   public void deactivate() {
      super.deactivate();
   }

   public StopWatch getTimer() {
      return this.timer;
   }

   public Vec3d getTargetPosition() {
      return this.targetPosition;
   }

   public Random getRandom() {
      return this.random;
   }

   public double getRotationAngle() {
      return this.rotationAngle;
   }

   public BooleanSetting getAuto() {
      return this.auto;
   }

   public RadioSetting getMode() {
      return this.mode;
   }

   public SliderSettings getTimer2() {
      return this.timer2;
   }
}
