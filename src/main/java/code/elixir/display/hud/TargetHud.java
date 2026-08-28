package code.elixir.display.hud;

import code.elixir.Elixir;
import code.elixir.common.animation.Animation;
import code.elixir.common.animation.Direction;
import code.elixir.common.animation.implement.Decelerate;
import code.elixir.common.animation.implement.EaseOut;
import code.elixir.features.impl.combat.AttackAura;
import code.elixir.features.impl.combat.ProjectHelper;
import code.elixir.features.impl.render.Hud;
import code.elixir.utils.client.managers.api.draggable.AbstractDraggable;
import code.elixir.utils.client.packet.network.ServerUtil;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.HudShine;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.geometry.Render2D;
import code.elixir.utils.display.render.post.KawaseBlur;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.display.render.shape.implement.Rectangle;
import code.elixir.utils.display.scissor.ScissorAssist;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.math.time.StopWatch;
import code.elixir.utils.theme.ThemeManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;

public class TargetHud extends AbstractDraggable {
   private static TargetHud instance;
   private final Animation healthAnimation;
   private final Animation absorptionAnimation;
   private final Animation toggleAnimation;
   private final Animation faceAlphaAnimation;
   private final Animation heartColorAnimation;
   private final StopWatch stopWatch = new StopWatch();
   private LivingEntity target;
   private Identifier cachedFaceTexture;
   private float health;
   private float absorption;
   private float smoothHealthFraction = 0.0F;
   private float smoothAbsorptionValue = 0.0F;
   private boolean showOnHover = false;
   private final FontRenderer nameFont = Fonts.getSize(16, Fonts.Type.SuisseIntlMedium);
   private final FontRenderer hpFont = Fonts.getSize(15, Fonts.Type.SuisseIntlMedium);
   private final FontRenderer iconFont = Fonts.getSize(16, Fonts.Type.ELIXIR_ICONS);
   private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private static final EquipmentSlot[] HAND_SLOTS = new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

   public static TargetHud getInstance() {
      if (instance == null) {
         instance = new TargetHud();
      }

      return instance;
   }

   public TargetHud() {
      super("Target Hud", 10, 120, 100, 32, true);
      instance = this;
      this.scaleAnimation = new EaseOut().setValue(1.0).setMs(150);
      this.healthAnimation = new Decelerate().setMs(250).setValue(1.0);
      this.absorptionAnimation = new Decelerate().setMs(250).setValue(1.0);
      this.toggleAnimation = new Decelerate().setMs(250).setValue(1.0);
      this.faceAlphaAnimation = new Decelerate().setMs(125).setValue(1.0);
      this.heartColorAnimation = new Decelerate().setMs(1500).setValue(0.0);
   }

   public boolean isShowOnHover() {
      return this.showOnHover;
   }

   public void setShowOnHover(boolean showOnHover) {
      this.showOnHover = showOnHover;
   }

   @Override
   public boolean canDraw(Hud hud, AbstractDraggable draggable) {
      if (!hud.isState()) {
         return false;
      } else {
         return !Hud.interfaceSettings.isSelected("Target Hud") ? false : this.visible();
      }
   }

   @Override
   public boolean visible() {
      if (Hud.getInstance() == null) {
         return false;
      } else {
         Hud.getInstance();
         boolean isActive = Hud.interfaceSettings.isSelected("Target Hud") && Hud.getInstance().state;
         return isActive && this.toggleAnimation.getOutput().floatValue() > 0.01F;
      }
   }

   @Override
   public void tick() {
      Hud.getInstance();
      if (Hud.interfaceSettings.isSelected("Target Hud") && Hud.getInstance().state) {
         if (this.heartColorAnimation.isFinished(Direction.FORWARDS)) {
            this.heartColorAnimation.setDirection(Direction.BACKWARDS);
         } else if (this.heartColorAnimation.isFinished(Direction.BACKWARDS)) {
            this.heartColorAnimation.setDirection(Direction.FORWARDS);
         }

         LivingEntity nextTarget = this.resolveTarget();
         if (nextTarget != null) {
            if (this.target != nextTarget) {
               this.resetHealthState(nextTarget);
            }

            this.target = nextTarget;
            this.stopWatch.reset();
            this.startAnimation();
            this.toggleAnimation.setDirection(Direction.FORWARDS);
            this.faceAlphaAnimation.setDirection(Direction.FORWARDS);
         } else {
            this.toggleAnimation.setDirection(Direction.BACKWARDS);
            this.faceAlphaAnimation.setDirection(Direction.BACKWARDS);
            if (this.toggleAnimation.isFinished(Direction.BACKWARDS)) {
               this.target = null;
               this.cachedFaceTexture = null;
            }
         }

         if (this.target != null) {
            this.updateCachedFaceTexture();
         }
      }
   }

