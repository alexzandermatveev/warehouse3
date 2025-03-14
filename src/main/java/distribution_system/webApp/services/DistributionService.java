package distribution_system.webApp.services;

import distribution_system.webApp.entities.*;
import distribution_system.webApp.enums.DistributionMethods;
import distribution_system.webApp.genetic.GeneticSolution;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DistributionService {

    public List<Results> processDistribution(List<DistributionMethods> request, Warehouse warehouse, List<Product> products) {

        warehouse.generateCells();
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
                    Results results1 = Distribution.distributeWithELECTRE_TRI(Warehouse.copy(warehouse), products);
                    results1.setTimeRequired(System.currentTimeMillis() - startTime);
                    yield results1;
                }
                case TOPSIS -> {
                    Results results1 = Distribution.distributeWithTOPSIS(Warehouse.copy(warehouse), products);
                    results1.setTimeRequired(System.currentTimeMillis() - startTime);
                    yield results1;
                }
                case ELECTRE_TRI_TOPSIS -> {
                    Results results1 = Distribution.distributeWithELECTRE_TRI_and_TOPSIS(Warehouse.copy(warehouse), products);
                    results1.setTimeRequired(System.currentTimeMillis() - startTime);
                    yield results1;
                }
                case GA -> {
                    GeneticSolution solution = GeneticSolution.runGeneticAlgorithm(Warehouse.copy(warehouse), products, 5, 100);
                    yield new Results(DistributionMethods.GA, solution.getFitness(),
                            System.currentTimeMillis() - startTime, solution);
                }
            };
            resultsList.add(results);
        });
        return resultsList;
    }
}
