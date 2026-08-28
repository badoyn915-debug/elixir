package code.elixir.features.impl.render.jumpCircle.receiver;

import code.elixir.events.player.JumpEvent;
import code.elixir.events.render.WorldRenderEvent;
import code.elixir.features.impl.render.jumpCircle.data.JumpCircleStorage;
import code.elixir.features.impl.render.jumpCircle.handler.JumpCircleHandler;
import code.elixir.features.impl.render.jumpCircle.updater.JumpCircleUpdater;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.features.module.setting.implement.RadioSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.color.ColorAssist;
import net.minecraft.entity.player.PlayerEntity;

public class JumpCircleModule extends Module {
   private final SliderSettings lifeTime = new SliderSettings("Время жизни", "").range(500, 1500).setInteger(true);
   private final SliderSettings size = new SliderSettings("Размер", "").range(1, 3).step(0.5F);
   public static RadioSetting colorMode = new RadioSetting("Режим цвета", "Выбор источника цвета", new String[]{"Клиентский", "Кастом"}, "Клиентский");
   public static ColorSetting color = new ColorSetting("Кастомный цвет", "Кастомный цвет круга")
      .value(ColorAssist.getClientColor())
      .visible(() -> colorMode.get().equals("Кастом"));
   private final JumpCircleStorage storage = new JumpCircleStorage(this);
   private final JumpCircleHandler handler = new JumpCircleHandler(this);
   private final JumpCircleUpdater updater = new JumpCircleUpdater(this);

   public JumpCircleModule() {
      super("JumpCircle", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.lifeTime, this.size, colorMode, color});
   }

   @EventHandler
   private void jumpEventReceive(JumpEvent jumpEvent) {
      PlayerEntity player = jumpEvent.getPlayer();
      this.storage.add(player);
   }

   @EventHandler
   private void renderEventReceive(WorldRenderEvent renderEvent) {
      if (this.storage.isPresent()) {
         this.updater.update();
         this.handler.provideRender(renderEvent.getStack(), renderEvent.getPartialTicks());
      }
   }

   public SliderSettings getLifeTime() {
      return this.lifeTime;
   }

   public SliderSettings getSize() {
      return this.size;
   }

   public JumpCircleStorage getStorage() {
      return this.storage;
   }

   public JumpCircleHandler getHandler() {
      return this.handler;
   }

   public JumpCircleUpdater getUpdater() {
      return this.updater;
   }
}
