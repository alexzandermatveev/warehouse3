package distribution_system.webApp.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import distribution_system.webApp.entities.Results;
import distribution_system.webApp.services.DistributionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DistributionController {

    private final DistributionService distributionService;


    @PostMapping("/distribute")
    public List<Results> getDistribution(@RequestBody JsonNode requestJson,
                                         @RequestParam(defaultValue = "false") boolean generateCells,
                                         @RequestParam(defaultValue = "1") long cellsAmount,
                                         @RequestParam(defaultValue = "false") boolean generateProducts,
                                         @RequestParam(defaultValue = "1") long productsAmount) {
        return distributionService.preDistributionMethod(requestJson, generateCells,
                cellsAmount, generateProducts, productsAmount);
    }
}
