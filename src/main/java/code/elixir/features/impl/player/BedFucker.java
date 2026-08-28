package code.elixir.features.impl.player;

import code.elixir.events.player.RotationUpdateEvent;
import code.elixir.events.render.WorldRenderEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.geometry.Render3D;
import code.elixir.utils.features.aura.utils.MathAngle;
import code.elixir.utils.features.aura.warp.TurnsConfig;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.math.task.TaskPriority;
import java.util.Comparator;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;

public class BedFucker extends Module {
   private BlockPos targetPos;
   private VoxelShape targetShape;
   private final BooleanSetting rotateSetting = new BooleanSetting("Поворот", "Поворачиваться к кровати").setValue(true);
   private final SliderSettings radiusSetting = new SliderSettings("Радиус", "Ломает кровати в радиусе").setValue(4.0F).range(1, 6);

   public BedFucker() {
      super("BedFucker", ModuleCategory.PLAYER);
      this.setup(new Setting[]{this.rotateSetting, this.radiusSetting});
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      if (this.targetPos != null && this.targetShape != null && !this.targetShape.isEmpty()) {
         Render3D.drawShape(this.targetPos, this.targetShape, ColorAssist.getClientColor(), 2.0F);
      }
   }

   @EventHandler
   public void onRotationUpdate(RotationUpdateEvent e) {
      if (e.getType() == 0 && mc.player != null && mc.world != null && mc.interactionManager != null) {
         this.targetPos = mc.player.getBlockPos();
         int radius = this.radiusSetting.getInt();
         this.targetPos = PlayerInteractionHelper.getCube(mc.player.getBlockPos(), radius, radius, true)
            .stream()
            .filter(this::isBed)
            .min(Comparator.comparingDouble(p -> p.toCenterPos().squaredDistanceTo(mc.player.getPos())))
            .orElse(null);
         if (this.targetPos != null) {
            if (this.rotateSetting.isValue()) {
               TurnsConnection.INSTANCE
                  .rotateTo(MathAngle.calculateAngle(this.targetPos.toCenterPos()), TurnsConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_1, this);
            }

            BlockState state = mc.world.getBlockState(this.targetPos);
            this.targetShape = state.getOutlineShape(mc.world, this.targetPos);
            mc.interactionManager.updateBlockBreakingProgress(this.targetPos, Direction.UP);
            mc.player.swingHand(Hand.MAIN_HAND);
         } else {
            this.targetShape = null;
         }
      }
   }

   private boolean isBed(BlockPos pos) {
      BlockState state = mc.world.getBlockState(pos);
      return state.getBlock() instanceof BedBlock;
   }
}
