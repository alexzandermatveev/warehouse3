package distribution_system.webApp.services;

import distribution_system.webApp.entities.*;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
public class GeneticSolution extends Solution {
    private Map<Cell, Product> mapping;
    @Setter
    private double fitness;

    public GeneticSolution() {
        this.mapping = new HashMap<>();
        this.fitness = Double.MAX_VALUE;
    }

    public void addMapping(Cell cell, Product product) {
        mapping.put(cell, product);
    }

    public void clearMapping() {
        mapping = new HashMap<>();
    }

    public void replaceFromMapping(Cell cell, Product product) {
        mapping.replace(cell, product);
    }

    private static void trainAndSort(List<GeneticSolution> population, Warehouse warehouse) {
        // Оценка пригодности
        for (GeneticSolution solution : population) {
            double fitness = Distribution.calculateObjectiveFunction(solution, warehouse);
            solution.setFitness(fitness);
        }

        // Сортировка по пригодности (меньше — лучше)
        population.sort(Comparator.comparingDouble(GeneticSolution::getFitness));
    }

    public static GeneticSolution runGeneticAlgorithm(Warehouse warehouse, List<Product> products, int populationSize, int generations) {
        if (populationSize < 2) {
            throw new RuntimeException("Популяция должна быть как минимум 2");
        }

        List<GeneticSolution> population = initializePopulation(warehouse, products, populationSize);

        for (int generation = 0; generation < generations; generation++) {

            trainAndSort(population, warehouse);

            // Вывод лучшего решения на текущем поколении
//            if (generation % 1000 == 0) {
//                System.out.printf("Поколение %d: Лучший фитнес = %.2f%n", generation, population.get(0).getFitness());
//            }
            // Отбор лучших решений (простой выбор двух лучших родителей)
            List<GeneticSolution> parents = List.of(population.get(0), population.get(1));

            // Отбор лучших решений (сложный выбор)
//            List<GeneticSolution> parents = selectParents(population, 5);

            // Создание нового поколения
            List<GeneticSolution> nextGeneration = new ArrayList<>();
            while (nextGeneration.size() < populationSize) {
                // Скрещивание
                GeneticSolution parent1 = parents.get(new Random().nextInt(parents.size()));
                GeneticSolution parent2 = parents.get(new Random().nextInt(parents.size()));
                GeneticSolution child = crossover(parent1, parent2);

                // Мутация
                mutate(child, 0.8);

                nextGeneration.add(child);
            }

            // Замена старого поколения
            population = nextGeneration;
        }

        trainAndSort(population, warehouse);

        // Возвращаем лучшее решение
        return population.get(0);
    }


