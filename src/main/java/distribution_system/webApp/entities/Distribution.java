package distribution_system.webApp.entities;

import distribution_system.webApp.enums.DistributionMethods;

import java.util.*;


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

        //решение
        Solution randomSolution = new Solution();

        // Размещаем товары
        for (int i = 0; i < products.size(); i++) {
            Cell cell = cells.get(i);
            Product product = products.get(i);
            cell.updateOccupiedStatus(true);
            randomSolution.addMapping(cell, product);
//            System.out.printf("Товар %s распределён в ячейку %s%n", product.getId(), cell.getId());
        }

//        return calculateObjectiveFunction(randomSolution, warehouse);
        return new Results(DistributionMethods.RANDOM, calculateObjectiveFunction(randomSolution, warehouse),
                0L, randomSolution, warehouse.getShelving().getRelativeShelving());
    }

    // Метод для ELECTRE TRI
    /*
    Для каждой пары Cell и Product рассчитывается оценка (score) на основе расстояния, востребованности и уровня.
    Товар размещается в ячейке с наивысшей оценкой.
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
                double score = calculateELECTREScore(cell, product, warehouse);
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
//            System.out.printf("Товар %s распределён в ячейку %s (ELECTRE TRI)%n", product.getId(), bestCell.getId());
        }

        // Рассчитываем целевую функцию
        return new Results(DistributionMethods.ELECTRE_TRI, calculateObjectiveFunction(electreTriSolution, warehouse),
                0L, electreTriSolution, warehouse.getShelving().getRelativeShelving());
    }

    // Метод для TOPSIS
    /*
    Используется матрица решений с критериями: расстояние, востребованность и уровень.
    Матрица нормализуется, взвешивается, и для каждой строки рассчитываются расстояния до идеального и анти-идеального решений.
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
        double[] weights = {0.4, 0.4, 0.2}; // Пример: расстояние, востребованность, уровень

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
//            System.out.printf("Товар %s распределён в ячейку %s (TOPSIS)%n", product.getId(), cell.getId());
        }

        // Рассчитываем целевую функцию
        return new Results(DistributionMethods.TOPSIS, calculateObjectiveFunction(topsisSolution, warehouse),
                0L, topsisSolution, warehouse.getShelving().getRelativeShelving());
    }

    // Вспомогательный метод для ELECTRE TRI
    private static double calculateELECTREScore(Cell cell, Product product, Warehouse warehouse) {
        int distance = Math.abs(cell.getCoordinates().get("x") - warehouse.getAssemblyPoint().get("x"))
                + Math.abs(cell.getCoordinates().get("y") - warehouse.getAssemblyPoint().get("y"));
        int levelWeight = warehouse.getShelving().getRelativeShelving().get(cell.getCoordinates().get("z"));
        // По сути цф из метода по расчету цф, но для конкретной пары продукт-ячейка
        return product.getDemand() * distance * 0.3 * levelWeight;
    }

    // Создание матрицы решений для TOPSIS
    private static double[][] createDecisionMatrix(List<Cell> cells, List<Product> products, Warehouse
            warehouse) {
        double[][] matrix = new double[cells.size()][3];
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
        }
        return matrix;
    }

    // Нормализация матрицы для TOPSIS
    private static double[][] normalizeMatrix(double[][] matrix) {
        int cols = matrix[0].length;
        for (int j = 0; j < cols; j++) {
            double sumSquares = 0;
            for (double[] doubles : matrix) {
                sumSquares += Math.pow(doubles[j], 2);
            }
            double normFactor = Math.sqrt(sumSquares);
            for (double[] doubles : matrix) {
                doubles[j] = doubles[j] / normFactor;
            }
        }
        return matrix;
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

        // Задаём правила для каждого столбца: минимизация или максимизация
        boolean[] isMaximization = {false, true, false}; // расстояние (min), спрос (max), уровень (min)

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

    private static double getElectreThreshold(List<Cell> cells, Product product, Warehouse warehouse, double coefficient) {
        // Здесь коэффициент имеет решающее значение. Чем он меньше,
        // тем точнее должны быть подобраны ячейки, но есть риск, что отобранных ячеек окажется меньше необходимого
        return coefficient * cells.stream()
                .map(cell -> calculateELECTREScore(cell, product, warehouse))
                .max(Double::compareTo)
                .orElse(Double.MAX_VALUE); // т.к. это минимальный порог
    }


    public static Results distributeWithELECTRE_TRI_and_TOPSIS(Warehouse warehouse, List<Product> products) {
        List<Cell> cells = warehouse.getCells();
        if (products.size() > cells.size()) {
            throw new IllegalStateException("Недостаточно ячеек для всех товаров");
        }

        Solution combinedSolution = new Solution();

        // 1. Используем ELECTRE TRI для предварительной фильтрации ячеек
        Map<Product, List<Cell>> filteredCells = new HashMap<>();
        for (Product product : products) {
            List<Cell> candidateCells = new ArrayList<>();
            double thresholdCoefficient = 0.2;
            do {
                thresholdCoefficient += 0.1; // каждую итерацию повышаем предел для большего отбора ячеек
                candidateCells = new ArrayList<>();
                for (Cell cell : cells) {
                    if (!cell.isOccupied()) {
                        double score = calculateELECTREScore(cell, product, warehouse);
                        if (score < getElectreThreshold(cells, product, warehouse, thresholdCoefficient)) { // пороговое значения для ELECTRE TRI
                            candidateCells.add(cell);
                        }
                    }
                }
            } while (candidateCells.isEmpty());
//            if (candidateCells.isEmpty()) {
//                throw new IllegalStateException("Для товара " + product.getId() + " не найдены подходящие ячейки (ELECTRE TRI).");
//            }
            filteredCells.put(product, candidateCells);
        }

        // 2. Применяем TOPSIS для выбора оптимальной ячейки
        for (Product product : products) {
            List<Cell> candidateCells = filteredCells.get(product).stream()
                    .filter(cell -> !cell.isOccupied())
                    .toList();
            double[][] decisionMatrix = createDecisionMatrix(candidateCells, List.of(product), warehouse);

            // Нормализация матрицы решений
            double[][] normalizedMatrix = normalizeMatrix(decisionMatrix);

            // Весовые коэффициенты
            double[] weights = {0.4, 0.4, 0.2}; // Примерные веса критериев

            // Взвешенная нормализация
            double[][] weightedMatrix = applyWeights(normalizedMatrix, weights);

            // Идеальные и анти-идеальные решения
            double[] idealSolution = calculateIdealSolution(weightedMatrix, true);
            double[] antiIdealSolution = calculateIdealSolution(weightedMatrix, false);

            // Считаем расстояния до идеальных решений
            double[] distancesToIdeal = calculateDistances(weightedMatrix, idealSolution);
            double[] distancesToAntiIdeal = calculateDistances(weightedMatrix, antiIdealSolution);

            // Выбираем ячейку с максимальным TOPSIS-скором
            double bestScore = Double.MAX_VALUE;
            Cell bestCell = null;
            for (int i = 0; i < candidateCells.size(); i++) {
                double score = distancesToAntiIdeal[i] / (distancesToIdeal[i] + distancesToAntiIdeal[i]);
                if (score < bestScore) { // минимизируем
                    bestScore = score;
                    bestCell = candidateCells.get(i);
                }
            }

            if (bestCell != null) {
                bestCell.updateOccupiedStatus(true);
                combinedSolution.addMapping(bestCell, product);
//                System.out.printf("Товар %s распределён в ячейку %s (ELECTRE TRI + TOPSIS)%n", product.getId(), bestCell.getId());
            }
        }

        // Рассчитываем целевую функцию для объединённого решения
        return new Results(DistributionMethods.ELECTRE_TRI_TOPSIS, calculateObjectiveFunction(combinedSolution, warehouse),
                0L, combinedSolution, warehouse.getShelving().getRelativeShelving());
    }


    // Метод для расчёта целевой функции
    public static double calculateObjectiveFunction(Solution solution, Warehouse warehouse) {
        double totalValue = 0.0;
        Map<String, Integer> assemblyPoint = warehouse.getAssemblyPoint();
        Map<Integer, Integer> relativeShelving = warehouse.getShelving().getRelativeShelving();

        for (Map.Entry<Cell, Product> entry : solution.getMapping().entrySet()) {
            Cell cell = entry.getKey();
            Product product = entry.getValue();

            // Расстояние до точки сборки (манхеттенское расстояние)
            int distance = Math.abs(cell.getCoordinates().get("x") - assemblyPoint.get("x"))
                    + Math.abs(cell.getCoordinates().get("y") - assemblyPoint.get("y"));

            // Уровень востребованности
            int demand = product.getDemand();

            // Считаем вклад в целевую функцию
            totalValue += demand * distance * 0.3 * relativeShelving.get(cell.getCoordinates().get("z"));
        }
        return totalValue;
    }
}
