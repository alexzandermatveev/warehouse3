package distribution_system.webApp.entities;

import java.util.HashMap;
import java.util.Map;


public class Shelving {

    public static final int SHOULDER_LEVEL_HEIGHT = 150; // уровень плеча, см
    public static final int HUMAN_LOWER_BOUND = 116; //нижняя граница грудной клетки среднего мужчины
    public final Map<Integer, Integer> relativeLevels;

    public Shelving(int cellHeight, int levels) {
        relativeLevels = calculateRelativeLevel(cellHeight, levels);
    }

    public static Map<Integer, Integer> calculateRelativeLevel(int cellHeight, int levels) {
        double maxRate = 0;
        int bestLevel = 1;
        for (int i = 0; i < levels; i++) {
            int cellBottom = i * cellHeight; // i начинается с нуля это как бы i-1, т.к. высота стенки не учитывается
            int cellTop = (i + 1) * cellHeight; // здесь + высота, т.к. это расстояние до верха
            if (HUMAN_LOWER_BOUND <= cellBottom && cellBottom <= SHOULDER_LEVEL_HEIGHT) {
                // если дном попал в промежуток
                double currentRate = (double) (SHOULDER_LEVEL_HEIGHT - cellBottom) / (SHOULDER_LEVEL_HEIGHT - HUMAN_LOWER_BOUND);
                if (maxRate < currentRate) {
                    maxRate = currentRate;
                    bestLevel = i;
                }
            } else if (HUMAN_LOWER_BOUND <= cellTop && cellTop <= SHOULDER_LEVEL_HEIGHT) {
                // если вершиной попал
                double currentRate = (double) (SHOULDER_LEVEL_HEIGHT - cellTop) / (SHOULDER_LEVEL_HEIGHT - HUMAN_LOWER_BOUND);
                if (maxRate < currentRate) {
                    maxRate = currentRate;
                    bestLevel = i;
                }
            }
        }
        Map<Integer, Integer> newRelLevels = new HashMap<>();
        for (int i = 1; i <= levels; i++) {
            newRelLevels.put(i, Math.abs(i - bestLevel) + 1); // ярус и относительный уровень по удобству (чем меньше - тем удобнее)
        }
        return newRelLevels;
    }
}
