package code.elixir.mixins;

import code.elixir.display.widgets.ClearButtonWidget;
import code.elixir.features.impl.movement.GuiMove;
import code.elixir.utils.display.interfaces.QuickImports;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({InventoryScreen.class})
public abstract class InventoryScreenMixin extends HandledScreen<PlayerScreenHandler> {
   @Unique
   private ClearButtonWidget deleteButton;
   @Unique
   private static volatile boolean deleting = false;
   @Unique
   private static final ScheduledExecutorService clearExecutor = Executors.newSingleThreadScheduledExecutor();

   @Unique
   public ClearButtonWidget getDeleteButton() {
      return this.deleteButton;
   }

   public InventoryScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
      super(handler, inventory, title);
   }

   @Inject(
      method = {"init"},
      at = {@At("TAIL")}
   )
   private void onInit(CallbackInfo ci) {
      if (GuiMove.mode.isSelected("SpookyTime")) {
         InventoryScreen screen = (InventoryScreen)(Object)this;
         this.deleteButton = new ClearButtonWidget(0, 0, 12, 12, () -> this.deleteAllItems());
         IScreen iScreen = (IScreen)screen;
         iScreen.getDrawables().add(this.deleteButton);
         iScreen.getSelectables().add(this.deleteButton);
         iScreen.getChildren().add(this.deleteButton);
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (this.deleteButton != null && GuiMove.mode.isSelected("SpookyTime")) {
         InventoryScreen screen;
         InventoryScreen screenBase = screen = (InventoryScreen)(Object)this;
         int guiLeft = (screenBase.width - this.backgroundWidth) / 2;
         int guiTop = (screenBase.height - 166) / 2;
         int buttonX = guiLeft + this.backgroundWidth - 12 - 5;
         int buttonY = guiTop + 5;
         this.deleteButton.setPosition(buttonX, buttonY);
      } else if (this.deleteButton != null && !GuiMove.mode.isSelected("SpookyTime")) {
         InventoryScreen screen = (InventoryScreen)(Object)this;
         IScreen iScreen = (IScreen)screen;
         iScreen.getDrawables().remove(this.deleteButton);
         iScreen.getSelectables().remove(this.deleteButton);
         iScreen.getChildren().remove(this.deleteButton);
         this.deleteButton = null;
      }
   }

   @Inject(
      method = {"init"},
      at = {@At("TAIL")}
   )
   private void addDropAllButton(CallbackInfo ci) {
      MinecraftClient mc = MinecraftClient.getInstance();
      InventoryScreen screen = (InventoryScreen)(Object)this;
      int x = screen.width / 2 - 40;
      int y = screen.height / 2 - 110;
      ButtonWidget dropAllButton = ButtonWidget.builder(Text.of("Выкинуть все"), button -> this.dropAllItems(mc)).position(x, y).size(80, 20).build();
      screen.addDrawableChild(dropAllButton);
   }

   private void dropAllItems(MinecraftClient mc) {
      ClientPlayerEntity player = mc.player;
      if (player != null && player.currentScreenHandler != null) {
         for (int i = 5; i <= 8; i++) {
            mc.interactionManager.clickSlot(player.currentScreenHandler.syncId, i, 1, SlotActionType.THROW, player);
         }

         mc.interactionManager.clickSlot(player.currentScreenHandler.syncId, 45, 1, SlotActionType.THROW, player);

         for (int var6 = 9; var6 < 36; var6++) {
            ItemStack stack = player.getInventory().getStack(var6);
            if (!stack.isEmpty()) {
               mc.interactionManager.clickSlot(player.currentScreenHandler.syncId, var6, 1, SlotActionType.THROW, player);
            }
         }

         for (int var7 = 0; var7 < 9; var7++) {
            ItemStack stack = player.getInventory().getStack(var7);
            if (!stack.isEmpty()) {
               mc.interactionManager.clickSlot(player.currentScreenHandler.syncId, var7 + 36, 1, SlotActionType.THROW, player);
            }
         }
      }
   }

   private void deleteAllItems() {
      if (!deleting) {
         deleting = true;

         for (int i = 0; i < 36; i++) {
            int slot = i;
            clearExecutor.schedule(
               () -> {
                  if (QuickImports.mc.player != null
                     && QuickImports.mc.interactionManager != null
                     && QuickImports.mc.player.currentScreenHandler != null
                     && QuickImports.mc.player.getInventory().getStack(slot).getItem() != Items.AIR) {
                     int containerSlot = slot < 9 ? slot + 36 : slot;
                     QuickImports.mc
                        .interactionManager
                        .clickSlot(QuickImports.mc.player.currentScreenHandler.syncId, containerSlot, 45, SlotActionType.SWAP, QuickImports.mc.player);
                  }
               },
               i * 80L,
               TimeUnit.MILLISECONDS
            );
         }

         clearExecutor.schedule(
            () -> {
               if (QuickImports.mc.player != null
                  && QuickImports.mc.interactionManager != null
                  && QuickImports.mc.player.currentScreenHandler != null
                  && QuickImports.mc.player.getInventory().getStack(40).getItem() != Items.AIR) {
                  QuickImports.mc
                     .interactionManager
                     .clickSlot(QuickImports.mc.player.currentScreenHandler.syncId, 45, 45, SlotActionType.SWAP, QuickImports.mc.player);
               }
            },
            2880L,
            TimeUnit.MILLISECONDS
         );
         clearExecutor.schedule(
            () -> {
               if (QuickImports.mc.player != null
                  && QuickImports.mc.interactionManager != null
                  && QuickImports.mc.player.currentScreenHandler != null
                  && QuickImports.mc.player.getInventory().getStack(39).getItem() != Items.AIR) {
                  QuickImports.mc
                     .interactionManager
                     .clickSlot(QuickImports.mc.player.currentScreenHandler.syncId, 5, 45, SlotActionType.SWAP, QuickImports.mc.player);
               }
            },
            2960L,
            TimeUnit.MILLISECONDS
         );
         clearExecutor.schedule(
            () -> {
               if (QuickImports.mc.player != null
                  && QuickImports.mc.interactionManager != null
                  && QuickImports.mc.player.currentScreenHandler != null
                  && QuickImports.mc.player.getInventory().getStack(38).getItem() != Items.AIR) {
                  QuickImports.mc
                     .interactionManager
                     .clickSlot(QuickImports.mc.player.currentScreenHandler.syncId, 6, 45, SlotActionType.SWAP, QuickImports.mc.player);
               }
            },
            3040L,
            TimeUnit.MILLISECONDS
         );
         clearExecutor.schedule(
            () -> {
               if (QuickImports.mc.player != null
                  && QuickImports.mc.interactionManager != null
                  && QuickImports.mc.player.currentScreenHandler != null
                  && QuickImports.mc.player.getInventory().getStack(37).getItem() != Items.AIR) {
                  QuickImports.mc
                     .interactionManager
                     .clickSlot(QuickImports.mc.player.currentScreenHandler.syncId, 7, 45, SlotActionType.SWAP, QuickImports.mc.player);
               }
            },
            3120L,
            TimeUnit.MILLISECONDS
         );
         clearExecutor.schedule(
            () -> {
               if (QuickImports.mc.player != null
                  && QuickImports.mc.interactionManager != null
                  && QuickImports.mc.player.currentScreenHandler != null
                  && QuickImports.mc.player.getInventory().getStack(36).getItem() != Items.AIR) {
                  QuickImports.mc
                     .interactionManager
                     .clickSlot(QuickImports.mc.player.currentScreenHandler.syncId, 8, 45, SlotActionType.SWAP, QuickImports.mc.player);
               }

               deleting = false;
            },
            3200L,
            TimeUnit.MILLISECONDS
         );
      }
   }
}
