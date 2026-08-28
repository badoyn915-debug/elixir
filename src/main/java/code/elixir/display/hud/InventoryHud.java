package code.elixir.display.hud;

import code.elixir.features.impl.render.Hud;
import code.elixir.utils.client.managers.api.draggable.AbstractDraggable;
import code.elixir.utils.display.render.geometry.Render2D;
import code.elixir.utils.display.render.post.KawaseBlur;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.theme.ThemeManager;
import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

public class InventoryHud extends AbstractDraggable {
   private static final InventoryHud INSTANCE = new InventoryHud();
   private static final float SLOT_SIZE = 16.0F;
   private static final float SLOT_GAP = 2.0F;
   private static final float PANEL_PADDING = 5.0F;
   private static final int COLS = 9;
   private static final int ROWS = 3;

   private InventoryHud() {
      super("Inventory", 4, 80, 170, 62, true);
   }

   public static InventoryHud getInstance() {
      return INSTANCE;
   }

   @Override
   public boolean visible() {
      Hud hud = Hud.getInstance();
      if (hud == null) {
         return false;
      } else {
         return !hud.isState() ? false : Hud.interfaceSettings.isSelected("Inventory");
      }
   }

   @Override
   public void drawDraggable(DrawContext context) {
      if (mc.player != null) {
         MatrixStack matrix = context.getMatrices();
         float gridWidth = 160.0F;
         float gridHeight = 52.0F;
         float panelWidth = gridWidth + 10.0F;
         float panelHeight = gridHeight + 10.0F;
         this.setWidth(Math.round(panelWidth));
         this.setHeight(Math.round(panelHeight));
         this.clampToScreen();
         float panelX = this.getX();
         float panelY = this.getY();
         this.drawPanelBackground(matrix, panelX, panelY, panelWidth, panelHeight);
         float startX = panelX + 5.0F;
         float startY = panelY + 5.0F;

         for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
               int slot = col + row * 9 + 9;
               float slotX = startX + col * 18.0F;
               float slotY = startY + row * 18.0F;
               rectangle.render(ShapeProperties.create(matrix, slotX, slotY, 16.0, 16.0).round(3.0F).color(ThemeManager.BackgroundSettings.getColor()).build());
               ItemStack stack = mc.player.getInventory().getStack(slot);
               if (!stack.isEmpty()) {
                  Render2D.defaultDrawStack(context, stack, slotX + 1.0F, slotY + 1.0F, true, true, 0.85F);
               }
            }
         }
      }
   }

   private void drawPanelBackground(MatrixStack matrix, float x, float y, float width, float height) {
      Hud.getInstance();
      if (Hud.blur.isValue() && !KawaseBlur.INSTANCE.fbos.isEmpty()) {
         Render2D.rectangleWithMask(matrix.peek().getPositionMatrix(), x, y, width, height, 6.0F, 0, KawaseBlur.INSTANCE.fbos.getFirst().getColorAttachment());
      }

      rectangle.render(ShapeProperties.create(matrix, x, y, width, height).round(6.0F).softness(2.5F).color(new Color(0, 0, 0, 210).getRGB()).build());
   }
}
