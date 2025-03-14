package distribution_system.webApp.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import distribution_system.webApp.entities.Product;
import distribution_system.webApp.entities.Results;
import distribution_system.webApp.entities.Warehouse;
import distribution_system.webApp.enums.DistributionMethods;
import distribution_system.webApp.services.DistributionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DistributionController {

    private final ObjectMapper objectMapper;
    private final DistributionService distributionService;


    @PostMapping("/distribute")
    public List<Results> getDistribution(@RequestBody JsonNode requestJson,
                                         @RequestParam(defaultValue = "false") boolean generateCells,
                                         @RequestParam(defaultValue = "1") long cellsAmount,
                                         @RequestParam(defaultValue = "false") boolean generateProducts,
                                         @RequestParam(defaultValue = "1") long productsAmount) {
        try {
            // Получаем список методов
            List<DistributionMethods> methods = objectMapper.convertValue(requestJson.get("methods"), List.class);

            // Получаем конфигурацию склада и мэппим в Warehouse
            Warehouse warehouse = objectMapper.treeToValue(requestJson.get("warehouseConfig").get("Warehouse"), Warehouse.class);
            List<Product> products = objectMapper.treeToValue(requestJson.get("warehouseConfig").get("products"), List.class);

            /*TODO 1. сделать генерацию продуктов (ориентируясь на первый элемент в списке products), просто насоздавать n копий
            2. сделать генерацию ячеек (вроде как уже есть)
            3. добавить эндпоинт по генерации случайных продуктов и ячеек (как в прошлых программах было)

             */

            // Передаем в сервис и получаем результат
            return distributionService.processDistribution(methods, warehouse, products);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка обработки JSON-запроса", e);
        }
    }
}
