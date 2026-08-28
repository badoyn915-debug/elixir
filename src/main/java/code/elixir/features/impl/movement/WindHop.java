package code.elixir.features.impl.movement;

import code.elixir.events.keyboard.KeyEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.features.rotate.RotationUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class WindHop extends Module {
   private static final double WALL_CHECK_DISTANCE = 2.5;
   private static final double MIN_UPWARD_VELOCITY = 0.05;
   private final BindSetting bind = new BindSetting("Бинд", "Использовать заряд ветра по бинду");
   private boolean bindQueued;

   public WindHop() {
      super("WindHop", "WindHop", ModuleCategory.MOVEMENT);
      this.setup(new Setting[]{this.bind});
   }

   public static WindHop getInstance() {
      return Instance.get(WindHop.class);
   }

   @Override
   public void deactivate() {
      this.bindQueued = false;
      super.deactivate();
   }

   @EventHandler
   public void onKey(KeyEvent event) {
      if (event.isKeyDown(this.bind.getKey())) {
         this.bindQueued = true;
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null && mc.interactionManager != null && this.bindQueued) {
         this.bindQueued = false;
         this.useWindCharge();
      }
   }

   private void useWindCharge() {
      int slot = this.findWindChargeSlot();
      if (slot != -1 && mc.getNetworkHandler() != null) {
         boolean offhand = slot == 40;
         boolean hotbar = slot < 9;
         int previousSlot = mc.player.getInventory().selectedSlot;
         Hand hand = offhand ? Hand.OFF_HAND : Hand.MAIN_HAND;
         float yaw = mc.player.getYaw();
         float pitch = this.resolvePitch(yaw);
         if (hotbar && !offhand && slot != previousSlot) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
         }

         mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(hand, 0, yaw, pitch));
         mc.player.swingHand(hand);
         if (hotbar && !offhand && slot != previousSlot) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
         }
      }
   }

   private float resolvePitch(float yaw) {
      if (!mc.player.isOnGround() && this.blockBelowOffset(0.0, -0.6, 0.0) == Blocks.AIR && mc.player.getVelocity().y > 0.05) {
         for (int angle = 0; angle < 360; angle += 45) {
            if (this.raycastWall(yaw, angle, 2.5F)) {
               return normalizeYaw(yaw + angle);
            }
         }
      }

      return 90.0F;
   }

   private boolean raycastWall(float yaw, float angleOffset, float distance) {
      Vec3d eye = mc.player.getEyePos();
      BlockHitResult hit = mc.world
         .raycast(
            new RaycastContext(
               eye,
               eye.add(RotationUtils.getRotationVector(normalizeYaw(yaw + angleOffset), 0.0F).multiply(distance)),
               ShapeType.COLLIDER,
               FluidHandling.NONE,
               mc.player
            )
         );
      return hit.getType() == Type.BLOCK;
   }

   private Block blockBelowOffset(double x, double y, double z) {
      if (mc.player != null && mc.world != null) {
         BlockPos pos = BlockPos.ofFloored(mc.player.getPos().add(x, y, z));
         return mc.world.getBlockState(pos).getBlock();
      } else {
         return Blocks.AIR;
      }
   }

   private int findWindChargeSlot() {
      if (mc.player == null) {
         return -1;
      } else if (mc.player.getOffHandStack().isOf(Items.WIND_CHARGE)) {
         return 40;
      } else {
         for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.WIND_CHARGE)) {
               return i;
            }
         }

         return -1;
      }
   }

   private static float normalizeYaw(float yaw) {
      while (yaw > 180.0F) {
         yaw -= 360.0F;
      }

      while (yaw < -180.0F) {
         yaw += 360.0F;
      }

      return yaw;
   }
}
