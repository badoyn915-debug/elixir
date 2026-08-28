package code.elixir.features.impl.misc;

import code.elixir.events.chat.ChatEvent;
import code.elixir.events.item.UsingItemEvent;
import code.elixir.events.keyboard.KeyEvent;
import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.InputEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.impl.misc.serverhelper.BravoHvHHandler;
import code.elixir.features.impl.misc.serverhelper.CustomHandler;
import code.elixir.features.impl.misc.serverhelper.FunTimeHandler;
import code.elixir.features.impl.misc.serverhelper.HolyWorldHandler;
import code.elixir.features.impl.misc.serverhelper.LonyGriefHandler;
import code.elixir.features.impl.misc.serverhelper.MetaHvHHandler;
import code.elixir.features.impl.misc.serverhelper.MineBlazeHandler;
import code.elixir.features.impl.misc.serverhelper.ReallyworldHandler;
import code.elixir.features.impl.misc.serverhelper.ServerHelperHandler;
import code.elixir.features.impl.misc.serverhelper.ServerHelperOverlay;
import code.elixir.features.impl.misc.serverhelper.ServerHelperRenderItem;
import code.elixir.features.impl.misc.serverhelper.SpookyTimeHandler;
import code.elixir.features.impl.misc.serverhelper.SunriseHandler;
import code.elixir.features.impl.movement.GuiMove;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.packet.network.Network;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.interactions.inv.InventoryFlowManager;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.interactions.item.ItemUseHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

