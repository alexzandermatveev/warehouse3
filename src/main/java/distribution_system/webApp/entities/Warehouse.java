package distribution_system.webApp.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class Warehouse {

    private final String id;
    private final Map<String, Integer> assemblyPoint;
    private final Map<String, Integer> cellSize;
    private final int levels;
    private final Shelving shelving;
    private final long totalCells;

    private static final ObjectMapper objectMapper = new ObjectMapper();


    @Getter
    private List<Cell> cells;

    @JsonCreator
    public Warehouse(String id, Map<String, Integer> assemblyPoint,
                     Map<String, Integer> cellSize, int levels, long totalCells) {
        this.id = id;
        this.assemblyPoint = assemblyPoint;
        this.cells = new ArrayList<>();
        this.cellSize = cellSize;
        this.levels = levels;
        shelving = new Shelving(cellSize.get("height"), levels);
        this.totalCells = totalCells;

    }

    public void addCell(Cell cell) {
        this.cells.add(cell);
    }

    public void addCells(List<Cell> cells) {
        this.cells.addAll(cells);
    }

    public void generateCells(){
        this.cells.addAll(Cell.generateCells(totalCells, levels));
    }

    public String toJson(Warehouse warehouse) {
        try {
            return objectMapper.writeValueAsString(warehouse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка парсинга Warehouse в JSON ", e);
        }
    }

    public static Warehouse fromJson(String jsonString) {
        try {
            return objectMapper.readValue(jsonString, Warehouse.class);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка парсинга JSON в Warehouse", e);
        }
    }

    public static Warehouse copy(Warehouse warehouse) {
        Warehouse copy = new Warehouse(
                warehouse.getId(),
                new HashMap<String, Integer>(warehouse.assemblyPoint),
                new HashMap<String, Integer>(warehouse.cellSize),
                warehouse.levels,
                warehouse.getTotalCells()
        );
        copy.cells = warehouse.getCells().stream()
                .map(Cell::clone)
                .collect(Collectors.toCollection(ArrayList::new));
        return copy;
    }

}
