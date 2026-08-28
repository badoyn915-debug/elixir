package code.elixir.features.impl.misc;

import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.utils.client.logs.Logger;
import code.elixir.utils.client.managers.event.EventHandler;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.network.packet.s2c.play.RemoveEntityStatusEffectS2CPacket;
import net.minecraft.text.Text;

public class DebugModule extends Module {
   private final MultiSelectSetting logs = new MultiSelectSetting("Логи", "Что логировать в консоль")
      .value("Взрывные зелья", "Эффекты сущностей")
      .selected("Взрывные зелья", "Эффекты сущностей");
   private final Set<Integer> seenPotionEntities = new HashSet<>();
   private final Set<Integer> seenAreaClouds = new HashSet<>();

   public DebugModule() {
      super("DebugModule", ModuleCategory.MISC);
      this.setup(new Setting[]{this.logs});
   }

   private boolean logPotions() {
      return this.logs.isSelected("Взрывные зелья");
   }

   private boolean logEffects() {
      return this.logs.isSelected("Эффекты сущностей");
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.world != null) {
         if (this.logPotions()) {
            for (Entity entity : mc.world.getEntities()) {
               if (entity instanceof PotionEntity potion) {
                  int id = potion.getId();
                  ItemStack stack;
                  if (this.seenPotionEntities.add(id)
                     && (stack = potion.getStack()) != null
                     && !stack.isEmpty()
                     && (stack.isOf(Items.SPLASH_POTION) || stack.isOf(Items.LINGERING_POTION))) {
                     String name = stack.getName().getString();
                     String effects = this.describePotionEffects(stack);
                     Logger.info(
                        "[DebugModule] PotionEntity #"
                           + id
                           + " @("
                           + String.format("%.2f", entity.getX())
                           + ", "
                           + String.format("%.2f", entity.getY())
                           + ", "
                           + String.format("%.2f", entity.getZ())
                           + ") "
                           + name
                           + (effects.isEmpty() ? "" : " | " + effects)
                     );
                  }
               } else {
                  AreaEffectCloudEntity cloud;
                  int id;
                  if (entity instanceof AreaEffectCloudEntity && this.seenAreaClouds.add(id = (cloud = (AreaEffectCloudEntity)entity).getId())) {
                     String effects = this.describeCloudEffects(cloud);
                     Logger.info(
                        "[DebugModule] AreaEffectCloud #"
                           + id
                           + " @("
                           + String.format("%.2f", entity.getX())
                           + ", "
                           + String.format("%.2f", entity.getY())
                           + ", "
                           + String.format("%.2f", entity.getZ())
                           + ") "
                           + (effects.isEmpty() ? "" : "| " + effects)
                     );
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onPacket(PacketEvent e) {
      if (mc.world != null) {
         if (this.logEffects()) {
            Packet<?> packet = e.getPacket();
            if (packet instanceof EntityStatusEffectS2CPacket p) {
               Entity entity = mc.world.getEntityById(p.getEntityId());
               String entityName = this.describeEntity(entity);
               String effectName = Text.translatable(((StatusEffect)p.getEffectId().value()).getTranslationKey()).getString();
               int level = p.getAmplifier() + 1;
               int durationTicks = p.getDuration();
               Logger.info(
                  "[DebugModule] EFFECT_ADD "
                     + entityName
                     + " -> "
                     + effectName
                     + " "
                     + level
                     + " ("
                     + this.formatDuration(durationTicks)
                     + ") ambient="
                     + p.isAmbient()
                     + " particles="
                     + p.shouldShowParticles()
                     + " icon="
                     + p.shouldShowIcon()
               );
            } else {
               if (packet instanceof RemoveEntityStatusEffectS2CPacket p) {
                  Entity entity = mc.world.getEntityById(p.entityId());
                  String entityName = this.describeEntity(entity);
                  String effectName = Text.translatable(((StatusEffect)p.effect().value()).getTranslationKey()).getString();
                  Logger.info("[DebugModule] EFFECT_REMOVE " + entityName + " -> " + effectName);
               }
            }
         }
      }
   }

   private String describePotionEffects(ItemStack stack) {
      PotionContentsComponent component = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS);
      if (component == null) {
         return "";
      } else {
         StringBuilder sb = new StringBuilder();

         for (StatusEffectInstance eff : component.getEffects()) {
            if (!sb.isEmpty()) {
               sb.append(", ");
            }

            sb.append(this.describeEffect(eff));
         }

         return sb.toString();
      }
   }

   private String describeCloudEffects(AreaEffectCloudEntity cloud) {
      PotionContentsComponent contents = this.getCloudPotionContents(cloud);
      if (contents == null) {
         return "";
      } else {
         StringBuilder sb = new StringBuilder();

         for (StatusEffectInstance eff : contents.getEffects()) {
            if (!sb.isEmpty()) {
               sb.append(", ");
            }

            sb.append(this.describeEffect(eff));
         }

         return sb.toString();
      }
   }

   private PotionContentsComponent getCloudPotionContents(AreaEffectCloudEntity cloud) {
      Object value = this.tryInvokeNoArg(cloud, "getPotionContents");
      if (value == null) {
         value = this.tryInvokeNoArg(cloud, "getPotionContentsComponent");
      }

      if (value == null) {
         value = this.tryInvokeNoArg(cloud, "getPotionContentsComponent");
      }

      PotionContentsComponent pc;
      return value instanceof PotionContentsComponent ? (pc = (PotionContentsComponent)value) : null;
   }

   private Object tryInvokeNoArg(Object target, String methodName) {
      try {
         Method m = target.getClass().getMethod(methodName);
         return m.invoke(target);
      } catch (Throwable var4) {
         return null;
      }
   }

   private String describeEffect(StatusEffectInstance eff) {
      String name = Text.translatable(((StatusEffect)eff.getEffectType().value()).getTranslationKey()).getString();
      int lvl = eff.getAmplifier() + 1;
      return name + " " + lvl + " (" + this.formatDuration(eff.getDuration()) + ")";
   }

   private String describeEntity(Entity entity) {
      if (entity == null) {
         return "unknown_entity";
      } else {
         return entity instanceof LivingEntity living
            ? living.getName().getString() + "(id=" + living.getId() + ")"
            : entity.getType().toString() + "(id=" + entity.getId() + ")";
      }
   }

   private String formatDuration(int ticks) {
      if (ticks < 0) {
         return "∞";
      } else {
         int seconds = ticks / 20;
         int minutes = seconds / 60;
         int var4;
         return minutes > 0 ? minutes + "m " + seconds + "s" : (var4 = seconds % 60) + "s";
      }
   }
}
