package code.elixir.utils.math.task;

public enum TaskPriority {
   CRITICAL_FOR_USER_PROTECTION(60),
   CRUCIAL_FOR_PLAYER_LIFE(40),
   HIGH_IMPORTANCE_3(35),
   HIGH_IMPORTANCE_2(30),
   HIGH_IMPORTANCE_1(20),
   STANDARD(0),
   LOW_PRIORITY(-20);

   private final int priority;

   public int getPriority() {
      return this.priority;
   }

   private TaskPriority(int priority) {
      this.priority = priority;
   }
}
