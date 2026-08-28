package code.elixir.features.impl.combat;

import code.elixir.common.repository.friend.FriendUtils;
import code.elixir.events.player.EntitySpawnEvent;
import code.elixir.events.player.PostMotionEvent;
import code.elixir.events.player.RotationUpdateEvent;
import code.elixir.features.impl.render.Prediction;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.features.module.setting.implement.RadioSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.features.aura.rotations.constructor.LinearConstructor;
import code.elixir.utils.features.aura.utils.MathAngle;
import code.elixir.utils.features.aura.warp.Turns;
import code.elixir.utils.features.aura.warp.TurnsConfig;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.interactions.inv.InventoryFlowManager;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.interactions.simulate.PlayerSimulation;
import code.elixir.utils.math.script.Script;
import code.elixir.utils.math.task.TaskPriority;
import code.elixir.utils.math.time.StopWatch;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.IntStream;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

public class TargetPearl extends Module {
   private final StopWatch stopWatch = new StopWatch();
   private final Script script = new Script();
   private final RadioSetting modeSetting = new RadioSetting("Mode", "When will target pearl work", new String[]{"Bind", "Always"}, "Always");
   private final RadioSetting targetSetting = new RadioSetting(
      "Targets", "Targets for which pearls will be thrown", new String[]{"Aura Target", "All"}, "Aura Target"
   );
   private final BindSetting throwSetting = new BindSetting("Throw", "Throw Key").visible(() -> this.modeSetting.get().equals("Bind"));
   private final SliderSettings distanceSetting = new SliderSettings("Distance", "Target Pearl Trigger Distance").setValue(10.0F).range(5, 15);

   public TargetPearl() {
      super("TargetPearl", "TargetPearl", ModuleCategory.COMBAT);
      this.setup(new Setting[]{this.modeSetting, this.targetSetting, this.throwSetting, this.distanceSetting});
   }

   @EventHandler
   public void onEntitySpawn(EntitySpawnEvent e) {
      if (e.getEntity() instanceof EnderPearlEntity pearl) {
         mc.world
            .getPlayers()
            .stream()
            .filter(p -> p.distanceTo(pearl) <= 3.0F)
            .min(Comparator.comparingDouble(p -> p.distanceTo(pearl)))
            .ifPresent(arg_0 -> pearl.setOwner(arg_0));
      }
   }

   @EventHandler
   public void onRotationUpdate(RotationUpdateEvent e) {
      if (e.getType() == 0) {
         LivingEntity auraTarget = AttackAura.getInstance().getTarget();
         if (auraTarget == null) {
            auraTarget = AttackAura.getInstance().getLastTarget();
         }

         LivingEntity target = auraTarget;
         Slot slot = InventoryTask.getSlot(Items.ENDER_PEARL);
         if (slot == null || !this.stopWatch.finished(1000.0)) {
            return;
         }

         if (this.modeSetting.get().equals("Bind") && !PlayerInteractionHelper.isKey(this.throwSetting)) {
            return;
         }

         if (PlayerInteractionHelper.streamEntities()
            .filter(EnderPearlEntity.class::isInstance)
            .map(EnderPearlEntity.class::cast)
            .anyMatch(pearl -> Objects.equals(pearl.getOwner(), mc.player))) {
            this.stopWatch.reset();
            return;
         }

         Prediction prediction = Prediction.getInstance();
         PlayerInteractionHelper.streamEntities()
            .filter(EnderPearlEntity.class::isInstance)
            .map(EnderPearlEntity.class::cast)
            .filter(
               pearl -> !FriendUtils.isFriend(pearl.getOwner())
                  && (this.targetSetting.get().equals("All") || target != null && target.equals(pearl.getOwner()))
            )
            .min(
               Comparator.comparingDouble(
                  pearl -> TurnsConnection.computeRotationDifference(
                     MathAngle.cameraAngle(), MathAngle.calculateAngle(prediction.calcTrajectory(pearl).getPos())
                  )
               )
            )
            .ifPresent(
               pearl -> {
                  HitResult targetResult = prediction.calcTrajectory(pearl);
                  if (targetResult != null && !(mc.player.getPos().distanceTo(targetResult.getPos()) <= this.distanceSetting.getInt())) {
                     Vec3d eyePos = mc.player.getEyePos().add(mc.player.getPos().subtract(PlayerSimulation.simulateLocalPlayer(1).pos));
                     float yaw = MathAngle.fromVec3d(targetResult.getPos().subtract(eyePos)).getYaw();
                     IntStream.range(-89, 89)
                        .mapToObj(pitch -> new Turns(yaw, pitch))
                        .filter(angle -> {
                           HitResult playerResult = prediction.checkTrajectory(
                              angle.toVector(), new EnderPearlEntity(mc.world, mc.player, slot.getStack()), 1.5
                           );
                           return playerResult != null && playerResult.getPos().distanceTo(targetResult.getPos()) <= 3.0;
                        })
                        .max(Comparator.comparingDouble(Turns::getPitch))
                        .ifPresent(
                           angle -> {
                              TurnsConnection.INSTANCE
                                 .rotateTo(
                                    new Turns.VecRotation(angle, angle.toVector()),
                                    mc.player,
                                    1,
                                    new TurnsConfig(new LinearConstructor(), true, true),
                                    TaskPriority.HIGH_IMPORTANCE_3,
                                    this
                                 );
                              InventoryFlowManager.unPressMoveKeys();
                              this.script.cleanup().addTickStep(0, () -> {
                                 InventoryTask.swapAndUse(Items.ENDER_PEARL, angle, false);
                                 InventoryFlowManager.enableMoveKeys();
                              });
                              pearl.setOwner(null);
                              this.stopWatch.reset();
                           }
                        );
                  }
               }
            );
      }
   }

   @EventHandler
   public void onPostMotion(PostMotionEvent e) {
      this.script.update();
   }
}
