package code.elixir.features.impl.render;

import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class Fullbright extends Module {
   public final SelectSetting modeSetting = new SelectSetting("Режим", "Выберите режим работы").value("Ночное зрение", "Гамма").selected("Гамма");
   private double previousGamma = 1.0;

   public static Fullbright getInstance() {
      return Instance.get(Fullbright.class);
   }

   public Fullbright() {
      super("Fullbright", "Fullbright", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.modeSetting});
   }

   @Override
   public void activate() {
      super.activate();
      if (mc != null && mc.options != null) {
         this.previousGamma = (Double)mc.options.getGamma().getValue();
      }
   }

   @Override
   public void deactivate() {
      if (mc != null && mc.options != null) {
         mc.options.getGamma().setValue(this.previousGamma);
      }

      if (mc.player != null && this.modeSetting.isSelected("Ночное зрение")) {
         mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
      }

      super.deactivate();
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player != null && mc.options != null) {
         if (this.modeSetting.isSelected("Гамма")) {
            mc.options.getGamma().setValue(10.0);
         } else if (this.modeSetting.isSelected("Ночное зрение") && !mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
         }
      }
   }
}
