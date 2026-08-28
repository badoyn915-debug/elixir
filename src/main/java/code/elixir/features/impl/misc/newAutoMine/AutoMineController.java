package code.elixir.features.impl.misc.newAutoMine;

import code.elixir.events.player.InputEvent;
import code.elixir.events.player.RotationUpdateEvent;
import code.elixir.features.impl.misc.newAutoMine.api.MineParser;
import net.minecraft.util.math.BlockPos;

public class AutoMineController {
   private AutoMineState autoMineState = AutoMineState.STAY;
   private final AutoMineStateSelector autoMineStateSelector = new AutoMineStateSelector(this);
   private final RegionState regionState = new RegionState(new BlockPos(-33, 76, -7), new BlockPos(-54, 85, -28));
   private final MineParser mineParser = new MineParser();
   private final ConnectProvider connectProvider = new ConnectProvider();

   public void onTick() {
      this.autoMineState = this.autoMineStateSelector.selectState();
      this.update();
   }

   public void onRotationUpdate(RotationUpdateEvent rotationUpdateEvent) {
   }

   public void onInput(InputEvent inputEvent) {
   }

   private void update() {
      switch (this.autoMineState) {
      }
   }

   private void miningState() {
   }

   private void walkToRegionState() {
   }

   private void idlingWaitWarpState() {
   }

   private void idlingWaitAnarchyState() {
   }

   public AutoMineState getAutoMineState() {
      return this.autoMineState;
   }

   public AutoMineStateSelector getAutoMineStateSelector() {
      return this.autoMineStateSelector;
   }

   public RegionState getRegionState() {
      return this.regionState;
   }

   public MineParser getMineParser() {
      return this.mineParser;
   }

   public ConnectProvider getConnectProvider() {
      return this.connectProvider;
   }

   public void setAutoMineState(AutoMineState autoMineState) {
      this.autoMineState = autoMineState;
   }
}
