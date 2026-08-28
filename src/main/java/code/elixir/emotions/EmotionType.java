package code.elixir.emotions;

public enum EmotionType {
   HELLO("Приветствие", "Машет рукой и здоровается", "HI", 2500L),
   HAPPY("Радость", "Поднимает руки вверх от счастья", "HP", 2500L),
   CLAP("Хлопки", "Аплодирует обеими руками", "CL", 2500L),
   CRY("Плач", "Закрывает лицо и плачет", "CR", 2500L),
   DEB("Деб", "Резкая поза с наклоном головы", "DB", 2500L),
   FLOSS("Флосс", "Танцевальное движение в стороны", "FL", 4500L),
   GET_GRIDDY("Griddy", "Бегущий танец с качом корпуса", "GG", 5500L),
   POINT("Указать", "Показывает рукой вперёд", "PT", 2500L),
   SALUTE("Салют", "Прикладывает руку к виску", "SA", 2500L),
   THINK("Задуматься", "Подпирает подбородок и смотрит в сторону", "TH", 2500L),
   STRETCH("Потянуться", "Тянется вверх обеими руками", "UP", 2500L),
   DANCE("Танец", "Свободный танец с покачиваниями", "DN", 4500L),
   FACEPALM("Фейспалм", "Прикладывает ладонь к лицу", "FP", 2500L),
   SHRUG("Пожать плечами", "Разводит руками в недоумении", "SH", 2500L);

   private final String title;
   private final String description;
   private final String shortLabel;
   private final long durationMs;

   private EmotionType(String title, String description, String shortLabel, long durationMs) {
      this.title = title;
      this.description = description;
      this.shortLabel = shortLabel;
      this.durationMs = durationMs;
   }

   public String getTitle() {
      return this.title;
   }

   public String getDescription() {
      return this.description;
   }

   public String getShortLabel() {
      return this.shortLabel;
   }

   public long getDurationMs() {
      return this.durationMs;
   }
}
