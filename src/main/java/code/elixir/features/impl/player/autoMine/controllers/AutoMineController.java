package code.elixir.features.impl.player.autoMine.controllers;

import code.elixir.Elixir;
import code.elixir.events.player.InputEvent;
import code.elixir.events.player.RotationUpdateEvent;
import code.elixir.events.render.DrawEvent;
import code.elixir.events.render.WorldRenderEvent;
import code.elixir.features.impl.misc.creeperFarm.WalkDirectionUtil;
import code.elixir.features.impl.player.autoMine.api.MineApiParser;
import code.elixir.features.impl.player.autoMine.receivers.AutoMineModule;
import code.elixir.features.impl.player.autoMine.state.AutoMineState;
import code.elixir.utils.client.chat.ChatMessage;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.aura.utils.MathAngle;
import code.elixir.utils.features.aura.warp.Turns;
import code.elixir.utils.features.aura.warp.TurnsConfig;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import code.elixir.utils.math.task.TaskPriority;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class AutoMineController implements QuickImports {
   private final RegionController regionController;
   private final MineController mineController;
   private final MineApiParser apiParser;
   private final RepairController repairController;
   private AutoMineState state = AutoMineState.WAITING_FOR_MINE;
   private long stateStartTime = 0L;
   private long lastCommandTime = 0L;
   static final int MINE_TIME_THRESHOLD = 12;
   static final long ANARCHY_LOAD_DELAY = 3000L;
   static final long WARP_DELAY = 7000L;
   static final long COMMAND_COOLDOWN = 1000L;
   static final double PROXIMITY_RADIUS = 7.0;
   private String currentAnarchy = null;
   private boolean connectMessageSent = false;
   private boolean alreadyOnAnarchy = false;
   private int noTargetTicks = 0;
   static final int NO_TARGET_THRESHOLD = 40;

   public AutoMineController() {
      this.regionController = new RegionController(RegionController.createRegion(new BlockPos(-33, 76, -7), new BlockPos(-54, 85, -28)));
      this.mineController = new MineController(this.regionController);
      this.apiParser = new MineApiParser();
      this.repairController = new RepairController();
   }

   public void onTick() {
      this.apiParser.fetchAsync();
      switch (this.state) {
         case WAITING_FOR_MINE:
            this.tickWaitingForMine();
            break;
         case JOINING_ANARCHY:
            this.tickJoiningAnarchy();
            break;
         case WAITING_ANARCHY_LOAD:
            this.tickWaitingAnarchyLoad();
            break;
         case WARPING_TO_MINE:
            this.tickWarpingToMine();
            break;
         case WAITING_WARP:
            this.tickWaitingWarp();
            break;
         case RUNNING_TO_CENTER:
            this.tickRunningToCenter();
            break;
         case MINING:
            this.tickMining();
            break;
         case MINE_FINISHED:
            this.tickMineFinished();
            break;
         case REPAIRING:
            this.tickRepairing();
      }
   }

   private void tickWaitingForMine() {
      if (this.apiParser.isMineReady(12)) {
         String anarchyCmd = this.apiParser.getAnarchyCommand();
         if (anarchyCmd != null) {
            if (this.alreadyOnAnarchy) {
               this.applyAnarchyLoadDone();
               return;
            }

            if (!this.connectMessageSent) {
               String mineType = this.apiParser.getMineType() != null ? this.apiParser.getMineType() : "?";
               int timeSec = this.apiParser.getTimeSeconds();
               ChatMessage.brandmessage("Подключение: " + anarchyCmd + " (" + mineType + "), шахта через " + timeSec + " сек");
               this.connectMessageSent = true;
            }

            this.setState(AutoMineState.JOINING_ANARCHY);
            return;
         }
      } else {
         this.connectMessageSent = false;
      }

      if (this.isPlayerInRegion()) {
         this.regionController.onTick();
         if (this.regionController.getTargetBlock() != null) {
            this.setState(AutoMineState.MINING);
         }
      }
   }

   private void tickJoiningAnarchy() {
      String anarchyCmd = this.apiParser.getAnarchyCommand();
      if (anarchyCmd == null) {
         this.setState(AutoMineState.WAITING_FOR_MINE);
      } else {
         if (this.canSendCommand()) {
            this.sendCommand(anarchyCmd);
            this.currentAnarchy = anarchyCmd;
            this.setState(AutoMineState.WAITING_ANARCHY_LOAD);
         }
      }
   }

   private void tickWaitingAnarchyLoad() {
      if (this.getStateElapsed() >= 3000L) {
         this.applyAnarchyLoadDone();
      }
   }

   public void notifyAlreadyConnected() {
      this.alreadyOnAnarchy = true;
      if (this.state == AutoMineState.WAITING_ANARCHY_LOAD) {
         this.applyAnarchyLoadDone();
      }
   }

   private void applyAnarchyLoadDone() {
      if (this.isPlayerInRegion()) {
         this.setState(AutoMineState.MINING);
      } else {
         this.setState(AutoMineState.WARPING_TO_MINE);
      }
   }

   private void tickWarpingToMine() {
      if (this.isPlayerInRegion()) {
         this.setState(AutoMineState.MINING);
      } else {
         if (this.canSendCommand()) {
            this.sendCommand("/warp mine");
            this.setState(AutoMineState.WAITING_WARP);
         }
      }
   }

   private void tickWaitingWarp() {
      if (this.getStateElapsed() >= 7000L) {
         this.setState(AutoMineState.RUNNING_TO_CENTER);
      }
   }

   private void tickRunningToCenter() {
      Vec3d center = this.regionController.getRegionState().getCenterVec();
      double dist = center.subtract(mc.player.getPos()).horizontalLength();
      if (dist < 11.0) {
         this.setState(AutoMineState.MINING);
      }

      this.regionController.onTick();
   }

   private void tickMining() {
      if (this.repairController.needsRepair()) {
         this.setState(AutoMineState.REPAIRING);
         this.repairController.startRepair();
      } else if (!this.isPlayerNearRegion()) {
         this.setState(AutoMineState.WARPING_TO_MINE);
      } else {
         this.regionController.onTick();
         this.mineController.onTick();
         this.noTargetTicks = this.regionController.getTargetBlock() == null ? ++this.noTargetTicks : 0;
         if (this.noTargetTicks >= 40) {
            this.setState(AutoMineState.MINE_FINISHED);
         }
      }
   }

   private void tickRepairing() {
      if (this.repairController.tickRepair()) {
         this.repairController.reset();
         this.setState(AutoMineState.MINING);
      }
   }

   private void tickMineFinished() {
      this.noTargetTicks = 0;
      this.currentAnarchy = null;
      this.connectMessageSent = false;
      this.alreadyOnAnarchy = false;
      this.setState(AutoMineState.WAITING_FOR_MINE);
   }

   public void onInput(InputEvent inputEvent) {
      switch (this.state) {
         case RUNNING_TO_CENTER:
            Vec3d center = this.regionController.getRegionState().getCenterVec();
            Vec3d direction = center.subtract(mc.player.getPos());
            float yaw = TurnsConnection.INSTANCE.getRotation().getYaw();
            PlayerInput pi = WalkDirectionUtil.fromVec3d(direction, yaw);
            inputEvent.setDirectional(pi.forward(), pi.backward(), pi.left(), pi.right());
            break;
         case MINING:
            this.mineController.onInput(inputEvent);
      }
   }

   public void onRotationUpdate(RotationUpdateEvent event) {
      switch (this.state) {
         case RUNNING_TO_CENTER:
            Vec3d center = this.regionController.getRegionState().getCenterVec();
            Turns turns = MathAngle.calculateAngle(center);
            TurnsConnection.INSTANCE
               .rotateTo(
                  turns,
                  new TurnsConfig(this.mineController.getSpAngle(), true, false),
                  TaskPriority.HIGH_IMPORTANCE_1,
                  Elixir.getInstance().getModuleProvider().get(AutoMineModule.class)
               );
            break;
         case MINING:
            this.mineController.onRotationUpdate(event);
         case MINE_FINISHED:
         case REPAIRING:
      }
   }

   public void onWorldRender(WorldRenderEvent event) {
      this.regionController.onWorldRender(event);
      if (this.state == AutoMineState.MINING) {
         this.mineController.onWorldRender(event);
      }
   }

   public void onDraw(DrawEvent drawEvent) {
      if (this.state == AutoMineState.MINING) {
         this.mineController.onDraw(drawEvent);
      }
   }

   public void onWorldChange() {
      this.regionController.onWorldChange();
      this.mineController.onWorldChange();
      if (this.state == AutoMineState.WAITING_ANARCHY_LOAD) {
         this.stateStartTime = System.currentTimeMillis();
      }
   }

   private void setState(AutoMineState newState) {
      this.state = newState;
      this.stateStartTime = System.currentTimeMillis();
   }

   private long getStateElapsed() {
      return System.currentTimeMillis() - this.stateStartTime;
   }

   private boolean canSendCommand() {
      return System.currentTimeMillis() - this.lastCommandTime >= 1000L;
   }

   private void sendCommand(String command) {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         String cmd = command.startsWith("/") ? command.substring(1) : command;
         mc.getNetworkHandler().sendCommand(cmd);
         this.lastCommandTime = System.currentTimeMillis();
      }
   }

   private boolean isPlayerInRegion() {
      if (mc.player == null) {
         return false;
      } else {
         Vec3d playerPos = mc.player.getPos();
         Vec3d center = this.regionController.getRegionState().getCenterVec();
         double dist = Math.abs(center.subtract(playerPos).horizontalLength());
         return dist < 13.0;
      }
   }

   private boolean isPlayerNearRegion() {
      if (mc.player == null) {
         return false;
      } else {
         double px = mc.player.getX();
         double pz = mc.player.getZ();
         BlockPos min = this.regionController.getRegionState().getMin();
         BlockPos max = this.regionController.getRegionState().getMax();
         double minX = Math.min(min.getX(), max.getX());
         double maxX = Math.max(min.getX(), max.getX());
         double minZ = Math.min(min.getZ(), max.getZ());
         double maxZ = Math.max(min.getZ(), max.getZ());
         double dx = px < minX ? minX - px : (px > maxX ? px - maxX : 0.0);
         double dz = pz < minZ ? minZ - pz : (pz > maxZ ? pz - maxZ : 0.0);
         double dist = Math.sqrt(dx * dx + dz * dz);
         return dist <= 7.0;
      }
   }

   public RegionController getRegionController() {
      return this.regionController;
   }

   public MineController getMineController() {
      return this.mineController;
   }

   public MineApiParser getApiParser() {
      return this.apiParser;
   }

   public RepairController getRepairController() {
      return this.repairController;
   }

   public AutoMineState getState() {
      return this.state;
   }

   public long getStateStartTime() {
      return this.stateStartTime;
   }

   public long getLastCommandTime() {
      return this.lastCommandTime;
   }

   public String getCurrentAnarchy() {
      return this.currentAnarchy;
   }

   public boolean isConnectMessageSent() {
      return this.connectMessageSent;
   }

   public boolean isAlreadyOnAnarchy() {
      return this.alreadyOnAnarchy;
   }

   public int getNoTargetTicks() {
      return this.noTargetTicks;
   }
}