public class ServerHelper extends Module {
   private final SelectSetting serverMode = new SelectSetting("Сервер", "Режим сервера")
      .value("SpookyTime", "HolyWorld", "LonyGrief", "Reallyworld", "MetaHvH", "BravoHvH", "Sunrise", "MineBlaze", "FunTime", "Custom")
      .selected("SpookyTime");
   private final BooleanSetting onlyHotbar = new BooleanSetting("Только с хотбара", "Использовать только предметы из хотбара").setValue(false);
   private final BindSetting dezorientaciyaKeyBind = this.bind("Дезориентация", () -> this.isMode("SpookyTime"));
   private final BindSetting yavnayaPylKeyBind = this.bind("Явная пыль", () -> this.isMode("SpookyTime"));
   private final BindSetting plastKeyBind = this.bind("Пласт", () -> this.isMode("SpookyTime"));
   private final BindSetting trapkaSpookyKeyBind = this.bind("Трапка Spooky", () -> this.isMode("SpookyTime"));
   private final BindSetting ognennyySmerchKeyBind = this.bind("Огненный смерч", () -> this.isMode("SpookyTime"));
   private final BindSetting snezhokZamorozkaKeyBind = this.bind("Снежок заморозка", () -> this.isMode("SpookyTime"));
   private final BindSetting bozhyaAuraKeyBind = this.bind("Божья аура", () -> this.isMode("SpookyTime"));
   private final BindSetting obychnayaLivalkaKeyBind = this.bind("Обычная ливалка", () -> this.isMode("LonyGrief"));
   private final BindSetting unikalnoePeryshkoKeyBind = this.bind("Уникальное перышко", () -> this.isMode("LonyGrief"));
   private final BindSetting livalkaSPlatformoyKeyBind = this.bind("Ливалка с платформой", () -> this.isMode("LonyGrief"));
   private final BindSetting unikalnayaTrapkaKeyBind = this.bind("Уникальная трапка", () -> this.isMode("LonyGrief"));
   private final BooleanSetting avtoChatIgraUravnenieEnabled = new BooleanSetting("Авто чат-игра (уравнение)", "Решает уравнения в чате")
      .setValue(false)
      .visible(() -> this.isMode("LonyGrief"));
   private final BindSetting puzyrekOpytaKeyBind = this.bind("Пузырёк опыта", () -> this.isMode("HolyWorld"));
   private final BindSetting trapkaKeyBind = this.bind("Трапка", () -> this.isMode("HolyWorld"));
   private final BindSetting vzryvnayaTrapkaKeyBind = this.bind("Взрывная трапка", () -> this.isMode("HolyWorld"));
   private final BindSetting proschalnyyGulKeyBind = this.bind("Прощальный гул", () -> this.isMode("HolyWorld"));
   private final BindSetting vzryvnayaShtuchkaKeyBind = this.bind("Взрывная штучка", () -> this.isMode("HolyWorld"));
   private final BindSetting stanKeyBind = this.bind("Стан", () -> this.isMode("HolyWorld"));
   private final BindSetting komSnegaKeyBind = this.bind("Ком снега", () -> this.isMode("HolyWorld"));
   private final BindSetting ryukzakKeyBind = this.bind("Рюкзак", () -> this.isMode("HolyWorld"));
   private final BooleanSetting antiCrashEnabled = new BooleanSetting("Anti-Crash", "Блокирует краш-пакеты HolyWorld")
      .setValue(true)
      .visible(() -> this.isMode("HolyWorld"));
   private final BindSetting antiPoletKeyBind = this.bind("Анти полет", () -> this.isMode("Reallyworld"));
   private final BindSetting zeleGrinchaKeyBind = this.bind("Зелье Гринча", () -> this.isMode("Reallyworld"));
   private final BindSetting autoshiftShiftDlyaSharaKeyBind = this.bind("AutoShift (Шифт для шара)", () -> this.isMode("Reallyworld"));
   private final BindSetting novogodniyUzhasKeyBind = this.bind("Новогодний ужас", () -> this.isMode("Reallyworld"));
   private final BindSetting essenciyaKromeshnikaKeyBind = this.bind("Эссенция кромешника", () -> this.isMode("Reallyworld"));
   private final BindSetting snezhokKeyBind = this.bind("Снежок", () -> this.isMode("Reallyworld"));
   private final BindSetting lovushkaKeyBind = this.bind("Ловушка", () -> this.isMode("Reallyworld"));
   private final BooleanSetting zakryvatMenyuEnabled = new BooleanSetting("Закрывать меню", "Закрывает меню Reallyworld")
      .setValue(false)
      .visible(() -> this.isMode("Reallyworld"));
   private final BooleanSetting filtrChataEnabled = new BooleanSetting("Фильтр чата", "Фильтрует запрещённые слова")
      .setValue(false)
      .visible(() -> this.isMode("Reallyworld"));
   private final BooleanSetting autoFixAllEnabled = new BooleanSetting("Auto /fix all", "Авто /fix all после PvP")
      .setValue(false)
      .visible(() -> this.isMode("Reallyworld"));
   private final BindSetting svapGolovyKeyBind = this.bind("Свап головы", () -> this.isMode("MetaHvH"));
   private final BindSetting svapBotinokKeyBind = this.bind("Свап ботинок", () -> this.isMode("MetaHvH"));
   private final BindSetting avtoEcVklVyklKeyBind = this.bind("Авто EC (вкл/выкл)", () -> this.isMode("MetaHvH"));
   private final SelectSetting vyborShlemaMode = new SelectSetting("Выбор шлема", "Тип шлема для MetaHvH")
      .value("Незеритовый", "Золотой")
      .selected("Незеритовый")
      .visible(() -> this.isMode("MetaHvH"));
   private final SliderSettings zaderzhkaSvapaSetting = new SliderSettings("Задержка свапа", "Задержка свапа головы")
      .setValue(2.0F)
      .range(1.0F, 5.0F)
      .step(1.0F)
      .visible(() -> this.isMode("MetaHvH"));
   private final BooleanSetting avtoBotinkiVodaEnabled = new BooleanSetting("Авто ботинки (вода)", "Меняет ботинки в воде")
      .setValue(false)
      .visible(() -> this.isMode("MetaHvH"));
   private final SliderSettings radiusEcSetting = new SliderSettings("Радиус EC", "Радиус поиска EC")
      .setValue(3.0F)
      .range(1.0F, 5.0F)
      .step(1.0F)
      .visible(() -> this.isMode("MetaHvH"));
   private final BindSetting zeleKilleraKeyBind = this.bind("Зелье киллера", () -> this.isMode("BravoHvH"));
   private final BindSetting zeleMedikaKeyBind = this.bind("Зелье медика", () -> this.isMode("BravoHvH"));
   private final BindSetting pozvatPomoschKeyBind = this.bind("Позвать помощь", () -> this.isMode("BravoHvH"));
   private final SliderSettings porogHpDlyaAvtoSetting = new SliderSettings("Порог HP для авто", "HP для авто медика")
      .setValue(6.0F)
      .range(1.0F, 20.0F)
      .step(1.0F)
      .visible(() -> this.isMode("BravoHvH"));
   private final BooleanSetting avtoMedikPriNizkomHpEnabled = new BooleanSetting("Авто медик при низком HP", "Авто зелье медика")
      .setValue(false)
      .visible(() -> this.isMode("BravoHvH"));
   private final BindSetting obychnayaTrapkaKeyBind = this.bind("Обычная трапка", () -> this.isMode("Sunrise"));
   private final BindSetting horoshayaTrapkaKeyBind = this.bind("Хорошая трапка", () -> this.isMode("Sunrise"));
   private final BindSetting otlichnayaTrapkaKeyBind = this.bind("Отличная трапка", () -> this.isMode("Sunrise"));
   private final BindSetting epicheskayaTrapkaKeyBind = this.bind("Эпическая трапка", () -> this.isMode("Sunrise"));
   private final BindSetting legendarnayaTrapkaKeyBind = this.bind("Легендарная трапка", () -> this.isMode("Sunrise"));
   private final BindSetting livalkaKeyBind = this.bind("Ливалка", () -> this.isMode("Sunrise"));
   private final BindSetting amuletTeleportaciiKeyBind = this.bind("Амулет телепортации", () -> this.isMode("Sunrise"));
   private final BindSetting skoplenieSvetaKeyBind = this.bind("Скопление света", () -> this.isMode("Sunrise"));
   private final BindSetting volnaOgnyaKeyBind = this.bind("Волна огня", () -> this.isMode("Sunrise"));
   private final BindSetting poroshokVozvrascheniyaKeyBind = this.bind("Порошок возвращения", () -> this.isMode("Sunrise"));
   private final BindSetting poroshokDezorganizaciiKeyBind = this.bind("Порошок дезорганизации", () -> this.isMode("Sunrise"));
   private final BindSetting razrushitelStoekKeyBind = this.bind("Разрушитель Стоек", () -> this.isMode("Sunrise"));
   private final BindSetting fitilKeyBind = this.bind("Фитиль", () -> this.isMode("MineBlaze"));
   private final BindSetting perestavlyayuscheeZeleKeyBind = this.bind("Переставляющее зелье", () -> this.isMode("MineBlaze"));
   private final BindSetting vzryvnoeZeleKeyBind = this.bind("Взрывное зелье", () -> this.isMode("MineBlaze"));
   private final BindSetting trapaKeyBind = this.bind("Трапа", () -> this.isMode("FunTime"));
   private final BindSetting dezorentKeyBind = this.bind("Дезорент", () -> this.isMode("FunTime"));
   private final BindSetting plastKeyBind2 = this.bind("Пласт", () -> this.isMode("FunTime"));
   private final BindSetting yavnayaPylKeyBind2 = this.bind("Явная пыль", () -> this.isMode("FunTime"));
   private final BindSetting bozhyaAuraKeyBind2 = this.bind("Божья аура", () -> this.isMode("FunTime"));
   private final BindSetting ognenSmerchKeyBind = this.bind("Огнен смерч", () -> this.isMode("FunTime"));
   private final BindSetting snezhokKeyBind2 = this.bind("Снежок", () -> this.isMode("FunTime"));
   private final CustomHandler customHandler = new CustomHandler();
   private final Map<String, ServerHelperHandler> handlers = new HashMap<>();
   private final ServerHelperOverlay overlay = new ServerHelperOverlay();
   private final List<BindSetting> customDynamicBinds = new ArrayList<>();
   private int usePhase;
   private int savedSlot = -1;
   private int inventorySlot = -1;
   private int executeTick;
   private int inputDelayTicks;
   private boolean wasInPvp;
   private int nextFixTick;
   private ServerHelperHandler cachedRenderHandler;
   private int cachedRenderTick = -1;
   private boolean cachedOnlyHotbar;
   private String cachedMode = "";
   private List<ServerHelperRenderItem> cachedRenderItems = List.of();

