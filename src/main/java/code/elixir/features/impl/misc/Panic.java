package code.elixir.features.impl.misc;

import code.elixir.Elixir;
import code.elixir.events.chat.ChatEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.managers.event.EventManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class Panic extends Module {
   private static boolean panicMode = false;
   private static String recoveryCode = "";
   private boolean executed = false;
   private final Map<String, Boolean> savedModuleStates = new HashMap<>();
   private boolean countdownActive = false;
   private int countdownValue = 5;
   private long lastCountdownTime = 0L;
   private boolean codeShown = false;

   public Panic() {
      super("Panic", ModuleCategory.MISC);
      Elixir.getInstance().getEventManager();
      EventManager.register(this);
   }

   public static boolean isPanicMode() {
      return panicMode;
   }

   public static String getRecoveryCode() {
      return recoveryCode;
   }

   @Override
   public void activate() {
      this.executed = false;
      this.codeShown = false;
      this.countdownActive = false;
      this.countdownValue = 5;
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (this.isState() && !this.executed && mc.player != null && mc.world != null) {
         this.startPanicSequence();
         this.executed = true;
      }

      long currentTime;
      if (this.countdownActive && mc.player != null && (currentTime = System.currentTimeMillis()) - this.lastCountdownTime >= 1000L) {
         this.lastCountdownTime = currentTime;
         if (this.countdownValue > 0) {
            mc.player.sendMessage(Text.literal("" + Formatting.RED + Formatting.BOLD + this.countdownValue), false);
            this.countdownValue--;
         } else {
            this.countdownActive = false;
            this.activatePanicMode();
         }
      }
   }

   @EventHandler
   public void onChat(ChatEvent event) {
      if (panicMode && event.getMessage().equals(recoveryCode)) {
         event.cancel();
         this.restoreClient();
      }
   }

   private void startPanicSequence() {
      recoveryCode = String.format("%06d", new Random().nextInt(1000000));
      this.savedModuleStates.clear();
      Elixir.getInstance().getModuleRepository().modules().forEach(module -> this.savedModuleStates.put(module.getName(), module.isState()));
      if (mc.player != null) {
         mc.player.sendMessage(Text.literal(""), false);
         mc.player.sendMessage(Text.literal("" + Formatting.GOLD + Formatting.BOLD + "═══════════════════════════"), false);
         mc.player.sendMessage(Text.literal("" + Formatting.RED + Formatting.BOLD + "PANIC MODE ACTIVATED"), false);
         mc.player.sendMessage(Text.literal(""), false);
         mc.player.sendMessage(Text.literal(Formatting.YELLOW + "Код восстановления: " + Formatting.GREEN + Formatting.BOLD + recoveryCode), false);
         mc.player.sendMessage(Text.literal(Formatting.GRAY + "Запомните код для восстановления!"), false);
         mc.player.sendMessage(Text.literal("" + Formatting.GOLD + Formatting.BOLD + "═══════════════════════════"), false);
         mc.player.sendMessage(Text.literal(""), false);
      }

      this.codeShown = true;
      this.countdownActive = true;
      this.countdownValue = 5;
      this.lastCountdownTime = System.currentTimeMillis();
   }

   private void activatePanicMode() {
      panicMode = true;
      Elixir.getInstance().getModuleRepository().modules().forEach(module -> {
         if (module.isState() && !(module instanceof Panic)) {
            module.setState(false);
         }
      });
      if (mc.inGameHud != null && mc.inGameHud.getChatHud() != null) {
         mc.inGameHud.getChatHud().clear(false);
      }

      this.setState(false);
   }

   private void restoreClient() {
      panicMode = false;
      recoveryCode = "";
      if (mc.player != null) {
         mc.player.sendMessage(Text.literal(""), false);
         mc.player.sendMessage(Text.literal("" + Formatting.GREEN + Formatting.BOLD + "✓ Код принят!"), false);
         mc.player.sendMessage(Text.literal(Formatting.YELLOW + "Клиент восстановлен!"), false);
         mc.player.sendMessage(Text.literal(""), false);
      }

      Elixir.getInstance().getModuleRepository().modules().forEach(module -> {
         Boolean savedState = this.savedModuleStates.get(module.getName());
         if (savedState != null && savedState != module.isState()) {
            module.setState(savedState);
         }
      });
      this.savedModuleStates.clear();
   }

   @Override
   public void deactivate() {
      super.deactivate();
      this.countdownActive = false;
      this.countdownValue = 5;
      this.executed = false;
      this.codeShown = false;
   }

   @Override
   public void setState(boolean state) {
      if (state != this.state) {
         this.state = state;
         if (state) {
            this.activate();
         } else {
            this.deactivate();
         }
      }
   }
}
