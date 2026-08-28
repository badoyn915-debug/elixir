package code.elixir.common.repository.staff;

import code.elixir.Elixir;
import code.elixir.utils.client.managers.file.FileController;
import code.elixir.utils.client.managers.file.impl.StaffFile;
import java.util.ArrayList;
import java.util.List;

public class StaffRepository {
   private static final List<StaffRepository.Staff> staffList = new ArrayList<>();

   public static void addStaff(String name) {
      if (!isStaff(name)) {
         staffList.add(new StaffRepository.Staff(name));
      }
   }

   public static void removeStaff(String name) {
      staffList.removeIf(staff -> staff.getName().equalsIgnoreCase(name));
   }

   public static boolean isStaff(String name) {
      return staffList.stream().anyMatch(staff -> staff.getName().equalsIgnoreCase(name));
   }

   public static void clear() {
      staffList.clear();
   }

   public static List<StaffRepository.Staff> getStaff() {
      return staffList;
   }

   private static void save() {
      FileController fileController = Elixir.getInstance().getFileController();
      if (fileController != null) {
         fileController.saveFile(StaffFile.class);
      }
   }

   public record Staff(String name) {
      public String getName() {
         return this.name;
      }
   }
}
