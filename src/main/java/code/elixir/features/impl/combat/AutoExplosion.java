package code.elixir.features.impl.combat;

import code.elixir.events.block.BlockBreakingEvent;
import code.elixir.events.block.BlockUpdateEvent;
import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.features.aura.RotationStorage;
import code.elixir.utils.features.aura.utils.MathAngle;
import code.elixir.utils.features.rotate.Rotation;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.interactions.inv.InventoryToolkit;
import java.util.Iterator;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AutoExplosion extends Module {
   private static final double EXPAND = 0.0;
   private final BooleanSetting protectSelf = new BooleanSetting("Не взрывать себя", "Не взрывает кристалл на одной высоте с вами").setValue(true);
   private final BooleanSetting protectItems = new BooleanSetting("Не взрывать, если рядом предметы", "Пропускает взрыв, если рядом лежат предметы")
      .setValue(false);
   private final BooleanSetting keepCrystalHand = new BooleanSetting("Оставлять кристал в руке", "Не возвращает слот после установки кристалла")
      .setValue(false);
   private BlockPos placePos;
   private int crystalSlot = -1;
   private boolean waitPlaceTick;
   private boolean restoreSlot;
   private int previousSlot;
   private boolean bedPlaceFlow;
   private Box attackBox;

   public AutoExplosion() {
      super("AutoExplosion", "Auto Explosion", ModuleCategory.COMBAT);
      this.setup(new Setting[]{this.protectSelf, this.protectItems, this.keepCrystalHand});
   }

   public static AutoExplosion getInstance() {
      return Instance.get(AutoExplosion.class);
   }

   @EventHandler
   public void onPacket(PacketEvent event) {
      if (event.isSend() && !PlayerInteractionHelper.nullCheck()) {
         if (event.getPacket() instanceof PlayerInteractBlockC2SPacket packet) {
            if (mc.player.getStackInHand(packet.getHand()).isOf(Items.OBSIDIAN)) {
               BlockPos targetPos = packet.getBlockHitResult().getBlockPos().offset(packet.getBlockHitResult().getSide());
               if (!mc.player.getItemCooldownManager().isCoolingDown(Items.END_CRYSTAL.getDefaultStack())) {
                  if (!this.protectItems.isValue() || !this.hasNearbyItems(targetPos)) {
                     int slot = this.findItemSlot(Items.END_CRYSTAL, true);
                     if (slot != -1) {
                        this.placePos = targetPos;
                        this.crystalSlot = slot;
                        this.bedPlaceFlow = false;
                        this.waitPlaceTick = true;
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onBlockUpdate(BlockUpdateEvent event) {
      if (event.type() == BlockUpdateEvent.Type.UPDATE) {
         if (this.placePos == null && !this.waitPlaceTick) {
            if (mc.player != null) {
               BlockPos pos = event.pos();
               if (event.state().isOf(Blocks.OBSIDIAN)) {
                  if (!mc.player.getMainHandStack().isOf(Items.OBSIDIAN) && !mc.player.getOffHandStack().isOf(Items.OBSIDIAN)) {
                     return;
                  }

                  if (mc.player.getBlockPos().getManhattanDistance(pos) > 6) {
                     return;
                  }

                  if (mc.player.getItemCooldownManager().isCoolingDown(Items.END_CRYSTAL.getDefaultStack())) {
                     return;
                  }

                  if (this.protectItems.isValue() && this.hasNearbyItems(pos)) {
                     return;
                  }

                  int slot = this.findItemSlot(Items.END_CRYSTAL, true);
                  if (slot == -1) {
                     return;
                  }

                  this.placePos = pos;
                  this.crystalSlot = slot;
                  this.bedPlaceFlow = false;
                  this.waitPlaceTick = true;
               } else if (event.state().getBlock() instanceof BedBlock) {
                  int slot = this.findItemSlot(Items.END_CRYSTAL, true);
                  if (slot == -1) {
                     return;
                  }

                  this.placePos = pos;
                  this.crystalSlot = slot;
                  this.bedPlaceFlow = true;
                  this.waitPlaceTick = true;
               }
            }
         }
      }
   }

   @EventHandler
   public void onBlockBreaking(BlockBreakingEvent event) {
      if (!PlayerInteractionHelper.nullCheck()) {
         if (!mc.player.getItemCooldownManager().isCoolingDown(Items.END_CRYSTAL.getDefaultStack())) {
            Block block = mc.world.getBlockState(event.blockPos()).getBlock();
            if (block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN) {
               this.attackBox = new Box(event.blockPos().up()).expand(0.0);
            }
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (!PlayerInteractionHelper.nullCheck()) {
         this.handleSlotRestore();
         this.handleCrystalPlace();
         this.handleCrystalAttack();
      }
   }

   private void handleSlotRestore() {
      if (this.restoreSlot) {
         this.restoreSlot = false;
         mc.player.getInventory().selectedSlot = this.previousSlot;
         InventoryToolkit.switchToSilent(this.previousSlot);
      }
   }

   private void handleCrystalPlace() {
      if (this.placePos != null) {
         if (mc.world.getBlockState(this.placePos).isAir()) {
            this.placePos = null;
         } else if (this.waitPlaceTick) {
            this.waitPlaceTick = false;
         } else {
            Vec3d eyes = mc.player.getEyePos();
            Vec3d hitPos = this.getClosestPoint(eyes, new Box(this.placePos));
            Vec3d delta = hitPos.subtract(eyes);
            float yaw = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
            float pitch = (float)(-Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z))));
            RotationStorage.update(new Rotation(yaw, pitch), 180.0F, 180.0F, 25.0F, 20.0F, 1, 6, false);
            this.previousSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = this.crystalSlot;
            InventoryToolkit.switchToSilent(this.crystalSlot);
            Vec3d direction = delta.normalize();
            BlockHitResult hit = new BlockHitResult(hitPos, Direction.getFacing(direction.x, direction.y, direction.z), this.placePos, false);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
            mc.player.swingHand(Hand.MAIN_HAND);
            this.restoreSlot = true;
            if (this.bedPlaceFlow) {
               this.crystalSlot = this.previousSlot;
               this.bedPlaceFlow = false;
            } else {
               this.placePos = null;
               if (this.keepCrystalHand.isValue()) {
                  this.restoreSlot = false;
               }
            }
         }
      }
   }

   private void handleCrystalAttack() {
      if (this.attackBox != null) {
         for (Entity entity : mc.world.getEntities()) {
            EndCrystalEntity crystal;
            if (entity instanceof EndCrystalEntity && this.attackBox.intersects((crystal = (EndCrystalEntity)entity).getBoundingBox())) {
               if (this.protectSelf.isValue() && Math.abs(mc.player.getY() - crystal.getY()) < 1.0) {
                  this.attackBox = null;
                  return;
               }

               BlockPos below = BlockPos.ofFloored(this.attackBox.getCenter()).down();
               if (this.protectItems.isValue() && this.hasNearbyItems(below)) {
                  this.attackBox = null;
                  return;
               }

               if (!crystal.getBoundingBox().intersects(mc.player.getBoundingBox())) {
                  Vec3d point = MathAngle.getClosestVec(crystal);
                  Vec3d delta = point.subtract(mc.player.getEyePos());
                  float yaw = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
                  float pitch = (float)(-Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z))));
                  RotationStorage.update(new Rotation(yaw, pitch), 180.0F, 180.0F, 25.0F, 20.0F, 1, 6, false);
               }

               mc.interactionManager.attackEntity(mc.player, crystal);
               mc.player.swingHand(Hand.MAIN_HAND);
               this.attackBox = null;
               return;
            }
         }
      }
   }

   private boolean hasNearbyItems(BlockPos pos) {
      Box box = new Box(pos).expand(1.0);
      Iterator iterator = mc.world.getEntitiesByClass(ItemEntity.class, box, e -> true).iterator();
      if (iterator.hasNext()) {
         Entity entity = (Entity)iterator.next();
         return true;
      } else {
         return false;
      }
   }

   private int findItemSlot(Item item, boolean hotbarOnly) {
      int limit = hotbarOnly ? 9 : 36;
      int found = -1;

      for (int i = 0; i < limit; i++) {
         if (mc.player.getInventory().getStack(i).isOf(item)) {
            found = i;
         }
      }

      return found;
   }

   private Vec3d getClosestPoint(Vec3d point, Box box) {
      double x = MathHelper.clamp(point.x, box.minX, box.maxX);
      double y = MathHelper.clamp(point.y, box.minY, box.maxY);
      double z = MathHelper.clamp(point.z, box.minZ, box.maxZ);
      return new Vec3d(x, y, z);
   }

   @Override
   public void deactivate() {
      this.placePos = null;
      this.crystalSlot = -1;
      this.waitPlaceTick = false;
      this.restoreSlot = false;
      this.bedPlaceFlow = false;
      this.attackBox = null;
      super.deactivate();
   }
}
