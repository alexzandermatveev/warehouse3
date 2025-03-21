package distribution_system.webApp.entities;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.*;

@Getter
@ToString
public class Cell implements Cloneable, Comparable<Cell> {
    private String id;
    private Map<String, Integer> coordinates;
    private boolean occupied;
    private LocalDateTime lastLoaded;
    private int rotationPeriod;


//    @JsonValue
//    public String toJson(){
//        return id;
//    }

    public Cell(String id, int x, int y, int z, int rotationPeriod) {
        this.id = id;
        this.coordinates = Map.of("x", x, "y", y, "z", z);
        this.rotationPeriod = rotationPeriod;
        this.occupied = false;
    }



    public void updateOccupiedStatus(boolean occupied) {
        this.occupied = occupied;
        this.lastLoaded = occupied ? LocalDateTime.now() : null;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("coordinates", coordinates);
        map.put("occupied", occupied);
        map.put("lastLoaded", lastLoaded != null ? lastLoaded.toString() : null);
        map.put("rotationPeriod", rotationPeriod);
        return map;
    }

    public static Cell fromMap(Map<String, Object> map) {
        Map<String, Integer> coordinates = (Map<String, Integer>) map.get("coordinates");
        Cell cell = new Cell(
                (String) map.get("id"),
                coordinates.get("x"),
                coordinates.get("y"),
                coordinates.get("z"),
                (int) map.get("rotationPeriod")
        );
        cell.occupied = (boolean) map.get("occupied");
        if (map.get("lastLoaded") != null) {
            cell.lastLoaded = LocalDateTime.parse((String) map.get("lastLoaded"));
        }
        return cell;
    }

    // Метод для генерации ячеек
    public static List<Cell> generateCells(long count, int levels) {
        List<Cell> cells = new ArrayList<>();
        Random random = new Random();
        int coordinateCounter = (int) Math.ceil(Math.sqrt((double) count / levels));
        int i = 1;
        for (int level = 1; level <= levels; level++) {
            for (int x = 1; x <= coordinateCounter; x++) {
                for (int y = 1; y <= coordinateCounter; y++) {
                    if (i <= count) {
                        cells.add(new Cell(
                                "C" + i,
                                x,
                                y,
                                level,
                                random.nextInt(100) + 1
                        ));
                        i++;
                    } else {
                        return cells;
                    }
                }
            }
        }
        return cells;
    }

    @Override
    public Cell clone() {
        try {
            return (Cell) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cell cell = (Cell) o;
        return Objects.equals(id, cell.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }


    @Override
    public int compareTo(Cell o) {
        int id = Integer.parseInt(this.id.substring(1));
        int anotherId = Integer.parseInt(o.id.substring(1));

        return Integer.compare(id, anotherId);
    }
}
