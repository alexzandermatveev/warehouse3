package distribution_system.webApp.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import distribution_system.webApp.entities.*;
import distribution_system.webApp.enums.DistributionMethods;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class DistributionService {

    private final ObjectMapper objectMapper;


    public List<Results> preDistributionMethod(JsonNode requestJson, boolean generateCells, long cellsAmount,
                                               boolean generateProducts, long productsAmount) {

        List<DistributionMethods> methods;
        Warehouse warehouse;
        List<Product> products;
        try {
            // Получаем список методов
            methods = objectMapper.treeToValue(requestJson.get("methods"), new TypeReference<List<DistributionMethods>>() {});

            // Получаем конфигурацию склада и мэппим в Warehouse
            warehouse = objectMapper.treeToValue(requestJson.get("warehouseConfig").get("Warehouse"), Warehouse.class);
            products = objectMapper.treeToValue(requestJson.get("warehouseConfig").get("products"), new TypeReference<List<Product>>() {});

        } catch (Exception e) {
            throw new RuntimeException("Ошибка обработки JSON-запроса", e);
        }
        if (methods == null || methods.isEmpty()) throw new RuntimeException("no method");

        if (warehouse != null) {
            if (generateProducts) {
                if (productsAmount > 0)
                    if (products == null || products.isEmpty())
                        products = Product.generateProducts(productsAmount);
                    else
                        products = Product.generateByFirst(productsAmount, products.get(0));
                else throw new RuntimeException("вне разрешенного диапазона кол-во продуктов");
            }
            if (generateCells) {
                if (cellsAmount > 0) {
                    warehouse.setTotalCells(cellsAmount);
                    warehouse.generateCells();
                } else throw new RuntimeException("вне разрешенного диапазона кол-во ячеек");
            } else if (warehouse.getCells() == null || warehouse.getCells().isEmpty()) {
                throw new RuntimeException("список cells пуст. Сгенерируйте или укажите в файле конфигураций");
            }
            if (warehouse.getCells().size() != warehouse.getTotalCells())
                warehouse.setTotalCells(warehouse.getCells().size());
            return processDistribution(methods, warehouse, products);
        }
        throw new RuntimeException("ошибка при создании Warehouse. Проверьте файл конфигураций");
    }

    public List<Results> processDistribution(List<DistributionMethods> request, Warehouse warehouse, List<Product> products) {

//        warehouse.generateCells();
        List<Results> resultsList = new ArrayList<>();
        request.forEach(method -> {
            long startTime = System.currentTimeMillis();
            Results results = switch (method) {
                case RANDOM -> {
                    Results results1 = Distribution.distributeProductsRandomly(Warehouse.copy(warehouse), products);
                    results1.setTimeRequired(System.currentTimeMillis() - startTime);
                    yield results1;
                }
                case ELECTRE_TRI -> {
                    Results results1 = Distribution.distributeWithELECTRE_TRI(Warehouse.copy(warehouse), new ArrayList<>(products));
                    results1.setTimeRequired(System.currentTimeMillis() - startTime);
                    yield results1;
                }
                case TOPSIS -> {
                    Results results1 = Distribution.distributeWithTOPSIS(Warehouse.copy(warehouse),new ArrayList<>(products));
                    results1.setTimeRequired(System.currentTimeMillis() - startTime);
                    yield results1;
                }
                case ELECTRE_TRI_TOPSIS -> {
                    Results results1 = Distribution.distributeWithELECTRE_TRI_and_TOPSIS(Warehouse.copy(warehouse), new ArrayList<>(products));
                    results1.setTimeRequired(System.currentTimeMillis() - startTime);
                    yield results1;
                }
                case GA -> {
                    GeneticSolution solution = GeneticSolution.runGeneticAlgorithm(Warehouse.copy(warehouse), new ArrayList<>(products), 5, 100);
                    yield new Results(DistributionMethods.GA, solution.getFitness(),
                            System.currentTimeMillis() - startTime, solution);
                }
            };
            resultsList.add(results);
        });
        return resultsList;
    }
}
