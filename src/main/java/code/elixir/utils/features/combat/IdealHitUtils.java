package code.elixir.utils.features.combat;

import code.elixir.features.impl.combat.AttackAura;
import code.elixir.features.impl.combat.CombatModules;
import code.elixir.features.impl.combat.PacketCriticals;
import code.elixir.features.impl.combat.TriggerBot;
import code.elixir.mixins.IEntity;
import code.elixir.utils.display.interfaces.QuickImports;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.item.ShovelItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class IdealHitUtils implements QuickImports {
   private static final int WATER_CRIT_INTENT_TICKS = 8;
   private static final int WATER_CRIT_CONTACT_TICKS = 10;
   private static final double WATER_CRIT_MIN_UPWARD_VELOCITY = 0.05;
   private static int lastWaterContactAge = Integer.MIN_VALUE;
   private static int lastWaterCritIntentAge = Integer.MIN_VALUE;

   public static float getAICooldown() {
      if (mc.player.getMainHandStack().getItem() == Items.AIR) {
         return 0.9F;
      } else {
         return !(mc.player.getMainHandStack().getItem() instanceof AxeItem) && !(mc.player.getMainHandStack().getItem() instanceof ShovelItem) ? 0.93F : 0.95F;
      }
   }

   public static boolean canAIFall() {
      BlockPos posWater = BlockPos.ofFloored(mc.player.getPos().add(0.0, -0.4F, 0.0));
      if (mc.world.getBlockState(posWater).isOf(Blocks.WATER)) {
         return true;
      } else if (getBlock(0.0, 3.0, 0.0) == Blocks.AIR && getBlock(0.0, 2.0, 0.0) == Blocks.AIR && getBlock(0.0, 1.0, 0.0) == Blocks.AIR) {
         return true;
      } else {
         return mc.player.fallDistance < (getBlock(0.0, 2.0, 0.0) != Blocks.AIR ? 0.08F : 0.6F) ? true : mc.player.fallDistance > 1.2F;
      }
   }

   public static boolean canCritical(LivingEntity target) {
      return canCriticalInternal(target, true);
   }

   public static boolean canStrictCritical(LivingEntity target) {
      return canCriticalInternal(target, false);
   }

   private static boolean canCriticalInternal(LivingEntity target, boolean allowSmartCrit) {
      if (mc.player == null) {
         return false;
      }

      AttackAura aura = CombatModules.getAura();
      boolean onlyCrits = aura != null && aura.isState() && aura.onlyCrits.isValue();
      if (onlyCrits) {
         return isInCritWindow(target);
      }

      boolean isJumpingOrInAir = (mc.options != null && mc.options.jumpKey.isPressed()) || !mc.player.isOnGround();
      if (isJumpingOrInAir) {
         return isInCritWindow(target);
      }

      if (cannotPerformCrit()) {
         return mc.player.getAttackCooldownProgress(0.5F) >= 0.95F;
      }

      if (allowSmartCrit && (aura == null || aura.smartCrit.isValue())) {
         return mc.player.isOnGround() && mc.player.getAttackCooldownProgress(0.5F) >= 0.95F;
      }

      return false;
   }

   public static boolean isInCritWindow() {
      return isInCritWindow(null);
   }

   public static boolean isInCritWindow(LivingEntity target) {
      if (mc.player == null) {
         return false;
      } else if (mc.player.isOnGround()
         || mc.player.isClimbing()
         || mc.player.isTouchingWater()
         || mc.player.isSubmergedInWater()
         || mc.player.isGliding()
         || mc.player.hasVehicle()
         || mc.player.getAbilities().flying
         || mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
         || mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
         return false;
      } else if (mc.player.getVelocity().y >= -0.01) {
         return false;
      } else if (mc.player.getAttackCooldownProgress(0.5F) < 0.95F) {
         return false;
      } else if (target == null) {
         return true;
      } else {
         AttackAura aura = CombatModules.getAura();
         float attackRange = aura != null ? aura.attackRange.getValue() : 3.0F;
         return mc.player.distanceTo(target) <= attackRange + 0.75F;
      }
   }

   private static boolean isNoJumpDelayCeilingCritIntent() {
      return CombatModules.getNoJumpDelay() != null
         && CombatModules.getNoJumpDelay().isState()
         && mc.options != null
         && mc.options.jumpKey.isPressed()
         && hasLowCeilingForJumpCrit();
   }

   private static boolean isNoJumpDelayJumpCritIntent() {
      return CombatModules.getNoJumpDelay() != null && CombatModules.getNoJumpDelay().isState() && mc.options != null && mc.options.jumpKey.isPressed();
   }

   private static boolean isNoJumpDelayCeilingCritWindow() {
      return mc.player != null
         && !mc.player.isOnGround()
         && mc.player.getVelocity().y <= 0.01
         && !mc.player.isTouchingWater()
         && !mc.player.isSubmergedInWater()
         && !mc.player.isInLava()
         && !mc.player.isClimbing()
         && !mc.player.hasVehicle()
         && !mc.player.getAbilities().flying;
   }

   public static boolean isNoJumpDelayJumpCritWindow() {
      return mc.player != null
         && mc.world != null
         && CombatModules.getNoJumpDelay() != null
         && CombatModules.getNoJumpDelay().isState()
         && mc.options != null
         && mc.options.jumpKey.isPressed()
         && !mc.player.isOnGround()
         && mc.player.fallDistance > 0.0F
         && mc.player.getVelocity().y < 0.0
         && !mc.player.isTouchingWater()
         && !mc.player.isSubmergedInWater()
         && !mc.player.isInLava()
         && !mc.player.isClimbing()
         && !mc.player.hasVehicle()
         && !mc.player.getAbilities().flying
         && !mc.player.hasStatusEffect(StatusEffects.LEVITATION)
         && !mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)
         && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
         && !mc.player.isGliding()
         && !isInCobweb();
   }

   private static boolean hasLowCeilingForJumpCrit() {
      if (mc.player != null && mc.world != null) {
         Box box = mc.player.getBoundingBox().contract(0.03);
         Box headBox = new Box(box.minX, box.maxY, box.minZ, box.maxX, box.maxY + 0.32, box.maxZ);

         for (BlockPos pos : BlockPos.iterate(
            MathHelper.floor(headBox.minX),
            MathHelper.floor(headBox.minY),
            MathHelper.floor(headBox.minZ),
            MathHelper.floor(headBox.maxX),
            MathHelper.floor(headBox.maxY),
            MathHelper.floor(headBox.maxZ)
         )) {
            BlockState state = mc.world.getBlockState(pos);
            if (!state.isAir() && !state.getCollisionShape(mc.world, pos).isEmpty()) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static boolean canPacketCrit() {
      return isInCobweb() || mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING);
   }

   private static void updateWaterCritState() {
      if (mc.player != null && mc.world != null) {
         boolean nearWaterSurface = isNearWaterSurface();
         if (nearWaterSurface) {
            lastWaterContactAge = mc.player.age;
            if (isWaterCritIntentState()) {
               lastWaterCritIntentAge = mc.player.age;
            }
         }
      } else {
         lastWaterContactAge = Integer.MIN_VALUE;
         lastWaterCritIntentAge = Integer.MIN_VALUE;
      }
   }

   private static boolean isWaterCritIntentState() {
      return mc.player != null && mc.options != null
         ? mc.options.jumpKey.isPressed() && !mc.player.isOnGround() && !mc.player.isSubmergedInWater() && mc.player.getVelocity().y > 0.05
         : false;
   }

   public static boolean isTryingWaterCrit() {
      return mc.player != null && mc.options != null && mc.options.jumpKey.isPressed()
         ? mc.player.age - lastWaterCritIntentAge <= 8 && mc.player.age - lastWaterContactAge <= 10
         : false;
   }

   public static boolean isWaterCritWindow() {
      return mc.player != null
         && !mc.player.isOnGround()
         && !mc.player.isTouchingWater()
         && !mc.player.isSubmergedInWater()
         && mc.player.fallDistance > 0.0F
         && mc.player.getVelocity().y < 0.0;
   }

   private static boolean isNearWaterSurface() {
      if (mc.player != null && mc.world != null) {
         BlockPos below = BlockPos.ofFloored(mc.player.getPos().add(0.0, -0.4F, 0.0));
         return mc.player.isTouchingWater() || mc.player.isSubmergedInWater() || mc.world.getBlockState(below).isOf(Blocks.WATER);
      } else {
         return false;
      }
   }

   public static boolean cannotPerformCrit() {
      double effectiveJumpHeight = mc.player.getStepHeight();
      Vec3d jumpVec = new Vec3d(0.0, effectiveJumpHeight, 0.0);
      Vec3d allowedMovement = ((IEntity)mc.player).invokeAdjustMovementForCollisions(jumpVec);
      boolean cobweb = isInCobweb();
      BlockPos posWater = BlockPos.ofFloored(mc.player.getPos().add(0.0, mc.player.getHeight() / 2.0F, 0.0));
      return mc.player.isInLava()
         || mc.player.isClimbing()
         || mc.world.getBlockState(posWater).isOf(Blocks.WATER)
         || mc.player.hasStatusEffect(StatusEffects.LEVITATION)
         || mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)
         || mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
         || cobweb
         || mc.player.isGliding()
         || mc.player.hasVehicle()
         || mc.player.getAbilities().flying
         || mc.player.isTouchingWater()
         || allowedMovement.y < mc.player.getStepHeight() - 0.5 && mc.player.isOnGround();
   }

   public static boolean isInCobweb() {
      Box box = mc.player.getBoundingBox();

      for (BlockPos pos : BlockPos.iterate(
         MathHelper.floor(box.minX),
         MathHelper.floor(box.minY),
         MathHelper.floor(box.minZ),
         MathHelper.floor(box.maxX),
         MathHelper.floor(box.maxY),
         MathHelper.floor(box.maxZ)
      )) {
         if (mc.world.getBlockState(pos).isOf(Blocks.COBWEB)) {
            return true;
         }
      }

      return false;
   }

   public static Block getBlock(double x, double y, double z) {
      return mc.world.getBlockState(mc.player.getBlockPos().add((int)x, (int)y, (int)z)).getBlock();
   }

   public static boolean isJumpBlockedByCeiling() {
      if (mc.player == null || mc.world == null) {
         return false;
      } else {
         return !mc.player.isTouchingWater() && !mc.player.isSubmergedInWater() && !mc.player.isClimbing() && !mc.player.getAbilities().flying
            ? mc.player.isOnGround() && hasBlockAboveHead() && !canMoveUpAfterJump()
            : false;
      }
   }

   public static boolean isDivineCriticalState() {
      return mc.player == null
         ? false
         : mc.player.fallDistance > 0.0F
            && !mc.player.isOnGround()
            && !mc.player.hasStatusEffect(StatusEffects.LEVITATION)
            && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
            && !mc.player.isTouchingWater()
            && !mc.player.isSubmergedInWater()
            && !mc.player.isInLava()
            && !mc.player.isClimbing()
            && !mc.player.hasVehicle()
            && !mc.player.getAbilities().flying
            && !mc.player.isGliding();
   }

   private static boolean hasBlockAboveHead() {
      Box box = mc.player.getBoundingBox();
      Box headBox = new Box(box.minX, box.maxY, box.minZ, box.maxX, box.maxY + 2.5, box.maxZ);

      for (BlockPos pos : BlockPos.iterate(
         MathHelper.floor(headBox.minX),
         MathHelper.floor(headBox.minY),
         MathHelper.floor(headBox.minZ),
         MathHelper.floor(headBox.maxX),
         MathHelper.floor(headBox.maxY),
         MathHelper.floor(headBox.maxZ)
      )) {
         BlockState state = mc.world.getBlockState(pos);
         if (!state.isAir() && !state.getCollisionShape(mc.world, pos).isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private static boolean canMoveUpAfterJump() {
      double upHeight = mc.player.getHeight() + 0.2;
      Box upBox = mc.player.getBoundingBox().offset(0.0, upHeight, 0.0);

      for (BlockPos pos : BlockPos.iterate(
         MathHelper.floor(upBox.minX),
         MathHelper.floor(upBox.minY),
         MathHelper.floor(upBox.minZ),
         MathHelper.floor(upBox.maxX),
         MathHelper.floor(upBox.maxY),
         MathHelper.floor(upBox.maxZ)
      )) {
         BlockState state = mc.world.getBlockState(pos);
         if (!state.isAir() && !state.getCollisionShape(mc.world, pos).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public static boolean findFall(float fallDistance) {
      Vec3d rotationVec = mc.player.getRotationVector();
      double tempVelocityX = mc.player.getVelocity().x;
      double tempVelocityY = mc.player.getVelocity().y;
      double tempVelocityZ = mc.player.getVelocity().z;
      float n = MathHelper.cos(mc.player.getPitch() * (float) (Math.PI / 180.0));
      n = (float)(n * n * Math.min(rotationVec.length() / 0.4, 1.0));
      Vec3d vec3d = new Vec3d(tempVelocityX, tempVelocityY, tempVelocityZ).add(0.0, 0.08 * (-1.0 + n * 0.75), 0.0);
      tempVelocityY = vec3d.y * 0.98F;
      return tempVelocityY < fallDistance;
   }

   private IdealHitUtils() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