   public static ServerHelper getInstance() {
      return Instance.get(ServerHelper.class);
   }

   public ServerHelper() {
      super("ServerHelper", "ServerHelper", ModuleCategory.MISC);
      SpookyTimeHandler spooky = new SpookyTimeHandler(
         List.of(
            this.dezorientaciyaKeyBind,
            this.yavnayaPylKeyBind,
            this.plastKeyBind,
            this.trapkaSpookyKeyBind,
            this.ognennyySmerchKeyBind,
            this.snezhokZamorozkaKeyBind,
            this.bozhyaAuraKeyBind
         )
      );
      LonyGriefHandler lony = new LonyGriefHandler(
         List.of(this.obychnayaLivalkaKeyBind, this.unikalnoePeryshkoKeyBind, this.livalkaSPlatformoyKeyBind, this.unikalnayaTrapkaKeyBind),
         () -> this.avtoChatIgraUravnenieEnabled.isValue() && this.isMode("LonyGrief")
      );
      HolyWorldHandler holy = new HolyWorldHandler(
         List.of(
            this.puzyrekOpytaKeyBind,
            this.trapkaKeyBind,
            this.vzryvnayaTrapkaKeyBind,
            this.proschalnyyGulKeyBind,
            this.vzryvnayaShtuchkaKeyBind,
            this.stanKeyBind,
            this.komSnegaKeyBind
         ),
         this.antiCrashEnabled,
         this.ryukzakKeyBind
      );
      ReallyworldHandler rw = new ReallyworldHandler(
         List.of(
            this.antiPoletKeyBind,
            this.zeleGrinchaKeyBind,
            this.autoshiftShiftDlyaSharaKeyBind,
            this.novogodniyUzhasKeyBind,
            this.essenciyaKromeshnikaKeyBind,
            this.snezhokKeyBind,
            this.lovushkaKeyBind
         ),
         this.zakryvatMenyuEnabled,
         this.filtrChataEnabled
      );
      MetaHvHHandler meta = new MetaHvHHandler(
         List.of(this.svapGolovyKeyBind, this.svapBotinokKeyBind, this.avtoEcVklVyklKeyBind),
         this.vyborShlemaMode,
         this.zaderzhkaSvapaSetting,
         this.avtoBotinkiVodaEnabled,
         this.radiusEcSetting
      );
      BravoHvHHandler bravo = new BravoHvHHandler(
         List.of(this.zeleKilleraKeyBind, this.zeleMedikaKeyBind, this.pozvatPomoschKeyBind), this.porogHpDlyaAvtoSetting, this.avtoMedikPriNizkomHpEnabled
      );
      SunriseHandler sunrise = new SunriseHandler(
         List.of(
            this.obychnayaTrapkaKeyBind,
            this.horoshayaTrapkaKeyBind,
            this.otlichnayaTrapkaKeyBind,
            this.epicheskayaTrapkaKeyBind,
            this.legendarnayaTrapkaKeyBind,
            this.livalkaKeyBind,
            this.amuletTeleportaciiKeyBind,
            this.skoplenieSvetaKeyBind,
            this.volnaOgnyaKeyBind,
            this.poroshokVozvrascheniyaKeyBind,
            this.poroshokDezorganizaciiKeyBind,
            this.razrushitelStoekKeyBind
         )
      );
      MineBlazeHandler mineBlaze = new MineBlazeHandler(List.of(this.fitilKeyBind, this.perestavlyayuscheeZeleKeyBind, this.vzryvnoeZeleKeyBind));
      FunTimeHandler funTime = new FunTimeHandler(
         List.of(
            this.trapaKeyBind,
            this.dezorentKeyBind,
            this.plastKeyBind2,
            this.yavnayaPylKeyBind2,
            this.bozhyaAuraKeyBind2,
            this.ognenSmerchKeyBind,
            this.snezhokKeyBind2
         )
      );
      this.handlers.put("SpookyTime", spooky);
      this.handlers.put("LonyGrief", lony);
      this.handlers.put("HolyWorld", holy);
      this.handlers.put("Reallyworld", rw);
      this.handlers.put("MetaHvH", meta);
      this.handlers.put("BravoHvH", bravo);
      this.handlers.put("Sunrise", sunrise);
      this.handlers.put("MineBlaze", mineBlaze);
      this.handlers.put("FunTime", funTime);
      this.handlers.put("Custom", this.customHandler);
      this.setup(
         new Setting[]{
            this.serverMode,
            this.onlyHotbar,
            this.dezorientaciyaKeyBind,
            this.yavnayaPylKeyBind,
            this.plastKeyBind,
            this.trapkaSpookyKeyBind,
            this.ognennyySmerchKeyBind,
            this.snezhokZamorozkaKeyBind,
            this.bozhyaAuraKeyBind,
            this.obychnayaLivalkaKeyBind,
            this.unikalnoePeryshkoKeyBind,
            this.livalkaSPlatformoyKeyBind,
            this.unikalnayaTrapkaKeyBind,
            this.avtoChatIgraUravnenieEnabled,
            this.antiPoletKeyBind,
            this.zeleGrinchaKeyBind,
            this.autoshiftShiftDlyaSharaKeyBind,
            this.novogodniyUzhasKeyBind,
            this.essenciyaKromeshnikaKeyBind,
            this.snezhokKeyBind,
            this.lovushkaKeyBind,
            this.zakryvatMenyuEnabled,
            this.filtrChataEnabled,
            this.autoFixAllEnabled,
            this.puzyrekOpytaKeyBind,
            this.trapkaKeyBind,
            this.vzryvnayaTrapkaKeyBind,
            this.proschalnyyGulKeyBind,
            this.vzryvnayaShtuchkaKeyBind,
            this.stanKeyBind,
            this.komSnegaKeyBind,
            this.ryukzakKeyBind,
            this.antiCrashEnabled,
            this.svapGolovyKeyBind,
            this.svapBotinokKeyBind,
            this.avtoEcVklVyklKeyBind,
            this.vyborShlemaMode,
            this.zaderzhkaSvapaSetting,
            this.avtoBotinkiVodaEnabled,
            this.radiusEcSetting,
            this.zeleKilleraKeyBind,
            this.zeleMedikaKeyBind,
            this.pozvatPomoschKeyBind,
            this.porogHpDlyaAvtoSetting,
            this.avtoMedikPriNizkomHpEnabled,
            this.obychnayaTrapkaKeyBind,
            this.horoshayaTrapkaKeyBind,
            this.otlichnayaTrapkaKeyBind,
            this.epicheskayaTrapkaKeyBind,
            this.legendarnayaTrapkaKeyBind,
            this.livalkaKeyBind,
            this.amuletTeleportaciiKeyBind,
            this.skoplenieSvetaKeyBind,
            this.volnaOgnyaKeyBind,
            this.poroshokVozvrascheniyaKeyBind,
            this.poroshokDezorganizaciiKeyBind,
            this.razrushitelStoekKeyBind,
            this.fitilKeyBind,
            this.perestavlyayuscheeZeleKeyBind,
            this.vzryvnoeZeleKeyBind,
            this.trapaKeyBind,
            this.dezorentKeyBind,
            this.plastKeyBind2,
            this.yavnayaPylKeyBind2,
            this.bozhyaAuraKeyBind2,
            this.ognenSmerchKeyBind,
            this.snezhokKeyBind2,
            this.customHandler.getOpenGuiBind()
         }
      );
      this.refreshCustomBinds();
   }

