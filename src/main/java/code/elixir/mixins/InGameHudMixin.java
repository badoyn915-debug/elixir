package code.elixir.mixins;

import code.elixir.features.impl.render.CrossHair;
import code.elixir.features.impl.render.Hud;
import code.elixir.features.impl.render.NoRender;
import code.elixir.utils.display.interfaces.QuickImports;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({InGameHud.class})
public abstract class InGameHudMixin implements QuickImports {
   @Final
   @Shadow
   private MinecraftClient client;

   @Unique
   private Hud getHud() {
      return Hud.getInstance();
   }

   @Shadow
   protected abstract void renderStatusBars(DrawContext var1);

   @Shadow
   protected abstract void renderMountHealth(DrawContext var1);

   @Inject(
      method = {"renderCrosshair"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/client/gui/hud/InGameHud;CROSSHAIR_TEXTURE:Lnet/minecraft/util/Identifier;"
      )},
      cancellable = true
   )
   public void renderCrosshairHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      CrossHair crossHair = CrossHair.getInstance();
      if (crossHair.isState()) {
         crossHair.onRenderCrossHair();
         ci.cancel();
      }
   }

   @Inject(
      at = {@At("HEAD")},
      method = {"renderStatusEffectOverlay"},
      cancellable = true
   )
   public void renderStatusEffectOverlayHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      Hud hud = this.getHud();
      if (hud != null && hud.isState() && Hud.interfaceSettings.isSelected("Potions")) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderScoreboardSidebarHook(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
      NoRender noRender = NoRender.getInstance();
      if (noRender != null && noRender.shouldHideScoreboard()) {
         ci.cancel();
      } else {
         Hud hud = this.getHud();
         if (hud != null && hud.isState() && Hud.interfaceSettings.isSelected("Score Board")) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"renderVignetteOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderVignette(DrawContext context, Entity entity, CallbackInfo ci) {
      NoRender noRender = NoRender.getInstance();
      if (noRender != null && noRender.shouldHideVignette()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderTitleAndSubtitle"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderTitleAndSubtitle(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      NoRender noRender = NoRender.getInstance();
      if (noRender != null && noRender.shouldHideTitles()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderNauseaOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderNauseaOverlay(DrawContext context, float nauseaStrength, CallbackInfo ci) {
      NoRender noRender = NoRender.getInstance();
      if (noRender != null && noRender.shouldHideBadEffects()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderPortalOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderPortalOverlay(DrawContext context, float nauseaStrength, CallbackInfo ci) {
      NoRender noRender = NoRender.getInstance();
      if (noRender != null && noRender.shouldHideBadEffects()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderOverlayMessage"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderOverlayMessage(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      Hud hud = this.getHud();
      if (hud != null && hud.isState() && (Hud.interfaceSettings.isSelected("HotBar") || Hud.interfaceSettings.isSelected("CustomHotbar"))) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderExperienceLevel"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderExperienceLevel(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      Hud hud = this.getHud();
      if (hud != null && hud.isState() && (Hud.interfaceSettings.isSelected("HotBar") || Hud.interfaceSettings.isSelected("CustomHotbar"))) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderMainHud"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderMainHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      Hud hud = this.getHud();
      if (hud != null && hud.isState() && (Hud.interfaceSettings.isSelected("HotBar") || Hud.interfaceSettings.isSelected("CustomHotbar"))) {
         context.drawGuiTexture(RenderLayer::getGuiTextured, InGameHud.HOTBAR_ATTACK_INDICATOR_BACKGROUND_TEXTURE, 0, 0, 1, 1);
         if (this.client.interactionManager != null && this.client.interactionManager.hasStatusBars()) {
            this.renderStatusBars(context);
         }

         this.renderMountHealth(context);
         ci.cancel();
      }
   }
}