    private static List<GeneticSolution> initializePopulation(Warehouse warehouse, List<Product> products, int populationSize) {
        List<GeneticSolution> population = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < populationSize; i++) {
            GeneticSolution solution = new GeneticSolution();
            List<Cell> cells = new ArrayList<>(warehouse.getCells());
            Collections.shuffle(cells);

            // Гарантируем уникальность товаров
            Set<Product> usedProducts = new HashSet<>();
            for (int j = 0; j < products.size(); j++) {
                if (!usedProducts.contains(products.get(j))) {
                    solution.addMapping(cells.get(j), products.get(j));
                    usedProducts.add(products.get(j));
                }
            }

            population.add(solution);
        }
        return population;
    }


    // Кастомный класс для Map.Entry
    static class CustomEntry<K, V> implements Map.Entry<K, V> {
        private final K key;
        private final V value;

        public CustomEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException("Immutable entry");
        }

        // Переопределяем equals и hashCode для уникальности
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CustomEntry<?, ?> that = (CustomEntry<?, ?>) o;
            return Objects.equals(key, that.key) && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value);
        }

        @Override
        public String toString() {
            return "CustomEntry{" + "key=" + key + ", value=" + value + '}';
        }
    }

    private static GeneticSolution crossover(GeneticSolution parent1, GeneticSolution parent2) {
        GeneticSolution child = new GeneticSolution();
        Random random = new Random();
        // Сортируем родителей по ключам (ячейкам)
        List<Map.Entry<Cell, Product>> sortedParent1 = new ArrayList<>(parent1.getMapping().entrySet());
        List<Map.Entry<Cell, Product>> sortedParent2 = new ArrayList<>(parent2.getMapping().entrySet());

        //сортируем по товару, так как его меньше чем возможных ячеек
        sortedParent1.sort(Map.Entry.comparingByValue());
        sortedParent2.sort(Map.Entry.comparingByValue());

        int crossoverPoint = random.nextInt(sortedParent1.size());

        HashSet<Cell> hashCells = new HashSet<>();
        HashSet<Product> hashProducts = new HashSet<>();


        boolean firstOrder = crossoverPoint > sortedParent1.size() / 2;

        for (int i = 0; i < sortedParent1.size(); i++) {
            Cell cell;
            Product product;
            if (i <= crossoverPoint) {
                cell = firstOrder ? sortedParent1.get(i).getKey() : sortedParent2.get(i).getKey();
                product = firstOrder ? sortedParent1.get(i).getValue() : sortedParent2.get(i).getValue();
            } else {
                cell = firstOrder ? sortedParent2.get(i).getKey() : sortedParent1.get(i).getKey();
                product = firstOrder ? sortedParent2.get(i).getValue() : sortedParent1.get(i).getValue();
            }

            // Добавляем только уникальные пары
            if (!hashCells.contains(cell) && !hashProducts.contains(product)) {
                child.addMapping(cell, product);
                hashCells.add(cell);
                hashProducts.add(product);
            } else {
                // получается, не попали с ячейкой, так как товары отсортированы
                if (i > crossoverPoint) { // инвертируем условие
                    cell = firstOrder ? sortedParent1.get(i).getKey() : sortedParent2.get(i).getKey();
                    product = firstOrder ? sortedParent1.get(i).getValue() : sortedParent2.get(i).getValue();
                } else {
                    cell = firstOrder ? sortedParent2.get(i).getKey() : sortedParent1.get(i).getKey();
                    product = firstOrder ? sortedParent2.get(i).getValue() : sortedParent1.get(i).getValue();
                }
                // Добавляем только уникальные пары
                if (!hashCells.contains(cell) && !hashProducts.contains(product)) {
                    child.addMapping(cell, product);
                    hashCells.add(cell);
                    hashProducts.add(product);
                } else {
                    //если и обратная ячейка занята, то дополняем из оставшихся
                    HashSet<Cell> remainedCells = new HashSet<>(sortedParent1.stream().map(Map.Entry::getKey).toList());
                    remainedCells.addAll(sortedParent2.stream().map(Map.Entry::getKey).toList());

                    HashSet<Product> remainedProducts = new HashSet<>(sortedParent1.stream().map(Map.Entry::getValue).toList());
                    remainedProducts.addAll(sortedParent2.stream().map(Map.Entry::getValue).toList());

                    remainedCells.removeAll(hashCells);
                    remainedProducts.removeAll(hashProducts);
                    for (Product aProduct : remainedProducts) {
                        for (Cell aCell : remainedCells) {
                            if (!hashCells.contains(aCell) && !hashProducts.contains(aProduct)) {
                                int oldSize = child.getMapping().size(); // изменится ли размер child map, т.к. это hashMap
                                // тут перезапись происходит в сете
                                Product oldProduct = child.getMapping().get(aCell);
                                child.addMapping(aCell, aProduct);
                                if (child.getMapping().size() > oldSize) {
                                    //если добавился в hashMap - засчитываем итерацию, перебираем следующий товар
                                    hashCells.add(aCell);
                                    hashProducts.add(aProduct);
                                    i++;
                                    break;
                                } else {
                                    // Возвращаем на место при неудачной перезаписи
                                    child.addMapping(aCell, oldProduct);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (child.getMapping().size() < sortedParent1.size()) {
            System.out.printf("Добавлено пар: %d%n", child.getMapping().size());
            System.out.printf("Осталось товаров: %d%n", hashProducts.size());
            System.out.printf("Осталось ячеек: %d%n", hashCells.size());
            child.getMapping().forEach((cell, product) ->
                    System.out.printf("Ячейка: %s, Товар: %s%n", cell.getId(), product.getId()));

            throw new RuntimeException("ребенок меньше родителя");
        }
        return child;
    }

    private static void mutate(GeneticSolution solution, double mutationRate) {
        Random random = new Random();

        // Пороговое значение для совершения мутации
        if (random.nextDouble() > mutationRate) return;

        List<Map.Entry<Cell, Product>> entries = new ArrayList<>(solution.getMapping().entrySet());
        HashSet<CustomEntry<Cell, Product>> customEntries = new HashSet<>();
        for (Map.Entry<Cell, Product> entry : entries) {
            customEntries.add(new CustomEntry<Cell, Product>(entry.getKey(), entry.getValue()));
        }


        List<CustomEntry<Cell, Product>> customEntryList = new ArrayList<>(customEntries);
        CustomEntry<Cell, Product> entry1;
        CustomEntry<Cell, Product> entry2;
        do {
            int random1 = random.nextInt(entries.size());
            int random2 = random.nextInt(entries.size());
            entry1 = customEntryList.get(random1);
            entry2 = customEntryList.get(random2);
        } while (entry1.value == entry2.value);


        customEntries.remove(entry1);
        customEntries.remove(entry2);

        CustomEntry<Cell, Product> newEntry1 = new CustomEntry<>(entry1.key, entry2.value);
        CustomEntry<Cell, Product> newEntry2 = new CustomEntry<>(entry2.key, entry1.value);

        customEntries.add(newEntry1);
        customEntries.add(newEntry2);

        if (customEntryList.size() != customEntries.size()) {
            throw new RuntimeException("Сет меньшей размерности чем оригинал");
        }
        // Перезаписываем решение
        solution.clearMapping();
        for (CustomEntry<Cell, Product> entry : customEntries) {
            solution.addMapping(entry.key, entry.value);
        }
    }

    private static List<GeneticSolution> selectParents(List<GeneticSolution> population, int numParents) {
        List<GeneticSolution> rankedPopulation = new ArrayList<>(population);

        Random random = new Random();
        List<GeneticSolution> parents = new ArrayList<>();
        Set<Integer> selectedIndices = new HashSet<>(); // Уникальность родителей

        double[] probabilities = new double[rankedPopulation.size()];
        double total = 0.0;

        // Создаём массив вероятностей выбора
        for (int i = 0; i < rankedPopulation.size(); i++) {
            probabilities[i] = 1.0 / (i + 1); // Чем выше ранг, тем больше вероятность
            total += probabilities[i];
        }

        // Нормализуем вероятности
        for (int i = 0; i < probabilities.length; i++) {
            probabilities[i] /= total;
        }

        // Выбор родителей
        while (parents.size() < numParents) {
            double r = random.nextDouble();
            double cumulativeProbability = 0.0;
            for (int i = 0; i < probabilities.length; i++) {
                cumulativeProbability += probabilities[i];
                if (r <= cumulativeProbability && !selectedIndices.contains(i)) {
                    parents.add(rankedPopulation.get(i));
                    selectedIndices.add(i);
                    break;
                }
            }
        }
        return parents;
    }


}

