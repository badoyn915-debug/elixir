package code.elixir.mixins;

import code.elixir.Elixir;
import code.elixir.display.screens.ias.IASScreen;
import code.elixir.events.container.SetScreenEvent;
import code.elixir.events.player.GameUpdateEvent;
import code.elixir.events.player.HotBarUpdateEvent;
import code.elixir.features.impl.combat.NoInteract;
import code.elixir.utils.client.FakePlayerManager;
import code.elixir.utils.client.logs.Logger;
import code.elixir.utils.client.managers.event.SafeEvents;
import code.elixir.utils.client.managers.file.exception.FileProcessingException;
import code.elixir.utils.client.window.WindowStyle;
import code.elixir.utils.client.window.WindowTitleAnimation;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.render.font.Fonts;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.ActionResult.Success;
import net.minecraft.util.ActionResult.SwingSource;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({MinecraftClient.class})
public abstract class MinecraftClientMixin implements QuickImports {
   @Shadow
   @Nullable
   public ClientPlayerInteractionManager interactionManager;
   @Shadow
   @Nullable
   public ClientPlayerEntity player;
   @Shadow
   @Final
   public GameRenderer gameRenderer;
   @Shadow
   @Nullable
   public Screen currentScreen;
   @Shadow
   @Nullable
   public HitResult crosshairTarget;
   private WindowTitleAnimation titleUtil;
   @Unique
   private long gameUpdateLastHookTime;
   @Unique
   private int gameUpdateAccumulatedCalls;
   private int titleUpdateCounter = 0;
   @Unique
   private boolean elixir$glintFixApplied;
   @Unique
   private boolean elixir$accountRestored;

   @Shadow
   @Nullable
   public abstract ClientPlayNetworkHandler getNetworkHandler();

   @Inject(
      at = {@At("TAIL")},
      method = {"<init>"}
   )
   private void onInit(RunArgs args, CallbackInfo ci) {
      if (System.getProperty("java.version").equals("sk3dguard-production-vm")) {
         System.setProperty("sun.font.layout.ffm", "false");
      }

      Fonts.init();

      try {
         this.titleUtil = WindowTitleAnimation.getInstance();
         MinecraftClient client = MinecraftClient.getInstance();
         if (client != null && client.getWindow() != null) {
            client.getWindow().setTitle(this.titleUtil.getCurrentTitle());
         }
      } catch (Exception var4) {
      }
   }

   @Inject(
      at = {@At("HEAD")},
      method = {"stop"}
   )
   private void stop(CallbackInfo ci) {
      Logger.info("Stopping for MinecraftClient");

      try {
         Elixir alphaDlc = Elixir.getInstance();
         if (alphaDlc != null && alphaDlc.isInitialized()) {
            try {
               alphaDlc.getFileController().saveFiles();
            } catch (FileProcessingException var8) {
               Logger.error("Error occurred while saving files: " + var8.getMessage() + " " + var8.getCause());
            } finally {
               if (alphaDlc.getFileController() != null) {
                  alphaDlc.getFileController().stopAutoSave();
               }
            }
         }
      } catch (Exception var10) {
      }
   }

   @Inject(
      method = {"doItemUse"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/Hand;values()[Lnet/minecraft/util/Hand;"
      )},
      cancellable = true
   )
   public void doItemUseHook(CallbackInfo ci) {
      NoInteract noInteract = NoInteract.getInstance();
      if (noInteract != null && noInteract.isState() && this.player != null && this.interactionManager != null) {
         for (Hand hand : Hand.values()) {
            ActionResult result;
            if (!this.player.getStackInHand(hand).isEmpty() && (result = this.interactionManager.interactItem((PlayerEntity)(Object)this.player, hand)).isAccepted()) {
               Success success;
               if (result instanceof Success && (success = (Success)result).swingSource().equals(SwingSource.CLIENT)) {
                  this.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
                  this.player.swingHand(hand);
               }

               ci.cancel();
            }
         }
      }
   }

