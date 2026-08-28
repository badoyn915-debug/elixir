package code.elixir.features.impl.combat;

import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.client.Instance;

public class NoInteract extends Module {
   public static NoInteract getInstance() {
      return Instance.get(NoInteract.class);
   }

   public NoInteract() {
      super("NoInteract", "NoInteract", ModuleCategory.COMBAT);
   }
}
