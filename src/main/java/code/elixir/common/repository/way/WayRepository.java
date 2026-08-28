package code.elixir.common.repository.way;

import code.elixir.events.render.DrawEvent;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.managers.event.EventManager;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.display.interfaces.QuickLogger;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.math.projection.Projection;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class WayRepository implements QuickImports, QuickLogger {
   public List<Way> wayList = new ArrayList<>();

   public WayRepository(EventManager eventManager) {
      EventManager.register(this);
   }

   public boolean isEmpty() {
      return this.wayList.isEmpty();
   }

   public void addWay(String name, BlockPos pos, String server) {
      this.wayList.add(new Way(name, pos, server));
   }

   public void upsertWay(String name, BlockPos pos, String server) {
      if (name != null) {
         for (int i = 0; i < this.wayList.size(); i++) {
            Way w = this.wayList.get(i);
            if (w != null && w.name().equalsIgnoreCase(name)) {
               this.wayList.set(i, new Way(name, pos, server));
               return;
            }
         }

         this.addWay(name, pos, server);
      }
   }

   public boolean hasWay(String text) {
      return this.wayList.stream().anyMatch(s -> s.name().equalsIgnoreCase(text));
   }

   public void deleteWay(String name) {
      this.wayList.removeIf(macro -> macro.name().equalsIgnoreCase(name));
   }

   public void clearList() {
      if (!this.isEmpty()) {
         this.wayList.clear();
      }
   }

   @EventHandler
   public void onDraw(DrawEvent e) {
      if (!this.isEmpty() && mc.player != null) {
         String currentServer = mc.getNetworkHandler() == null
            ? "vanilla"
            : (mc.getNetworkHandler().getServerInfo() == null ? "vanilla" : mc.getNetworkHandler().getServerInfo().address);
         MatrixStack matrix = e.getDrawContext().getMatrices();
         double[] screen = new double[3];
         RenderSystem.disableDepthTest();
         this.wayList
            .forEach(
               way -> {
                  if (way.server().equalsIgnoreCase(currentServer)) {
                     Vec3d wayVec = way.pos().toCenterPos();
                     if (Projection.isInFrontOfCamera(wayVec)) {
                        if (Projection.worldSpaceToScreenSpaceInto(wayVec.x, wayVec.y, wayVec.z, screen)) {
                           double distance = mc.getEntityRenderDispatcher().camera.getPos().distanceTo(wayVec);
                           String text = way.name() + " - " + Math.round(distance) + "m";
                           FontRenderer font = Fonts.getSize(10, Fonts.Type.SuisseIntlMedium);
                           float textWidth = font.getStringWidth(text);
                           float textHeight = font.getStringHeight(text);
                           float paddingX = 5.0F;
                           float paddingY = 3.0F;
                           float boxWidth = textWidth + paddingX * 2.0F;
                           float boxHeight = textHeight + paddingY * 2.0F;
                           float x = (float)screen[0] - boxWidth / 2.0F;
                           float y = (float)screen[1] - boxHeight / 2.0F;
                           rectangle.render(
                              ShapeProperties.create(matrix, x, y, boxWidth, boxHeight).round(4.0F).color(new Color(0, 0, 0, 255).getRGB()).build()
                           );
                           rectangle.render(
                              ShapeProperties.create(matrix, x, y, boxWidth, boxHeight)
                                 .round(4.0F)
                                 .thickness(3.0F)
                                 .outlineColor(new Color(220, 225, 235, 235).getRGB())
                                 .color(0)
                                 .build()
                           );
                           font.drawCenteredBoth(matrix, text, x + boxWidth / 2.0F, y, boxHeight, ColorAssist.getText());
                        }
                     }
                  }
               }
            );
         RenderSystem.enableDepthTest();
      }
   }
}
