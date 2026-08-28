package code.elixir.utils.features.aura;

import code.elixir.features.impl.combat.AttackAura;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import code.elixir.utils.features.rotate.Rotation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public final class AuraMixinHelper {
   public static boolean isCombatActive() {
      AttackAura aura = AttackAura.getInstance();
      return aura != null && aura.isCombatRotationActive();
   }

   public static boolean hasMoveCorrection() {
      AttackAura aura = AttackAura.getInstance();
      return aura != null && aura.hasMoveCorrection();
   }

   public static float resolveYaw(float fallback) {
      Rotation rotation = resolveRotation();
      return rotation != null ? rotation.getYaw() : fallback;
   }

   public static float resolvePitch(float fallback) {
      Rotation rotation = resolveRotation();
      return rotation != null ? rotation.getPitch() : fallback;
   }

   private static Rotation resolveRotation() {
      if (TurnsConnection.INSTANCE.getCurrentAngle() != null) {
         return new Rotation(TurnsConnection.INSTANCE.getRotation().getYaw(), TurnsConnection.INSTANCE.getRotation().getPitch());
      } else {
         RotationStorage storage = RotationStorage.instance;
         ClientPlayerEntity player = MinecraftClient.getInstance().player;
         return storage != null && storage.isRotating() && player != null ? new Rotation(player.getYaw(), player.getPitch()) : null;
      }
   }

   private AuraMixinHelper() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
