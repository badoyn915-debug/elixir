package code.elixir.display.hud;

import code.elixir.common.animation.Animation;
import code.elixir.common.animation.Direction;
import code.elixir.common.animation.implement.Decelerate;
import code.elixir.common.animation.implement.EaseOut;
import code.elixir.events.packet.PacketEvent;
import code.elixir.features.impl.render.Hud;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.chat.StringHelper;
import code.elixir.utils.client.managers.api.draggable.AbstractDraggable;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.BackgroundIcons;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.geometry.Render2D;
import code.elixir.utils.display.render.post.KawaseBlur;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.math.time.StopWatch;
import code.elixir.utils.theme.ThemeManager;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.CooldownUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.registry.Registries;

public class CoolDowns extends AbstractDraggable {
   public final List<CoolDowns.CoolDown> list = new ArrayList<>();
   private final List<CoolDowns.CoolDown> renderSnapshot = new ArrayList<>();
   private long lastItemChange = 0L;
   private int currentItemIndex = 0;
   private static final Item[] EXAMPLE_ITEMS = new Item[]{
      Items.ENDER_EYE,
      Items.ENDER_PEARL,
      Items.SUGAR,
      Items.MACE,
      Items.ENCHANTED_GOLDEN_APPLE,
      Items.TRIDENT,
      Items.CROSSBOW,
      Items.DRIED_KELP,
      Items.NETHERITE_SCRAP
   };
   private final FontRenderer fontMedium = Fonts.getSize(13, Fonts.Type.SuisseIntlMedium);
   private final FontRenderer fontHeader = Fonts.getSize(16, Fonts.Type.DEFAULT);
   private final FontRenderer fontDuration = Fonts.getSize(13, Fonts.Type.DEFAULT);
   private final FontRenderer fontItemName = Fonts.getSize(14, Fonts.Type.SuisseIntlMedium);

   public static CoolDowns getInstance() {
      return Instance.getDraggable(CoolDowns.class);
   }

   public CoolDowns() {
      super("Cooldowns", 10, 40, 80, 23, true);
      this.scaleAnimation = new EaseOut().setValue(1.0).setMs(100);
   }

