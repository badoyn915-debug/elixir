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
import java.util.Objects;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;

public class Nuker extends Module {
   public BlockPos pos;
   private VoxelShape shape;
   private final BooleanSetting rotateSetting = new BooleanSetting("Поворот", "Ломать блоки ниже игрока").setValue(true);
   private final BooleanSetting downSetting = new BooleanSetting("Вниз", "Ломать блоки ниже игрока").setValue(true);
   private final SliderSettings radiusSetting = new SliderSettings("Радиус", "Ломает блоки в радиусе вокруг вас").setValue(3.0F).range(1, 6);

   public Nuker() {
      super("Nuker", ModuleCategory.PLAYER);
      this.setup(new Setting[]{this.rotateSetting, this.downSetting, this.radiusSetting});
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      if (this.pos != null && this.shape != null && !this.shape.isEmpty()) {
         Render3D.drawShape(this.pos, this.shape, ColorAssist.getClientColor(), 2.0F);
      }
   }

   @EventHandler
   public void onRotationUpdate(RotationUpdateEvent e) {
      if (e.getType() == 0) {
         this.pos = PlayerInteractionHelper.getCube(
               mc.player.getBlockPos(), this.radiusSetting.getInt(), this.radiusSetting.getInt(), this.downSetting.isValue()
            )
            .stream()
            .filter(this::validBlock)
            .min(Comparator.comparingDouble(this::blockPriority))
            .orElse(null);
         if (this.pos != null) {
            if (this.rotateSetting.isValue()) {
               TurnsConnection.INSTANCE.rotateTo(MathAngle.calculateAngle(this.pos.toCenterPos()), TurnsConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_1, this);
            }

            this.shape = mc.world.getBlockState(this.pos).getOutlineShape(mc.world, this.pos);
            mc.interactionManager.updateBlockBreakingProgress(this.pos, Direction.UP);
            mc.player.swingHand(Hand.MAIN_HAND);
         }
      }
   }

   private double blockPriority(BlockPos pos) {
      String var2 = mc.world.getBlockState(pos).getBlock().getTranslationKey().replace("block.minecraft.", "");

      return switch (var2) {
         case "blue_bed" -> 0.0;
         case "red_bed" -> 0.0;
         case "pink_bed" -> 0.0;
         case "yellow_bed" -> 0.0;
         case "white_bed" -> 0.0;
         case "gray_bed" -> 0.0;
         case "lightblue_bed" -> 0.0;
         case "cyan_bed" -> 0.0;
         case "purple_bed" -> 0.0;
         case "magent_bed" -> 0.0;
         case "ancient_debris" -> 1.0;
         default -> mc.player.squaredDistanceTo(pos.toCenterPos());
      };
   }

   private boolean validBlock(BlockPos pos) {
      BlockState state = Objects.requireNonNull(mc.world).getBlockState(pos);
      return !PlayerInteractionHelper.isAir(state)
         && state.getBlock() != Blocks.WATER
         && state.getBlock() != Blocks.LAVA
         && state.getBlock() != Blocks.BEDROCK
         && state.getBlock() != Blocks.BARRIER;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public VoxelShape getShape() {
      return this.shape;
   }

   public BooleanSetting getRotateSetting() {
      return this.rotateSetting;
   }

   public BooleanSetting getDownSetting() {
      return this.downSetting;
   }

   public SliderSettings getRadiusSetting() {
      return this.radiusSetting;
   }
}
