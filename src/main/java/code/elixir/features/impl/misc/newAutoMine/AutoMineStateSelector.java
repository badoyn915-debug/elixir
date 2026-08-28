package code.elixir.features.impl.misc.newAutoMine;

import code.elixir.features.impl.misc.newAutoMine.api.MineParser;
import code.elixir.utils.display.interfaces.QuickImports;

public class AutoMineStateSelector implements QuickImports {
   private final AutoMineController autoMineController;

   public AutoMineStateSelector(AutoMineController autoMineController) {
      this.autoMineController = autoMineController;
   }

   public AutoMineState selectState() {
      RegionState regionState = this.autoMineController.getRegionState();
      MineParser mineParser = this.autoMineController.getMineParser();
      ConnectProvider connectProvider = this.autoMineController.getConnectProvider();
      switch (this.autoMineController.getAutoMineState()) {
         default:
            return AutoMineState.ON_MINING;
      }
   }
}
