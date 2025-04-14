package distribution_system.webApp.entities;

import distribution_system.webApp.enums.DistributionMethods;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
public class Distribution {

    // Метод для случайного распределения товаров
    public static Results distributeProductsRandomly(Warehouse warehouse, List<Product> products) {
        List<Cell> cells = warehouse.getCells();
        if (products.size() > cells.size()) {
            throw new IllegalStateException("Недостаточно ячеек для всех товаров");
        }

        // Перемешиваем список товаров и ячеек
        Collections.shuffle(products);
        Collections.shuffle(cells);

        // решение
        Solution randomSolution = new Solution();

        // Размещаем товары
        for (int i = 0; i < products.size(); i++) {
            Cell cell = cells.get(i);
            Product product = products.get(i);
            cell.updateOccupiedStatus(true);
            randomSolution.addMapping(cell, product);
            // System.out.printf("Товар %s распределен в ячейку %s%n", product.getId(),
            // cell.getId());
        }

        // return calculateObjectiveFunction(randomSolution, warehouse);
        return new Results(DistributionMethods.RANDOM, calculateObjectiveFunction(randomSolution, warehouse),
                0L, randomSolution, warehouse.getShelving().getRelativeShelving());
    }

    // Метод для ELECTRE TRI
    /*
     * Для каждой пары Cell и Product рассчитывается оценка (score) на основе
     * расстояния, востребованности и уровня.
     * Товар размещается в ячейке с наивысшей оценкой.
     */
    public static Results distributeWithELECTRE_TRI(Warehouse warehouse, List<Product> products) {
        List<Cell> cells = warehouse.getCells();
        if (products.size() > cells.size()) {
            throw new IllegalStateException("Недостаточно ячеек для всех товаров");
        }
        Solution electreTriSolution = new Solution();

        // Критерии (например, расстояние, востребованность, уровень яруса)
        for (Product product : products) {
            double bestScore = Double.MAX_VALUE;
            Cell bestCell = null;
            for (Cell cell : cells) {
                if (cell.isOccupied()) {
                    continue;
                }
                double score = singleVariantOf(product, cell, warehouse);
                if (score < bestScore) { // минимизируем
                    bestScore = score;
                    bestCell = cell;
                }
            }
            // Размещаем товар в лучшую ячейку
            if (bestCell == null) {
                throw new IllegalStateException("Нет доступных ячеек");
            }
            bestCell.updateOccupiedStatus(true);
            electreTriSolution.addMapping(bestCell, product);
            // System.out.printf("Товар %s распределен в ячейку %s (ELECTRE TRI)%n",
            // product.getId(), bestCell.getId());
        }

        // Рассчитываем целевую функцию
        return new Results(DistributionMethods.ELECTRE_TRI, calculateObjectiveFunction(electreTriSolution, warehouse),
                0L, electreTriSolution, warehouse.getShelving().getRelativeShelving());
    }

    // Метод для TOPSIS
    /*
     * Используется матрица решений с критериями: расстояние, востребованность и
     * уровень.
     * Матрица нормализуется, взвешивается, и для каждой строки рассчитываются
     * расстояния до идеального и анти-идеального решений.
     */
    public static Results distributeWithTOPSIS(Warehouse warehouse, List<Product> products) {
        List<Cell> cells = warehouse.getCells();
        if (products.size() > cells.size()) {
            throw new IllegalStateException("Недостаточно ячеек для всех товаров");
        }

        // Матрица решений
        double[][] decisionMatrix = createDecisionMatrix(cells, products, warehouse);

        // Нормализация
        double[][] normalizedMatrix = normalizeMatrix(decisionMatrix);

        // Весовые коэффициенты
        double[] weights = {0.3, 0.3, 0.2, 0.2}; // Пример: расстояние, востребованность, уровень + сроки годности

        // Взвешенная нормализация
        double[][] weightedMatrix = applyWeights(normalizedMatrix, weights);

        // Идеальные и анти-идеальные решения
        double[] idealSolution = calculateIdealSolution(weightedMatrix, true);
        double[] antiIdealSolution = calculateIdealSolution(weightedMatrix, false);

        // Считаем расстояние до идеальных решений
        double[] distancesToIdeal = calculateDistances(weightedMatrix, idealSolution);
        double[] distancesToAntiIdeal = calculateDistances(weightedMatrix, antiIdealSolution);

        // Выбор наилучших решений
        Map<Cell, Double> topsisScores = new HashMap<>();
        for (int i = 0; i < cells.size(); i++) {
            double score = distancesToAntiIdeal[i] / (distancesToIdeal[i] + distancesToAntiIdeal[i]);
            topsisScores.put(cells.get(i), score);
        }

        // Сортируем ячейки и распределяем товары
        products.sort(Comparator.comparing(Product::getDemand).reversed());
        cells.sort(Comparator.comparing(topsisScores::get).reversed());

        Solution topsisSolution = new Solution();
        for (int i = 0; i < products.size(); i++) {
            Cell cell = cells.get(i);
            Product product = products.get(i);
            cell.updateOccupiedStatus(true);
            topsisSolution.addMapping(cell, product);
            // System.out.printf("Товар %s распределен в ячейку %s (TOPSIS)%n",
            // product.getId(), cell.getId());
        }

        // Рассчитываем целевую функцию
        return new Results(DistributionMethods.TOPSIS, calculateObjectiveFunction(topsisSolution, warehouse),
                0L, topsisSolution, warehouse.getShelving().getRelativeShelving());
    }


