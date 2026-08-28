package code.elixir.utils.interactions.simulate;

import code.elixir.utils.display.interfaces.QuickImports;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class ArrowSimulation implements QuickImports {
   public final ClientWorld world;
   public Vec3d pos;
   public Vec3d velocity;
   public final boolean collideEntities;
   public boolean inGround = false;

   public ArrowSimulation(ClientWorld world, Vec3d pos, Vec3d velocity) {
      this(world, pos, velocity, true);
   }

   public ArrowSimulation(ClientWorld world, Vec3d pos, Vec3d velocity, boolean collideEntities) {
      this.world = world;
      this.pos = pos;
      this.velocity = velocity;
      this.collideEntities = collideEntities;
   }

   public HitResult tick() {
      if (this.inGround) {
         return null;
      } else {
         Vec3d newPos = this.pos.add(this.velocity);
         double drag = this.isTouchingWater() ? 0.6 : 0.99;
         this.velocity = this.velocity.multiply(drag);
         this.velocity = new Vec3d(this.velocity.x, this.velocity.y - 0.05F, this.velocity.z);
         HitResult hitResult = this.updateCollision(this.pos, newPos);
         if (hitResult != null) {
            this.pos = hitResult.getPos();
            this.inGround = true;
            return hitResult;
         } else {
            this.pos = newPos;
            return null;
         }
      }
   }

   private HitResult updateCollision(Vec3d pos, Vec3d newPos) {
      ClientWorld world = this.world;
      ArrowEntity arrowEntity = new ArrowEntity(this.world, this.pos.x, this.pos.y, this.pos.z, new ItemStack(Items.ARROW), null);
      BlockHitResult blockHitResult = world.raycast(new RaycastContext(pos, newPos, ShapeType.COLLIDER, FluidHandling.NONE, arrowEntity));
      if (this.collideEntities) {
         double size = 0.45;
         EntityHitResult entityHitResult;
         if ((
                  entityHitResult = ProjectileUtil.getEntityCollision(
                     this.world,
                     arrowEntity,
                     pos,
                     newPos,
                     new Box(-0.45, -size, -size, size, size, size).offset(pos).stretch(newPos.subtract(pos)).expand(1.0),
                     entity -> !entity.isSpectator() && entity.isAlive() && (entity.canHit() || entity != mc.player && entity == arrowEntity)
                        ? !arrowEntity.isConnectedThroughVehicle(entity)
                        : false
                  )
               )
               != null
            && entityHitResult.getType() != Type.MISS) {
            return entityHitResult;
         }
      }

      return blockHitResult != null && blockHitResult.getType() != Type.MISS ? blockHitResult : null;
   }

   private boolean isTouchingWater() {
      return false;
   }
}
