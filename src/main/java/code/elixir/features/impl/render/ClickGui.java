package code.elixir.features.impl.render;

import code.elixir.Elixir;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.ModuleRepository;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.RadioSetting;
import code.elixir.features.module.setting.implement.SliderSettings;

public class ClickGui extends Module {
   private static ClickGui instance;
   public static final int OPEN_KEY = 344;

   // Mode Switch: Mode 1 = Classic Columns, Mode 2 = Liquid Glass Sidebar
   private final RadioSetting guiType = new RadioSetting("Режим GUI", "Выбор режима отображения ClickGUI", new String[]{"Классический", "Liquid Glass"}, "Liquid Glass");

   private final BooleanSetting showCustomization = new BooleanSetting("Показывать Customization", "Показывает панель Customization в GUI")
      .setValue(true);
   private final BooleanSetting showModuleDescriptions = new BooleanSetting("Описание модулей", "Показывает описание модуля при наведении курсора")
      .setValue(true);
   private final BooleanSetting backgroundBlur = new BooleanSetting("Blur Background", "Легкое размытие заднего фона при открытом ClickGUI")
      .setValue(true);
   private final BooleanSetting gradient = new BooleanSetting("Градиент", "Показывает градиент снизу экрана")
      .setValue(true);
   private final BooleanSetting snowflakes = new BooleanSetting("Snowflakes", "Падающие снежинки на фоне интерфейса")
      .setValue(true);
   private final BooleanSetting shine = new BooleanSetting("Блик", "Анимированная полоска блика на панелях ClickGUI")
      .setValue(false);
   private final SliderSettings shineSize = new SliderSettings("Размер блика", "Ширина полоски блика")
      .setValue(1.0F)
      .range(0.5F, 2.0F)
      .step(0.05F)
      .visible(this.shine::isValue);
   private final SliderSettings shineSpeed = new SliderSettings("Скорость блика", "Скорость движения блика")
      .setValue(1.0F)
      .range(0.5F, 2.0F)
      .step(0.05F)
      .visible(this.shine::isValue);
   private final SliderSettings shineBrightness = new SliderSettings("Яркость блика", "Яркость свечения блика")
      .setValue(0.45F)
      .range(0.1F, 1.0F)
      .step(0.05F)
      .visible(this.shine::isValue);
   private final SliderSettings gradientIntensity = new SliderSettings("Интенсивность градиента", "Высота градиента")
      .setValue(0.3F)
      .range(0.1F, 0.5F)
      .step(0.05F);
   public static SliderSettings guiScale = new SliderSettings("Масштаб GUI", "Масштаб интерфейса ClickGUI")
      .setValue(1.0F)
      .range(0.5F, 1.5F)
      .step(0.05F);
   public static SliderSettings scrollSpeed = new SliderSettings("Скорость скролла", "Количество модулей за один скролл")
      .setValue(1.0F)
      .range(1.0F, 4.0F)
      .step(1.0F);

   private final BooleanSetting liquidGlass = new BooleanSetting("Liquid Glass (iOS)", "Эффект жидкого матового стекла как в iOS / iPhone")
      .setValue(true)
      .visible(() -> this.guiType.get().equals("Liquid Glass"));
   private final SliderSettings glassAlpha = new SliderSettings("Прозрачность стекла", "Прозрачность панелей интерфейса")
      .setValue(0.85F)
      .range(0.3F, 1.0F)
      .step(0.02F)
      .visible(() -> this.liquidGlass.isValue() && this.guiType.get().equals("Liquid Glass"));
   private final SliderSettings glassFresnel = new SliderSettings("Блик по краям (Edge)", "Интенсивность светлого контура стекла")
      .setValue(0.12F)
      .range(0.0F, 0.5F)
      .step(0.01F)
      .visible(() -> this.liquidGlass.isValue() && this.guiType.get().equals("Liquid Glass"));

   public static ClickGui getInstance() {
      if (instance != null) {
         return instance;
      } else {
         try {
            Elixir alphaDlc = Elixir.getInstance();
            if (alphaDlc == null) {
               return null;
            } else {
               ModuleRepository moduleRepository = alphaDlc.getModuleRepository();
               if (moduleRepository == null) {
                  return null;
               } else {
                  instance = moduleRepository.modules().stream().filter(m -> m instanceof ClickGui).map(m -> (ClickGui)m).findFirst().orElse(null);
                  return instance;
               }
            }
         } catch (Exception var2) {
            return null;
         }
      }
   }

   public ClickGui() {
      super("ClickGui", "ClickGui", ModuleCategory.RENDER);
      this.setup(
         new Setting[]{
            this.guiType,
            this.liquidGlass,
            this.glassAlpha,
            this.glassFresnel,
            guiScale,
            this.snowflakes,
            this.showCustomization,
            this.showModuleDescriptions,
            this.backgroundBlur,
            this.gradient,
            this.shine,
            this.shineSize,
            this.shineSpeed,
            this.shineBrightness,
            this.gradientIntensity,
            scrollSpeed
         }
      );
      this.setState(true);
      this.setKey(344);
      instance = this;
   }

   public boolean isClassic() {
      return this.guiType.get().equals("Классический");
   }

   public boolean isLiquidGlass() {
      return this.guiType.get().equals("Liquid Glass");
   }

   public String getGuiType() {
      return this.guiType.get();
   }

   @Override
   public void setKey(int key) {
      super.setKey(344);
   }

   @Override
   public int getKey() {
      return 344;
   }

   @Override
   public void setSecondKey(int secondKey) {
      super.setSecondKey(-1);
   }

   @Override
   public int getSecondKey() {
      return -1;
   }

   public boolean shouldRenderSnowflakes() {
      return this.snowflakes.isValue();
   }

   public boolean shouldRenderShine() {
      return this.shine.isValue();
   }

   public float getShineSize() {
      return this.shineSize.getValue();
   }

   public float getShineSpeed() {
      return this.shineSpeed.getValue();
   }

   public float getShineBrightness() {
      return this.shineBrightness.getValue();
   }

   public static void enforceOpenKey() {
      ClickGui clickGui = getInstance();
      if (clickGui != null && clickGui.getKey() != 344) {
         clickGui.setKey(344);
      }
   }

   public boolean shouldRenderCustomization() {
      return this.showCustomization.isValue();
   }

   public boolean shouldShowCustomization() {
      return this.showCustomization.isValue();
   }

   public boolean shouldShowModuleDescriptions() {
      return this.showModuleDescriptions.isValue();
   }

   public boolean shouldRenderBackgroundBlur() {
      return this.backgroundBlur.isValue();
   }

   public boolean shouldRenderLiquidGlass() {
      return this.liquidGlass.isValue();
   }

   public float getGlassAlpha() {
      return this.glassAlpha.getValue();
   }

   public float getGlassFresnel() {
      return this.glassFresnel.getValue();
   }

   public boolean shouldRenderGradient() {
      return this.gradient.isValue();
   }

   public float getGradientIntensity() {
      return this.gradientIntensity.getValue();
   }

   public float getGuiScale() {
      return guiScale.getValue();
   }

   public static float getStaticGuiScale() {
      return guiScale != null ? guiScale.getValue() : 1.0F;
   }

   public static float getStaticScrollSpeed() {
      return scrollSpeed != null ? scrollSpeed.getValue() : 1.0F;
   }
}
