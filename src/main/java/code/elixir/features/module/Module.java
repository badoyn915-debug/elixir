package code.elixir.features.module;

import code.elixir.Elixir;
import code.elixir.common.animation.Animation;
import code.elixir.common.animation.Direction;
import code.elixir.common.animation.implement.Decelerate;
import code.elixir.display.hud.DynamicIsland;
import code.elixir.features.impl.render.Hud;
import code.elixir.features.module.setting.SettingRepository;
import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.utils.client.managers.event.EventManager;
import code.elixir.utils.client.sound.SoundManager;
import code.elixir.utils.display.interfaces.QuickImports;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;

public class Module extends SettingRepository implements QuickImports {
   private final String name;
   private final String visibleName;
   private final ModuleCategory category;
   private final Animation animation = new Decelerate().setMs(175).setValue(1.0);
   private int key = -1;
   private int type = 1;
   private int secondKey = -1;
   public boolean state;
   private boolean hidden;

   public Module(String name, ModuleCategory category) {
      this.name = name;
      this.category = category;
      this.visibleName = name;
   }

   public Module(String name, String visibleName, ModuleCategory category) {
      this.name = name;
      this.visibleName = visibleName;
      this.category = category;
   }

   public BindSetting getBind() {
      BindSetting bind = new BindSetting("Бинд", "Клавиша активации");
      bind.setKey(this.key);
      bind.setSecondKey(this.secondKey);
      return bind;
   }

   public void setBindFromSetting(BindSetting bind) {
      if (bind != null) {
         this.key = bind.getKey();
         this.secondKey = bind.getSecondKey();
      }
   }

   public void switchState() {
      this.setState(!this.state);
   }

   public void setState(boolean state) {
      this.animation.setDirection(state ? Direction.FORWARDS : Direction.BACKWARDS);
      if (state != this.state) {
         this.state = state;
         this.handleStateChange();
      }
   }

   private void handleStateChange() {
      try {
         MinecraftClient mc = MinecraftClient.getInstance();
         Hud hud = Hud.getInstance();
         if (hud == null) {
            if (this.state) {
               this.activate();
            } else {
               this.deactivate();
            }

            this.toggleSilent(this.state);
            return;
         }

         if (mc.player != null && mc.world != null) {
            String cleanName = Formatting.strip(this.visibleName) == null ? this.visibleName : Formatting.strip(this.visibleName);
            if (this.state) {
               DynamicIsland.notifyModuleToggle(cleanName, true);
               SoundManager.playSound(SoundManager.ENABLE_MODULE);
               this.activate();
            } else {
               DynamicIsland.notifyModuleToggle(cleanName, false);
               SoundManager.playSound(SoundManager.DISABLE_MODULE);
               this.deactivate();
            }
         }

         this.toggleSilent(this.state);
      } catch (Throwable var5) {
         this.state = false;
         LogManager.getLogger("Elixir").error("Failed to change state for module {}", this.name, var5);
      }
   }

   private void toggleSilent(boolean activate) {
      EventManager eventManager = Elixir.getInstance().getEventManager();
      if (eventManager != null) {
         try {
            if (activate) {
               if (!EventManager.canInspectEventHandlers(this)) {
                  this.state = false;
                  return;
               }

               EventManager.register(this);
            } else {
               eventManager.unregister(this);
            }
         } catch (Throwable var4) {
            this.state = false;
            LogManager.getLogger("Elixir").error("Failed to {} event handlers for module {}", activate ? "register" : "unregister", this.name, var4);
         }
      }
   }

   public void activate() {
   }

   public void deactivate() {
   }

   public String getName() {
      return this.name;
   }

   public String getVisibleName() {
      return this.visibleName;
   }

   public ModuleCategory getCategory() {
      return this.category;
   }

   public Animation getAnimation() {
      return this.animation;
   }

   public int getKey() {
      return this.key;
   }

   public int getType() {
      return this.type;
   }

   public int getSecondKey() {
      return this.secondKey;
   }

   public boolean isState() {
      return this.state;
   }

   public boolean isHidden() {
      return this.hidden;
   }

   public void setKey(int key) {
      this.key = key;
   }

   public void setType(int type) {
      this.type = type;
   }

   public void setSecondKey(int secondKey) {
      this.secondKey = secondKey;
   }

   public void setHidden(boolean hidden) {
      this.hidden = hidden;
   }
}
