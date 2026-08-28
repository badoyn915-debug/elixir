package code.elixir.mixins;

import code.elixir.features.impl.render.BlockOverlay;
import code.elixir.features.impl.render.ShaderFog;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({WorldRenderer.class})
public abstract class WorldRendererMixin {
   @Shadow
   protected abstract void renderMain(
      FrameGraphBuilder var1,
      Frustum var2,
      Camera var3,
      Matrix4f var4,
      Matrix4f var5,
      Fog var6,
      boolean var7,
      boolean var8,
      RenderTickCounter var9,
      Profiler var10
   );

   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/WorldRenderer;renderMain(Lnet/minecraft/client/render/FrameGraphBuilder;Lnet/minecraft/client/render/Frustum;Lnet/minecraft/client/render/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/render/Fog;ZZLnet/minecraft/client/render/RenderTickCounter;Lnet/minecraft/util/profiler/Profiler;)V"
      )
   )
   private void onRender(
      WorldRenderer instance,
      FrameGraphBuilder frameGraphBuilder,
      Frustum frustum,
      Camera camera,
      Matrix4f positionMatrix,
      Matrix4f projectionMatrix,
      Fog fog,
      boolean renderBlockOutline,
      boolean hasEntitiesToRender,
      RenderTickCounter renderTickCounter,
      Profiler profiler
   ) {
      BlockOverlay blockOverlay = BlockOverlay.getInstance();
      boolean shouldRenderOutline = blockOverlay == null || !blockOverlay.isState();
      this.renderMain(
         frameGraphBuilder, frustum, camera, positionMatrix, projectionMatrix, fog, shouldRenderOutline, hasEntitiesToRender, renderTickCounter, profiler
      );
   }

   @Inject(
      method = {"renderSky"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderSky(FrameGraphBuilder frameGraphBuilder, Camera camera, float tickDelta, Fog fog, CallbackInfo ci) {
      if (ShaderFog.shouldReplaceVanillaSky()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderClouds"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderClouds(
      FrameGraphBuilder frameGraphBuilder,
      Matrix4f matrix4f,
      Matrix4f matrix4f2,
      CloudRenderMode cloudRenderMode,
      Vec3d vec3d,
      float f,
      int i,
      float f2,
      CallbackInfo ci
   ) {
      if (ShaderFog.shouldReplaceVanillaSky()) {
         ci.cancel();
      }
   }
}