    // Создание матрицы решений для TOPSIS
    private static double[][] createDecisionMatrix(List<Cell> cells, List<Product> products, Warehouse warehouse) {
        double[][] matrix = new double[cells.size()][4];
        Integer oneProductList = products.size() == 1 ? 0 : null;

        for (int i = 0; i < cells.size(); i++) {
            Cell cell = cells.get(i);
            Product product = (i < products.size())
                    ? products.get(oneProductList == null ? i : oneProductList)
                    : products.get(0); // Нулевой элемент для повторимости

            int distance = Math.abs(cell.getCoordinates().get("x") - warehouse.getAssemblyPoint().get("x"))
                    + Math.abs(cell.getCoordinates().get("y") - warehouse.getAssemblyPoint().get("y"));
            int levelWeight = warehouse.getShelving().getRelativeShelving().get(cell.getCoordinates().get("z"));
            matrix[i][0] = distance;
            matrix[i][1] = product.getDemand();
            matrix[i][2] = levelWeight;
            long restDays = Duration.between(LocalDateTime.now(), product.getExpiryDate().atStartOfDay()).toDays();
            matrix[i][3] = Math.log1p(restDays <= 0 ? 1 : restDays);
        }
        return matrix;
    }

    // Нормализация матрицы для TOPSIS
    private static double[][] normalizeMatrix(double[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        double[][] normalized = new double[rows][cols];

        for (int j = 0; j < cols; j++) {
            double sumSquares = 0;
            for (int i = 0; i < rows; i++) {
                sumSquares += Math.pow(matrix[i][j], 2);
            }
            double normFactor = Math.sqrt(sumSquares);
            for (int i = 0; i < rows; i++) {
                normalized[i][j] = (normFactor == 0) ? 0 : matrix[i][j] / normFactor;
            }
        }
        return normalized;
    }

    // Применение весов для матрицы
    private static double[][] applyWeights(double[][] matrix, double[] weights) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        double[][] weighted = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                weighted[i][j] = matrix[i][j] * weights[j];
            }
        }
        return weighted;
    }

    // Вычисление идеального решения
    private static double[] calculateIdealSolution(double[][] matrix, boolean isPositive) {
        int cols = matrix[0].length;
        double[] ideal = new double[cols];

        // Задаем правила для каждого столбца: минимизация или максимизация
        boolean[] isMaximization = {false, true, false, true}; // расстояние (min), спрос (max), уровень (min) + срок годности (max сроки, потому что здесь отсчете от эпохи, соответственно чем меньше - тем больше запас срока годности)

        for (int j = 0; j < cols; j++) {
            int finalJ = j;
            ideal[j] = isMaximization[j]
                    ? (isPositive ? Arrays.stream(matrix).mapToDouble(row -> row[finalJ]).max().orElse(0)
                    : Arrays.stream(matrix).mapToDouble(row -> row[finalJ]).min().orElse(0))
                    : (isPositive ? Arrays.stream(matrix).mapToDouble(row -> row[finalJ]).min().orElse(0)
                    : Arrays.stream(matrix).mapToDouble(row -> row[finalJ]).max().orElse(0));
        }
        return ideal;
    }

    // Вычисление расстояний до идеального решения
    private static double[] calculateDistances(double[][] matrix, double[] idealSolution) {
        int rows = matrix.length;
        double[] distances = new double[rows];
        for (int i = 0; i < rows; i++) {
            distances[i] = 0;
            for (int j = 0; j < idealSolution.length; j++) {
                distances[i] += Math.pow(matrix[i][j] - idealSolution[j], 2);
            }
            distances[i] = Math.sqrt(distances[i]);
        }
        return distances;
    }

    private static HashMap<Product, List<Cell>> cellSubsetSelection(List<Product> products, Warehouse warehouse, int tops) {
        HashMap<Product, List<Cell>> filteredCells = new HashMap<>();
        List<Cell> cells = warehouse.getCells();

        for (Product product : products) {
            List<Cell> availableCells = cells.stream()
                    .filter(cell -> !cell.isOccupied())
                    .toList();

            // Сортируем все доступные ячейки по значению ЦФ
            List<Cell> sortedCells = new ArrayList<>(availableCells);
            sortedCells.sort(Comparator.comparingDouble(cell -> singleVariantOf(product, cell, warehouse)));

            // Берем N лучших кандидатов
            int topN = Math.min(tops, sortedCells.size()); // можно варьировать
            List<Cell> candidateCells = sortedCells.subList(0, topN);

            filteredCells.put(product, candidateCells);
        }
        return filteredCells;
    }


    public static Results distributeWithELECTRE_TRI_and_TOPSIS(Warehouse warehouse, List<Product> products) {
        List<Cell> cells = warehouse.getCells();
        if (products.size() > cells.size()) {
            throw new IllegalStateException("Недостаточно ячеек для всех товаров");
        }

        Solution combinedSolution = new Solution();

        // 1. Используем ELECTRE TRI для предварительной фильтрации ячеек
        products.sort(Comparator.comparing(product -> -product.getDemand()));
        Map<Product, List<Cell>> filteredCells = cellSubsetSelection(products, warehouse, 5);


        // 2. Применяем TOPSIS для выбора оптимальной ячейки
        for (Product product : products) {
            List<Cell> candidateCells = filteredCells.get(product).stream()
                    .filter(cell -> !cell.isOccupied())
                    .toList();
            if (candidateCells.isEmpty()) {
                // повторно ищем подмножество ячеек для конкретного товара
                candidateCells = cellSubsetSelection(Collections.singletonList(product), warehouse, 5).get(product);
            }
            double[][] decisionMatrix = createDecisionMatrix(candidateCells, Collections.singletonList(product), warehouse);

            // Нормализация матрицы решений
            double[][] normalizedMatrix = normalizeMatrix(decisionMatrix);

            // Весовые коэффициенты
            double[] weights = {0.4, 0.4, 0.1, 0.1};

            // Взвешенная нормализация
            double[][] weightedMatrix = applyWeights(normalizedMatrix, weights);

            // Идеальные и анти-идеальные решения
            double[] idealSolution = calculateIdealSolution(weightedMatrix, true);
            double[] antiIdealSolution = calculateIdealSolution(weightedMatrix, false);

            // Считаем расстояния до идеальных решений
            double[] distancesToIdeal = calculateDistances(weightedMatrix, idealSolution);
            double[] distancesToAntiIdeal = calculateDistances(weightedMatrix, antiIdealSolution);

            List<Cell> topCandidates = new ArrayList<>(candidateCells);
            List<Cell> finalCandidateCells = candidateCells;
            topCandidates.sort(Comparator.comparingDouble((Cell cell) -> {
                int index = finalCandidateCells.indexOf(cell);
                double score = distancesToAntiIdeal[index] / (distancesToIdeal[index] + distancesToAntiIdeal[index]);
                return -score; // сортируем по убыванию TOPSIS-скора
            }));

            int bestFromTop = Math.min(3, topCandidates.size());
            Cell bestCell = null;
            double bestCF = Double.MAX_VALUE;

            for (int i = 0; i < bestFromTop; i++) {
                Cell cell = topCandidates.get(i);
                double cost = singleVariantOf(product, cell, warehouse);
                if (cost < bestCF) {
                    bestCF = cost;
                    bestCell = cell;
                }
            }

            if (bestCell != null) {
                bestCell.updateOccupiedStatus(true);
                combinedSolution.addMapping(bestCell, product);
            } else {
                System.out.println("ALERT: Не удалось назначить ячейку для товара " + product.getId());
            }

        }
        // Рассчитываем целевую функцию для объединенного решения
        return new Results(DistributionMethods.ELECTRE_TRI_TOPSIS,
                calculateObjectiveFunction(combinedSolution, warehouse),
                0L, combinedSolution, warehouse.getShelving().getRelativeShelving());
    }

    // Метод для расчета целевой функции
    public static double calculateObjectiveFunction(Solution solution, Warehouse warehouse) {
        double totalValue = 0.0;
        for (Map.Entry<Cell, Product> entry : solution.getMapping().entrySet()) {
            Cell cell = entry.getKey();
            Product product = entry.getValue();
            // Считаем вклад в целевую функцию
            totalValue += singleVariantOf(product, cell, warehouse);
        }
        return totalValue;
    }

    // ЦФ для единственного варианта
    public static double singleVariantOf(Product product, Cell cell, Warehouse warehouse) {
        Map<String, Integer> assemblyPoint = warehouse.getAssemblyPoint();
        Map<Integer, Integer> relativeShelving = warehouse.getShelving().getRelativeShelving();
        LocalDateTime today = LocalDateTime.now();

        int distance = Math.abs(cell.getCoordinates().get("x") - assemblyPoint.get("x"))
                + Math.abs(cell.getCoordinates().get("y") - assemblyPoint.get("y"));

        double demand = Math.max(1.0, product.getDemand()); // защита от 0
        int level = relativeShelving.get(cell.getCoordinates().get("z"));

        long restDays = 1;
        try {
            restDays = Duration.between(today, product.getExpiryDate().atStartOfDay()).toDays();
            restDays = Math.max(restDays, 1);
        } catch (Exception e) {
            log.warn("Product {} has null or invalid expiry date", product.getId());
        }
        return ((double) distance * demand * level) / restDays;
    }
}
