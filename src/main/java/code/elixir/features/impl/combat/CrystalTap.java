package code.elixir.features.impl.combat;

import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;

public class CrystalTap extends Module {
   private final BooleanSetting autoAddCrystal = new BooleanSetting("Автоматически ставить кристалл", "Автоматически ставить кристалл").setValue(true);
   private final SliderSettings addCooldown = new SliderSettings("Задержка на ставку кристалла", "Задержка на ставку кристалла")
      .setValue(0.0F)
      .range(0, 5)
      .step(1.0F);
   private final SliderSettings attackCooldown = new SliderSettings("Задержка на атаку кристалла", "Задержка на атаку кристалла")
      .setValue(0.0F)
      .range(0, 5)
      .step(1.0F);
   private int attackCrystalCooldown;
   private int addCrystalCooldown;

   public static CrystalTap getInstance() {
      return Instance.get(CrystalTap.class);
   }

   public CrystalTap() {
      super("CrystalTap", "CrystalTap", ModuleCategory.COMBAT);
      this.setup(new Setting[]{this.autoAddCrystal, this.addCooldown, this.attackCooldown});
   }

   private void addCrystal() {
      HitResult class_2392 = mc.crosshairTarget;
      BlockHitResult obsidianTarget;
      if (class_2392 instanceof BlockHitResult
         && mc.world.getBlockState((obsidianTarget = (BlockHitResult)class_2392).getBlockPos()).getBlock() == Blocks.OBSIDIAN) {
         for (Entity entity : mc.world.getEntities()) {
            EndCrystalEntity crystal;
            if (entity instanceof EndCrystalEntity && (crystal = (EndCrystalEntity)entity).getBlockPos().down().equals(obsidianTarget.getBlockPos())) {
               return;
            }
         }

         Direction side = obsidianTarget.getSide();
         BlockHitResult crystalHit = new BlockHitResult(obsidianTarget.getPos(), side, obsidianTarget.getBlockPos(), true);
         if (mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL) {
            return;
         }

         if (this.addCrystalCooldown <= this.addCooldown.getInt()) {
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, crystalHit);
            this.addCrystalCooldown = 0;
         } else {
            this.addCrystalCooldown++;
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         if (this.autoAddCrystal.isValue()) {
            this.addCrystal();
         }

         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity crystal) {
               HitResult class_2392 = mc.crosshairTarget;
               EntityHitResult hitResult;
               if (class_2392 instanceof EntityHitResult && (hitResult = (EntityHitResult)class_2392).getEntity() instanceof EndCrystalEntity) {
                  if (this.attackCrystalCooldown >= this.attackCooldown.getInt()) {
                     mc.interactionManager.attackEntity(mc.player, hitResult.getEntity());
                     mc.player.swingHand(Hand.MAIN_HAND);
                     this.attackCrystalCooldown = 0;
                  } else {
                     this.attackCrystalCooldown++;
                  }
               }
            }
         }
      }
   }

   public BooleanSetting getAutoAddCrystal() {
      return this.autoAddCrystal;
   }

   public SliderSettings getAddCooldown() {
      return this.addCooldown;
   }

   public SliderSettings getAttackCooldown() {
      return this.attackCooldown;
   }

   public int getAttackCrystalCooldown() {
      return this.attackCrystalCooldown;
   }

   public int getAddCrystalCooldown() {
      return this.addCrystalCooldown;
   }
}
