package code.elixir.features.impl.combat;

import code.elixir.events.player.InputEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.interactions.simulate.ArrowSimulation;
import code.elixir.utils.interactions.simulate.PlayerSimulation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AutoDodge extends Module implements QuickImports {
   private final BooleanSetting allowRotation = new BooleanSetting("Разрешить поворот", "Разрешить изменение поворота для уклонения").setValue(false);
   private final BooleanSetting allowJump = new BooleanSetting("Разрешить прыжок", "Разрешить прыжок для уклонения")
      .setValue(true)
      .visible(() -> this.allowRotation.isValue());
   private final BooleanSetting allowTimer = new BooleanSetting("Использовать таймер", "Использовать таймер для ускорения уклонения").setValue(false);
   private final SliderSettings timerSpeed = new SliderSettings("Скорость таймера", "Скорость таймера при уклонении")
      .setValue(2.0F)
      .range(1.0F, 10.0F)
      .step(0.1F)
      .visible(() -> this.allowTimer.isValue());
   private final MultiSelectSetting ignore = new MultiSelectSetting("Игнорировать", "Условия, при которых модуль не работает")
      .value("Открытый инвентарь", "Использование предмета", "Скаффолд");
   private static final double SAFE_DISTANCE_WITH_PADDING = 0.7;
   private static final int MAX_SIMULATION_TICKS = 80;

   public AutoDodge() {
      super("AutoDodge", "AutoDodge", ModuleCategory.COMBAT);
      this.setup(new Setting[]{this.allowRotation, this.allowJump, this.allowTimer, this.timerSpeed, this.ignore});
   }

   @Override
   public boolean isState() {
      if (!super.isState()) {
         return false;
      } else if (PlayerInteractionHelper.nullCheck()) {
         return false;
      } else {
         return !this.ignore.isSelected("Открытый инвентарь") && mc.currentScreen instanceof InventoryScreen
            ? false
            : this.ignore.isSelected("Использование предмета") || !mc.player.isUsingItem();
      }
   }

   @EventHandler
   public void onInput(InputEvent e) {
      if (this.isState()) {
         List<Entity> arrows = this.findFlyingArrows();
         if (!arrows.isEmpty()) {
            PlayerSimulation simulatedPlayer = PlayerSimulation.fromClientPlayer(
               PlayerSimulation.SimulatedPlayerInput.fromClientPlayer(mc.player.input.playerInput)
            );
            AutoDodge.HitInfo hitInfo = this.getInflictedHits(simulatedPlayer, arrows, 80, 0.7);
            if (hitInfo != null) {
               AutoDodge.DodgePlan dodgePlan = this.planEvasion(hitInfo);
               if (dodgePlan != null) {
                  if (dodgePlan.forward != 0.0F || dodgePlan.sideways != 0.0F) {
                     e.setDirectional(dodgePlan.forward > 0.0F, dodgePlan.forward < 0.0F, dodgePlan.sideways > 0.0F, dodgePlan.sideways < 0.0F);
                  }

                  if (this.allowRotation.isValue() && dodgePlan.yawChange != null) {
                     mc.player.setYaw(dodgePlan.yawChange);
                  }

                  if (dodgePlan.shouldJump && this.allowRotation.isValue() && this.allowJump.isValue() && mc.player.isOnGround()) {
                     e.setJump(true);
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent e) {
   }

   private List<Entity> findFlyingArrows() {
      ArrayList<Entity> arrows = new ArrayList<>();
      if (mc.world == null) {
         return arrows;
      } else {
         for (Entity entity : mc.world.getEntities()) {
            if ((entity instanceof ArrowEntity || entity instanceof PersistentProjectileEntity || entity instanceof TridentEntity) && !entity.isOnGround()) {
               arrows.add(entity);
            }
         }

         return arrows;
      }
   }

   private AutoDodge.HitInfo getInflictedHits(PlayerSimulation simulatedPlayer, List<Entity> arrows, int maxTicks, double hitboxExpansion) {
      ArrayList<ArrowSimulation> simulatedArrows = new ArrayList<>();

      for (Entity arrow : arrows) {
         Vec3d pos = arrow.getPos();
         Vec3d velocity = arrow.getVelocity();
         simulatedArrows.add(new ArrowSimulation(mc.world, pos, velocity, false));
      }

      for (int i = 0; i < maxTicks; i++) {
         simulatedPlayer.tick();

         for (int arrowIndex = 0; arrowIndex < simulatedArrows.size(); arrowIndex++) {
            ArrowSimulation arrow = simulatedArrows.get(arrowIndex);
            if (!arrow.inGround) {
               Vec3d lastPos = arrow.pos;
               arrow.tick();
               Box playerHitBox = new Box(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3).expand(hitboxExpansion).offset(simulatedPlayer.pos);
               if (playerHitBox.intersects(lastPos, arrow.pos)) {
                  return new AutoDodge.HitInfo(i, arrows.get(arrowIndex), arrow.pos, lastPos, arrow.velocity);
               }
            }
         }
      }

      return null;
   }

   private AutoDodge.DodgePlan planEvasion(AutoDodge.HitInfo hitInfo) {
      if (hitInfo == null) {
         return null;
      } else {
         Vec3d arrowDir = hitInfo.arrowVelocity.normalize();
         Vec3d playerPos = mc.player.getPos();
         Vec3d hitPos = hitInfo.hitPos;
         Vec3d toPlayer = playerPos.subtract(hitPos).normalize();
         Vec3d perpendicular = arrowDir.crossProduct(new Vec3d(0.0, 1.0, 0.0)).normalize();
         if (perpendicular.lengthSquared() < 0.1) {
            perpendicular = arrowDir.crossProduct(new Vec3d(1.0, 0.0, 0.0)).normalize();
         }

         float yaw = mc.player.getYaw();
         double yawRad = Math.toRadians(yaw);
         Vec3d forward = new Vec3d(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
         Vec3d right = new Vec3d(Math.cos(yawRad), 0.0, Math.sin(yawRad));
         double forwardDot = perpendicular.dotProduct(forward);
         double rightDot = perpendicular.dotProduct(right);
         float forwardInput = (float)MathHelper.clamp(forwardDot * 2.0, -1.0, 1.0);
         float sidewaysInput = (float)MathHelper.clamp(rightDot * 2.0, -1.0, 1.0);
         Float yawChange = null;
         if (this.allowRotation.isValue()) {
            double targetYaw = Math.toDegrees(Math.atan2(-perpendicular.x, perpendicular.z));
            yawChange = (float)MathHelper.wrapDegrees(targetYaw);
         }

         boolean shouldJump = hitInfo.hitPos.y < playerPos.y + 0.5;
         return new AutoDodge.DodgePlan(forwardInput, sidewaysInput, yawChange, shouldJump, false);
      }
   }

   private static class DodgePlan {
      final float forward;
      final float sideways;
      final Float yawChange;
      final boolean shouldJump;
      final boolean useTimer;

      DodgePlan(float forward, float sideways, Float yawChange, boolean shouldJump, boolean useTimer) {
         this.forward = forward;
         this.sideways = sideways;
         this.yawChange = yawChange;
         this.shouldJump = shouldJump;
         this.useTimer = useTimer;
      }
   }

   private static class HitInfo {
      final int tickDelta;
      final Entity arrowEntity;
      final Vec3d hitPos;
      final Vec3d prevArrowPos;
      final Vec3d arrowVelocity;

      HitInfo(int tickDelta, Entity arrowEntity, Vec3d hitPos, Vec3d prevArrowPos, Vec3d arrowVelocity) {
         this.tickDelta = tickDelta;
         this.arrowEntity = arrowEntity;
         this.hitPos = hitPos;
         this.prevArrowPos = prevArrowPos;
         this.arrowVelocity = arrowVelocity;
      }
   }
}
