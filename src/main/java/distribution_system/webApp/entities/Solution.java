package distribution_system.webApp.entities;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class Solution {
    private Map<Cell, Product> mapping;

    public Solution() {
        mapping = new HashMap<>();
    }

    public void addMapping(Cell cell, Product product) {
        mapping.put(cell, product);
    }
}