   private LivingEntity resolveTarget() {
      LivingEntity auraTarget = AttackAura.getInstance().getTarget();
      if (auraTarget == null) {
         ProjectHelper projectHelper = ProjectHelper.getInstance();
         auraTarget = projectHelper != null ? projectHelper.getHudTarget() : null;
      }

      if (auraTarget != null) {
         return auraTarget;
      } else {
         LivingEntity lookedAt;
         if (this.showOnHover && (lookedAt = this.getLookedAtEntity()) != null) {
            return lookedAt;
         } else {
            return PlayerInteractionHelper.isChat(mc.currentScreen) && mc.player != null ? mc.player : null;
         }
      }
   }

   private LivingEntity getLookedAtEntity() {
      if (mc != null && mc.crosshairTarget != null) {
         HitResult class_2392 = mc.crosshairTarget;
         LivingEntity living;
         EntityHitResult hit;
         return class_2392 instanceof EntityHitResult
               && (hit = (EntityHitResult)class_2392).getEntity() instanceof LivingEntity
               && (living = (LivingEntity)hit.getEntity()) != mc.player
               && living.isAlive()
            ? living
            : null;
      } else {
         return null;
      }
   }

   private void resetHealthState(LivingEntity entity) {
      float maxHp = ServerUtil.getMaxHealth(entity);
      float actualHp = ServerUtil.getHealthValue(entity);
      this.smoothHealthFraction = maxHp > 0.0F ? MathHelper.clamp(actualHp / maxHp, 0.0F, 1.0F) : 0.0F;
      this.smoothAbsorptionValue = MathHelper.clamp(entity.getAbsorptionAmount(), 0.0F, 7.1F);
   }

   private void updateHealthAnimations(LivingEntity entity) {
      float maxHp = ServerUtil.getMaxHealth(entity);
      if (!(maxHp <= 0.0F)) {
         float targetFraction = entity.isAlive() ? MathHelper.clamp(ServerUtil.getHealthValue(entity) / maxHp, 0.0F, 1.0F) : 0.0F;
         this.smoothHealthFraction = Calculate.interpolateSmooth(8.0, this.smoothHealthFraction, targetFraction);
         float targetAbsorption = MathHelper.clamp(entity.getAbsorptionAmount(), 0.0F, 7.1F);
         this.smoothAbsorptionValue = Calculate.interpolateSmooth(8.0, this.smoothAbsorptionValue, targetAbsorption);
      }
   }

   private void updateCachedFaceTexture() {
      LivingEntity entity = this.target;
      if (entity != null) {
         if (mc.getEntityRenderDispatcher().getRenderer(entity) instanceof LivingEntityRenderer renderer) {
            LivingEntityRenderState state = (LivingEntityRenderState)renderer.getAndUpdateRenderState(entity, tickCounter.getTickDelta(false));
            Identifier texture;
            if (state != null && (texture = renderer.getTexture(state)) != null) {
               this.cachedFaceTexture = texture;
            }
         }
      }
   }

   @Override
   public void drawDraggable(DrawContext context) {
      Hud.getInstance();
      float alpha;
      if (Hud.interfaceSettings.isSelected("Target Hud")
         && Hud.getInstance().state
         && (alpha = this.toggleAnimation.getOutput().floatValue()) > 0.01F
         && this.target != null) {
         MatrixStack matrix = context.getMatrices();
         this.renderTargetHud(context, matrix, this.target, alpha);
      }
   }

