package code.elixir.mixins;

import code.elixir.display.hud.CustomHudOverlay;
import code.elixir.events.render.AspectRatioEvent;
import code.elixir.events.render.FovEvent;
import code.elixir.events.render.WorldRenderEvent;
import code.elixir.features.impl.misc.FreeCam;
import code.elixir.features.impl.render.HandShader;
import code.elixir.features.impl.render.NoRender;
import code.elixir.features.impl.render.ShaderFog;
import code.elixir.utils.client.managers.event.SafeEvents;
import code.elixir.utils.display.render.geometry.Render3D;
import code.elixir.utils.features.aura.utils.MathAngle;
import code.elixir.utils.features.aura.utils.RaycastAngle;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profilers;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({GameRenderer.class})
public abstract class GameRendererMixin {
   @Final
   @Shadow
   private MinecraftClient client;
   private Framebuffer elixir$beforeHandFbo = null;
   private Framebuffer elixir$afterHandFbo = null;
   @Shadow
   private float zoom;
   @Shadow
   private float zoomX;
   @Shadow
   private float zoomY;

   @Shadow
   public abstract float getFarPlaneDistance();

   @Inject(
      method = {"updateCrosshairTarget"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/GameRenderer;findCrosshairTarget(Lnet/minecraft/entity/Entity;DDF)Lnet/minecraft/util/hit/HitResult;"
      )},
      cancellable = true
   )
   private void onUpdateTargetedEntity(float tickDelta, CallbackInfo ci) {
      if (!PlayerInteractionHelper.nullCheck()) {
         FreeCam debugCamera = FreeCam.getInstance();
         if (debugCamera.isState()) {
            Profilers.get().pop();
            this.client.crosshairTarget = RaycastAngle.raycast(debugCamera.pos, 4.5, MathAngle.cameraAngle(), false);
            ci.cancel();
         }
      }
   }

   @Redirect(
      method = {"findCrosshairTarget"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/Entity;raycast(DFZ)Lnet/minecraft/util/hit/HitResult;"
      )
   )
   private HitResult hookRaycast(Entity instance, double maxDistance, float tickDelta, boolean includeFluids) {
      if (this.client.player != null && instance == (Object)this.client.player && TurnsConnection.INSTANCE != null) {
         return RaycastAngle.raycast(maxDistance, TurnsConnection.INSTANCE.getRotation(), includeFluids);
      } else {
         return instance.raycast(maxDistance, tickDelta, includeFluids);
      }
   }

   @Redirect(
      method = {"findCrosshairTarget"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/Entity;getRotationVec(F)Lnet/minecraft/util/math/Vec3d;"
      )
   )
   private Vec3d hookRotationVector(Entity instance, float tickDelta) {
      if (this.client.player != null && TurnsConnection.INSTANCE != null && TurnsConnection.INSTANCE.getRotation() != null) {
         return TurnsConnection.INSTANCE.getRotation().toVector();
      }
      return instance.getRotationVec(tickDelta);
   }

   @Inject(
      method = {"getBasicProjectionMatrix"},
      at = {@At("TAIL")},
      cancellable = true
   )
   public void getBasicProjectionMatrixHook(float fovDegrees, CallbackInfoReturnable<Matrix4f> cir) {
      AspectRatioEvent aspectRatioEvent = SafeEvents.dispatch(() -> new AspectRatioEvent());
      if (aspectRatioEvent != null && aspectRatioEvent.isCancelled()) {
         Matrix4f matrix4f = new Matrix4f();
         if (this.zoom != 1.0F) {
            matrix4f.translate(this.zoomX, -this.zoomY, 0.0F);
            matrix4f.scale(this.zoom, this.zoom, 1.0F);
         }

         matrix4f.perspective(fovDegrees * (float) (Math.PI / 180.0), aspectRatioEvent.getRatio(), 0.05F, this.getFarPlaneDistance());
         cir.setReturnValue(matrix4f);
      }
   }

   @ModifyExpressionValue(
      method = {"getFov"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/lang/Integer;intValue()I",
         remap = false
      )}
   )
   private int hookGetFov(int original) {
      FovEvent event = SafeEvents.dispatch(() -> new FovEvent());
      return event != null && event.isCancelled() ? event.getFov() : original;
   }

   @Inject(
      method = {"renderWorld"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z",
         opcode = 180,
         ordinal = 0
      )}
   )
   public void hookWorldRender(RenderTickCounter tickCounter, CallbackInfo ci, @Local(ordinal = 2) Matrix4f matrix4f) {
      if (this.client.getEntityRenderDispatcher() != null && this.client.getEntityRenderDispatcher().camera != null) {
         MatrixStack matrixStack = new MatrixStack();
         matrixStack.multiplyPositionMatrix(matrix4f);
         matrixStack.translate(this.client.getEntityRenderDispatcher().camera.getPos().negate());
         Render3D.setLastProjMat(RenderSystem.getProjectionMatrix());
         Render3D.setLastWorldSpaceMatrix(matrixStack.peek());
         ShaderFog shaderFog = ShaderFog.getInstance();
         if (shaderFog != null && shaderFog.isState()) {
            shaderFog.renderSkyShader();
         }

         WorldRenderEvent event;
         if ((event = SafeEvents.dispatch(() -> new WorldRenderEvent(matrixStack, tickCounter.getTickDelta(false)))) != null) {
            Render3D.onWorldRender(event);
         }
      }
   }

   @Inject(
      method = {"tiltViewWhenHurt"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onTiltViewWhenHurt(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
      NoRender noRender = NoRender.getInstance();
      if (noRender != null && noRender.isState() && noRender.modeSetting.isSelected("Тряска экрана")) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderWorld"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/GameRenderer;renderHand(Lnet/minecraft/client/render/Camera;FLorg/joml/Matrix4f;)V",
         shift = Shift.BEFORE
      )}
   )
   private void captureBeforeHand(RenderTickCounter tickCounter, CallbackInfo ci) {
      HandShader handShader = HandShader.getInstance();
      if (handShader != null && handShader.isState() && handShader.shouldRender() && this.client.getFramebuffer() != null) {
         if (this.elixir$beforeHandFbo == null) {
            this.elixir$beforeHandFbo = new SimpleFramebuffer(
               this.client.getFramebuffer().textureWidth, this.client.getFramebuffer().textureHeight, true
            );
         }

         if (this.elixir$beforeHandFbo.textureWidth != this.client.getFramebuffer().textureWidth
            || this.elixir$beforeHandFbo.textureHeight != this.client.getFramebuffer().textureHeight) {
            this.elixir$beforeHandFbo.resize(this.client.getFramebuffer().textureWidth, this.client.getFramebuffer().textureHeight);
         }

         handShader.beginHandCapture();
         this.elixir$copyColorFramebuffer(this.client.getFramebuffer(), this.elixir$beforeHandFbo);
         this.client.getFramebuffer().beginWrite(true);
      } else {
         if (handShader != null) {
            handShader.invalidateCapture();
         }
      }
   }

   @Inject(
      method = {"renderWorld"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/GameRenderer;renderHand(Lnet/minecraft/client/render/Camera;FLorg/joml/Matrix4f;)V",
         shift = Shift.AFTER
      )}
   )
   private void captureAfterHand(RenderTickCounter tickCounter, CallbackInfo ci) {
      HandShader handShader = HandShader.getInstance();
      if (handShader != null && handShader.isState() && handShader.shouldRender() && this.client.getFramebuffer() != null) {
         if (this.elixir$afterHandFbo == null) {
            this.elixir$afterHandFbo = new SimpleFramebuffer(
               this.client.getFramebuffer().textureWidth, this.client.getFramebuffer().textureHeight, true
            );
         }

         if (this.elixir$afterHandFbo.textureWidth != this.client.getFramebuffer().textureWidth
            || this.elixir$afterHandFbo.textureHeight != this.client.getFramebuffer().textureHeight) {
            this.elixir$afterHandFbo.resize(this.client.getFramebuffer().textureWidth, this.client.getFramebuffer().textureHeight);
         }

         this.elixir$copyColorFramebuffer(this.client.getFramebuffer(), this.elixir$afterHandFbo);
         this.client.getFramebuffer().beginWrite(false);
         handShader.onShaderCapture(this.elixir$beforeHandFbo, this.elixir$afterHandFbo);
         handShader.renderCapturedEffect();
      }
   }

   private void elixir$copyColorFramebuffer(Framebuffer src, Framebuffer dst) {
      RenderSystem.assertOnRenderThread();
      dst.beginWrite(false);
      src.drawInternal(dst.textureWidth, dst.textureHeight);
      dst.endWrite();
   }

   @Inject(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/DrawContext;draw()V",
         ordinal = 0,
         shift = Shift.AFTER
      )}
   )
   private void elixir$renderCustomHud(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci, @Local DrawContext drawContext) {
      if (this.client.isFinishedLoading() && tick && this.client.world != null) {
         if (!(this.client.currentScreen instanceof ChatScreen)) {
            CustomHudOverlay.render(drawContext, tickCounter, true);
         }
      }
   }
}
