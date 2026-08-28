package code.elixir.features.module;

import code.elixir.common.logger.implement.ConsoleLogger;
import code.elixir.display.screens.clickgui.MenuScreen;
import code.elixir.events.keyboard.KeyEvent;
import code.elixir.features.impl.misc.Panic;
import code.elixir.features.module.exception.ModuleException;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.managers.event.EventManager;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.interfaces.QuickLogger;
import java.util.List;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class ModuleSwitcher implements QuickLogger, QuickImports {
   private final List<Module> modules;

   public ModuleSwitcher(List<Module> modules, EventManager eventManager) {
      this.modules = modules;
      EventManager.register(this);
   }

   @EventHandler
   public void onKey(KeyEvent event) {
      if (!Panic.isPanicMode()) {
         if (event.key() == 344 && event.action() == 1 && mc.currentScreen == null && mc.player != null) {
            this.openClickGui();
         } else {
            for (Module module : this.modules) {
               if (!module.getName().equals("ClickGui")) {
                  int keyCode = module.getKey();
                  int secondKey = module.getSecondKey();
                  boolean isCombo = secondKey != -1 && secondKey != -1;
                  if (isCombo) {
                     if (event.key() == secondKey && mc.currentScreen == null && this.isKeyPressed(keyCode)) {
                        try {
                           if (module.getName().equals("ClickGui") && event.action() == 0) {
                              this.openClickGui();
                              return;
                           }

                           this.handleModuleState(module, event.action());
                        } catch (Exception var10) {
                           this.handleException(module.getName(), var10);
                        }
                     }
                  } else if (event.key() == keyCode && mc.currentScreen == null) {
                     try {
                        if (module.getName().equals("ClickGui") && event.action() == 0) {
                           this.openClickGui();
                           return;
                        }

                        this.handleModuleState(module, event.action());
                     } catch (Exception var9) {
                        this.handleException(module.getName(), var9);
                     }
                  }
               }
            }
         }
      }
   }

   private void openClickGui() {
      MenuScreen.getInstance().openGui();
   }

   private boolean isKeyPressed(int keyCode) {
      return keyCode != -1 && keyCode != -1 ? GLFW.glfwGetKey(mc.getWindow().getHandle(), keyCode) == 1 : false;
   }

   public void handleModuleState(Module module, int action) {
      if (!module.getName().equals("ClickGui")) {
         if (module.getType() == 1 && action == 1) {
            module.switchState();
         }
      }
   }

   public void handleException(String moduleName, Exception e) {
      ConsoleLogger consoleLogger = new ConsoleLogger();
      if (e instanceof ModuleException) {
         this.logDirect("[" + moduleName + "] " + Formatting.RED + e.getMessage());
      } else {
         consoleLogger.log("Error in module " + moduleName + ": " + e.getMessage());
      }
   }
}
