package code.elixir.features.impl.movement;

import code.elixir.events.player.InputEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.interactions.inv.InventoryTask;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.util.math.Vec3d;

public class ElytraJump extends Module {
   private static final float PITCH_CLAMP_MIN = -90.0F;
   private static final float PITCH_CLAMP_MAX = 90.0F;
   private static final double VERTICAL_BOOST = 0.08;
   private static final double MIN_FALL = 0.1;
   private static final float MIN_FALL_DISTANCE = 0.5F;
   private static final double HORIZONTAL_EPSILON = 0.01;
   private final BooleanSetting autoSwap = new BooleanSetting("Авто-свап элитры", "Надевает элитру при отключении модуля").setValue(false);

   public ElytraJump() {
      super("ElytraJump", "Elytra Jump", ModuleCategory.MOVEMENT);
      this.setup(new Setting[]{this.autoSwap});
   }

   private boolean wearingElytra() {
      return mc.player != null && mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (this.state && !PlayerInteractionHelper.nullCheck()) {
         if (mc.player.isGliding()) {
            float pitch = mc.player.getPitch();
            if (pitch < -90.0F || pitch > 90.0F) {
               mc.player.setPitch(0.0F);
            }

            mc.player.headYaw = mc.player.getYaw();
            mc.player.bodyYaw = mc.player.getYaw();
            mc.player.prevHeadYaw = mc.player.getYaw();
            mc.player.prevBodyYaw = mc.player.getYaw();
         }

         if (!mc.player.getAbilities().flying
            && mc.player.isOnGround()
            && !mc.player.isSubmergedInWater()
            && !mc.player.isInLava()
            && this.wearingElytra()
            && !mc.options.jumpKey.isPressed()) {
            mc.player.jump();
         }

         if (!mc.player.getAbilities().flying
            && !mc.player.isOnGround()
            && !mc.player.isSubmergedInWater()
            && !mc.player.isInLava()
            && !mc.player.isGliding()
            && this.canStartGlide()) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_FALL_FLYING));
            mc.player.startGliding();
         }

         if (mc.player.isOnGround() || mc.player.isSubmergedInWater() || mc.player.isInLava()) {
            mc.options.jumpKey.setPressed(false);
         }

         if (mc.player.hurtTime > 0 && this.autoSwap.isValue() && this.wearingElytra()) {
            this.swapElytraFromInventory();
         } else if (this.wearingElytra()) {
            mc.options.jumpKey.setPressed(true);
            if (mc.player.isGliding()) {
               this.applyVerticalBoost();
            }
         } else if (this.autoSwap.isValue()) {
            this.equipElytra();
         } else {
            mc.options.jumpKey.setPressed(false);
         }
      }
   }

   @EventHandler
   public void onInput(InputEvent event) {
      if (this.state && mc.player != null && mc.player.isGliding()) {
         event.setForward(0.0F);
         event.setStrafe(0.0F);
      }
   }

   private void applyVerticalBoost() {
      Vec3d velocity = mc.player.getVelocity();
      boolean fallingFast = velocity.y > 0.1 || mc.player.fallDistance > 0.5F;
      boolean horizontalStill = Math.abs(velocity.x) <= 0.01 && Math.abs(velocity.z) <= 0.01;
      if (fallingFast && horizontalStill) {
         mc.player.setVelocity(0.0, velocity.y + 0.08, 0.0);
      }
   }

   private boolean canStartGlide() {
      if (!this.wearingElytra()) {
         return false;
      } else {
         ItemStack chest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
         return !mc.player.isSubmergedInWater()
            && !mc.player.isInLava()
            && !mc.player.hasVehicle()
            && chest.isOf(Items.ELYTRA)
            && chest.getDamage() < chest.getMaxDamage() - 1;
      }
   }

   private void equipElytra() {
      int slot = InventoryTask.findInventorySlot(Items.ELYTRA);
      if (slot != -1) {
         this.swapChest(slot);
      }
   }

   private void swapElytraFromInventory() {
      int slot = InventoryTask.findInventorySlot(Items.ELYTRA);
      if (slot != -1) {
         this.swapChest(slot);
      }
   }

   private void swapChest(int slot) {
      InventoryTask.moveItem(slot, 6, true, true);
      if (mc.player.networkHandler != null) {
         mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
      }
   }

   @Override
   public void deactivate() {
      if (mc.player != null) {
         mc.options.jumpKey.setPressed(false);
         if (this.autoSwap.isValue() && this.wearingElytra()) {
            this.swapElytraFromInventory();
         }
      }

      super.deactivate();
   }
}
