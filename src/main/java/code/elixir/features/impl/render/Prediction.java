package code.elixir.features.impl.render;

import code.elixir.events.render.DrawEvent;
import code.elixir.events.render.WorldRenderEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.DecompilerSupport;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.geometry.Render2D;
import code.elixir.utils.display.render.geometry.Render3D;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.features.aura.utils.RaycastAngle;
import code.elixir.utils.features.aura.warp.Turns;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.math.calc.Calculate;
import code.elixir.utils.math.projection.Projection;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Quaternionf;

public class Prediction extends Module {
   final List<Prediction.Point> points = new ArrayList<>();

   public static Prediction getInstance() {
      return Instance.get(Prediction.class);
   }

   public Prediction() {
      super("Prediction", "Prediction", ModuleCategory.RENDER);
   }

   @EventHandler
   public void onDraw(DrawEvent e) {
      if (e != null && e.getDrawContext() != null) {
         DrawContext context = e.getDrawContext();

         for (Prediction.Point point : this.points) {
            Vec3d vec3d = Projection.worldSpaceToScreenSpace(point.pos);
            int ticks = point.ticks;
            if (Projection.canSee(point.pos)) {
               FontRenderer font = Fonts.getSize(13);
               double time = ticks * 50 / 1000.0;
               String text = String.format("%.1f", time).replace(",", ".").replace(".0", "") + " сек";
               float textWidth = font.getStringWidth(text);
               float posX = (float)(vec3d.getX() + textWidth / 2.0F - 6.0);
               float posY = (float)(vec3d.getY() + 4.0);
               float padding = 3.0F;
               float iconSize = 8.0F;
               rectangle.render(
                  ShapeProperties.create(
                        context.getMatrices(), posX - textWidth + iconSize + padding - 8.0F, posY - padding, padding + textWidth + padding + 8.0F, 10.0
                     )
                     .round(1.5F)
                     .color(new Color(27, 27, 30, 255).getRGB())
                     .build()
               );
               font.drawString(context.getMatrices(), text, posX - textWidth + 8.0F + padding * 2.0F, posY + 0.5F, -1);
               Render2D.defaultDrawStack(context, point.stack, posX - textWidth - padding + 7.0F, posY - padding + 0.35F, true, false, 0.45F);
            }
         }
      }
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      if (mc.player != null && mc.world != null) {
         this.points.clear();
         this.drawPredictionInHand(e.getStack(), mc.player.getHandItems(), TurnsConnection.INSTANCE.getRotation());
         this.getProjectiles().forEach(entity -> {
            Vec3d motion = entity.getVelocity();
            Vec3d pos = entity.getPos();

            for (int i = 0; i < 300; i++) {
               Vec3d prevPos = pos;
               pos = pos.add(motion);
               motion = this.calculateMotion(entity, prevPos, motion);
               BlockHitResult result = RaycastAngle.raycast(prevPos, pos, ShapeType.COLLIDER, entity);
               if (!result.getType().equals(Type.MISS)) {
                  pos = result.getPos();
               }

               Render3D.drawLine(prevPos, pos, ColorAssist.multAlpha(ColorAssist.fade(i), MathHelper.clamp(i / 25.0F, 0.0F, 1.0F)), 2.0F, false);
               if (!result.getType().equals(Type.MISS) || pos.y < -128.0) {
                  this.BreakingBad(entity, pos, i);
               }
            }
         });
      }
   }