   private void renderTargetHud(DrawContext context, MatrixStack matrix, LivingEntity target, float animation) {
      this.updateHealthAnimations(target);
      RenderSystem.disableDepthTest();
      matrix.push();
      float posX = this.getX();
      float posY = this.getY();
      float width = 100.0F;
      float height = 36.0F;
      float visualWidth = 89.0F;
      Hud.getInstance();
      if (Hud.blur.isValue() && !KawaseBlur.INSTANCE.fbos.isEmpty()) {
         Render2D.rectangleWithMask(
            matrix.peek().getPositionMatrix(), posX, posY, visualWidth, height, 8.0F, 0, KawaseBlur.INSTANCE.fbos.getFirst().getColorAttachment()
         );
      }

      rectangle.render(
         ShapeProperties.create(matrix, posX, posY, visualWidth, height)
            .round(8.0F)
            .softness(2.5F)
            .color(ColorAssist.multAlpha(new Color(0, 0, 0, 210).getRGB(), animation))
            .build()
      );
      float maxHp = ServerUtil.getMaxHealth(target);
      float displayHp = this.smoothHealthFraction * maxHp;
      this.health = this.smoothHealthFraction * 100.0F;
      float rawAbsorption = MathHelper.clamp(target.getAbsorptionAmount(), 0.0F, 7.1F);
      boolean hasAbsorptionEffect = target.hasStatusEffect(StatusEffects.ABSORPTION) && rawAbsorption > 0.0F;
      float targetAbsorptionPercent = hasAbsorptionEffect && maxHp > 0.0F ? MathHelper.clamp(this.smoothAbsorptionValue / maxHp * 100.0F, 0.0F, 100.0F) : 0.0F;
      this.absorption = MathHelper.clamp(targetAbsorptionPercent, 0.0F, 100.0F);
      float headX = posX + 9.0F;
      float headY = posY + 3.6F;
      float headSize = 20.0F;
      float contentX = headX + headSize - 0.6F;
      float contentY = posY + 5.0F;
      FontRenderer nameFont = this.nameFont;
      String targetName = target.getNameForScoreboard();
      float maxNameWidth = visualWidth - (contentX - posX) - 4.0F;
      float barPadding = 4.0F;
      float barX = posX + barPadding;
      float barY = posY + height - 7.5F;
      float barWidth = visualWidth - barPadding * 2.0F;
      float barHeight = 3.5F;
      rectangle.render(
         ShapeProperties.create(matrix, barX, barY, barWidth, barHeight)
            .round(1.75F)
            .color(ColorAssist.multAlpha(ThemeManager.BackgroundSettings.getColor(), animation))
            .build()
      );
      float healthWidth = this.health / 100.0F * barWidth;
      if (healthWidth > 1.0F) {
         Color gradColor1 = new Color(
            ColorAssist.getHudColor() >> 16 & 0xFF, ColorAssist.getHudColor() >> 8 & 0xFF, ColorAssist.getHudColor() & 0xFF, (int)(255.0F * animation)
         );
         Color gradColor2 = new Color(
            ColorAssist.getHudColor2() >> 16 & 0xFF, ColorAssist.getHudColor2() >> 8 & 0xFF, ColorAssist.getHudColor2() & 0xFF, (int)(255.0F * animation)
         );
         Color gradColor2Dark = new Color(
            Math.max(0, (ColorAssist.getHudColor2() >> 16 & 0xFF) * 8 / 10),
            Math.max(0, (ColorAssist.getHudColor2() >> 8 & 0xFF) * 8 / 10),
            Math.max(0, (ColorAssist.getHudColor2() & 0xFF) * 8 / 10),
            (int)(255.0F * animation)
         );
         rectangle.render(
            ShapeProperties.create(matrix, barX, barY, healthWidth, barHeight)
               .round(1.75F)
               .color(gradColor2Dark.getRGB(), gradColor2.getRGB(), gradColor2.getRGB(), gradColor1.getRGB())
               .build()
         );
      }

      if (hasAbsorptionEffect && this.absorption > 0.01F) {
         float absorptionWidth = this.absorption / 100.0F * barWidth;
         if (absorptionWidth > 1.0F) {
            float absorptionStartX = barX + healthWidth;
            float availableRight = Math.max(0.0F, barWidth - healthWidth);
            float drawWidth = Math.min(absorptionWidth, availableRight);
            if (drawWidth <= 1.0F && healthWidth > 1.0F) {
               drawWidth = Math.min(absorptionWidth, healthWidth);
               absorptionStartX = barX + Math.max(0.0F, healthWidth - drawWidth);
            }

            if (drawWidth > 1.0F) {
               rectangle.render(
                  ShapeProperties.create(matrix, absorptionStartX, barY, drawWidth, barHeight)
                     .round(1.75F)
                     .color(
                        new Color(255, 215, 0, (int)(200.0F * animation)).getRGB(),
                        new Color(255, 215, 0, (int)(200.0F * animation)).getRGB(),
                        new Color(255, 165, 0, (int)(200.0F * animation)).getRGB(),
                        new Color(255, 165, 0, (int)(200.0F * animation)).getRGB()
                     )
                     .build()
               );
            }
         }
      } else {
         this.smoothAbsorptionValue = 0.0F;
         this.absorption = 0.0F;
      }

      Rectangle.flushPending();
      ScissorAssist scissorManager = Elixir.getInstance().getScissorManager();
      scissorManager.push(matrix.peek().getPositionMatrix(), contentX, contentY - 1.0F, maxNameWidth, 18.0F);
      float nameCharSpacing = -0.5F;
      nameFont.drawStringWithSpacing(
         matrix, targetName, contentX, contentY + 0.5F, ColorAssist.multAlpha(ThemeManager.textColor.getColor(), animation), nameCharSpacing
      );
      scissorManager.pop();
      String hpValue = String.format(Locale.ENGLISH, "%.0f", displayHp);
      FontRenderer hpFont = this.hpFont;
      FontRenderer iconFont = this.iconFont;
      float hpTextX = Math.round(contentX) - 0.1F;
      float textY = contentY + 11.5F;
      float hpTextH = hpFont.getStringHeight("0");
      float iconH = iconFont.getStringHeight("r");
      float iconY = textY + (hpTextH - iconH) / 2.0F + 0.2F;
      iconFont.drawRainbowString(
         matrix, "r", hpTextX, iconY, ColorAssist.multAlpha(ColorAssist.getHudColor(), animation), ColorAssist.multAlpha(ColorAssist.getHudColor2(), animation)
      );
      float iconWidth = iconFont.getStringWidth("r");
      float currentX = Math.round(hpTextX + iconWidth + 0.2F);
      int hpColor = ColorAssist.multAlpha(ThemeManager.textColor.getColor(), animation);
      hpFont.drawString(matrix, hpValue, currentX, textY, hpColor);
      currentX += hpFont.getStringWidth(hpValue);
      if (rawAbsorption > 0.0F) {
         String absValueNum = String.format(Locale.ENGLISH, "%.0f", this.smoothAbsorptionValue);
         String absTextPart = " (" + absValueNum + ")";
         int absColor = ColorAssist.multAlpha(new Color(255, 215, 0).getRGB(), animation);
         hpFont.drawString(matrix, absTextPart, currentX, textY, absColor);
      }

      this.drawFace(context, matrix, animation);
      float equipmentGap = 1.5F;
      float equipmentY = posY + height + equipmentGap;
      float equipmentHeight = this.renderEquipmentRow(context, matrix, target, posX, equipmentY, visualWidth, animation);
      float totalHeight = height + equipmentGap + equipmentHeight;
      this.setWidth((int)width);
      this.setHeight((int)Math.ceil(totalHeight));
      matrix.pop();
      RenderSystem.enableDepthTest();
   }

