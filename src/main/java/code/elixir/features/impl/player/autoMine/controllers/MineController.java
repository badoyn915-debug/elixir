package code.elixir.features.impl.player.autoMine.controllers;

import code.elixir.Elixir;
import code.elixir.events.player.InputEvent;
import code.elixir.events.player.RotationUpdateEvent;
import code.elixir.events.render.DrawEvent;
import code.elixir.events.render.WorldRenderEvent;
import code.elixir.features.impl.misc.creeperFarm.WalkDirectionUtil;
import code.elixir.features.impl.player.autoMine.mine.MineMovementBrainer;
import code.elixir.features.impl.player.autoMine.mine.state.MineState;
import code.elixir.features.impl.player.autoMine.receivers.AutoMineModule;
import code.elixir.features.impl.player.autoMine.region.utils.WorldUtility;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.aura.rotations.constructor.LinearConstructor;
import code.elixir.utils.features.aura.utils.MathAngle;
import code.elixir.utils.features.aura.warp.Turns;
import code.elixir.utils.features.aura.warp.TurnsConfig;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import code.elixir.utils.interactions.inv.InventoryResult;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.interactions.inv.InventoryToolkit;
import code.elixir.utils.math.task.TaskPriority;
import net.minecraft.item.PickaxeItem;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class MineController implements QuickImports {
   private final RegionController regionController;
   private final MineState mineState;
   private final MineMovementBrainer brainer;
   private final LinearConstructor spAngle = new LinearConstructor();
   private long lastSwapTime = -1L;
   private long lastBreakUpdate = -1L;
   private BlockPos lastBreakingBlockPos = null;

   public MineController(RegionController regionController) {
      this.regionController = regionController;
      this.mineState = new MineState();
      this.brainer = new MineMovementBrainer();
   }

   public void onWorldRender(WorldRenderEvent worldRenderEvent) {
   }

   public void onDraw(DrawEvent drawEvent) {
      double x = mc.getWindow().getScaledWidth() / 2.0 + 10.0;
      double y = mc.getWindow().getScaledHeight() / 2.0 + 38.0;
      String var10000 = this.brainer.isPathClear() ? "Walking" : "Digging";
      if (this.brainer.isStuck()) {
         String status = "STUCK — Digging";
      }

      if (this.regionController.getTargetBlock() == null) {
         String var8 = "No target";
      }
   }

   public void onTick() {
      this.mineState.onTick();
      this.swapPickaxe();
      BlockPos targetBlock = this.regionController.getTargetBlock();
      this.brainer.update(targetBlock);
      if (targetBlock == null) {
         mc.options.attackKey.setPressed(false);
      } else {
         BlockPos blockToLookAt = this.getLookTarget(targetBlock);
         BlockPos blockToMine = null;
         Direction hitSide = Direction.UP;
         if (mc.crosshairTarget instanceof BlockHitResult blockHitResult) {
            BlockPos hitPos = blockHitResult.getBlockPos();
            double hitDist = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(hitPos));
            double targetDist = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(targetBlock));
            boolean shouldMine = false;
            if (this.brainer.getObstacleBlock() != null && hitPos.equals(this.brainer.getObstacleBlock())) {
               shouldMine = true;
            } else if (hitPos.equals(targetBlock)) {
               shouldMine = true;
            } else if (hitDist < 5.0 && RegionController.isValidBlockState(WorldUtility.fromBlockPos(hitPos))) {
               shouldMine = true;
            } else if (hitDist < 5.0 && this.isNearTarget(hitPos, targetBlock)) {
               shouldMine = true;
            } else if (!(hitDist < 5.0) || !this.brainer.isShouldDigObstacle() && !this.brainer.isStuck()) {
               if (hitDist < targetDist && hitDist < 5.0 && !WorldUtility.fromBlockPos(hitPos).isAir()) {
                  shouldMine = true;
               } else if (targetDist < 4.5 && hitDist < 5.0 && !WorldUtility.fromBlockPos(hitPos).isAir()) {
                  shouldMine = true;
               }
            } else {
               shouldMine = true;
            }

            if (shouldMine) {
               blockToMine = hitPos.toImmutable();
               hitSide = blockHitResult.getSide();
               this.lastBreakingBlockPos = blockToMine;
               this.lastBreakUpdate = System.currentTimeMillis();
            }
         } else {
            blockToMine = this.lastBreakingBlockPos != null && System.currentTimeMillis() - this.lastBreakUpdate < 500L
               ? this.lastBreakingBlockPos
               : blockToLookAt;
         }

         if (blockToMine != null && mc.world != null && !mc.world.getBlockState(blockToMine).isAir()) {
            mc.options.attackKey.setPressed(true);
            mc.interactionManager.updateBlockBreakingProgress(blockToMine, hitSide);
            mc.player.swingHand(Hand.MAIN_HAND);
         } else if (System.currentTimeMillis() - this.lastBreakUpdate > 200L) {
            mc.options.attackKey.setPressed(false);
         }
      }
   }

   private boolean isNearTarget(BlockPos hitPos, BlockPos targetBlock) {
      return Math.abs(hitPos.getX() - targetBlock.getX()) <= 2
         && Math.abs(hitPos.getY() - targetBlock.getY()) <= 2
         && Math.abs(hitPos.getZ() - targetBlock.getZ()) <= 2;
   }

   public void onInput(InputEvent inputEvent) {
      BlockPos targetBlock = this.regionController.getTargetBlock();
      if (targetBlock != null) {
         BlockPos obstacle = this.brainer.getObstacleBlock();
         if (obstacle != null && this.brainer.isShouldDigObstacle()) {
            float clientYaw = TurnsConnection.INSTANCE.getRotation().getYaw();
            Vec3d toObstacle = Vec3d.ofCenter(obstacle).subtract(mc.player.getPos());
            PlayerInput pi = WalkDirectionUtil.fromVec3d(toObstacle, clientYaw);
            inputEvent.setDirectional(pi.forward(), false, false, false);
         } else {
            BlockPos moveTarget = this.brainer.getMoveTarget();
            if (moveTarget == null) {
               moveTarget = targetBlock;
            }

            float clientYaw = TurnsConnection.INSTANCE.getRotation().getYaw();
            Vec3d targetDirection = Vec3d.ofCenter(moveTarget).subtract(mc.player.getPos());
            PlayerInput playerInput = WalkDirectionUtil.fromVec3d(targetDirection, clientYaw);
            inputEvent.setDirectional(playerInput.forward(), playerInput.backward(), playerInput.left(), playerInput.right());
         }
      }
   }

   public void onRotationUpdate(RotationUpdateEvent rotationUpdateEvent) {
      BlockPos targetBlock = this.regionController.getTargetBlock();
      if (targetBlock != null) {
         BlockPos lookAt = this.getLookTarget(targetBlock);
         Turns turns = MathAngle.calculateAngle(Vec3d.ofCenter(lookAt));
         TurnsConnection.INSTANCE
            .rotateTo(
               turns,
               new TurnsConfig(this.spAngle, true, false),
               TaskPriority.HIGH_IMPORTANCE_1,
               Elixir.getInstance().getModuleProvider().get(AutoMineModule.class)
            );
      }
   }

   private BlockPos getLookTarget(BlockPos targetBlock) {
      BlockPos obstacle = this.brainer.getObstacleBlock();
      return obstacle != null && this.brainer.isShouldDigObstacle() ? obstacle : targetBlock;
   }

   public void onWorldChange() {
      this.brainer.reset();
      this.lastBreakingBlockPos = null;
   }

   private void swapPickaxe() {
      if (!(mc.player.getMainHandStack().getItem() instanceof PickaxeItem) && System.currentTimeMillis() - this.lastSwapTime > 2000L) {
         InventoryResult inventoryResult = InventoryToolkit.findInInventory(itemStack -> itemStack.getItem() instanceof PickaxeItem);
         if (inventoryResult == null || inventoryResult.slot() == -1 || inventoryResult.slot() - 36 == mc.player.getInventory().selectedSlot) {
            return;
         }

         if (inventoryResult.slot() - 36 != 0) {
            InventoryTask.swapSlots(inventoryResult.slot(), 0);
         }

         if (inventoryResult.slot() - 36 < 9 && inventoryResult.slot() - 36 != mc.player.getInventory().selectedSlot) {
            mc.player.getInventory().selectedSlot = Math.clamp((long)(inventoryResult.slot() - 36), 0, 8);
         }

         this.lastSwapTime = System.currentTimeMillis();
      }
   }

   public RegionController getRegionController() {
      return this.regionController;
   }

   public MineState getMineState() {
      return this.mineState;
   }

   public MineMovementBrainer getBrainer() {
      return this.brainer;
   }

   public LinearConstructor getSpAngle() {
      return this.spAngle;
   }

   public long getLastSwapTime() {
      return this.lastSwapTime;
   }

   public long getLastBreakUpdate() {
      return this.lastBreakUpdate;
   }

   public BlockPos getLastBreakingBlockPos() {
      return this.lastBreakingBlockPos;
   }
}
