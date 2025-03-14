package distribution_system.webApp.entities;

import distribution_system.webApp.enums.DistributionMethods;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Results implements Serializable {
    private DistributionMethods method;
    private double score;
    private long timeRequired;
    private Solution solution;

}