   @Override
   public boolean visible() {
      if (this.list.isEmpty() && !PlayerInteractionHelper.isChat(mc.currentScreen)) {
         return false;
      } else {
         Hud.getInstance();
         return !Hud.interfaceSettings.isSelected("Cooldowns") ? false : Hud.getInstance().state;
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         FontRenderer iconFont = Fonts.getSize(15, Fonts.Type.ELIXIR_ICONS);
         float pX = this.getX() + this.getWidth() - iconFont.getStringWidth("p") - 8.0F;
         float pY = this.getY() + 9.5F;
         float pWidth = iconFont.getStringWidth("p");
         float pHeight = iconFont.getStringHeight("p");
         if (mouseX >= pX && mouseX <= pX + pWidth && mouseY >= pY && mouseY <= pY + pHeight) {
            Hud.getInstance();
            ArrayList<String> selected = new ArrayList<>(Hud.interfaceSettings.getSelected());
            selected.remove("Cooldowns");
            Hud.getInstance();
            Hud.interfaceSettings.setSelected(selected);
            return true;
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public void tick() {
      this.list.removeIf(c -> c.anim.isFinished(Direction.BACKWARDS));
      this.list
         .stream()
         .filter(c -> !Objects.requireNonNull(mc.player).getItemCooldownManager().isCoolingDown(c.item.getDefaultStack()))
         .forEach(coolDown -> coolDown.anim.setDirection(Direction.BACKWARDS));
      long currentTime;
      if (this.list.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen) && (currentTime = System.currentTimeMillis()) - this.lastItemChange >= 1000L) {
         this.currentItemIndex = (this.currentItemIndex + 1) % EXAMPLE_ITEMS.length;
         this.lastItemChange = currentTime;
      }
   }

   @Override
   public void packet(PacketEvent e) {
      if (!PlayerInteractionHelper.nullCheck()) {
         Packet<?> packet = e.getPacket();
         if (packet instanceof CooldownUpdateS2CPacket c) {
            Item item = (Item)Registries.ITEM.get(c.cooldownGroup());
            this.list.stream().filter(coolDown -> coolDown.item.equals(item)).forEach(coolDown -> coolDown.anim.setDirection(Direction.BACKWARDS));
            if (c.cooldown() != 0) {
               this.list.add(new CoolDowns.CoolDown(item, new StopWatch().setMs(-c.cooldown() * 50L), new Decelerate().setMs(150).setValue(1.0)));
            }
         } else if (packet instanceof PlayerRespawnS2CPacket) {
            this.list.clear();
         }
      }
   }

   @Override
   public void drawDraggable(DrawContext context) {
      MatrixStack matrix = context.getMatrices();
      this.renderSnapshot.clear();
      this.renderSnapshot.addAll(this.list);
      FontRenderer fontMedium = this.fontMedium;
      FontRenderer font = this.fontHeader;
      FontRenderer fontDuration = this.fontDuration;
      FontRenderer fontItemName = this.fontItemName;
      float posX = this.getX();
      float posY = this.getY();
      float maxElementsWidth = HudListPanel.headerMinWidth(font, "Cooldowns");
      float headerHeight = 21.0F;
      float cooldownHeight = 11.0F;
      boolean isFound = false;

      for (CoolDowns.CoolDown coolDown : this.renderSnapshot) {
         if (coolDown.anim.getOutput().floatValue() > 0.0F) {
            isFound = true;
            String name = coolDown.item.getDefaultStack().getName().getString();
            if (name.equals("Зачарованное золотое яблоко")) {
               name = "Чарка";
            }

            float elementsWidth;
            if ((
                  elementsWidth = HudListPanel.rowWidth(
                     fontItemName, name, 0.0F, fontDuration, StringHelper.getDuration((int)(-coolDown.time.elapsedTime() / 1000L))
                  )
               )
               > maxElementsWidth) {
               maxElementsWidth = elementsWidth;
            }
         }
      }

      if (this.list.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
         isFound = true;
         float elementsWidth = HudListPanel.rowWidth(fontItemName, "Example", 0.0F, fontDuration, "**:**");
         if (elementsWidth > maxElementsWidth) {
            maxElementsWidth = elementsWidth;
         }
      }

      float totalContentHeight = 0.0F;

      for (CoolDowns.CoolDown coolDownx : this.renderSnapshot) {
         float animValue = coolDownx.anim.getOutput().floatValue();
         if (animValue > 0.0F) {
            totalContentHeight += cooldownHeight * animValue;
         }
      }

      if (this.list.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
         totalContentHeight += cooldownHeight;
      }

      float f = HudListPanel.panelHeight(totalContentHeight);
      float width = maxElementsWidth;
      Hud.getInstance();
      if (Hud.blur.isValue() && !KawaseBlur.INSTANCE.fbos.isEmpty()) {
         Render2D.rectangleWithMask(
            matrix.peek().getPositionMatrix(), posX, posY, maxElementsWidth, f, 6.0F, 0, KawaseBlur.INSTANCE.fbos.getFirst().getColorAttachment()
         );
      }

      rectangle.render(
         ShapeProperties.create(matrix, posX, posY, maxElementsWidth, f).round(6.0F).softness(2.5F).color(new Color(0, 0, 0, 210).getRGB()).build()
      );
      float headerTextY = HudListPanel.headerTextY(font, posY, "Cooldowns");
      font.drawString(matrix, "Cooldowns", Math.round(posX + 7.0F), Math.round(headerTextY), ThemeManager.textColor.getColor());
      float repBoxW = fontDuration.getStringWidth("0:00") + 6.0F;
      float timerCenterX = posX + maxElementsWidth - 6.5F - repBoxW / 2.0F;
      BackgroundIcons.drawHudHeaderIcon(context, BackgroundIcons.COOLDOWNS, font, "Cooldowns", headerTextY, timerCenterX, ColorAssist.getHudColor());
      posY = HudListPanel.contentStartY(posY);
      if (this.list.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
         Item item = EXAMPLE_ITEMS[this.currentItemIndex];
         String namex = "Example";
         String duration = "**:**";
         matrix.push();
         matrix.translate(posX + maxElementsWidth / 2.0F, posY + cooldownHeight / 2.0F, 0.0F);
         matrix.scale(1.0F, 1.0F, 1.0F);
         matrix.translate(-(posX + maxElementsWidth / 2.0F), -(posY + cooldownHeight / 2.0F), 0.0F);
         Render2D.defaultDrawStack(context, item.getDefaultStack(), posX + 7.0F, HudListPanel.rowIconY(posY, 9.0F), false, false, 0.5F);
         rectangle.render(
            ShapeProperties.create(matrix, posX + 18.0F, HudListPanel.rowDotY(posY), 2.0, 2.0).round(1.0F).color(new Color(87, 87, 90, 255).getRGB()).build()
         );
         float nameY = HudListPanel.alignedRowTextY(fontItemName, fontItemName, posY);
         fontItemName.drawString(matrix, namex, Math.round(posX + 22.0F), Math.round(nameY), ThemeManager.textColor.getColor());
         float durationWidth = fontDuration.getStringWidth(duration);
         float durationBoxWidth = durationWidth + 6.0F;
         float durationBoxX = posX + maxElementsWidth - 6.5F - durationBoxWidth;
         float durationBoxY = HudListPanel.rowBadgeY(posY, 8.0F);
         rectangle.render(
            ShapeProperties.create(matrix, durationBoxX, durationBoxY, durationBoxWidth, 8.0)
               .round(2.0F)
               .thickness(1.0F)
               .outlineColor(new Color(33, 33, 33, 255).getRGB())
               .color(new Color(0, 0, 0, 100).getRGB())
               .build()
         );
         TimerTextAnimator.draw(
            fontDuration,
            matrix,
            "cooldowns_example_duration",
            duration,
            Math.round(durationBoxX + durationBoxWidth / 2.0F - durationWidth / 2.0F + 0.2F),
            (int)HudListPanel.rowBadgeTextY(fontDuration, posY, 8.0F, duration),
            ThemeManager.textColor.getColor()
         );
         matrix.pop();
         posY += cooldownHeight;
      } else {
         int timerIndex = 0;

         for (CoolDowns.CoolDown coolDownxx : this.renderSnapshot) {
            float animValue = coolDownxx.anim.getOutput().floatValue();
            if (animValue > 0.0F) {
               int time = (int)(-coolDownxx.time.elapsedTime() / 1000L);
               String namex = coolDownxx.item.getDefaultStack().getName().getString();
               if (namex.equals("Зачарованное золотое яблоко")) {
                  namex = "Чарка";
               }

               String duration = StringHelper.getDuration(time);
               int alpha = (int)(255.0F * animValue);
               Color textColor = new Color(255, 255, 255, alpha);
               matrix.push();
               matrix.translate(posX + width / 2.0F, posY + cooldownHeight * animValue / 2.0F, 0.0F);
               matrix.scale(animValue, animValue, animValue);
               matrix.translate(-(posX + width / 2.0F), -(posY + cooldownHeight * animValue / 2.0F), 0.0F);
               new Color(ColorAssist.getHudColor() >> 16 & 0xFF, ColorAssist.getHudColor() >> 8 & 0xFF, ColorAssist.getHudColor() & 0xFF, alpha);
               new Color(ColorAssist.getHudColor2() >> 16 & 0xFF, ColorAssist.getHudColor2() >> 8 & 0xFF, ColorAssist.getHudColor2() & 0xFF, alpha);
               Render2D.defaultDrawStack(context, coolDownxx.item.getDefaultStack(), posX + 7.0F, HudListPanel.rowIconY(posY, 9.0F), false, false, 0.5F);
               rectangle.render(
                  ShapeProperties.create(matrix, posX + 18.0F, HudListPanel.rowDotY(posY), 2.0, 2.0)
                     .round(1.0F)
                     .color(new Color(87, 87, 90, alpha).getRGB())
                     .build()
               );
               float nameX = posX + 22.0F;
               float nameY = HudListPanel.alignedRowTextY(fontItemName, fontItemName, posY);
               fontItemName.drawString(matrix, namex, nameX, nameY, textColor.getRGB());
               float durationWidth = fontDuration.getStringWidth(duration);
               float durationBoxWidth = durationWidth + 6.0F;
               float durationBoxX = posX + width - 6.5F - durationBoxWidth;
               float durationBoxY = HudListPanel.rowBadgeY(posY, 8.0F);
               rectangle.render(
                  ShapeProperties.create(matrix, durationBoxX, durationBoxY, durationBoxWidth, 8.0F * animValue)
                     .round(2.0F)
                     .thickness(1.0F)
                     .outlineColor(new Color(33, 33, 33, alpha).getRGB())
                     .color(new Color(0, 0, 0, (int)(100.0F * animValue)).getRGB())
                     .build()
               );
               String timerKey = "cooldowns_duration_" + coolDownxx.item.getTranslationKey() + "_" + timerIndex;
               TimerTextAnimator.draw(
                  fontDuration,
                  matrix,
                  timerKey,
                  duration,
                  Math.round(durationBoxX + durationBoxWidth / 2.0F - durationWidth / 2.0F + 0.2F),
                  (int)HudListPanel.rowBadgeTextY(fontDuration, posY, 8.0F, duration),
                  textColor.getRGB()
               );
               timerIndex++;
               matrix.pop();
               posY += cooldownHeight * animValue;
            }
         }
      }

      this.setWidth((int)width);
      this.setHeight((int)f);
   }

   public record CoolDown(Item item, StopWatch time, Animation anim) {
   }
}
