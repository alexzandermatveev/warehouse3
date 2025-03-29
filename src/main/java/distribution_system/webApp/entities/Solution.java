package distribution_system.webApp.entities;

import lombok.Getter;

import java.util.*;

@Getter
public class Solution {
    protected Map<Cell, Product> mapping;

    public Solution() {
        mapping = new HashMap<>();
    }

    public void addMapping(Cell cell, Product product) {
        mapping.put(cell, product);
    }

    public TreeSet<Pair> prepareForJSON() {
        TreeSet<Pair> pairs = new TreeSet<>(new Comparator<Pair>() {
            @Override
            public int compare(Pair pair1, Pair pair2) {
                return pair1.getCell().compareTo(pair2.getCell());
            }
        });
        long i = 1;
        for (Map.Entry<Cell, Product> entry : mapping.entrySet()) {
            pairs.add(new Pair("Pair" + i++, entry.getKey(), entry.getValue()));
        }
        if (pairs.size() == mapping.size())
            return pairs;
        else
            throw new RuntimeException("разные размеры итоговых решений");
    }
}