   public void drawPredictionInHand(MatrixStack matrix, Iterable<ItemStack> stacks, Turns angle) {
      if (mc.player == null || mc.world == null) return;
      Item activeItem = mc.player.getActiveItem().getItem();
      Iterator<ItemStack> iterator = stacks.iterator();
      if (iterator.hasNext()) {
         ItemStack stack = iterator.next();
         Item item = stack.getItem();
         List<HitResult> list = null;

         if (item instanceof ExperienceBottleItem) {
            list = this.checkTrajectory(new ExperienceBottleEntity(mc.world, mc.player, stack), 0.8, angle);
         } else if (item instanceof SplashPotionItem) {
            list = this.checkTrajectory(new PotionEntity(mc.world, mc.player, stack), 0.55, angle);
         } else if (item instanceof TridentItem && item.equals(activeItem) && mc.player.getItemUseTime() >= 10) {
            list = this.checkTrajectory(new TridentEntity(mc.world, mc.player, stack), 2.5, angle);
         } else if (item instanceof SnowballItem) {
            list = this.checkTrajectory(new SnowballEntity(mc.world, mc.player, stack), 1.5, angle);
         } else if (item instanceof EggItem) {
            list = this.checkTrajectory(new EggEntity(mc.world, mc.player, stack), 1.5, angle);
         } else if (item instanceof EnderPearlItem) {
            list = this.checkTrajectory(new EnderPearlEntity(mc.world, mc.player, stack), 1.5, angle);
         } else if (item instanceof BowItem && item.equals(activeItem) && mc.player.isUsingItem()) {
            list = this.checkTrajectory(
               new ArrowEntity(mc.world, mc.player, stack, stack),
               3.0F * MathHelper.clamp((mc.player.getItemUseTime() + tickCounter.getTickDelta(false)) / 20.0F, 0.0F, 1.0F),
               angle
            );
         } else if (item instanceof CrossbowItem && CrossbowItem.isCharged(stack)) {
            ChargedProjectilesComponent component = (ChargedProjectilesComponent)stack.get(DataComponentTypes.CHARGED_PROJECTILES);
            ArrayList<HitResult> list2 = new ArrayList<>();
            if (component != null && !component.getProjectiles().isEmpty()) {
               float velocity = ((ItemStack)component.getProjectiles().getFirst()).isOf(Items.FIREWORK_ROCKET) ? 100.0F : 3.0F;
               list2.add(this.checkTrajectory(angle.toVector(), new ArrowEntity(mc.world, mc.player, stack, stack), velocity));
               if (component.getProjectiles().size() > 2) {
                  float pitchAbs = angle.getPitch() / 90.0F;
                  float delta = pitchAbs * pitchAbs * pitchAbs * pitchAbs * pitchAbs;
                  float yaw = MathHelper.lerp(Math.abs(delta), 10, 90);
                  float pitch = MathHelper.lerp(delta, 0, 10);
                  list2.add(
                     this.checkTrajectory(angle.addYaw(-yaw).addPitch(-pitch).toVector(), new ArrowEntity(mc.world, mc.player, stack, stack), velocity)
                  );
                  list2.add(this.checkTrajectory(angle.addYaw(yaw).toVector(), new ArrowEntity(mc.world, mc.player, stack, stack), velocity));
               }
            }
            list = list2;
         }

         List var26;
         if (list != null && !(var26 = list.stream().filter(Objects::nonNull).toList()).isEmpty()) {
            this.renderProjectileResults(matrix, var26);
         }
      }
   }

   public void renderProjectileResults(MatrixStack matrix, List<HitResult> results) {
      for (HitResult result : results) {
         Direction direction = this.getDirection(result);
         int color = result.getType().equals(Type.ENTITY) ? new Color(255, 0, 0, 250).getRGB() : new Color(0, 255, 0, 250).getRGB();
         double width = 0.3;

         Quaternionf quaternionf = switch (direction) {
            case WEST, EAST -> RotationAxis.POSITIVE_Z.rotationDegrees(90.0F);
            case SOUTH, NORTH -> RotationAxis.POSITIVE_X.rotationDegrees(90.0F);
            default -> new Quaternionf();
         };
         matrix.push();
         matrix.translate(result.getPos());
         matrix.multiply(quaternionf);
         Entry entry = matrix.peek().copy();
         int size = 90;

         for (int i = 0; i <= size; i++) {
            Render3D.drawLine(entry, Calculate.cosSin(i, size, width), Calculate.cosSin(i + 1, size, width), color, color, 2.0F, false);
         }

         Render3D.drawLine(entry, new Vec3d(0.0, 0.0, -width), new Vec3d(0.0, 0.0, width), color, color, 2.0F, false);
         Render3D.drawLine(entry, new Vec3d(-width, 0.0, 0.0), new Vec3d(width, 0.0, 0.0), color, color, 2.0F, false);
         matrix.pop();
      }
   }

   public List<Entity> getProjectiles() {
      return PlayerInteractionHelper.streamEntities()
         .filter(e -> (e instanceof PersistentProjectileEntity || e instanceof ThrownItemEntity || e instanceof ItemEntity) && !this.visible(e))
         .toList();
   }

