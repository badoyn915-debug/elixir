package code.elixir.features.impl.misc;

import code.elixir.emotions.EmotionWheelManager;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.features.module.setting.implement.ButtonSetting;
import code.elixir.utils.client.Instance;

public class Globals extends Module {
   public static final boolean ITEM_REPLACER_SYNC_ENABLED = true;
   private final BindSetting emotionWheelBind = new BindSetting("Колесо эмоций", "Клавиша открытия коеса эмоций").setKey(86);
   private final ButtonSetting openEmotionWheelButton = new ButtonSetting("Открыть колесо эмоций", "Открывает экран выбора эмоций")
      .setButtonName("Open")
      .setRunnable(() -> {
         EmotionWheelManager manager = EmotionWheelManager.getInstance();
         if (manager != null) {
            manager.openFromGlobals();
         }
      });

   public static Globals getInstance() {
      return Instance.get(Globals.class);
   }

   public Globals() {
      super("Globals", "Globals", ModuleCategory.MISC);
      this.setup(new Setting[]{this.emotionWheelBind, this.openEmotionWheelButton});
      this.setState(true);
   }

   public static void enforceEnabled() {
      Globals globals = getInstance();
      if (globals != null && !globals.isState()) {
         globals.setState(true);
      }
   }

   @Override
   public void activate() {
      if (!this.isState()) {
         this.setState(true);
      }
   }

   @Override
   public void deactivate() {
      this.setState(true);
   }

   public BindSetting getEmotionWheelBind() {
      return this.emotionWheelBind;
   }

   public static boolean isItemReplacerSyncEnabled() {
      return true;
   }
}
