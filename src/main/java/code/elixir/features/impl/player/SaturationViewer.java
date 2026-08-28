package code.elixir.features.impl.player;

import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.client.Instance;

public class SaturationViewer extends Module {
   public static SaturationViewer getInstance() {
      return Instance.get(SaturationViewer.class);
   }

   public SaturationViewer() {
      super("SaturationViewer", ModuleCategory.PLAYER);
   }
}