   public List<HitResult> checkTrajectory(ProjectileEntity entity, double velocity, Turns angle) {
      return new ArrayList<>(Collections.singleton(this.checkTrajectory(angle.toVector(), entity, velocity)));
   }

   public HitResult checkTrajectory(Vec3d lookVec, ProjectileEntity entity, double velocity) {
      if (mc.player == null) return null;
      float sqrt = MathHelper.sqrt(lookVec.toVector3f().lengthSquared());
      Vec3d class_2432;
      if (entity instanceof ArrowEntity arrow && arrow.getItemStack().getItem().equals(Items.CROSSBOW)) {
         class_2432 = Vec3d.ZERO;
      } else {
         class_2432 = mc.player.getVelocity();
      }

      return this.traceTrajectory(
         mc.player.getEyePos().add(Calculate.interpolate(mc.player).subtract(mc.player.getPos())),
         lookVec.multiply(velocity / sqrt).add(class_2432),
         entity
      );
   }

   public HitResult calcTrajectory(ProjectileEntity e) {
      return this.traceTrajectory(e.getPos(), e.getVelocity(), e);
   }

   public HitResult traceTrajectory(Vec3d pos, Vec3d motion, ProjectileEntity entity) {
      for (int i = 0; i < 300; i++) {
         Vec3d prevPos = pos;
         pos = pos.add(motion);
         Vec3d finalPos = pos;
         motion = this.calculateMotion(entity, prevPos, motion);
         BlockHitResult result = RaycastAngle.raycast(prevPos, finalPos, ShapeType.COLLIDER, entity);
         if (!result.getType().equals(Type.MISS)) {
            return result;
         }

         if (PlayerInteractionHelper.streamEntities().filter(ent -> {
            LivingEntity living;
            return ent instanceof LivingEntity && (living = (LivingEntity)ent) != entity.getOwner() && living.isAlive();
         }).anyMatch(ent -> ent.getBoundingBox().expand(0.3).intersects(prevPos, finalPos))) {
            return new HitResult(finalPos) {
               public Type getType() {
                  return Type.ENTITY;
               }
            };
         }

         if (pos.y < -128.0) {
            break;
         }
      }

      return null;
   }

   public Vec3d calculateMotion(Entity entity, Vec3d prevPos, Vec3d motion) {
      boolean isInWater = mc.world != null && mc.world.getFluidState(BlockPos.ofFloored(prevPos)).isIn(FluidTags.WATER);
      double d;
      if (entity instanceof TridentEntity) {
         d = 0.99;
      } else if (entity instanceof PersistentProjectileEntity) {
         d = isInWater ? 0.6 : 0.99;
      } else {
         d = isInWater ? 0.8 : 0.99;
      }

      return motion.multiply(d).add(0.0, -entity.getFinalGravity(), 0.0);
   }

   private void BreakingBad(Entity entity, Vec3d pos, int ticks) {
      if (entity instanceof ItemEntity item) {
         this.points.add(new Prediction.Point(item.getStack(), pos, ticks));
      } else if (entity instanceof ThrownItemEntity thrown) {
         this.points.add(new Prediction.Point(thrown.getStack(), pos, ticks));
      } else if (entity instanceof PersistentProjectileEntity persistent) {
         this.points.add(new Prediction.Point(persistent.getItemStack(), pos, ticks));
      }
   }

   private Direction getDirection(HitResult result) {
      return result instanceof BlockHitResult blockHitResult
         ? blockHitResult.getSide()
         : Direction.getFacing(result.getPos().subtract(mc.player.getEyePos()).normalize());
   }

   private boolean visible(Entity entity) {
      boolean posChange = entity.getX() == entity.prevX && entity.getY() == entity.prevY && entity.getZ() == entity.prevZ;
      boolean itemEntityCheck = entity instanceof ItemEntity
         && (entity.isOnGround() || PlayerInteractionHelper.isBoxInBlock(entity.getBoundingBox().expand(2.0), Blocks.WATER));
      return posChange || itemEntityCheck;
   }

   private record Point(ItemStack stack, Vec3d pos, int ticks) {
   }
}
