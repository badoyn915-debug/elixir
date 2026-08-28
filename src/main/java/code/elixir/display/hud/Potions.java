package code.elixir.display.hud;

import code.elixir.common.animation.Animation;
import code.elixir.common.animation.Direction;
import code.elixir.common.animation.implement.Decelerate;
import code.elixir.common.animation.implement.EaseOut;
import code.elixir.events.packet.PacketEvent;
import code.elixir.features.impl.render.Hud;
import code.elixir.utils.client.managers.api.draggable.AbstractDraggable;
import code.elixir.utils.client.sound.SoundManager;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.BackgroundIcons;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.geometry.Render2D;
import code.elixir.utils.display.render.post.KawaseBlur;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.theme.ThemeManager;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Map.Entry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.RemoveEntityStatusEffectS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class Potions extends AbstractDraggable {
   private final List<Potions.Potion> list = new ArrayList<>();
   private final List<Potions.Potion> renderSnapshot = new ArrayList<>();
   private static final RegistryEntry<StatusEffect>[] NEGATIVE_EFFECTS = new RegistryEntry[]{
      StatusEffects.POISON,
      StatusEffects.WITHER,
      StatusEffects.NAUSEA,
      StatusEffects.BLINDNESS,
      StatusEffects.HUNGER,
      StatusEffects.SLOWNESS,
      StatusEffects.MINING_FATIGUE,
      StatusEffects.INSTANT_DAMAGE,
      StatusEffects.WEAKNESS,
      StatusEffects.LEVITATION,
      StatusEffects.UNLUCK,
      StatusEffects.BAD_OMEN
   };
   private long lastEffectChange = 0L;
   private RegistryEntry<StatusEffect> currentRandomEffect = StatusEffects.SPEED;
   private static List<RegistryEntry<StatusEffect>> cachedAllEffects;
   private static final Random RANDOM = new Random();
   private final FontRenderer fontMedium = Fonts.getSize(13, Fonts.Type.SuisseIntlMedium);
   private final FontRenderer fontHeader = Fonts.getSize(16, Fonts.Type.DEFAULT);
   private final FontRenderer fontLevel = Fonts.getSize(11, Fonts.Type.SuisseIntlMedium);
   private final FontRenderer fontDuration = Fonts.getSize(13, Fonts.Type.DEFAULT);
   private final FontRenderer fontPotionName = Fonts.getSize(14, Fonts.Type.SuisseIntlMedium);
   private final Map<String, StatusEffectInstance> activeEffects = new HashMap<>();
   private final Map<String, Boolean> warnedEffects = new HashMap<>();

   public Potions() {
      super("Potions", 200, 40, 90, 23, true);
      this.scaleAnimation = new EaseOut().setValue(1.0).setMs(100);
   }

   @Override
   public boolean visible() {
      Hud.getInstance();
      return Hud.interfaceSettings.isSelected("Potions") && Hud.getInstance().state
         ? !this.list.isEmpty() || PlayerInteractionHelper.isChat(mc.currentScreen)
         : false;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         FontRenderer iconFont = Fonts.getSize(15, Fonts.Type.ELIXIR_ICONS);
         float pX = this.getX() + this.getWidth() - iconFont.getStringWidth("p") - 4.0F;
         float pY = this.getY() + 7.0F;
         float pWidth = iconFont.getStringWidth("p");
         float pHeight = iconFont.getStringHeight("p");
         if (mouseX >= pX && mouseX <= pX + pWidth && mouseY >= pY && mouseY <= pY + pHeight) {
            Hud.getInstance();
            ArrayList<String> selected = new ArrayList<>(Hud.interfaceSettings.getSelected());
            selected.remove("Potions");
            Hud.getInstance();
            Hud.interfaceSettings.setSelected(selected);
            return true;
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public void tick() {
      this.list.removeIf(p -> p.anim.isFinished(Direction.BACKWARDS));
      this.list.forEach(p -> p.effect.update(mc.player, null));
      if (Hud.getInstance().notificationSettings.isSelected("Р—Р°РєР°РЅС‡РёРІР°СЋС‰РµРјСЃСЏ Р·РµР»СЊРµ") && !PlayerInteractionHelper.nullCheck()) {
         ArrayList<String> effectsToRemove = new ArrayList<>();

         for (Entry<String, StatusEffectInstance> entry : this.activeEffects.entrySet()) {
            String effectKey = entry.getKey();
            StatusEffectInstance trackedEffect = entry.getValue();
            boolean stillActive = false;
            if (mc.player != null) {
               for (StatusEffectInstance activeEffect : mc.player.getStatusEffects()) {
                  if (activeEffect.getEffectType().getIdAsString().equals(trackedEffect.getEffectType().getIdAsString())) {
                     stillActive = true;
                     this.activeEffects.put(effectKey, activeEffect);
                     int remainingTicks = activeEffect.getDuration();
                     int warningTicks = 200;
                     boolean alreadyWarned = this.warnedEffects.getOrDefault(effectKey, false);
                     if (remainingTicks <= warningTicks && remainingTicks > 0 && !alreadyWarned) {
                        this.sendPotionEndingSoonNotification(activeEffect);
                        this.warnedEffects.put(effectKey, true);
                     }

                     if (remainingTicks > warningTicks) {
                        this.warnedEffects.remove(effectKey);
                     }
                     break;
                  }
               }
            }

            if (!stillActive) {
               this.sendPotionEndedNotification(trackedEffect);
               effectsToRemove.add(effectKey);
               this.warnedEffects.remove(effectKey);
            }
         }

         effectsToRemove.forEach(this.activeEffects::remove);
      }

      long currentTime;
      if (this.list.isEmpty()
         && PlayerInteractionHelper.isChat(mc.currentScreen)
         && (currentTime = System.currentTimeMillis()) - this.lastEffectChange >= 1000L) {
         if (cachedAllEffects == null) {
            cachedAllEffects = new ArrayList<>();

            for (Identifier id : Registries.STATUS_EFFECT.getIds()) {
               Registries.STATUS_EFFECT.getEntry(id).ifPresent(cachedAllEffects::add);
            }
         }

         if (!cachedAllEffects.isEmpty()) {
            this.currentRandomEffect = cachedAllEffects.get(RANDOM.nextInt(cachedAllEffects.size()));
            this.lastEffectChange = currentTime;
         }
      }
   }

   @Override
   public void packet(PacketEvent e) {
      Packet<?> packet = e.getPacket();
      if (packet instanceof EntityStatusEffectS2CPacket effect) {
         if (!PlayerInteractionHelper.nullCheck() && effect.getEntityId() == Objects.requireNonNull(mc.player).getId()) {
            RegistryEntry effectId = effect.getEffectId();
            String effectKey = effectId.getIdAsString();
            boolean isNewEffect = !this.activeEffects.containsKey(effectKey);
            this.list.stream().filter(p -> p.effect.getEffectType().getIdAsString().equals(effectKey)).forEach(s -> s.anim.setDirection(Direction.BACKWARDS));
            StatusEffectInstance newEffect = new StatusEffectInstance(
               effectId, effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.shouldShowParticles(), effect.shouldShowIcon()
            );
            this.list.add(new Potions.Potion(newEffect, new Decelerate().setMs(150).setValue(1.0)));
            this.activeEffects.put(effectKey, newEffect);
            if (isNewEffect && Hud.getInstance().notificationSettings.isSelected("РџРѕР»СѓС‡РµРЅРЅРѕРј Р·РµР»СЊРµ")) {
               this.sendPotionReceivedNotification(newEffect);
            }
         }
      } else if (packet instanceof RemoveEntityStatusEffectS2CPacket effectx) {
         String effectKey = effectx.effect().getIdAsString();
         this.list.stream().filter(s -> s.effect.getEffectType().getIdAsString().equals(effectKey)).forEach(s -> s.anim.setDirection(Direction.BACKWARDS));
         StatusEffectInstance removedEffect = this.activeEffects.remove(effectKey);
         if (removedEffect != null && Hud.getInstance().notificationSettings.isSelected("Р—Р°РєР°РЅС‡РёРІР°СЋС‰РµРјСЃСЏ Р·РµР»СЊРµ")) {
            this.sendPotionEndedNotification(removedEffect);
         }

         this.warnedEffects.remove(effectKey);
      } else if (packet instanceof PlayerRespawnS2CPacket) {
         this.list.clear();
         this.activeEffects.clear();
         this.warnedEffects.clear();
      } else {
         if (packet instanceof GameJoinS2CPacket) {
            this.list.clear();
            this.activeEffects.clear();
            this.warnedEffects.clear();
         }
      }
   }

   @Override
   public void drawDraggable(DrawContext context) {
      this.drawMode1(context);
   }

   private void drawMode1(DrawContext context) {
      MatrixStack matrix = context.getMatrices();
      this.renderSnapshot.clear();
      this.renderSnapshot.addAll(this.list);
      FontRenderer fontMedium = this.fontMedium;
      FontRenderer font = this.fontHeader;
      FontRenderer fontLevel = this.fontLevel;
      FontRenderer fontDuration = this.fontDuration;
      FontRenderer fontPotionName = this.fontPotionName;
      float posX = this.getX();
      float posY = this.getY();
      float maxElementsWidth = HudListPanel.headerMinWidth(font, "Potions");
      float headerHeight = 21.0F;
      float potionHeight = 11.0F;
      boolean isFound = false;

      for (Potions.Potion potion : this.renderSnapshot) {
         if (potion.anim.getOutput().floatValue() > 0.0F) {
            isFound = true;
            StatusEffectInstance effect = potion.effect;
            String name = ((StatusEffect)effect.getEffectType().value()).getName().getString();
            String duration = this.getDuration(effect);
            int amplifier = effect.getAmplifier();
            float levelWidth = 0.0F;
            if (amplifier > 0) {
               levelWidth = fontLevel.getStringWidth(" LVL " + (amplifier + 1)) + 3.0F;
            }

            float elementsWidth;
            if ((elementsWidth = HudListPanel.rowWidth(fontPotionName, name, levelWidth, fontDuration, duration)) > maxElementsWidth) {
               maxElementsWidth = elementsWidth;
            }
         }
      }

      if (this.list.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
         isFound = true;
         float elementsWidth = HudListPanel.rowWidth(fontPotionName, "Example", 0.0F, fontDuration, "**:**");
         if (elementsWidth > maxElementsWidth) {
            maxElementsWidth = elementsWidth;
         }
      }

      float totalContentHeight = 0.0F;

      for (Potions.Potion potionx : this.renderSnapshot) {
         float animValue = potionx.anim.getOutput().floatValue();
         if (animValue > 0.0F) {
            totalContentHeight += potionHeight * animValue;
         }
      }

      if (this.list.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
         totalContentHeight += potionHeight;
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
      float headerTextY = HudListPanel.headerTextY(font, posY, "Potions");
      font.drawString(matrix, "Potions", Math.round(posX + 7.0F), Math.round(headerTextY), ThemeManager.textColor.getColor());
      float repBoxW = fontDuration.getStringWidth("0:00") + 6.0F;
      float timerCenterX = posX + maxElementsWidth - 6.5F - repBoxW / 2.0F;
      BackgroundIcons.drawHudHeaderIcon(context, BackgroundIcons.POTIONS, font, "Potions", headerTextY, timerCenterX, ColorAssist.getHudColor());
      posY = HudListPanel.contentStartY(posY);
      if (this.list.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
         String namex = "Example";
         String durationx = "**:**";
         matrix.push();
         matrix.translate(posX + maxElementsWidth / 2.0F, posY + potionHeight / 2.0F, 0.0F);
         matrix.scale(1.0F, 1.0F, 1.0F);
         matrix.translate(-(posX + maxElementsWidth / 2.0F), -(posY + potionHeight / 2.0F), 0.0F);
         Render2D.drawSprite(
            matrix,
            mc.getStatusEffectSpriteManager().getSprite(this.currentRandomEffect),
            posX + 7.0F,
            (int)HudListPanel.rowIconY(posY, 9.0F),
            9.0F,
            9,
            ThemeManager.textColor.getColor()
         );
         rectangle.render(
            ShapeProperties.create(matrix, posX + 17.1F, HudListPanel.rowDotY(posY), 2.0, 2.0).round(1.0F).color(new Color(87, 87, 90, 255).getRGB()).build()
         );
         float nameY = HudListPanel.alignedRowTextY(fontPotionName, fontPotionName, posY, namex);
         fontPotionName.drawString(matrix, namex, Math.round(posX + 22.0F), Math.round(nameY), ThemeManager.textColor.getColor());
         float durationWidth = fontDuration.getStringWidth(durationx);
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
            "potions_example_duration",
            durationx,
            Math.round(durationBoxX + durationBoxWidth / 2.0F - durationWidth / 2.0F + 0.2F),
            (int)HudListPanel.rowBadgeTextY(fontDuration, posY, 8.0F, durationx),
            ThemeManager.textColor.getColor()
         );
         matrix.pop();
         posY += potionHeight;
      } else {
         int timerIndex = 0;

         for (Potions.Potion potionxx : this.renderSnapshot) {
            float animValue = potionxx.anim.getOutput().floatValue();
            if (animValue > 0.0F) {
               StatusEffectInstance effectx = potionxx.effect;
               String namex = ((StatusEffect)effectx.getEffectType().value()).getName().getString();
               String durationx = this.getDuration(effectx);
               int amplifierx = effectx.getAmplifier();
               boolean isBadEffect = this.isBadEffect(effectx.getEffectType());
               int textAlpha = 255;
               if (effectx.getDuration() <= 200 && effectx.getDuration() > 0) {
                  double output = 0.5 + 0.5 * Math.cos((Math.PI * 2) * (System.currentTimeMillis() % 700L) / 700.0);
                  textAlpha = (int)(100.0 + 155.0 * output);
               } else if (effectx.getDuration() == 0) {
                  textAlpha = 0;
               }

               int alpha = (int)(textAlpha * animValue);
               boolean isLightTheme = ThemeManager.BackgroundGui.getColor() == Color.WHITE.getRGB()
                  || (ThemeManager.BackgroundGui.getColor() >> 16 & 0xFF) > 200;
               Color textColor = isBadEffect ? new Color(255, 85, 75, alpha) : (isLightTheme ? new Color(0, 0, 0, alpha) : new Color(255, 255, 255, alpha));
               int iconColor = ColorAssist.rgba(255, 255, 255, alpha);
               matrix.push();
               matrix.translate(posX + width / 2.0F, posY + potionHeight * animValue / 2.0F, 0.0F);
               matrix.scale(animValue, animValue, animValue);
               matrix.translate(-(posX + width / 2.0F), -(posY + potionHeight * animValue / 2.0F), 0.0F);
               new Color(ColorAssist.getHudColor() >> 16 & 0xFF, ColorAssist.getHudColor() >> 8 & 0xFF, ColorAssist.getHudColor() & 0xFF, alpha);
               new Color(ColorAssist.getHudColor2() >> 16 & 0xFF, ColorAssist.getHudColor2() >> 8 & 0xFF, ColorAssist.getHudColor2() & 0xFF, alpha);
               Render2D.drawSprite(
                  matrix,
                  mc.getStatusEffectSpriteManager().getSprite(effectx.getEffectType()),
                  posX + 7.0F,
                  (int)HudListPanel.rowIconY(posY, 9.0F),
                  9.0F,
                  9,
                  iconColor
               );
               rectangle.render(
                  ShapeProperties.create(matrix, posX + 17.1F, HudListPanel.rowDotY(posY), 2.0, 2.0)
                     .round(1.0F)
                     .color(new Color(87, 87, 90, alpha).getRGB())
                     .build()
               );
               float nameX = posX + 22.0F;
               float nameY = HudListPanel.alignedRowTextY(fontPotionName, fontPotionName, posY, namex);
               fontPotionName.drawString(matrix, namex, nameX, nameY, textColor.getRGB());
               if (amplifierx > 0) {
                  String level = " LVL " + (amplifierx + 1);
                  float levelX = Math.round(posX + 22.0F + fontPotionName.getStringWidth(namex) + 3.0F);
                  Color levelColor = new Color(157, 157, 160, alpha);
                  float levelY = HudListPanel.alignedRowTextY(fontPotionName, fontLevel, posY, namex);
                  fontLevel.drawString(matrix, level, levelX, Math.round(levelY), levelColor.getRGB());
               }

               float durationWidth = fontDuration.getStringWidth(durationx);
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
               String timerKey = "potions_duration_" + effectx.getEffectType().getIdAsString() + "_" + effectx.getAmplifier() + "_" + timerIndex;
               TimerTextAnimator.draw(
                  fontDuration,
                  matrix,
                  timerKey,
                  durationx,
                  Math.round(durationBoxX + durationBoxWidth / 2.0F - durationWidth / 2.0F + 0.2F),
                  (int)HudListPanel.rowBadgeTextY(fontDuration, posY, 8.0F, durationx),
                  textColor.getRGB()
               );
               timerIndex++;
               matrix.pop();
               posY += potionHeight * animValue;
            }
         }
      }

      this.setWidth((int)width);
      this.setHeight((int)f);
   }

   private String getDuration(StatusEffectInstance pe) {
      int var1 = pe.getDuration();
      int mins = var1 / 1200;
      return !pe.isInfinite() && mins <= 60 ? mins + ":" + String.format("%02d", var1 % 1200 / 20) : "**:**";
   }

   private boolean isBadEffect(RegistryEntry<StatusEffect> effect) {
      for (RegistryEntry<StatusEffect> negativeEffect : NEGATIVE_EFFECTS) {
         if (effect == negativeEffect) {
            return true;
         }
      }

      return false;
   }

   private void sendPotionReceivedNotification(StatusEffectInstance effect) {
      String effectName = ((StatusEffect)effect.getEffectType().value()).getName().getString();
      String message = effectName + " РїРѕР»СѓС‡РµРЅ!";
      Notifications.getInstance().addList(Text.literal(message), 1000, SoundManager.ENABLE_MODULE, false, null);
      SoundManager.playSound(SoundManager.ENABLE_MODULE);
   }

   private void sendPotionEndedNotification(StatusEffectInstance effect) {
      String effectName = ((StatusEffect)effect.getEffectType().value()).getName().getString();
      String message = effectName + " Р·Р°РєРѕРЅС‡РёР»СЃСЏ!";
      Notifications.getInstance().addList(Text.literal(message), 1000, SoundManager.DISABLE_MODULE, false, null);
      SoundManager.playSound(SoundManager.DISABLE_MODULE);
   }

   private void sendPotionEndingSoonNotification(StatusEffectInstance effect) {
      String effectName = ((StatusEffect)effect.getEffectType().value()).getName().getString();
      String message = effectName + " Р·Р°РєРѕРЅС‡РёС‚СЃСЏ С‡РµСЂРµР· 10 СЃРµРєСѓРЅРґ!";
      Notifications.getInstance().addList(Text.literal(message), 1000, null, false, null);
   }

   private void drawMode2(DrawContext context) {
      MatrixStack matrix = context.getMatrices();
      this.renderSnapshot.clear();
      this.renderSnapshot.addAll(this.list);
      FontRenderer titleFont = Fonts.getSize(14, Fonts.Type.BOLD);
      FontRenderer moduleFont = Fonts.getSize(13, Fonts.Type.BOLD);
      FontRenderer levelFont = Fonts.getSize(11, Fonts.Type.BOLD);
      FontRenderer durationFont = Fonts.getSize(12, Fonts.Type.BOLD);
      float posX = this.getX();
      float posY = this.getY();
      float panelWidth = 95.0F;
      float titleHeight = 24.0F;
      float potionHeight = 16.0F;
      float spacing = -3.5F;
      float titleSpacing = -1.5F;
      int potionCount = 0;

      for (Potions.Potion potion : this.renderSnapshot) {
         if (potion.anim.getOutput().floatValue() > 0.0F) {
            potionCount++;
         }
      }

      boolean showExamples = this.list.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen);
      if (showExamples) {
         potionCount = 3;
      }

      float contentHeight = potionCount * (potionHeight + spacing) - (potionCount > 0 ? spacing : 0.0F);
      float totalHeight = titleHeight + titleSpacing + 7.0F + contentHeight;
      this.setWidth((int)panelWidth);
      this.setHeight((int)totalHeight);
      rectangle.render(ShapeProperties.create(matrix, posX, posY, panelWidth, totalHeight).round(7.0F).color(ThemeManager.BackgroundGui.getColor()).build());
      rectangle.render(
         ShapeProperties.create(matrix, posX + 4.5F, posY + 4.5F, panelWidth - 9.0F, titleHeight - 9.0F)
            .round(3.5F)
            .color(ThemeManager.BackgroundSettings.getColor())
            .build()
      );
      titleFont.drawString(matrix, "Potions", posX + 10.0F, posY + 10.0F, ThemeManager.textColor.getColor());
      Fonts.getSize(19, Fonts.Type.ICONS).drawString(matrix, "D", posX + panelWidth - 19.0F, posY + 9.5F, ThemeManager.textColor.getColor());
      float currentY = posY + titleHeight + titleSpacing;
      rectangle.render(
         ShapeProperties.create(matrix, posX + 4.5F, currentY, panelWidth - 9.0F, contentHeight)
            .round(3.5F)
            .color(ThemeManager.BackgroundSettings.getColor())
            .build()
      );
      if (showExamples) {
         String[] exampleNames = new String[]{"Speed", "Strength", "Regen"};
         String[] exampleDurations = new String[]{"5:00", "3:30", "10:00"};

         for (int i = 0; i < 3; i++) {
            this.drawPotionEntry(
               matrix,
               currentY,
               posX,
               potionHeight,
               moduleFont,
               levelFont,
               durationFont,
               exampleNames[i],
               exampleDurations[i],
               0,
               1.0F,
               false,
               this.currentRandomEffect
            );
            currentY += potionHeight + spacing;
         }
      } else {
         for (Potions.Potion potionx : this.renderSnapshot) {
            float animValue = potionx.anim.getOutput().floatValue();
            if (animValue > 0.0F) {
               StatusEffectInstance effect = potionx.effect;
               String name = ((StatusEffect)effect.getEffectType().value()).getName().getString();
               String duration = this.getDuration(effect);
               int amplifier = effect.getAmplifier();
               boolean isBad = this.isBadEffect(effect.getEffectType());
               this.drawPotionEntry(
                  matrix,
                  currentY,
                  posX,
                  potionHeight,
                  moduleFont,
                  levelFont,
                  durationFont,
                  name,
                  duration,
                  amplifier,
                  animValue,
                  isBad,
                  effect.getEffectType()
               );
               currentY += (potionHeight + spacing) * animValue;
            }
         }
      }
   }

   private void drawPotionEntry(
      MatrixStack matrix,
      float currentY,
      float posX,
      float potionHeight,
      FontRenderer moduleFont,
      FontRenderer levelFont,
      FontRenderer durationFont,
      String name,
      String duration,
      int amplifier,
      float animValue,
      boolean isBad,
      RegistryEntry<StatusEffect> effectType
   ) {
      float barWidth = 1.3F;
      float barHeight = 7.0F;
      float barX = posX + 4.5F + 2.8F;
      float barY = currentY + 5.5F;
      int alpha = (int)(255.0F * animValue);
      Color barColor1 = new Color(ColorAssist.getHudColor() >> 16 & 0xFF, ColorAssist.getHudColor() >> 8 & 0xFF, ColorAssist.getHudColor() & 0xFF, alpha);
      Color barColor2 = new Color(ColorAssist.getHudColor2() >> 16 & 0xFF, ColorAssist.getHudColor2() >> 8 & 0xFF, ColorAssist.getHudColor2() & 0xFF, alpha);
      rectangle.render(
         ShapeProperties.create(matrix, barX, barY, barWidth, barHeight * animValue)
            .round(0.9F)
            .color(barColor1.getRGB(), barColor2.getRGB(), barColor1.getRGB(), barColor2.getRGB())
            .build()
      );
      float iconX = barX + barWidth + 3.5F;
      float iconY = currentY + potionHeight - 11.0F;
      Render2D.drawSprite(matrix, mc.getStatusEffectSpriteManager().getSprite(effectType), iconX, iconY, 9.0F, 9, ColorAssist.rgba(255, 255, 255, alpha));
      float nameX = iconX + 11.0F;
      float nameY = currentY + potionHeight - 9.0F;
      Color textColor = isBad
         ? new Color(255, 85, 75, alpha)
         : new Color(
            ThemeManager.textColor.getColor() >> 16 & 0xFF, ThemeManager.textColor.getColor() >> 8 & 0xFF, ThemeManager.textColor.getColor() & 0xFF, alpha
         );
      moduleFont.drawString(matrix, name, nameX, nameY, textColor.getRGB());
      if (amplifier > 0) {
         String level = " LVL " + (amplifier + 1);
         float levelX = nameX + moduleFont.getStringWidth(name) + 3.0F;
         Color levelColor = new Color(157, 157, 160, alpha);
         float levelY = HudListPanel.alignedRowTextY(moduleFont, levelFont, currentY);
         levelFont.drawString(matrix, level, levelX, levelY, levelColor.getRGB());
      }

      float durationWidth = durationFont.getStringWidth(duration);
      float durationX = posX + 95.0F - 6.5F - durationWidth;
      Color durationColor = new Color(150, 150, 150, alpha);
      durationFont.drawString(matrix, duration, durationX, nameY, durationColor.getRGB());
   }

   private record Potion(StatusEffectInstance effect, Animation anim) {
   }
}
