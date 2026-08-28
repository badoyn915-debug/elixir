package code.elixir.display.screens.clickgui.components.implement.other;

import code.elixir.display.screens.clickgui.MenuScreen;
import code.elixir.display.screens.clickgui.components.AbstractComponent;
import code.elixir.display.screens.clickgui.components.implement.category.CategoryComponent;
import code.elixir.display.screens.clickgui.components.implement.settings.TextComponent;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.interactions.inv.InventoryFlowManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;

public class CategoryContainerComponent extends AbstractComponent {
   private final List<CategoryComponent> categoryComponents = new ArrayList<>();

   public void initializeCategoryComponents() {
      this.categoryComponents.clear();

      for (ModuleCategory category : ModuleCategory.values()) {
         this.categoryComponents.add(new CategoryComponent(category));
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      float offset = 0.0F;

      for (CategoryComponent component : this.categoryComponents) {
         component.x = this.x + 6.0F;
         component.y = this.y + 3.0F + offset;
         component.render(context, mouseX, mouseY, delta);
         offset += 30.0F;
      }
   }

   @Override
   public void tick() {
      if (!TextComponent.typing && !MenuScreen.hasActiveClickGuiInputs()) {
         InventoryFlowManager.updateMoveKeys();
      } else {
         InventoryFlowManager.unPressMoveKeys();
      }

      this.categoryComponents.forEach(AbstractComponent::tick);
      super.tick();
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.categoryComponents.forEach(categoryComponent -> categoryComponent.mouseClicked(mouseX, mouseY, button));
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.categoryComponents.forEach(categoryComponent -> categoryComponent.mouseReleased(mouseX, mouseY, button));
      return super.mouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      this.categoryComponents.forEach(categoryComponent -> categoryComponent.mouseDragged(mouseX, mouseY, button, deltaX, deltaY));
      return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      this.categoryComponents.forEach(categoryComponent -> categoryComponent.mouseScrolled(mouseX, mouseY, amount));
      return super.mouseScrolled(mouseX, mouseY, amount);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      this.categoryComponents.forEach(categoryComponent -> categoryComponent.keyPressed(keyCode, scanCode, modifiers));
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      this.categoryComponents.forEach(categoryComponent -> categoryComponent.charTyped(chr, modifiers));
      return super.charTyped(chr, modifiers);
   }
}
