package code.elixir.display.screens.clickgui.components.implement.module;

import code.elixir.features.module.Module;
import java.util.HashMap;
import java.util.Map;

public final class ModuleDescriptions {
   public static final String DEFAULT = "Описание модуля отсутствует";
   private static final Map<String, String> BY_NAME = new HashMap<>();

   private ModuleDescriptions() {
   }

   public static String getDescription(Module module) {
      if (module == null) {
         return "Описание модуля отсутствует";
      } else {
         String description = BY_NAME.get(module.getName());
         return description != null ? description : "Описание модуля отсутствует";
      }
   }

   public static String tryGetDescription(Module module) {
      try {
         return getDescription(module);
      } catch (Throwable var2) {
         return null;
      }
   }

   public static boolean isMissingDescription(String description) {
      return description == null || description.isEmpty() || "Описание модуля отсутствует".equals(description);
   }

   static {
      BY_NAME.put("Particles", "Отображает частицы в мире");
      BY_NAME.put("LineGlyphs", "Рисует светящиеся линейные глифы вокруг игрока");
      BY_NAME.put("TargetStrafe", "Преследует цель во время атаки");
      BY_NAME.put("Strafe", "Позволяет стрейфиться");
      BY_NAME.put("AirStuck", "Позволяет зависнуть в воздухе");
      BY_NAME.put("ElytraMotion", "Замирает на элитрах возле противника");
      BY_NAME.put("ElytraRecast", "Автоматически прожимает пробел на элитре");
      BY_NAME.put("ElytraJump", "Вертикальный полёт на элитре под Grim");
      BY_NAME.put("GrimGlide", "Передвижение на элитре без фейерверков");
      BY_NAME.put("ElytraTarget", "Топовый перегон на элитре");
      BY_NAME.put("ElytraWhatsapp", "Зависание в воздухе на элитре для таргета");
      BY_NAME.put("Flight", "Полёт с режимами Элитра Y и Grim Glide");
      BY_NAME.put("ElytraResolver", "Долёт и отлёт для элитра-ротки");
      BY_NAME.put("HighJump", "Подбрасывает вас вверх");
      BY_NAME.put("ShiftTap", "Автоматически приседает при ударе");
      BY_NAME.put("AspectRatio", "Изменяет соотношение сторон экрана");
      BY_NAME.put("FreeLook", "Позволяет свободно вращать камеру");
      BY_NAME.put("ClickPearl", "Бросает эндер-жемчуг по нажатию клавиши");
      BY_NAME.put("ClickFriend", "Добавляет игроков в список друзей при нажатии бинда");
      BY_NAME.put("WindHop", "Использует заряд ветра для прыжка");
      BY_NAME.put("TargetESP", "Подсвечивает противника");
      BY_NAME.put("NoWeb", "Убирает замедление при движении через паутину");
      BY_NAME.put("ServerHelper", "Предоставляет полезные функции для игры на серверах");
      BY_NAME.put("AstrummcHelper", "Подсказывает ресурсы для крафта легендарных оружий на Astrummc");
      BY_NAME.put("ItemScroller", "Уменьшает заддержку на складирование предметов");
      BY_NAME.put("Hud", "Отображает дополнительную информацию на экране");
      BY_NAME.put("ClickGui", "Настраивает внешний вид и поведение ClickGui");
      BY_NAME.put("AuctionHelper", "Помогает на аукционе");
      BY_NAME.put("Prediction", "Предсказывает траекторию полета снарядов");
      BY_NAME.put("XRay", "Позволяет видеть сквозь блоки для поиска руд");
      BY_NAME.put("Aura", "Автоматически наводится и бьёт цель");
      BY_NAME.put("TriggerBot", "Автоматически атакует при наведении на цель");
      BY_NAME.put("AutoSwap", "Позволяет менять предметы во второй руке по бинду");
      BY_NAME.put("NoFriendDamage", "Не дает ударить друзей");
      BY_NAME.put("AntiBot", "Определяет и метит ботов, вызванных системой античита");
      BY_NAME.put("FastBow", "Быстро стреляет из лука при зажатой ПКМ");
      BY_NAME.put("AutoExplosion", "Автоматически ставит и взрывает кристаллы");
      BY_NAME.put("AutoSprint", "Автоматически включает спринт при движении");
      BY_NAME.put("NoPush", "Отключает отталкивание");
      BY_NAME.put("ElytraHelper", "Помощник для действий с элитрами");
      BY_NAME.put("ServerJoiner", "Автоматически подключается к серверу");
      BY_NAME.put("NoDelay", "Убирает задержки при выполнении действий");
      BY_NAME.put("Velocity", "Уменьшает отбрасывание персонажа");
      BY_NAME.put("AutoRespawn", "Автоматически возрождает игрока после смерти");
      BY_NAME.put("NoSlow", "Убирает замедление при использовании предметов");
      BY_NAME.put("GuiMove", "Позволяет двигаться при открытом интерфейсе");
      BY_NAME.put("Blink", "Замедляет отправку пакетов на сервер");
      BY_NAME.put("AutoTool", "Автоматически выбирает подходящий инструмент для добычи");
      BY_NAME.put("FastBreak", "Ускоряет разрушение блоков");
      BY_NAME.put("CameraSettings", "Настраивает камеру игрока");
      BY_NAME.put("Speed", "Увеличивает скорость передвижения игрока");
      BY_NAME.put("SwingAnimation", "Настраивает анимацию взмаха руки");
      BY_NAME.put("ViewModel", "Изменяет отображение позиции предметов в руке");
      BY_NAME.put("BlockOverlay", "Подсвечивает блоки для лучшей видимости");
      BY_NAME.put("ItemReplacer", "Заменяет модели мечей и топоров только у вас");
      BY_NAME.put("AutoBrew", "Автоматически варит зелья на варочной стойке");
      BY_NAME.put("ElytraGlide", "Позволяет плавно скользить при движении");
      BY_NAME.put("Esp", "Показывает сущностей через стены");
      BY_NAME.put("EspSkeleton", "Рисует скелет игроков через стены");
      BY_NAME.put("NameTags", "Отображает информацию о сущности над её головой");
      BY_NAME.put("ShulkerView", "Позволяет видеть содержимое шалкер-боксов");
      BY_NAME.put("BlockESP", "Подсвечивает определенные блоки");
      BY_NAME.put("AutoTotem", "Автоматически использует тотемы бессмертия");
      BY_NAME.put("EnderChestPlus", "Расширяет функционал эндер-сундука");
      BY_NAME.put("FreeCam", "Предоставляет инструменты для свободной камеры");
      BY_NAME.put("ChestStealer", "Быстро забирает предметы из контейнеров");
      BY_NAME.put("AutoTpAccept", "Автоматически принимает запросы на телепортацию");
      BY_NAME.put("AutoAuth", "Автоматически вводит пароль/регистрируется для авторизации");
      BY_NAME.put("Arrows", "Отображает стрелочки на сущностей");
      BY_NAME.put("AutoLeave", "Автоматически покидает сервер при угрозе");
      BY_NAME.put("WorldTweaks", "Настраивает параметры мира");
      BY_NAME.put("NoClip", "Позволяет проходить сквозь блоки");
      BY_NAME.put("NoRender", "Отключает отображение определенных объектов");
      BY_NAME.put("NameProtect", "Скрывает имена игроков для защиты");
      BY_NAME.put("SeeInvisible", "Позволяет видеть невидимых игроков");
      BY_NAME.put("AutoArmor", "Автоматически надевает лучшую броню");
      BY_NAME.put("AutoUse", "Автоматически использует предметы");
      BY_NAME.put("NoInteract", "Блокирует взаимодействие с объектами");
      BY_NAME.put("CrossHair", "Настраивает отображение прицела");
      BY_NAME.put("SuperFireWork", "Усиливает действие фейерверка для полета");
      BY_NAME.put("Spider", "Позволяет ползать по стенам как паук");
      BY_NAME.put("SafeWalk", "Предотвращает падение с краев блоков");
      BY_NAME.put("ServerRPSpoofer", "Подделывает данные ресурс-пака для серверов");
      BY_NAME.put("NoFall", "Предотвращает получение урона от падения");
      BY_NAME.put("DeathCoords", "Сохраняет координаты места смерти");
      BY_NAME.put("XCarry", "Позволяет хранить предметы в слотах для крафта");
      BY_NAME.put("AntiAFK", "Предотвращает кик за бездействие");
      BY_NAME.put("FakePing", "Изменяет отображаемый пинг");
      BY_NAME.put("LevitationControl", "Контролирует эффект левитации");
      BY_NAME.put("KillEffect", "Добавляет визуальные эффекты при убийстве");
      BY_NAME.put("UseTracker", "Отслеживает использование предметов");
      BY_NAME.put("ShulkerBypass", "Позволяет доставать вещи из шалкер-боксов находясь в пвп");
      BY_NAME.put("ChinaHat", "Добавляет отображение китайской шляпы");
      BY_NAME.put("CustomCape", "Отображает кастомный плащ Elixir на вашем персонаже");
      BY_NAME.put("UseIndicator", "Показывает предмет и прогресс еды/зелий на теле игрока");
      BY_NAME.put("PacketCriticals", "Наносит критические удары без надобности прыгать");
      BY_NAME.put("JumpCircle", "Добавляет отображение круга при прыжке");
      BY_NAME.put("AutoDodge", "Автоматически уклоняется от снарядов");
      BY_NAME.put("ClientSounds", "Настраивает звуки клиента");
      BY_NAME.put("Chams", "Подсвечивает игроков поверх модели");
   }
}
