package monoton.control.staff;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class StaffManager {
    public static List<Staff> staffList = new ArrayList<>();
    private static final File file = new File("C:\\Monoton\\game\\Monoton\\staff.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static class Staff {
        private String name;
        private String prefix;

        public Staff(String name, String prefix) {
            this.name = name;
            this.prefix = prefix != null ? prefix : "";
        }

        public String getName() {
            return name;
        }

        public String getPrefix() {
            return prefix;
        }
    }

    public void init() throws Exception {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
            staffList.add(new Staff("Mr_Bibys_YT", ""));
            staffList.add(new Staff("MrDomer", ""));
            updateFile();
        } else {
            readStaff();
        }
    }

    public void addStaff(String name, String prefix) {
        staffList.add(new Staff(name, prefix != null ? prefix : ""));
        updateFile();
    }

    public boolean isStaff(String name) {
        return staffList.stream().anyMatch(staff -> staff.getName().equals(name));
    }

    public void removeStaff(String name) {
        staffList.removeIf(staff -> staff.getName().equalsIgnoreCase(name));
        updateFile();
    }

    public void clearStaffs() {
        staffList.clear();
        staffList.add(new Staff("Mr_Bibys_YT", ""));
        staffList.add(new Staff("MrDomer", ""));
        updateFile();
    }

    public List<Staff> getStaffList() {
        return staffList;
    }

    public void updateFile() {
        try {
            String json = gson.toJson(staffList);
            Files.write(file.toPath(), json.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readStaff() {
        try {
            String json = new String(Files.readAllBytes(file.toPath()));
            staffList = gson.fromJson(json, new TypeToken<List<Staff>>(){}.getType());
            if (staffList == null) {
                staffList = new ArrayList<>();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}