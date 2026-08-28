package code.elixir.features.impl.combat;

import code.elixir.events.player.AttackEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.client.managers.event.EventHandler;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Hand;

public class Neuro extends Module {
   public static boolean isTraining = false;
   public static String currentConfig = "";
   public static List<Neuro.Record> trainingData = new ArrayList<>();
   private int playIndex = 0;

   public Neuro() {
      super("Neuro", "Обучаемая нейросеть для AttackAura", ModuleCategory.COMBAT);
   }

   @Override
   public void activate() {
      super.activate();
      this.playIndex = 0;
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player != null) {
         if (isTraining) {
            boolean swinging = mc.player.deathTime > 0;
            trainingData.add(new Neuro.Record(mc.player.getPitch(), mc.player.getYaw(), swinging, false));
         } else if (this.isState()) {
            if (trainingData.isEmpty()) {
               return;
            }

            if (this.playIndex >= trainingData.size()) {
               this.playIndex = 0;
            }

            Neuro.Record r = trainingData.get(this.playIndex);
            float yawRand = (float)(Math.random() - 0.5) * 2.0F;
            float pitchRand = (float)(Math.random() - 0.5) * 2.0F;
            mc.player.setYaw(r.yaw + yawRand);
            mc.player.setPitch(r.pitch + pitchRand);
            if (r.swinging) {
               mc.player.swingHand(Hand.MAIN_HAND);
            }

            if (r.attacking) {
            }

            this.playIndex++;
         }
      }
   }

   @EventHandler
   public void onAttack(AttackEvent event) {
      if (isTraining && !trainingData.isEmpty()) {
         trainingData.get(trainingData.size() - 1).attacking = true;
      }
   }

   public static class Record {
      public float pitch;
      public float yaw;
      public boolean swinging;
      public boolean attacking;

      public Record(float p, float y, boolean s, boolean a) {
         this.pitch = p;
         this.yaw = y;
         this.swinging = s;
         this.attacking = a;
      }
   }
}
