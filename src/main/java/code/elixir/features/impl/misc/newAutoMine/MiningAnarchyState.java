package code.elixir.features.impl.misc.newAutoMine;

import code.elixir.features.impl.player.autoMine.api.MineType;

public class MiningAnarchyState {
   private final int anarchy;
   private final MineType mineType;
   private final long startTime;
   private final int time;

   public MiningAnarchyState(int anarchy, MineType mineType, int time) {
      this.anarchy = anarchy;
      this.mineType = mineType;
      this.startTime = System.currentTimeMillis();
      this.time = time;
   }

   public MiningAnarchyState(String anarchy, MineType mineType, int time) {
      this(Integer.parseInt(anarchy.replaceAll("\\D", "")), mineType, time);
   }

   public long getRemainingTime() {
      return this.time - (System.currentTimeMillis() - this.startTime) / 1000L;
   }

   @Override
   public boolean equals(Object obj) {
      return !(obj instanceof MiningAnarchyState state) ? false : state.anarchy == this.anarchy;
   }

   public int getAnarchy() {
      return this.anarchy;
   }

   public MineType getMineType() {
      return this.mineType;
   }

   public long getStartTime() {
      return this.startTime;
   }

   public int getTime() {
      return this.time;
   }
}