   private float renderEquipmentRow(
      DrawContext context, MatrixStack matrix, LivingEntity target, float anchorX, float panelY, float hudVisualWidth, float animation
   ) {
      float panelsGap = 8.0F;
      float armorWidth = this.measureEquipmentPanelWidth(ARMOR_SLOTS.length);
      float handsWidth = this.measureEquipmentPanelWidth(HAND_SLOTS.length);
      float totalWidth = armorWidth + panelsGap + handsWidth;
      float rowX = anchorX + (hudVisualWidth - totalWidth) / 2.0F;
      float panelHeight = this.renderEquipmentSlotsPanel(context, matrix, target, ARMOR_SLOTS, rowX, panelY, animation);
      this.renderEquipmentSlotsPanel(context, matrix, target, HAND_SLOTS, rowX + armorWidth + panelsGap, panelY, animation);
      return panelHeight;
   }

   private float measureEquipmentPanelWidth(int slotCount) {
      float slotSize = 12.0F;
      float spacing = 1.5F;
      float padding = 2.0F;
      return slotCount * slotSize + (slotCount - 1) * spacing + padding * 2.0F;
   }

   private float renderEquipmentSlotsPanel(
      DrawContext context, MatrixStack matrix, LivingEntity target, EquipmentSlot[] slots, float panelX, float panelY, float animation
   ) {
      float slotSize = 12.0F;
      float spacing = 1.5F;
      float padding = 2.0F;
      float durabilityBarHeight = 1.0F;
      float barSpacing = 1.0F;
      float itemScale = 0.7F;
      float panelRound = 4.0F;
      float contentWidth = slots.length * slotSize + (slots.length - 1) * spacing;
      float panelWidth = contentWidth + padding * 2.0F;
      float panelHeight = padding * 2.0F + slotSize + barSpacing + durabilityBarHeight;
      Hud.getInstance();
      if (Hud.blur.isValue() && !KawaseBlur.INSTANCE.fbos.isEmpty()) {
         Render2D.rectangleWithMask(
            matrix.peek().getPositionMatrix(), panelX, panelY, panelWidth, panelHeight, panelRound, 0, KawaseBlur.INSTANCE.fbos.getFirst().getColorAttachment()
         );
      }

      rectangle.render(
         ShapeProperties.create(matrix, panelX, panelY, panelWidth, panelHeight)
            .round(panelRound)
            .softness(2.5F)
            .color(ColorAssist.multAlpha(new Color(0, 0, 0, 210).getRGB(), animation))
            .build()
      );
      ArrayList<float[]> emptySlots = new ArrayList<>();

      for (int i = 0; i < slots.length; i++) {
         float slotX = panelX + padding + i * (slotSize + spacing);
         float slotY = panelY + padding;
         rectangle.render(
            ShapeProperties.create(matrix, slotX, slotY, slotSize, slotSize)
               .round(2.0F)
               .color(ColorAssist.multAlpha(ThemeManager.BackgroundSettings.getColor(), animation))
               .build()
         );
         ItemStack stack = target.getEquippedStack(slots[i]);
         if (stack.isEmpty()) {
            emptySlots.add(new float[]{slotX, slotY});
         } else {
            float itemVisualSize = 16.0F * itemScale;
            float itemOffset = (slotSize - itemVisualSize) / 2.0F - 1.0F;
            float itemX = slotX + itemOffset;
            float itemY = slotY + itemOffset;
            Render2D.defaultDrawStack(context, stack, itemX, itemY, false, false, itemScale);
            if (stack.isDamageable()) {
               int maxDurability = stack.getMaxDamage();
               int currentDurability = maxDurability - stack.getDamage();
               float durabilityPercent = maxDurability > 0 ? (float)currentDurability / maxDurability : 0.0F;
               float barX = slotX + 1.0F;
               float barY = slotY + slotSize + barSpacing;
               float barWidth = slotSize - 2.0F;
               rectangle.render(
                  ShapeProperties.create(matrix, barX, barY, barWidth, durabilityBarHeight)
                     .round(0.5F)
                     .color(ColorAssist.multAlpha(new Color(0, 0, 0, 210).getRGB(), animation))
                     .build()
               );
               Color barColor = this.getDurabilityColor(durabilityPercent);
               float filledWidth = barWidth * durabilityPercent;
               if (filledWidth > 0.5F) {
                  rectangle.render(
                     ShapeProperties.create(matrix, barX, barY, filledWidth, durabilityBarHeight)
                        .round(0.5F)
                        .color(ColorAssist.multAlpha(barColor.getRGB(), animation))
                        .build()
                  );
               }
            }
         }
      }

      Rectangle.flushPending();

      for (float[] emptySlot : emptySlots) {
         this.drawEmptySlotCross(matrix, emptySlot[0], emptySlot[1], slotSize, animation);
      }

      Hud.getInstance();
      if (Hud.shine.isValue() && Hud.shineElements.isSelected("Target Hud")) {
         HudShine.render(
            matrix, panelX, panelY, panelWidth, panelHeight, panelRound, Hud.shineSize.getValue(), Hud.shineSpeed.getValue(), Hud.shineBrightness.getValue()
         );
      }

      return panelHeight;
   }

