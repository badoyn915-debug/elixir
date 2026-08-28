package code.elixir.common.repository.macro;

import code.elixir.events.keyboard.KeyEvent;
import code.elixir.features.impl.misc.Panic;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.managers.event.EventManager;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.interfaces.QuickLogger;
import java.util.ArrayList;
import java.util.List;

public class MacroRepository implements QuickImports, QuickLogger {
   public List<Macro> macroList = new ArrayList<>();

   public MacroRepository(EventManager eventManager) {
      EventManager.register(this);
   }

   public void addMacro(String name, String message, int key) {
      this.macroList.add(new Macro(name, message, key));
   }

   public boolean hasMacro(String text) {
      return this.macroList.stream().anyMatch(macro -> macro.name().equalsIgnoreCase(text));
   }

   public void deleteMacro(String text) {
      this.macroList.removeIf(macro -> macro.name().equalsIgnoreCase(text));
   }

   public void clearList() {
      this.macroList.clear();
   }

   @EventHandler
   public void onKey(KeyEvent e) {
      if (!Panic.isPanicMode()) {
         if (mc.player != null && e.action() == 0 && mc.currentScreen == null) {
            this.macroList
               .stream()
               .filter(macro -> macro.key() == e.key())
               .findFirst()
               .ifPresent(macro -> mc.player.networkHandler.sendChatMessage(macro.message()));
         }
      }
   }
}
