package code.elixir.features.impl.movement.autoMystGlider.states;

import code.elixir.features.impl.movement.autoMystGlider.handers.StealHandler;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.math.time.StopWatch;
import net.minecraft.util.math.BlockPos;

public class ConnectionState implements QuickImports {
   private final GlideState glideState;
   private final StealHandler stealHandler;
   private boolean connected = true;
   private int anarchy = -1;
   private long leaveTime;
   private long remainingTime;
   private long phaseStartTime = System.currentTimeMillis();
   private GlidePhase phase = GlidePhase.FLYING;
   private StopWatch timer = new StopWatch();

   public ConnectionState(GlideState glideState, StealHandler stealHandler) {
      this.glideState = glideState;
      this.stealHandler = stealHandler;
   }

   public boolean isReconnected() {
      return this.phase == GlidePhase.LOOTING;
   }

   public void transitionTo(GlidePhase newPhase) {
      this.phase = newPhase;
      this.phaseStartTime = System.currentTimeMillis();
   }

   public void hub(GlidePhase nextPhase) {
      if (this.timer.finished(1000.0)) {
         this.timer.reset();
         this.connected = false;
         this.leaveTime = System.currentTimeMillis();
         mc.getNetworkHandler().sendCommand("hub");
         this.transitionTo(nextPhase);
      }
   }

   public void joinAnarchy(int num, GlidePhase nextPhase) {
      if (this.timer.finished(1000.0)) {
         this.timer.reset();
         this.connected = true;
         this.anarchy = num;
         mc.getNetworkHandler().sendCommand("an" + num);
         this.transitionTo(nextPhase);
      }
   }

   public void reload() {
      this.connected = true;
      this.leaveTime = -1L;
      this.remainingTime = -1L;
      this.phase = GlidePhase.FLYING;
      this.phaseStartTime = System.currentTimeMillis();
      this.timer.reset();
   }

   public boolean successfully() {
      return mc.world.getBlockState(BlockPos.ofFloored(mc.player.getPos())).isSolid();
   }

   public void setConnected(boolean connected) {
      this.connected = connected;
   }

   public void setAnarchy(int anarchy) {
      this.anarchy = anarchy;
   }

   public void setLeaveTime(long leaveTime) {
      this.leaveTime = leaveTime;
   }

   public void setRemainingTime(long remainingTime) {
      this.remainingTime = remainingTime;
   }

   public void setPhaseStartTime(long phaseStartTime) {
      this.phaseStartTime = phaseStartTime;
   }

   public void setPhase(GlidePhase phase) {
      this.phase = phase;
   }

   public void setTimer(StopWatch timer) {
      this.timer = timer;
   }

   public GlideState getGlideState() {
      return this.glideState;
   }

   public StealHandler getStealHandler() {
      return this.stealHandler;
   }

   public boolean isConnected() {
      return this.connected;
   }

   public int getAnarchy() {
      return this.anarchy;
   }

   public long getLeaveTime() {
      return this.leaveTime;
   }

   public long getRemainingTime() {
      return this.remainingTime;
   }

   public long getPhaseStartTime() {
      return this.phaseStartTime;
   }

   public GlidePhase getPhase() {
      return this.phase;
   }

   public StopWatch getTimer() {
      return this.timer;
   }
}