   private void drawEmptySlotCross(MatrixStack matrix, float slotX, float slotY, float slotSize, float animation) {
      float margin = 3.0F;
      float x1 = slotX + margin;
      float y1 = slotY + margin;
      float x2 = slotX + slotSize - margin;
      float y2 = slotY + slotSize - margin;
      int color = ColorAssist.multAlpha(new Color(150, 150, 155, 200).getRGB(), animation);
      float thickness = 1.25F;
      Render2D.drawLine(matrix, x1, y1, x2, y2, thickness, color);
      Render2D.drawLine(matrix, x1, y2, x2, y1, thickness, color);
   }

   private Color getDurabilityColor(float durabilityPercent) {
      if (durabilityPercent > 0.5F) {
         return new Color(76, 187, 23);
      } else {
         return durabilityPercent > 0.25F ? new Color(255, 187, 40) : new Color(255, 51, 51);
      }
   }

   private void drawFace(DrawContext context, MatrixStack matrix, float animation) {
      if (this.target != null && this.cachedFaceTexture != null) {
         float faceAlpha = animation * this.faceAlphaAnimation.getOutput().floatValue();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, MathHelper.clamp(faceAlpha, 0.0F, 1.0F));

         try {
            float headX = this.getX() + 4;
            float headY = this.getY() + 4.1F;
            float headSize = 20.0F;
            float headRound = 6.0F;
            Render2D.drawTexture(
               context,
               this.cachedFaceTexture,
               headX,
               headY,
               headSize,
               headRound,
               8,
               8,
               64,
               ColorAssist.getRect(1.0F),
               ColorAssist.multRed(-1, 1.0F + this.target.hurtTime / 4.0F)
            );
         } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.defaultBlendFunc();
         }
      }
   }
}
