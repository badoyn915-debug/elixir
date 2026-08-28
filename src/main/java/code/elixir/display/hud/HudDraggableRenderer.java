package code.elixir.display.hud;

import code.elixir.Elixir;
import code.elixir.features.impl.render.Hud;
import code.elixir.utils.client.managers.api.draggable.AbstractDraggable;
import code.elixir.utils.display.render.HudShine;
import code.elixir.utils.display.render.shape.implement.Rectangle;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public final class HudDraggableRenderer {
   private HudDraggableRenderer() {
   }

   public static void render(DrawContext context) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.player != null && client.world != null && !client.options.hudHidden) {
         Hud hud = Hud.getInstance();
         if (hud != null && hud.isState()) {
            List<AbstractDraggable> draggables = Elixir.getInstance().getDraggableRepository().draggable();
            int count = draggables.size();

            for (int i = 0; i < count; i++) {
               AbstractDraggable draggable = draggables.get(i);
               if (draggable.canDraw(hud, draggable)) {
                  draggable.startAnimation();
               } else {
                  draggable.stopAnimation();
               }

               float alpha = draggable.getScaleAnimation().getOutput().floatValue() * Hud.setAlpha.getValue();
               if (!draggable.isCloseAnimationFinished()) {
                  draggable.updateEditorMotion();
                  float offsetX = draggable.getRenderX() - draggable.getX();
                  float offsetY = draggable.getRenderY() - draggable.getY();
                  float sway = draggable.getSwayAngle();
                  float centerX = draggable.getX() + draggable.getWidth() / 2.0F;
                  float centerY = draggable.getY() + draggable.getHeight() / 2.0F;
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, MathHelper.clamp(alpha, 0.0F, 1.0F));

                  try {
                     float scale = draggable.getScaleFactor();
                     context.getMatrices().push();
                     context.getMatrices().translate(offsetX, offsetY, 0.0F);
                     context.getMatrices().translate(centerX, centerY, 0.0F);
                     context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sway));
                     context.getMatrices().scale(scale, scale, 1.0F);
                     context.getMatrices().translate(-centerX, -centerY, 0.0F);
                     Rectangle.beginBatch();

                     try {
                        draggable.drawDraggable(context);
                     } catch (Throwable t) {
                     } finally {
                        Rectangle.endBatch();
                     }

                     draggable.clampToScreen();
                     if (Hud.shine.isValue() && draggable.visible() && Hud.shineElements.isSelected(draggable.getName())) {
                        int panels = Rectangle.panelCount();
                        String name = draggable.getName();
                        boolean onlyLargest = "Target Hud".equals(name) || "TotemBar".equals(name);
                        boolean exceptLargest = "ArmorBar".equals(name) || "CustomHotbar".equals(name);
                        int largest = -1;
                        if (onlyLargest || exceptLargest) {
                           float bestArea = -1.0F;

                           for (int p = 0; p < panels; p++) {
                              float pw = Rectangle.panelWidth(p);
                              float ph = Rectangle.panelHeight(p);
                              if (pw >= 20.0F && ph >= 12.0F && pw * ph > bestArea) {
                                 bestArea = pw * ph;
                                 largest = p;
                              }
                           }
                        }

                        for (int px = 0; px < panels; px++) {
                           float pw = Rectangle.panelWidth(px);
                           float ph = Rectangle.panelHeight(px);
                           if (!(pw < 20.0F) && !(ph < 12.0F) && (!onlyLargest || px == largest) && (!exceptLargest || px != largest)) {
                              HudShine.render(
                                 context.getMatrices(),
                                 Rectangle.panelX(px),
                                 Rectangle.panelY(px),
                                 pw,
                                 ph,
                                 Rectangle.panelRound(px),
                                 Hud.shineSize.getValue(),
                                 Hud.shineSpeed.getValue(),
                                 Hud.shineBrightness.getValue()
                              );
                           }
                        }
                     }

                     context.getMatrices().pop();
                  } finally {
                     RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                  }
               }
            }
         }
      }
   }
}
