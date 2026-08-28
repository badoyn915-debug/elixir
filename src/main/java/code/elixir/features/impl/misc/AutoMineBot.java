package code.elixir.features.impl.misc;

import code.elixir.display.hud.Notifications;
import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.InputEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.client.chat.ChatMessage;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.features.aura.utils.MathAngle;
import code.elixir.utils.math.time.StopWatch;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class AutoMineBot extends Module {
   private static final String API_URL = "http://85.208.139.128:8000/api/mines/nearest";
   private static final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
   private final StopWatch commandDelay = new StopWatch();
   private final StopWatch warpDelay = new StopWatch();
   private final StopWatch replaceCommandDelay = new StopWatch();
   private final StopWatch updateTimer = new StopWatch();
   private final StopWatch doneBuildingTimer = new StopWatch();
   private final StopWatch baritoneWorkTimer = new StopWatch();
   private final StopWatch anarchyTimer = new StopWatch();
   private static final long COMMAND_DELAY_MS = 500L;
   private static final long WARP_DELAY_MS = 7100L;
   private static final long REPLACE_COMMAND_INTERVAL_MS = 1000L;
   private static final long UPDATE_INTERVAL_MS = 2000L;
   private static final long DONE_BUILDING_TIMEOUT_MS = 5000L;
   private static final long BARITONE_WORK_TIMEOUT_MS = 2000L;
   private static final long ANARCHY_TIMEOUT_MS = 40000L;
   private static final double NEARBY_RADIUS = 30.0;
   private static final BlockPos SEL_1 = new BlockPos(-96, 84, 38);
   private static final BlockPos SEL_2 = new BlockPos(-74, 77, 16);
   private static final Direction[] HORIZONTAL_FACES = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
   private AutoMineBot.State currentState = AutoMineBot.State.IDLE;
   private String currentAnarchy = "";
   private boolean sel1Set = false;
   private boolean sel2Set = false;
   private boolean baritoneWorking = false;
   private boolean doneBuildingReceived = false;
   private long lastDoneBuildingTime = 0L;
   private Vec3d goalMovementVec = null;
   private Vec3d calculatedGoalPartH = null;
   private final List<Vec3d> oldPositions = new ArrayList<>();
   private boolean holdForward = false;
   private boolean holdJump = false;

   public AutoMineBot() {
      super("AutoMineBot", "AutoMineBot", ModuleCategory.MISC);
      this.commandDelay.reset();
      this.warpDelay.reset();
      this.replaceCommandDelay.reset();
      this.updateTimer.reset();
   }

   @Override
   public void activate() {
      super.activate();
      ChatMessage.brandmessage("AutoMineBot включен. Начинаю работу...");
      this.currentState = AutoMineBot.State.SETTING_SEL;
      this.currentAnarchy = "";
      this.sel1Set = false;
      this.sel2Set = false;
      this.baritoneWorking = false;
      this.doneBuildingReceived = false;
      this.lastDoneBuildingTime = 0L;
      this.commandDelay.reset();
      this.warpDelay.reset();
      this.replaceCommandDelay.reset();
      this.updateTimer.reset();
      this.doneBuildingTimer.reset();
      this.baritoneWorkTimer.reset();
      this.anarchyTimer.reset();
   }

   @Override
   public void deactivate() {
      super.deactivate();
      ChatMessage.brandmessage("AutoMineBot выключен.");
      this.currentState = AutoMineBot.State.IDLE;
   }

   @EventHandler
   public void onInput(InputEvent e) {
      if (this.holdForward) {
         e.setForward(1.0F);
         e.setStrafe(0.0F);
      }

      if (this.holdJump) {
         e.setJump(true);
      }
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player != null && mc.world != null) {
         this.holdForward = false;
         this.holdJump = false;
         if (this.currentState == AutoMineBot.State.WARPING || this.currentState == AutoMineBot.State.WAITING_WARP) {
            this.goalMovementVec = Vec3d.ofCenter(SEL_1).add(Vec3d.ofCenter(SEL_2)).multiply(0.5);
            this.moveToGoal(this.goalMovementVec);
         }

         switch (this.currentState) {
            case IDLE:
            case WAITING_REPLACE:
            default:
               break;
            case SETTING_SEL:
               if (!this.sel1Set) {
                  this.sendCommand("#sel 1 -96 84 38");
                  this.sendCommand("#set smoothLook true");
                  this.sendCommand("#set smoothLookTicks 20");
                  this.sel1Set = true;
                  this.updateTimer.reset();
               } else if (!this.sel2Set && this.updateTimer.finished(500.0)) {
                  this.sendCommand("#sel 2 -74 77 16");
                  this.sendCommand("#set blocksToAvoidBreaking andesite,stone_bricks");
                  this.sel2Set = true;
                  this.updateTimer.reset();
               } else if (this.sel1Set && this.sel2Set && this.updateTimer.finished(500.0)) {
                  this.currentState = AutoMineBot.State.GETTING_ANARCHY;
                  this.updateTimer.reset();
               }
               break;
            case GETTING_ANARCHY:
               if (this.updateTimer.finished(2000.0)) {
                  this.updateTimer.reset();
                  this.fetchNearestAnarchy();
               }
               break;
            case TELEPORTING:
               if (this.commandDelay.finished(500.0)) {
                  this.commandDelay.reset();
                  this.currentState = AutoMineBot.State.WAITING_WARP;
                  this.sendCommand("#set allowPlace false");
                  this.sendCommand("/warp mine");
                  this.warpDelay.reset();
                  this.anarchyTimer.reset();
               }
               break;
            case WAITING_WARP:
               if (this.isNearbySel()) {
                  this.currentState = AutoMineBot.State.REPLACING;
                  this.replaceCommandDelay.reset();
               } else {
                  this.currentState = AutoMineBot.State.WARPING;
               }
               break;
            case WARPING:
               if (this.warpDelay.finished(7100.0)) {
                  this.warpDelay.reset();
                  this.currentState = AutoMineBot.State.REPLACING;
                  this.replaceCommandDelay.reset();
               }
               break;
            case REPLACING:
               if (this.anarchyTimer.finished(40000.0)) {
                  this.currentState = AutoMineBot.State.SWITCHING;
                  this.anarchyTimer.reset();
               } else {
                  if (this.baritoneWorking && this.baritoneWorkTimer.finished(2000.0)) {
                     this.baritoneWorking = false;
                     this.baritoneWorkTimer.reset();
                  }

                  if (this.replaceCommandDelay.finished(1000.0)) {
                     this.replaceCommandDelay.reset();
                     if (!this.baritoneWorking) {
                        this.sendCommand("#sel replace minecraft:diamond_ore minecraft:air");
                        this.baritoneWorking = true;
                        this.baritoneWorkTimer.reset();
                     }
                  }
               }
               break;
            case WAITING_DONE:
               if (this.anarchyTimer.finished(40000.0)) {
                  this.currentState = AutoMineBot.State.SWITCHING;
                  this.anarchyTimer.reset();
               } else if (this.doneBuildingReceived && this.lastDoneBuildingTime > 0L && System.currentTimeMillis() - this.lastDoneBuildingTime > 5000L) {
                  this.currentState = AutoMineBot.State.SWITCHING;
                  this.doneBuildingReceived = false;
                  this.baritoneWorking = false;
                  this.lastDoneBuildingTime = 0L;
               }
               break;
            case SWITCHING:
               if (this.updateTimer.finished(1000.0)) {
                  this.updateTimer.reset();
                  this.currentState = AutoMineBot.State.GETTING_ANARCHY;
                  this.currentAnarchy = "";
               }
         }
      }
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      if (e.getType() == PacketEvent.Type.RECEIVE) {
         Packet<?> class_25962 = e.getPacket();
         GameMessageS2CPacket gameMessage;
         String message;
         if (class_25962 instanceof GameMessageS2CPacket
            && (message = (gameMessage = (GameMessageS2CPacket)class_25962).content().getString()).contains("[Baritone]")) {
            if (message.contains("Filling now") && this.currentState == AutoMineBot.State.REPLACING) {
               this.baritoneWorking = true;
               this.baritoneWorkTimer.reset();
            }

            if (message.contains("Done building")) {
               long currentTime = System.currentTimeMillis();
               this.baritoneWorking = false;
               if (this.currentState == AutoMineBot.State.REPLACING) {
                  this.currentState = AutoMineBot.State.WAITING_DONE;
                  this.doneBuildingReceived = true;
                  this.lastDoneBuildingTime = currentTime;
                  this.doneBuildingTimer.reset();
               } else if (this.currentState == AutoMineBot.State.WAITING_DONE) {
                  this.lastDoneBuildingTime = currentTime;
                  this.doneBuildingTimer.reset();
               }
            }
         }
      }
   }

   private boolean isNearbySel() {
      if (mc.player == null) {
         return false;
      } else {
         BlockPos playerPos = mc.player.getBlockPos();
         Vec3d playerVec = mc.player.getPos();
         double distToSel1 = playerVec.distanceTo(Vec3d.ofCenter(SEL_1));
         double distToSel2 = playerVec.distanceTo(Vec3d.ofCenter(SEL_2));
         boolean inSel1Area = distToSel1 <= 30.0;
         boolean inSel2Area = distToSel2 <= 30.0;
         int minX = Math.min(SEL_1.getX(), SEL_2.getX());
         int maxX = Math.max(SEL_1.getX(), SEL_2.getX());
         int minY = Math.min(SEL_1.getY(), SEL_2.getY());
         int maxY = Math.max(SEL_1.getY(), SEL_2.getY());
         int minZ = Math.min(SEL_1.getZ(), SEL_2.getZ());
         int maxZ = Math.max(SEL_1.getZ(), SEL_2.getZ());
         boolean inSelArea = playerPos.getX() >= minX - 30.0
            && playerPos.getX() <= maxX + 30.0
            && playerPos.getY() >= minY - 30.0
            && playerPos.getY() <= maxY + 30.0
            && playerPos.getZ() >= minZ - 30.0
            && playerPos.getZ() <= maxZ + 30.0;
         return inSel1Area || inSel2Area || inSelArea;
      }
   }

   private void fetchNearestAnarchy() {
      try {
         HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://85.208.139.128:8000/api/mines/nearest"))
            .timeout(Duration.ofSeconds(5L))
            .GET()
            .build();
         httpClient.sendAsync(request, BodyHandlers.ofString())
            .thenAccept(
               response -> {
                  if (response.statusCode() == 200) {
                     try {
                        String responseBody = response.body();
                        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                        boolean found = json.get("found").getAsBoolean();
                        if (found) {
                           String mineInfo = json.get("mine").getAsString();
                           Pattern pattern = Pattern.compile("/an(\\d+)");
                           Matcher matcher = pattern.matcher(mineInfo);
                           if (matcher.find()) {
                              String anarchyNum = matcher.group(1);
                              String timeToAutoMine = "";
                              Pattern timePattern = Pattern.compile("(\\d{1,2}:\\d{2})");
                              Matcher timeMatcher = timePattern.matcher(mineInfo);
                              if (timeMatcher.find()) {
                                 timeToAutoMine = timeMatcher.group(1);
                              }

                              String finalTimeToAutoMine = timeToAutoMine;
                              mc.execute(
                                 () -> {
                                    if (!anarchyNum.equals(this.currentAnarchy)) {
                                       this.currentAnarchy = anarchyNum;
                                       this.currentState = AutoMineBot.State.TELEPORTING;
                                       this.sendCommand("/an" + anarchyNum);
                                       this.commandDelay.reset();
                                       if (!finalTimeToAutoMine.isEmpty()) {
                                          ChatMessage.brandmessage("Телепортируюсь на анархию " + anarchyNum + ", до авто-шахты " + finalTimeToAutoMine);
                                          Notifications.getInstance()
                                             .addList("Телепортируюсь на анархию " + anarchyNum + ", до авто-шахты " + finalTimeToAutoMine + " секунд", 5000);
                                       } else {
                                          ChatMessage.brandmessage("Телепортируюсь на анархию " + anarchyNum);
                                          Notifications.getInstance().addList("Телепортируюсь на анархию " + anarchyNum, 3000);
                                       }
                                    }
                                 }
                              );
                           }
                        }
                     } catch (Exception var14) {
                     }
                  }
               }
            )
            .exceptionally(ex -> null);
      } catch (Exception var2) {
      }
   }

   private void moveToGoal(Vec3d goal) {
      if (goal != null && mc.player != null && mc.world != null) {
         Float moveYaw = this.getYawMovementToGoalVec(goal, 2);
         if (moveYaw != null) {
            boolean canForward = MathAngle.computeAngleDifference(mc.player.getYaw(), moveYaw) < 45.0F && mc.player.squaredDistanceTo(goal) > 0.5;
            boolean canJump = canForward
               && mc.player.getMovementSpeed() > 0.14F
               && mc.player.horizontalCollision
               && mc.player.getActiveHand() != Hand.MAIN_HAND;
            if (canForward) {
               this.holdForward = true;
               float[] rot = this.rotationsToVec(goal);
               mc.player.setYaw(MathHelper.lerpAngleDegrees(0.35F, mc.player.getYaw(), rot[0]));
               if (canJump) {
                  this.holdJump = true;
               }
            }
         }
      }
   }

   private Float getYawMovementToGoalVec(Vec3d goal, int scanRayRange) {
      if (goal == null) {
         return null;
      } else {
         float predictValue = 1.5F;
         Vec3d scanAt = mc.player.getPos().add(mc.player.getVelocity().x * predictValue, 0.001, mc.player.getVelocity().z * predictValue);
         ArrayList<Vec3d> freeRays = new ArrayList<>();
         int stepDeg = 45;
         double offsetAABB = 0.5;

         for (float yaw = 0.0F; yaw < 360.0F; yaw += stepDeg) {
            double rad = Math.toRadians(yaw);
            Vec3d end = scanAt.add(-Math.sin(rad) * scanRayRange, 0.0, Math.cos(rad) * scanRayRange);
            BlockHitResult result = mc.world.raycast(new RaycastContext(scanAt, end, ShapeType.COLLIDER, FluidHandling.ANY, mc.player));
            if (result == null || result.getType() != Type.BLOCK) {
               Vec3d hitPos = result != null ? result.getPos() : end;
               if (!(hitPos.lengthSquared() < 1.0)) {
                  boolean blocked = false;

                  for (Direction dir : HORIZONTAL_FACES) {
                     Vec3d off = new Vec3d(dir.getOffsetX(), 0.0, dir.getOffsetZ()).multiply(offsetAABB);
                     if (!mc.world.getBlockState(BlockPos.ofFloored(hitPos.add(off))).isAir()) {
                        blocked = true;
                        break;
                     }
                  }

                  if (!blocked) {
                     freeRays.add(hitPos);
                  }
               }
            }
         }

         if (freeRays.isEmpty()) {
            return null;
         } else {
            this.calculatedGoalPartH = freeRays.get(0);
            freeRays.removeIf(n -> this.oldPositions.stream().anyMatch(old -> old.squaredDistanceTo(n) < 0.0225));
            if (freeRays.size() > 1) {
               freeRays.sort(Comparator.comparingDouble(a -> a.distanceTo(goal)));
            }

            if (!freeRays.isEmpty()) {
               this.calculatedGoalPartH = freeRays.get(0);
            }

            if (this.calculatedGoalPartH == null) {
               return null;
            } else {
               if (this.oldPositions.size() > 40) {
                  this.oldPositions.remove(0);
               }

               this.oldPositions.add(this.calculatedGoalPartH);
               return MathAngle.fromVec3d(this.calculatedGoalPartH.subtract(scanAt)).getYaw();
            }
         }
      }
   }

   private float[] rotationsToVec(Vec3d target) {
      Vec3d eye = mc.player.getEyePos();
      double dx = target.x - eye.x;
      double dy = target.y - (eye.y + 0.5);
      double dz = target.z - eye.z;
      double distXZ = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)(Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
      float pitch = (float)(-(Math.atan2(dy, distXZ) * (180.0 / Math.PI)));
      return new float[]{MathHelper.wrapDegrees(yaw), MathHelper.clamp(pitch, -90.0F, 90.0F)};
   }

   private void sendCommand(String command) {
      if (mc.player != null && mc.player.networkHandler != null) {
         mc.player.networkHandler.sendChatMessage(command);
      }
   }

   private static enum State {
      IDLE,
      SETTING_SEL,
      GETTING_ANARCHY,
      TELEPORTING,
      WAITING_WARP,
      WARPING,
      WAITING_REPLACE,
      REPLACING,
      WAITING_DONE,
      SWITCHING;
   }
}