   @Inject(
      method = {"doAttack"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void doAttackHook(CallbackInfoReturnable<Boolean> cir) {
      if (this.player != null && this.crosshairTarget != null && this.crosshairTarget.getType() == Type.ENTITY) {
         EntityHitResult entityHitResult = (EntityHitResult)(Object)this.crosshairTarget;
         if (FakePlayerManager.handleAttack(entityHitResult.getEntity())) {
            this.player.swingHand(Hand.MAIN_HAND);
            cir.setReturnValue(true);
         }
      }
   }

   @Inject(
      method = {"setScreen"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void setScreenHook(Screen screen, CallbackInfo ci) {
      try {
         SetScreenEvent event = SafeEvents.dispatch(() -> new SetScreenEvent(screen));
         Elixir alphaDlc = Elixir.getInstance();
         if (alphaDlc != null && alphaDlc.getDraggableRepository() != null && event != null) {
            alphaDlc.getDraggableRepository().draggable().forEach(drag -> drag.setScreen(event));
         }

         Screen eventScreen = event != null ? event.getScreen() : screen;
         if (screen != eventScreen) {
            mc.setScreen(eventScreen);
            ci.cancel();
         }
      } catch (Exception var7) {
      }
   }

   @Inject(
      method = {"onResolutionChanged"},
      at = {@At("TAIL")}
   )
   private void applyDarkMode(CallbackInfo ci) {
      Fonts.clearCache();

      try {
         String os = System.getProperty("os.name").toLowerCase();
         if (os.contains("linux")) {
            return;
         }

         MinecraftClient client = MinecraftClient.getInstance();
         if (client != null && client.getWindow() != null) {
            WindowStyle.setDarkMode(client.getWindow().getHandle());
         }
      } catch (Exception var4) {
      }
   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   private void onTick(CallbackInfo ci) {
      this.elixir$restoreGlintStrength();
      this.elixir$restoreActiveAccount();

      try {
         if (this.titleUtil == null) {
            this.titleUtil = WindowTitleAnimation.getInstance();
         }

         this.titleUtil.updateTitle();
         if (++this.titleUpdateCounter >= 10) {
            this.titleUpdateCounter = 0;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getWindow() != null) {
               client.getWindow().setTitle(this.titleUtil.getCurrentTitle());
            }
         }
      } catch (Exception var3) {
      }
   }

   @Unique
   private void elixir$restoreGlintStrength() {
      if (!this.elixir$glintFixApplied) {
         try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) {
               return;
            }

            this.elixir$glintFixApplied = true;
            boolean changed = false;
            if ((Double)client.options.getGlintStrength().getValue() <= 0.0) {
               client.options.getGlintStrength().setValue(0.75);
               changed = true;
            }

            if ((Double)client.options.getGlintSpeed().getValue() <= 0.0) {
               client.options.getGlintSpeed().setValue(0.5);
               changed = true;
            }

            if (changed) {
               client.options.write();
            }
         } catch (Exception var3) {
         }
      }
   }

   @Unique
   private void elixir$restoreActiveAccount() {
      if (!this.elixir$accountRestored) {
         try {
            Elixir alphaDlc = Elixir.getInstance();
            if (alphaDlc == null || !alphaDlc.isInitialized() || alphaDlc.getAccountRepository() == null) {
               return;
            }

            this.elixir$accountRestored = true;
            IASScreen.restoreActiveAccount();
         } catch (Exception var2) {
         }
      }
   }

   @Inject(
      method = {"updateWindowTitle"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onUpdateWindowTitle(CallbackInfo ci) {
      try {
         if (this.titleUtil == null) {
            this.titleUtil = WindowTitleAnimation.getInstance();
         }

         MinecraftClient client;
         if ((client = MinecraftClient.getInstance()) != null && client.getWindow() != null) {
            client.getWindow().setTitle(this.titleUtil.getCurrentTitle());
         }

         ci.cancel();
      } catch (Exception var3) {
      }
   }

   @Inject(
      method = {"handleInputEvents"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;getInventory()Lnet/minecraft/entity/player/PlayerInventory;"
      )},
      cancellable = true
   )
   public void handleInputEventsHook(CallbackInfo ci) {
      HotBarUpdateEvent event = SafeEvents.dispatch(() -> new HotBarUpdateEvent());
      if (event != null && event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void onRenderGameUpdate(boolean tick, CallbackInfo ci) {
      long now = Util.getMeasuringTimeNano();
      long delta = now - this.gameUpdateLastHookTime;
      this.gameUpdateAccumulatedCalls += (int)(delta / 4166666L);
      this.gameUpdateLastHookTime = this.gameUpdateLastHookTime + this.gameUpdateAccumulatedCalls * 4166666L;

      for (this.gameUpdateAccumulatedCalls = Math.min(this.gameUpdateAccumulatedCalls, 240);
         this.gameUpdateAccumulatedCalls > 0;
         this.gameUpdateAccumulatedCalls--
      ) {
         SafeEvents.dispatch(GameUpdateEvent::new);
      }
   }
}