   private BindSetting bind(String name, Supplier<Boolean> visible) {
      return new BindSetting(name, name).visible(visible);
   }

   private boolean isMode(String mode) {
      return this.serverMode.isSelected(mode);
   }

   public boolean isCustomMode() {
      return this.isMode("Custom");
   }

   public String getServerMode() {
      return this.serverMode.getSelected();
   }

   public void refreshCustomBinds() {
      this.customDynamicBinds.clear();
      this.customDynamicBinds.addAll(this.customHandler.getDynamicBinds());
      this.customDynamicBinds.forEach(xva$0 -> this.setup(new Setting[]{xva$0}));
   }

   public void queueSlotUse(int slot) {
      if (mc.player != null && slot >= 0) {
         if (slot < 9) {
            this.savedSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = slot;
            this.syncSelectedSlot();
            this.usePhase = 6;
            this.executeTick = mc.player.age + 2;
         } else {
            this.inputDelayTicks = 2;
            this.usePhase = 1;
            this.inventorySlot = slot;
            this.savedSlot = mc.player.getInventory().selectedSlot;
         }
      }
   }

   private ServerHelperHandler currentHandler() {
      return this.handlers.get(this.serverMode.getSelected());
   }

   private void applyOnlyHotbar(ServerHelperHandler handler) {
      if (this.helperSupportsHotbarOnly(handler)) {
         handler.setOnlyHotbar(this.onlyHotbar.isValue());
      }
   }

