package code.elixir.features.impl.movement;

import code.elixir.events.player.PlayerTravelEvent;
import code.elixir.events.player.SwimmingEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.logs.Logger;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.packet.network.Network;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import code.elixir.utils.input.MoveUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.world.GameMode;

public class WaterSpeed extends Module {
   private final SelectSetting modeSetting = new SelectSetting("Режим", "Выберите режим обхода").value("Vanilla", "FunTime", "Wall", "Grim");
   private final SelectSetting wallModeSetting = new SelectSetting("Режим от стен", "Режим отталкивания от стен")
      .value("MetaHvH", "FunTime")
      .visible(() -> this.modeSetting.isSelected("Wall"));
   private final SliderSettings ftSpeedSetting = new SliderSettings("Скорость", "Скорость FunTime")
      .range(0.1F, 0.5F)
      .setValue(0.2F)
      .visible(() -> this.modeSetting.isSelected("FunTime"));
   private double wallVerticalBoost = 0.05;
   private double wallRadius;
   private double wallBoost;

   public WaterSpeed() {
      super("WaterSpeed", "WaterSpeed", ModuleCategory.MOVEMENT);
      this.setup(new Setting[]{this.modeSetting, this.wallModeSetting, this.ftSpeedSetting});
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player != null && mc.world != null) {
         boolean isVanillaServer = Network.isVanilla();
         if (isVanillaServer || mc.getNetworkHandler() != null) {
            for (PlayerEntity player : mc.world.getPlayers()) {
               if (player != null && player != mc.player) {
                  boolean isSpectator = player.isSpectator();
                  PlayerListEntry entry = mc.getNetworkHandler() != null ? mc.getNetworkHandler().getPlayerListEntry(player.getUuid()) : null;
                  if (entry != null) {
                     GameMode gameMode = entry.getGameMode();
                     isSpectator = gameMode == GameMode.SPECTATOR;
                  }

                  if (isVanillaServer || isSpectator) {
                     String playerName = player.getName().getString();
                     String status = isSpectator ? "Spectator" : (isVanillaServer ? "Vanilla сервер" : "");
                     if (!status.isEmpty()) {
                        Logger.info(String.format("[WaterSpeed] Игрок %s: %s (UUID: %s)", playerName, status, player.getUuid()));
                     }
                  }
               }
            }
         }

         String var10 = this.modeSetting.getSelected();
         switch (var10) {
            case "Vanilla":
               this.handleVanilla();
               break;
            case "FunTime":
               this.handleFunTime();
               break;
            case "Wall":
               this.handleWall();
               break;
            case "Grim":
               this.handleGrim();
         }
      }
   }

   @EventHandler
   public void onSwimming(SwimmingEvent e) {
      if (this.modeSetting.isSelected("FunTime")) {
         if (mc.options.jumpKey.isPressed()) {
            float pitch = TurnsConnection.INSTANCE.getRotation().getPitch();
            float boost = pitch >= 0.0F ? MathHelper.clamp(pitch / 45.0F, 1.0F, 2.0F) : 0.8F;
            e.getVector().y = 0.5 * boost;
         } else if (mc.options.sneakKey.isPressed()) {
            e.getVector().y = -0.8;
         }
      }
   }

   @EventHandler
   public void onTravel(PlayerTravelEvent e) {
      if (e.isPre()) {
         if (mc.player != null && mc.player.isTouchingWater()) {
            if (this.modeSetting.isSelected("Wall") && mc.player.horizontalCollision) {
               Vec3d motion = e.getMotion();
               e.setMotion(new Vec3d(motion.x * 1.1, motion.y, motion.z * 1.1));
            }
         }
      }
   }

   private void handleVanilla() {
      if (mc.player.isTouchingWater() && !mc.player.isSwimming() && mc.options.forwardKey.isPressed()) {
         Vec3d velocity = mc.player.getVelocity();
         mc.player.setVelocity(velocity.x * 1.00061, velocity.y, velocity.z * 1.00061);
      }
   }

   private void handleFunTime() {
      if (mc.player.isTouchingWater() && mc.player.isOnGround()) {
         mc.player.jump();
         mc.player.setVelocity(mc.player.getVelocity().x, this.ftSpeedSetting.getValue(), mc.player.getVelocity().z);
      }
   }

   private void handleWall() {
      if (this.wallModeSetting.isSelected("FunTime")) {
         this.wallRadius = 0.35;
         this.wallBoost = 0.05;
      } else if (this.wallModeSetting.isSelected("MetaHvH")) {
         this.wallRadius = 0.35;
         this.wallBoost = 0.4;
      }

      if (mc.player.isTouchingWater()) {
         if (mc.player.horizontalCollision) {
            if (this.isWaterNearFeet()) {
               Direction collisionFace = this.getCollisionFace();
               if (collisionFace != null) {
                  Vec3d pushDir = new Vec3d(-collisionFace.getOffsetX(), 0.0, -collisionFace.getOffsetZ());
                  if (!(pushDir.lengthSquared() < 1.0E-6)) {
                     double[] moveDir = this.calculateDirection(mc.player.input.movementForward, mc.player.input.movementSideways, this.wallBoost);
                     Vec3d combined = new Vec3d(moveDir[0], 0.0, moveDir[1]).add(pushDir.normalize().multiply(this.wallBoost * 0.6));
                     Vec3d velocity = mc.player.getVelocity();
                     Vec3d result = velocity.add(combined);
                     double vertical = Math.max(velocity.y, this.wallVerticalBoost);
                     mc.player.setVelocity(result.x, vertical, result.z);
                     mc.player.fallDistance = 0.0F;
                  }
               }
            }
         }
      }
   }

   private void handleGrim() {
      if (mc.player != null && mc.world != null) {
         if (mc.options.jumpKey.isPressed()) {
            if (mc.player.isTouchingWater()) {
               BlockPos playerPos = mc.player.getBlockPos();
               double waterLevel = this.findWaterSurfaceLevel(playerPos);
               double playerEyeY = mc.player.getY() + mc.player.getStandingEyeHeight();
               if (playerEyeY >= waterLevel - 0.35 && playerEyeY <= waterLevel + 0.35) {
                  mc.player.setVelocity(mc.player.getVelocity().x, 2.5, mc.player.getVelocity().z);
                  MoveUtil.setMotion(MoveUtil.getMotion() * 5.0);
               } else if (playerEyeY < waterLevel - 0.35) {
                  mc.player.setVelocity(mc.player.getVelocity().x, 0.12, mc.player.getVelocity().z);
               }
            }
         }
      }
   }

   private double findWaterSurfaceLevel(BlockPos startPos) {
      BlockPos checkPos = startPos;

      for (int i = 0; i < 5; i++) {
         FluidState fluidState = mc.world.getFluidState(checkPos);
         FluidState aboveFluid = mc.world.getFluidState(checkPos.up());
         if (fluidState.isIn(FluidTags.WATER) && !aboveFluid.isIn(FluidTags.WATER)) {
            return checkPos.getY() + fluidState.getHeight(mc.world, checkPos);
         }

         checkPos = checkPos.up();
      }

      FluidState fluidState = mc.world.getFluidState(startPos);
      return fluidState.isIn(FluidTags.WATER) ? startPos.getY() + fluidState.getHeight(mc.world, startPos) : startPos.getY() + 1.0;
   }

   private Direction getCollisionFace() {
      Box box = mc.player.getBoundingBox();

      for (Direction dir : Type.HORIZONTAL) {
         Box shifted = box.offset(dir.getOffsetX() * 0.05, 0.0, dir.getOffsetZ() * 0.05);
         boolean hasCollision = mc.world.getBlockCollisions(mc.player, shifted).iterator().hasNext();
         if (hasCollision) {
            return dir;
         }
      }

      return null;
   }

   private boolean isWaterNearFeet() {
      Box box = mc.player.getBoundingBox();
      int minX = MathHelper.floor(box.minX - this.wallRadius);
      int maxX = MathHelper.floor(box.maxX + this.wallRadius);
      int minY = MathHelper.floor(box.minY - 0.2);
      int maxY = MathHelper.floor(box.minY + 0.2);
      int minZ = MathHelper.floor(box.minZ - this.wallRadius);
      int maxZ = MathHelper.floor(box.maxZ + this.wallRadius);
      Mutable mutablePos = new Mutable();

      for (int x = minX; x <= maxX; x++) {
         for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
               mutablePos.set(x, y, z);
               BlockState state = mc.world.getBlockState(mutablePos);
               if (state.getFluidState().isIn(FluidTags.WATER) || state.getFluidState().isIn(FluidTags.LAVA)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private double[] calculateDirection(float forward, float sideways, double distance) {
      float yaw = mc.player.getYaw();
      float sinYaw = MathHelper.sin((float)Math.toRadians(yaw + 90.0F));
      float cosYaw = MathHelper.cos((float)Math.toRadians(yaw + 90.0F));
      double xMovement = forward * distance * cosYaw + sideways * distance * sinYaw;
      double zMovement = forward * distance * sinYaw - sideways * distance * cosYaw;
      return new double[]{xMovement, zMovement};
   }

   public SelectSetting getModeSetting() {
      return this.modeSetting;
   }

   public SelectSetting getWallModeSetting() {
      return this.wallModeSetting;
   }

   public SliderSettings getFtSpeedSetting() {
      return this.ftSpeedSetting;
   }

   public double getWallVerticalBoost() {
      return this.wallVerticalBoost;
   }

   public double getWallRadius() {
      return this.wallRadius;
   }

   public double getWallBoost() {
      return this.wallBoost;
   }
}
