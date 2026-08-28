package code.elixir.features.impl.player;

import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.features.aura.warp.Turns;
import code.elixir.utils.features.aura.warp.TurnsConfig;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import code.elixir.utils.math.task.TaskPriority;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.util.math.MathHelper;

public class AutoPilot extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   public ItemEntity target;
   private float lastYaw;
   private float lastPitch;
   private float targetYaw;
   private float targetPitch;
   Turns rot = new Turns(0.0F, 0.0F);

   public AutoPilot() {
      super("AutoPilot", "AutoPilot", ModuleCategory.MISC);
   }

   public static AutoPilot getInstance() {
      return Instance.get(AutoPilot.class);
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null && mc.getNetworkHandler() != null) {
         this.target = this.findTarget();
         if (this.target != null) {
            double dx = this.target.getPos().getX() - mc.player.getPos().getX();
            double dy = this.target.getPos().getY() - (mc.player.getPos().getY() + mc.player.getEyeHeight(mc.player.getPose()));
            double dz = this.target.getPos().getZ() - mc.player.getPos().getZ();
            this.targetYaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI - 90.0);
            this.targetPitch = (float)(-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180.0 / Math.PI);
            float maxRotation = 1024.0F;
            float yawDiff = MathHelper.wrapDegrees(this.targetYaw - this.lastYaw);
            float yawStep = MathHelper.clamp(yawDiff, -maxRotation, maxRotation);
            this.lastYaw += yawStep;
            float pitchDiff = MathHelper.wrapDegrees(this.targetPitch - this.lastPitch);
            float pitchStep = MathHelper.clamp(pitchDiff, -maxRotation, maxRotation);
            this.lastPitch += pitchStep;
            mc.player.setYaw(this.lastYaw);
            mc.player.setPitch(this.lastPitch);
            this.rot.setYaw(this.lastYaw);
            this.rot.setPitch(this.lastPitch);
            TurnsConnection.INSTANCE.rotateTo(this.rot, TurnsConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_1, this);
         } else {
            this.lastYaw = mc.player.getYaw();
            this.lastPitch = mc.player.getPitch();
         }
      } else {
         this.target = null;
      }
   }

   private ItemEntity findTarget() {
      List items = mc.world
         .getEntitiesByClass(ItemEntity.class, mc.player.getBoundingBox().expand(50.0), e -> e.isAlive() && this.isValidItem(e))
         .stream()
         .sorted(Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e)))
         .collect(Collectors.toList());
      return items.isEmpty() ? null : (ItemEntity)items.get(0);
   }

   private boolean isValidItem(ItemEntity item) {
      ItemStack stack = item.getStack();
      return stack.getItem() == Items.SPAWNER
         || stack.getItem() == Items.PLAYER_HEAD
         || stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE
         || stack.getItem().toString().contains("_spawn_egg");
   }

   public void onDisable() {
      this.target = null;
      if (mc.player != null) {
         mc.getNetworkHandler()
            .sendPacket(
               new Full(
                  mc.player.getPos().getX(),
                  mc.player.getPos().getY(),
                  mc.player.getPos().getZ(),
                  mc.player.getYaw(),
                  mc.player.getPitch(),
                  mc.player.isOnGround(),
                  false
               )
            );
      }
   }
}