   private boolean helperSupportsHotbarOnly(ServerHelperHandler helper) {
      return helper instanceof SpookyTimeHandler
         || helper instanceof LonyGriefHandler
         || helper instanceof HolyWorldHandler
         || helper instanceof SunriseHandler
         || helper instanceof FunTimeHandler
         || helper instanceof CustomHandler;
   }

   private List<ServerHelperRenderItem> getRenderItems(ServerHelperHandler helper) {
      int tick = mc.player.age;
      String mode = this.serverMode.getSelected();
      boolean onlyBar = this.onlyHotbar.isValue();
      if (helper == this.cachedRenderHandler && tick == this.cachedRenderTick && onlyBar == this.cachedOnlyHotbar && mode.equals(this.cachedMode)) {
         return this.cachedRenderItems;
      } else {
         this.cachedRenderHandler = helper;
         this.cachedRenderTick = tick;
         this.cachedOnlyHotbar = onlyBar;
         this.cachedMode = mode;
         this.cachedRenderItems = helper.buildRenderItems(onlyBar);
         return this.cachedRenderItems;
      }
   }

   @EventHandler
   public void onKey(KeyEvent event) {
      if (mc.currentScreen == null && event.action() == 1) {
         ServerHelperHandler helper = this.currentHandler();
         if (helper != null) {
            this.applyOnlyHotbar(helper);
            helper.onKey(event.key());
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player != null) {
         if (this.inputDelayTicks > 0) {
            this.inputDelayTicks--;
         }

         this.handleAutoFixAll();
         ServerHelperHandler helper = this.currentHandler();
         if (helper != null) {
            HolyWorldHandler holy;
            if (helper instanceof HolyWorldHandler && (holy = (HolyWorldHandler)helper).isBackpackBusy()) {
               holy.tick();
            } else {
               helper.tick();
               this.processUseStateMachine(helper);
            }
         }
      }
   }

   private void processUseStateMachine(ServerHelperHandler helper) {
      if (this.usePhase == 6 && mc.player.age >= this.executeTick) {
         PlayerInteractionHelper.interactItem(Hand.MAIN_HAND);
         this.usePhase = 5;
         this.executeTick = mc.player.age + 2;
      }

      if (this.usePhase == 5 && mc.player.age >= this.executeTick) {
         mc.player.getInventory().selectedSlot = this.savedSlot;
         this.syncSelectedSlot();
         this.savedSlot = -1;
         this.usePhase = 0;
      }

      if (this.usePhase == 1 && this.inputDelayTicks == 0) {
         this.usePhase = 2;
      }

      if (this.usePhase == 2 && this.inventorySlot != -1) {
         this.handleInventorySwap(true);
         InventoryTask.clickSlot(this.inventorySlot, this.savedSlot, SlotActionType.SWAP, false);
         if (this.isFuntimeGuiMove()) {
            mc.player.closeHandledScreen();
         }

         this.usePhase = 3;
         this.executeTick = mc.player.age + 2;
      }

      if (this.usePhase == 3 && mc.player.age >= this.executeTick) {
         PlayerInteractionHelper.interactItem(Hand.MAIN_HAND);
         this.usePhase = 4;
         this.executeTick = mc.player.age + 3;
      }

      if (this.usePhase == 4 && mc.player.age >= this.executeTick) {
         this.handleInventorySwap(false);
         InventoryTask.clickSlot(this.inventorySlot, this.savedSlot, SlotActionType.SWAP, false);
         if (this.isFuntimeGuiMove()) {
            mc.player.closeHandledScreen();
         }

         this.usePhase = 0;
         this.inventorySlot = -1;
         this.savedSlot = -1;
         helper.clearPending();
      }

      if (helper.hasPendingUse() && this.usePhase == 0) {
         int slot = helper.getPendingSlot();
         if (slot == -1) {
            helper.clearPending();
         } else {
            if (slot < 9) {
               this.savedSlot = mc.player.getInventory().selectedSlot;
               mc.player.getInventory().selectedSlot = slot;
               this.syncSelectedSlot();
               this.usePhase = 6;
               this.executeTick = mc.player.age + 2;
            } else {
               this.inputDelayTicks = 2;
               this.usePhase = 1;
               this.inventorySlot = slot;
               this.savedSlot = mc.player.getInventory().selectedSlot;
            }

            helper.clearPending();
         }
      }
   }

   private void handleInventorySwap(boolean beforeUse) {
      GuiMove guiMove = GuiMove.getInstance();
      if (this.isFuntimeGuiMove() && beforeUse) {
         mc.player.closeHandledScreen();
      }

      if (guiMove != null && guiMove.isState()) {
         if (beforeUse) {
            InventoryFlowManager.disableMoveKeys();
         } else {
            InventoryFlowManager.enableMoveKeys();
         }
      }
   }

   private boolean isFuntimeGuiMove() {
      GuiMove guiMove = GuiMove.getInstance();
      return guiMove != null && guiMove.isState() && GuiMove.mode.isSelected("Funtime");
   }

   private boolean isGuiMoveActive() {
      GuiMove guiMove = GuiMove.getInstance();
      return guiMove != null && guiMove.isState();
   }

   private void handleAutoFixAll() {
      if (this.isMode("Reallyworld") && this.autoFixAllEnabled.isValue()) {
         boolean inPvp = Network.isPvp();
         if (this.wasInPvp && !inPvp && mc.player.age >= this.nextFixTick) {
            mc.player.networkHandler.sendChatCommand("fix all");
            this.nextFixTick = mc.player.age + 40;
         }

         this.wasInPvp = inPvp;
      } else {
         this.wasInPvp = Network.isPvp();
      }
   }

   @EventHandler
   public void onInput(InputEvent event) {
      if (mc.player == null || !ItemUseHelper.isUsingPriorityItem(mc.player)) {
         if (this.inputDelayTicks > 0 || this.usePhase >= 1 && this.usePhase <= 4) {
            event.inputNone();
         }
      }
   }

   @EventHandler
   public void onUsingItem(UsingItemEvent event) {
      if (mc.player == null || !this.isConsumingUse(event)) {
         if (mc.player == null || !ItemUseHelper.isUsingPriorityItem(mc.player)) {
            if (this.inputDelayTicks > 0 || this.usePhase >= 1 && this.usePhase <= 6) {
               event.cancel();
            }
         }
      }
   }

   private boolean isConsumingUse(UsingItemEvent event) {
      if (event.getType() == -1) {
         return event.getHand() != null
            ? ItemUseHelper.isPriorityUseItem(ItemUseHelper.getStackForHand(mc.player, event.getHand()))
            : ItemUseHelper.isPriorityUseItem(mc.player.getMainHandStack()) || ItemUseHelper.isPriorityUseItem(mc.player.getOffHandStack());
      } else {
         return mc.player.isUsingItem() ? ItemUseHelper.isPriorityUseItem(mc.player.getActiveItem()) : false;
      }
   }

   @EventHandler
   public void onPacket(PacketEvent event) {
      if (mc.player != null && mc.world != null) {
         ServerHelperHandler helper = this.currentHandler();
         if (helper != null && !event.isSend()) {
            helper.onPacketReceive(event);
         }

         Packet<?> class_25962;
         if (!event.isSend() && this.isMode("HolyWorld") && this.antiCrashEnabled.isValue() && (class_25962 = event.getPacket()) instanceof ExplosionS2CPacket) {
            ExplosionS2CPacket explosion = (ExplosionS2CPacket)class_25962;
            explosion.playerKnockback().ifPresent(knockback -> {
               if (Double.isInfinite(knockback.x) || Double.isInfinite(knockback.y) || Double.isInfinite(knockback.z)) {
                  event.cancel();
               }
            });
         }
      }
   }

   @EventHandler
   public void onChat(ChatEvent event) {
      ServerHelperHandler helper = this.currentHandler();
      if (helper != null) {
         helper.onChatSend(event);
      }
   }

   private void syncSelectedSlot() {
      if (mc.interactionManager != null) {
         mc.interactionManager.syncSelectedSlot();
      }
   }

   private void resetState() {
      this.usePhase = 0;
      this.savedSlot = -1;
      this.inventorySlot = -1;
      this.executeTick = 0;
      this.inputDelayTicks = 0;
      this.wasInPvp = false;
      this.nextFixTick = 0;
      this.cachedRenderItems = List.of();
      this.cachedRenderHandler = null;
      this.cachedRenderTick = -1;
      ServerHelperHandler helper = this.currentHandler();
      if (helper != null) {
         helper.clearPending();
      }
   }

   @Override
   public void deactivate() {
      super.deactivate();
      this.resetState();
   }
}
